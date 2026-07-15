package io.zartra.bedwars.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.lifecycle.Lifecycle;
import io.zartra.bedwars.api.recovery.Recovery;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.application.failure.ResiliencePolicies;
import io.zartra.bedwars.application.lifecycle.LifecycleCoordinator;
import io.zartra.bedwars.application.recovery.RecoveryCoordinator;
import io.zartra.bedwars.application.scheduler.BoundedTaskScheduler;
import io.zartra.bedwars.application.scheduler.StrictThreadGuard;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class M05ApplicationTest {
    private static final DefinitionId OWNER = DefinitionId.of("zartra", "owner");
    private static final TimeSource WALL =
            TimeSource.FixedTimeSource.at(Instant.parse("2026-01-01T00:00:00Z"));
    private static final MonotonicTimeSource ZERO = () -> 0L;

    @Test void schedulerCompletesWorkIsolatesFailuresAndNamesWorkers() throws Exception {
        final List<FailureReport> failures = Collections.synchronizedList(new ArrayList<FailureReport>());
        final BoundedTaskScheduler scheduler = new BoundedTaskScheduler(
                1, 4, "zbw-test", ZERO, WALL, failures::add);
        final SchedulerPort.Outcome<String> success = scheduler.submit(descriptor(true),
                context -> Thread.currentThread().getName()).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertTrue(success.isSuccess());
        assertTrue(success.value().get().startsWith("zbw-test-"));

        final SchedulerPort.Outcome<String> failed = scheduler.<String>submit(descriptor(false),
                context -> { throw new IllegalStateException("seed-secret-must-not-leak"); })
                .completion().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertFalse(failed.isSuccess());
        assertEquals(FailureKind.INTERNAL, failed.failure().get().kind());
        assertFalse(failed.failure().get().messageKey().contains("seed-secret"));
        assertEquals(1, failures.size());

        final SchedulerPort.Outcome<String> nullResult = scheduler.<String>submit(descriptor(false),
                context -> null).completion().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(FailureKind.INTERNAL, nullResult.failure().get().kind());
        final BoundedTaskScheduler.ShutdownReport report =
                scheduler.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertFalse(report.forced());
        assertTrue(report.terminated());
        assertEquals(1L, report.snapshot().completed());
        assertEquals(2L, report.snapshot().failed());
    }

    @Test void schedulerRejectsSaturationCancelsQueuedWorkAndExpiresDeadlines() throws Exception {
        final AtomicLong nanos = new AtomicLong();
        final BoundedTaskScheduler scheduler = new BoundedTaskScheduler(
                1, 1, "zbw-bound", nanos::get, WALL, ignored -> { });
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final SchedulerPort.TaskHandle<String> running = scheduler.submit(descriptor(true), context -> {
            started.countDown();
            release.await();
            return "running";
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        final SchedulerPort.TaskHandle<String> queued = scheduler.submit(descriptor(true),
                context -> "queued");
        final SchedulerPort.TaskHandle<String> rejected = scheduler.submit(descriptor(true),
                context -> "rejected");
        assertEquals(FailureKind.REJECTED, rejected.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).failure().get().kind());
        assertTrue(queued.cancel());
        assertFalse(queued.cancel());
        assertEquals(FailureKind.REJECTED, queued.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).failure().get().kind());
        release.countDown();
        assertTrue(running.completion().toCompletableFuture().get(2, TimeUnit.SECONDS).isSuccess());

        final CountDownLatch secondStarted = new CountDownLatch(1);
        final CountDownLatch secondRelease = new CountDownLatch(1);
        scheduler.submit(descriptor(true), context -> {
            secondStarted.countDown();
            secondRelease.await();
            return "block";
        });
        assertTrue(secondStarted.await(2, TimeUnit.SECONDS));
        final SchedulerPort.TaskHandle<String> expiring = scheduler.submit(
                descriptor(Duration.ofNanos(5), true), context -> "late");
        nanos.set(10L);
        secondRelease.countDown();
        assertEquals(FailureKind.TIMEOUT, expiring.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).failure().get().kind());
        scheduler.stopAdmission();
        assertEquals(FailureKind.REJECTED, scheduler.submit(descriptor(true), context -> "no")
                .completion().toCompletableFuture().get(2, TimeUnit.SECONDS)
                .failure().get().kind());
        final SchedulerPort.Snapshot snapshot = scheduler.snapshot();
        assertTrue(snapshot.rejected() >= 2);
        assertEquals(1L, snapshot.cancelled());
        scheduler.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test void forcedShutdownInterruptsRunningAndAccountsQueuedWork() throws Exception {
        final BoundedTaskScheduler scheduler = new BoundedTaskScheduler(
                1, 2, "zbw-stop", ZERO, WALL, ignored -> { throw new IllegalStateException("sink"); });
        final CountDownLatch started = new CountDownLatch(1);
        scheduler.submit(descriptor(true), context -> {
            started.countDown();
            Thread.sleep(10000L);
            return "late";
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        scheduler.submit(descriptor(true), context -> "queued");
        final BoundedTaskScheduler.ShutdownReport report =
                scheduler.shutdown(Duration.ofMillis(1), Duration.ofSeconds(2));
        assertTrue(report.forced());
        assertTrue(report.terminated());
        assertEquals(1, report.forceCancelled());
        assertEquals(2L, report.snapshot().cancelled());
        assertFalse(report.snapshot().accepting());
        assertThrows(IllegalArgumentException.class, () -> scheduler.shutdown(
                Duration.ZERO, Duration.ofSeconds(1)));
    }

    @Test void schedulerValidatesConstructionAndCoversRunningCancellationAndOverflow()
            throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new BoundedTaskScheduler(
                0, 1, "valid", ZERO, WALL, ignored -> { }));
        assertThrows(IllegalArgumentException.class, () -> new BoundedTaskScheduler(
                1, 0, "valid", ZERO, WALL, ignored -> { }));
        assertThrows(IllegalArgumentException.class, () -> new BoundedTaskScheduler(
                1, 1, null, ZERO, WALL, ignored -> { }));
        assertThrows(IllegalArgumentException.class, () -> new BoundedTaskScheduler(
                1, 1, "bad prefix", ZERO, WALL, ignored -> { }));

        final AtomicLong clock = new AtomicLong(1L);
        final BoundedTaskScheduler scheduler = new BoundedTaskScheduler(
                1, 2, "zbw-branches", clock::get, WALL, ignored -> { });
        final SchedulerPort.TaskHandle<String> overflow = scheduler.submit(
                descriptor(Duration.ofSeconds(Long.MAX_VALUE), true), context -> "ok");
        assertTrue(overflow.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).isSuccess());
        assertFalse(overflow.cancel());

        clock.set(0L);
        final SchedulerPort.TaskHandle<String> postDeadline = scheduler.submit(
                descriptor(Duration.ofNanos(5), true), context -> {
                    clock.set(10L);
                    return "late";
                });
        assertEquals(FailureKind.TIMEOUT, postDeadline.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).failure().get().kind());

        clock.set(0L);
        final CountDownLatch started = new CountDownLatch(1);
        final SchedulerPort.TaskHandle<String> running = scheduler.submit(descriptor(true), context -> {
            started.countDown();
            Thread.sleep(10000L);
            return "never";
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertTrue(running.cancel());
        assertEquals(FailureKind.REJECTED, running.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).failure().get().kind());
        scheduler.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> scheduler.shutdown(
                Duration.ofSeconds(1), Duration.ofSeconds(-1)));
    }

    @Test void threadGuardEnforcesBothDirections() {
        final AtomicLong owner = new AtomicLong(1L);
        final SchedulerPort.OwnerThreadDispatcher dispatcher = new SchedulerPort.OwnerThreadDispatcher() {
            @Override public boolean isOwnerThread(final DefinitionId ownerId) {
                return owner.get() == 1L;
            }
            @Override public SchedulerPort.TaskHandle<Void> dispatch(
                    final TaskDescriptor descriptor, final Runnable mutation) {
                throw new AssertionError("dispatch is not used by the guard");
            }
        };
        final StrictThreadGuard guard = new StrictThreadGuard(dispatcher);
        guard.requireOwnerThread(OWNER);
        assertThrows(SchedulerPort.ThreadAccessException.class,
                () -> guard.requireWorkerThread(OWNER));
        owner.set(0L);
        guard.requireWorkerThread(OWNER);
        assertThrows(SchedulerPort.ThreadAccessException.class,
                () -> guard.requireOwnerThread(OWNER));
        assertThrows(NullPointerException.class, () -> guard.requireOwnerThread(null));
    }

    @Test void retryAndCircuitPoliciesAreBounded() {
        final AtomicLong clock = new AtomicLong();
        final ResiliencePolicies.RetryPolicy policy = new ResiliencePolicies.RetryPolicy(
                3, Duration.ofMillis(10), Duration.ofMillis(25));
        assertEquals(3, policy.maximumAttempts());
        assertTrue(policy.permits(1, FailureKind.TIMEOUT, true));
        assertFalse(policy.permits(1, FailureKind.INTERNAL, true));
        assertFalse(policy.permits(1, FailureKind.TIMEOUT, false));
        assertFalse(policy.permits(3, FailureKind.TIMEOUT, true));
        assertFalse(policy.permits(0, FailureKind.UNAVAILABLE, true));
        assertTrue(policy.permits(2, FailureKind.CONFLICT, true));
        assertEquals(Duration.ofMillis(10), policy.delayAfter(1));
        assertEquals(Duration.ofMillis(20), policy.delayAfter(2));
        assertEquals(Duration.ofMillis(25), policy.delayAfter(4));
        assertThrows(IllegalArgumentException.class, () -> policy.delayAfter(0));
        assertThrows(IllegalArgumentException.class, () -> new ResiliencePolicies.RetryPolicy(
                17, Duration.ofMillis(1), Duration.ofMillis(2)));
        assertThrows(IllegalArgumentException.class, () -> new ResiliencePolicies.RetryPolicy(
                0, Duration.ofMillis(1), Duration.ofMillis(2)));
        assertThrows(IllegalArgumentException.class, () -> new ResiliencePolicies.RetryPolicy(
                2, Duration.ofMillis(3), Duration.ofMillis(2)));
        assertThrows(IllegalArgumentException.class, () -> new ResiliencePolicies.RetryPolicy(
                2, Duration.ZERO, Duration.ofMillis(2)));
        assertThrows(IllegalArgumentException.class, () -> new ResiliencePolicies.CircuitBreaker(
                0, Duration.ofMillis(1), clock::get));
        final ResiliencePolicies.CircuitBreaker breaker = new ResiliencePolicies.CircuitBreaker(
                2, Duration.ofNanos(10), clock::get);
        assertTrue(breaker.tryAcquire());
        breaker.onFailure();
        assertEquals(ResiliencePolicies.CircuitBreaker.State.CLOSED, breaker.state());
        breaker.onFailure();
        assertEquals(ResiliencePolicies.CircuitBreaker.State.OPEN, breaker.state());
        assertFalse(breaker.tryAcquire());
        clock.set(10L);
        assertTrue(breaker.tryAcquire());
        assertFalse(breaker.tryAcquire());
        breaker.onFailure();
        assertEquals(ResiliencePolicies.CircuitBreaker.State.OPEN, breaker.state());
        clock.set(20L);
        assertTrue(breaker.tryAcquire());
        breaker.onSuccess();
        assertEquals(ResiliencePolicies.CircuitBreaker.State.CLOSED, breaker.state());
        assertEquals(0, breaker.consecutiveFailures());
    }

    @Test void lifecycleOrdersTransitionsAndRollsBackFailures() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final RecordingComponent first = new RecordingComponent("a", order, false, ZERO);
        final RecordingComponent second = new RecordingComponent("b", order, false, ZERO);
        final BoundedTaskScheduler worker = scheduler("lifecycle-ok");
        final LifecycleCoordinator coordinator = new LifecycleCoordinator(
                Arrays.<Lifecycle.Component>asList(first, second), worker, ZERO, WALL);
        final Lifecycle.Report started = coordinator.start(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Lifecycle.State.RUNNING, started.state());
        final Lifecycle.Report stopped = coordinator.shutdown(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Lifecycle.State.STOPPED, stopped.state());
        assertEquals(Arrays.asList("start:a", "start:b", "drain:b", "stop:b",
                "drain:a", "stop:a"), order);
        worker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final List<String> rollback = Collections.synchronizedList(new ArrayList<String>());
        final BoundedTaskScheduler failingWorker = scheduler("lifecycle-fail");
        final LifecycleCoordinator failing = new LifecycleCoordinator(
                Arrays.<Lifecycle.Component>asList(
                        new RecordingComponent("a", rollback, false, ZERO),
                        new RecordingComponent("b", rollback, true, ZERO)),
                failingWorker, ZERO, WALL);
        final Lifecycle.Report failed = failing.start(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Lifecycle.State.FAILED, failed.state());
        assertEquals(Arrays.asList("start:a", "start:b", "drain:a", "stop:a"), rollback);
        assertThrows(IllegalStateException.class, () -> failing.start(descriptor(true)));
        failingWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test void lifecycleDeadlineForcesStartedComponentsAndRejectsDuplicates() throws Exception {
        final AtomicLong coordinatorClock = new AtomicLong();
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final BoundedTaskScheduler worker = scheduler("lifecycle-deadline");
        final RecordingComponent first = new RecordingComponent(
                "a", order, false, () -> coordinatorClock.addAndGet(20L));
        final LifecycleCoordinator coordinator = new LifecycleCoordinator(
                Arrays.<Lifecycle.Component>asList(first,
                        new RecordingComponent("b", order, false, ZERO)),
                worker, coordinatorClock::get, WALL);
        final Lifecycle.Report report = coordinator.start(
                descriptor(Duration.ofNanos(10), true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Lifecycle.State.FAILED, report.state());
        assertTrue(report.forced());
        assertTrue(order.contains("force:a"));
        worker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final RecordingComponent duplicate = new RecordingComponent(
                "same", new ArrayList<String>(), false, ZERO);
        assertThrows(IllegalArgumentException.class, () -> new LifecycleCoordinator(
                Arrays.<Lifecycle.Component>asList(duplicate, duplicate),
                scheduler("unused"), ZERO, WALL));
    }

    @Test void lifecycleIsolatesInvalidStatesDrainFailuresForceFailuresAndRejection()
            throws Exception {
        final BoundedTaskScheduler rejectedWorker = scheduler("lifecycle-rejected");
        rejectedWorker.stopAdmission();
        final LifecycleCoordinator rejected = new LifecycleCoordinator(
                Collections.<Lifecycle.Component>emptyList(), rejectedWorker, ZERO, WALL);
        assertFalse(rejected.start(descriptor(true)).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).isSuccess());
        assertEquals(Lifecycle.State.FAILED, rejected.state());
        rejectedWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final BoundedTaskScheduler invalidWorker = scheduler("lifecycle-invalid");
        final LifecycleCoordinator invalid = new LifecycleCoordinator(
                Collections.<Lifecycle.Component>singletonList(new PolicyComponent(
                        "invalid", Result.success(Lifecycle.State.STOPPED),
                        Result.success(Lifecycle.State.DRAINING),
                        Result.success(Lifecycle.State.STOPPED),
                        Result.success(Lifecycle.State.FORCED), () -> { })),
                invalidWorker, ZERO, WALL);
        assertEquals(Lifecycle.State.FAILED, invalid.start(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get().state());
        assertThrows(IllegalStateException.class, () -> invalid.shutdown(descriptor(true)));
        invalidWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final ApiError retryable = ApiError.of(DefinitionId.of("zartra", "dependency"),
                "lifecycle.dependency", ApiError.RetryDisposition.RETRYABLE);
        final ApiError permanent = ApiError.of(DefinitionId.of("zartra", "stop"),
                "lifecycle.stop", ApiError.RetryDisposition.PERMANENT);
        final BoundedTaskScheduler failureWorker = scheduler("lifecycle-policies");
        final LifecycleCoordinator failures = new LifecycleCoordinator(
                Arrays.<Lifecycle.Component>asList(
                        new PolicyComponent("a", Result.success(Lifecycle.State.RUNNING),
                                Result.success(Lifecycle.State.DRAINING),
                                Result.success(Lifecycle.State.STOPPED),
                                Result.success(Lifecycle.State.FORCED), () -> { }),
                        new PolicyComponent("b", Result.success(Lifecycle.State.RUNNING),
                                Result.success(Lifecycle.State.DRAINING),
                                Result.success(Lifecycle.State.RUNNING),
                                Result.success(Lifecycle.State.RUNNING), () -> { }),
                        new PolicyComponent("c", Result.success(Lifecycle.State.RUNNING),
                                Result.failure(retryable), Result.success(Lifecycle.State.STOPPED),
                                Result.failure(permanent), () -> { })),
                failureWorker, ZERO, WALL);
        assertEquals(Lifecycle.State.RUNNING, failures.start(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get().state());
        final Lifecycle.Report stopped = failures.shutdown(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Lifecycle.State.FORCED, stopped.state());
        assertTrue(stopped.forced());
        assertTrue(stopped.failures().size() >= 3);
        failureWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final List<String> rollbackOrder = new ArrayList<String>();
        final BoundedTaskScheduler rollbackWorker = scheduler("lifecycle-rollback-branches");
        final LifecycleCoordinator rollback = new LifecycleCoordinator(
                Arrays.<Lifecycle.Component>asList(
                        new PolicyComponent("a", Result.success(Lifecycle.State.RUNNING),
                                Result.failure(permanent), Result.success(Lifecycle.State.STOPPED),
                                Result.success(Lifecycle.State.FORCED),
                                () -> rollbackOrder.add("a")),
                        new PolicyComponent("b", Result.success(Lifecycle.State.RUNNING),
                                Result.success(Lifecycle.State.DRAINING),
                                Result.success(Lifecycle.State.STOPPED),
                                Result.success(Lifecycle.State.FORCED),
                                () -> rollbackOrder.add("b")),
                        new PolicyComponent("c", Result.failure(retryable),
                                Result.success(Lifecycle.State.DRAINING),
                                Result.success(Lifecycle.State.STOPPED),
                                Result.success(Lifecycle.State.FORCED), () -> { })),
                rollbackWorker, ZERO, WALL);
        final Lifecycle.Report rollbackReport = rollback.start(descriptor(true)).completion()
                .toCompletableFuture().get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Lifecycle.State.FAILED, rollbackReport.state());
        assertEquals(Collections.singletonList("a"), rollbackOrder);
        rollbackWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test void recoveryCoordinatorPersistsOrderedIdempotentCompletion() throws Exception {
        final BoundedTaskScheduler worker = scheduler("recovery-ok");
        final InMemoryMarkerStore store = new InMemoryMarkerStore(-1L);
        final RecoveryCoordinator coordinator = new RecoveryCoordinator(Arrays.asList(
                step("quiesce", Recovery.State.QUIESCED),
                step("route", Recovery.State.PLAYERS_ROUTED),
                step("reconcile", Recovery.State.RECONCILED)),
                store, worker, WALL, ignored -> { });
        final Recovery.Report report = coordinator.recover(
                descriptor(true), recoveryMarker()).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).value().get();
        assertTrue(report.recovered());
        assertEquals(Recovery.State.RECOVERED, report.marker().state());
        assertEquals(4L, report.marker().revision());
        assertTrue(report.failures().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> coordinator.recover(
                descriptor(true), report.marker()));
        worker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final Recovery.Step duplicate = step("same", Recovery.State.QUIESCED);
        assertThrows(IllegalArgumentException.class, () -> new RecoveryCoordinator(
                Arrays.asList(duplicate, duplicate), store, scheduler("recovery-duplicate"),
                WALL, ignored -> { }));
    }

    @Test void recoveryCoordinatorPersistsManualMarkersForFailuresAndInvalidPlans()
            throws Exception {
        final List<FailureReport> failures = new ArrayList<FailureReport>();
        final BoundedTaskScheduler stepWorker = scheduler("recovery-step-failure");
        final Recovery.Step failedStep = new Recovery.Step() {
            @Override public DefinitionId id() { return DefinitionId.of("zartra", "failed-step"); }
            @Override public Result<Recovery.State> execute(final Recovery.Marker current) {
                return Result.failure(ApiError.of(DefinitionId.of("zartra", "provider-failed"),
                        "recovery.provider_failed", ApiError.RetryDisposition.RETRYABLE));
            }
        };
        final RecoveryCoordinator failed = new RecoveryCoordinator(
                Collections.singletonList(failedStep), new InMemoryMarkerStore(-1L),
                stepWorker, WALL, failures::add);
        final Recovery.Report failureReport = failed.recover(
                descriptor(true), recoveryMarker()).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Recovery.State.MANUAL_REQUIRED, failureReport.marker().state());
        assertEquals(1, failures.size());
        stepWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final BoundedTaskScheduler invalidWorker = scheduler("recovery-invalid");
        final RecoveryCoordinator invalid = new RecoveryCoordinator(
                Collections.singletonList(step("same-state", Recovery.State.DETECTED)),
                new InMemoryMarkerStore(-1L), invalidWorker, WALL,
                ignored -> { throw new IllegalStateException("sink failure"); });
        final Recovery.Report invalidReport = invalid.recover(
                descriptor(true), recoveryMarker()).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Recovery.State.MANUAL_REQUIRED, invalidReport.marker().state());
        invalidWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final BoundedTaskScheduler incompleteWorker = scheduler("recovery-incomplete");
        final RecoveryCoordinator incomplete = new RecoveryCoordinator(
                Collections.<Recovery.Step>emptyList(), new InMemoryMarkerStore(-1L),
                incompleteWorker, WALL, ignored -> { });
        assertEquals(Recovery.State.MANUAL_REQUIRED, incomplete.recover(
                descriptor(true), recoveryMarker()).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).value().get().marker().state());
        incompleteWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test void recoveryCoordinatorSurfacesMarkerConflictsWithoutSilentCompletion()
            throws Exception {
        final BoundedTaskScheduler initialWorker = scheduler("recovery-initial-store");
        final RecoveryCoordinator initialFailure = new RecoveryCoordinator(
                Collections.<Recovery.Step>emptyList(), new InMemoryMarkerStore(0L),
                initialWorker, WALL, ignored -> { });
        final Recovery.Report initial = initialFailure.recover(
                descriptor(true), recoveryMarker()).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).value().get();
        assertEquals(Recovery.State.DETECTED, initial.marker().state());
        assertEquals(1, initial.failures().size());
        initialWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));

        final BoundedTaskScheduler finalWorker = scheduler("recovery-final-store");
        final RecoveryCoordinator finalFailure = new RecoveryCoordinator(Arrays.asList(
                step("quiesce", Recovery.State.QUIESCED),
                step("route", Recovery.State.PLAYERS_ROUTED),
                step("reconcile", Recovery.State.RECONCILED)),
                new InMemoryMarkerStore(4L), finalWorker, WALL, ignored -> { });
        final Recovery.Report finalReport = finalFailure.recover(
                descriptor(true), recoveryMarker()).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).value().get();
        assertFalse(finalReport.recovered());
        assertEquals(Recovery.State.RECONCILED, finalReport.marker().state());
        assertEquals(2, finalReport.failures().size());
        finalWorker.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private static BoundedTaskScheduler scheduler(final String name) {
        return new BoundedTaskScheduler(1, 4, name, ZERO, WALL, ignored -> { });
    }

    private static TaskDescriptor descriptor(final boolean idempotent) {
        return descriptor(Duration.ofSeconds(5), idempotent);
    }

    private static TaskDescriptor descriptor(final Duration timeout, final boolean idempotent) {
        return TaskDescriptor.of(TaskId.random(), DefinitionId.of("zartra", "operation"), OWNER,
                CorrelationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                timeout, idempotent);
    }

    private static Recovery.Marker recoveryMarker() {
        return new Recovery.Marker(MatchId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000020")),
                IdempotencyKey.of("zartra", "match/completion"),
                Recovery.State.DETECTED, 0L, WALL.now());
    }

    private static Recovery.Step step(final String id, final Recovery.State state) {
        return new Recovery.Step() {
            @Override public DefinitionId id() { return DefinitionId.of("zartra", id); }
            @Override public Result<Recovery.State> execute(final Recovery.Marker current) {
                return Result.success(state);
            }
        };
    }

    private static final class RecordingComponent implements Lifecycle.Component {
        private final DefinitionId id;
        private final List<String> order;
        private final boolean failStart;
        private final MonotonicTimeSource onStart;
        private RecordingComponent(final String id, final List<String> order,
                                   final boolean failStart, final MonotonicTimeSource onStart) {
            this.id = DefinitionId.of("zartra", id);
            this.order = order;
            this.failStart = failStart;
            this.onStart = onStart;
        }
        @Override public DefinitionId id() { return id; }
        @Override public Result<Lifecycle.State> start(final Duration remainingBudget) {
            order.add("start:" + id.path());
            onStart.readNanos();
            return failStart ? Result.failure(ApiError.of(
                    DefinitionId.of("zartra", "start-failed"), "lifecycle.start_failed",
                    ApiError.RetryDisposition.PERMANENT)) : Result.success(Lifecycle.State.RUNNING);
        }
        @Override public Result<Lifecycle.State> drain(final Duration remainingBudget) {
            order.add("drain:" + id.path());
            return Result.success(Lifecycle.State.DRAINING);
        }
        @Override public Result<Lifecycle.State> stop(final Duration remainingBudget) {
            order.add("stop:" + id.path());
            return Result.success(Lifecycle.State.STOPPED);
        }
        @Override public Result<Lifecycle.State> forceStop() {
            order.add("force:" + id.path());
            return Result.success(Lifecycle.State.FORCED);
        }
    }

    private static final class PolicyComponent implements Lifecycle.Component {
        private final DefinitionId id;
        private final Result<Lifecycle.State> start;
        private final Result<Lifecycle.State> drain;
        private final Result<Lifecycle.State> stop;
        private final Result<Lifecycle.State> force;
        private final Runnable onForce;
        private PolicyComponent(final String id, final Result<Lifecycle.State> start,
                                final Result<Lifecycle.State> drain,
                                final Result<Lifecycle.State> stop,
                                final Result<Lifecycle.State> force,
                                final Runnable onForce) {
            this.id = DefinitionId.of("zartra", id);
            this.start = start;
            this.drain = drain;
            this.stop = stop;
            this.force = force;
            this.onForce = onForce;
        }
        @Override public DefinitionId id() { return id; }
        @Override public Result<Lifecycle.State> start(final Duration remainingBudget) { return start; }
        @Override public Result<Lifecycle.State> drain(final Duration remainingBudget) { return drain; }
        @Override public Result<Lifecycle.State> stop(final Duration remainingBudget) { return stop; }
        @Override public Result<Lifecycle.State> forceStop() {
            onForce.run();
            return force;
        }
    }

    private static final class InMemoryMarkerStore implements Recovery.MarkerStore {
        private final long failRevision;
        private Recovery.Marker current;
        private InMemoryMarkerStore(final long failRevision) { this.failRevision = failRevision; }
        @Override public synchronized Result<Recovery.Marker> save(
                final Recovery.Marker marker, final long expectedPreviousRevision) {
            if (marker.revision() == failRevision) { return conflict(); }
            if (current == null) {
                if (expectedPreviousRevision != -1L) { return conflict(); }
            } else if (current.revision() != expectedPreviousRevision) {
                return conflict();
            }
            current = marker;
            return Result.success(marker);
        }
        private static Result<Recovery.Marker> conflict() {
            return Result.failure(ApiError.of(DefinitionId.of("zartra", "marker-conflict"),
                    "recovery.marker_conflict", ApiError.RetryDisposition.RETRYABLE));
        }
    }
}
