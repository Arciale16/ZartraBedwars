package io.zartra.bedwars.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.application.scheduler.BoundedTaskScheduler;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import io.zartra.bedwars.world.api.WorldProvider;
import io.zartra.bedwars.world.orchestration.WorldOrchestrator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class WorldFoundationTest {
    private BoundedTaskScheduler scheduler;

    @AfterEach void closeScheduler() throws InterruptedException {
        if (scheduler != null) {
            scheduler.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
        }
    }

    @Test void identifiersAndOperationsAreTypedAndValidated() {
        final WorldKey key = WorldKey.of("arena_01");
        assertEquals(key, WorldKey.of("arena_01"));
        assertEquals(key.hashCode(), WorldKey.of("arena_01").hashCode());
        assertEquals("arena_01", key.value());
        assertEquals("arena_01", key.toString());
        assertEquals(0, key.compareTo(WorldKey.of("arena_01")));
        assertTrue(key.compareTo(WorldKey.of("arena_02")) < 0);
        assertSame(key, key);
        assertNotEquals(key, WorldKey.of("arena_02"));
        assertNotEquals(key, "arena_01");
        assertNotEquals(key, null);
        assertThrows(NullPointerException.class, () -> key.compareTo(null));
        assertThrows(IllegalArgumentException.class, () -> WorldKey.of(null));
        assertThrows(IllegalArgumentException.class, () -> WorldKey.of("../escape"));
        final WorldOperation clone = operation(WorldOperation.Type.CLONE, WorldKey.of("target"),
                WorldKey.of("template"), Duration.ofSeconds(2));
        assertNotNull(clone.operationId());
        assertNotNull(clone.correlationId());
        assertEquals(WorldOperation.Type.CLONE, clone.type());
        assertEquals(WorldKey.of("target"), clone.target());
        assertEquals(Duration.ofSeconds(2), clone.timeout());
        assertTrue(clone.source().isPresent());
        final WorldOperation reset = WorldOperation.create(WorldOperation.Type.RESET,
                WorldKey.of("reset"), WorldKey.of("template"), Duration.ofSeconds(1));
        assertEquals(WorldOperation.Type.RESET, reset.type());
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.CLONE, WorldKey.of("target"), null, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.RESET, WorldKey.of("target"), null, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), WorldKey.of("source"),
                Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.UNLOAD, WorldKey.of("target"), WorldKey.of("source"),
                Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.CLONE, WorldKey.of("target"), WorldKey.of("target"),
                Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), null, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), null, Duration.ofSeconds(-1)));
        assertThrows(NullPointerException.class, () -> WorldOperation.of(null,
                CorrelationId.random(), WorldOperation.Type.LOAD, WorldKey.of("target"), null,
                Duration.ofSeconds(1)));
    }

    @Test void providerPlansAndResourceSnapshotsAreBounded() {
        final RecordingProvider provider = new RecordingProvider(false, false, false);
        final WorldOperation operation = operation(WorldOperation.Type.LOAD, WorldKey.of("target"),
                null, Duration.ofSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> new WorldProvider.Plan(
                operation, Collections.<WorldProvider.Step>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new WorldProvider.Plan(operation,
                Collections.<WorldProvider.Step>nCopies(17, provider.new RecordingStep(
                        "worker", WorldProvider.Affinity.WORKER))));
        assertThrows(IllegalArgumentException.class, () -> new WorldProvider.Plan(operation,
                Arrays.<WorldProvider.Step>asList(provider.new RecordingStep(
                        "worker", WorldProvider.Affinity.WORKER), null)));
        final WorldProvider.Plan plan = provider.plan(operation);
        assertSame(operation, plan.operation());
        assertEquals(2, plan.steps().size());
        assertThrows(UnsupportedOperationException.class,
                () -> plan.steps().add(plan.steps().get(0)));
        final WorldProvider.StepResult success = WorldProvider.StepResult.success();
        assertTrue(success.isSuccess());
        assertEquals(DefinitionId.of("zartra", "world/step_success"), success.reason());
        final DefinitionId failureReason = DefinitionId.of("zartra", "world/failed");
        final WorldProvider.StepResult failure = WorldProvider.StepResult.failure(failureReason);
        assertFalse(failure.isSuccess());
        assertEquals(failureReason, failure.reason());
        assertThrows(NullPointerException.class, () -> WorldProvider.StepResult.failure(null));
        final WorldProvider.ResourceSnapshot empty = provider.snapshot(WorldKey.of("target"));
        assertFalse(empty.loaded());
        assertEquals(0, empty.loadedChunks());
        assertEquals(0, empty.entities());
        assertEquals(0, empty.retainedHandles());
        assertTrue(empty.leakFreeAfterUnload());
        final WorldProvider.ResourceSnapshot loaded = new WorldProvider.ResourceSnapshot(true, 1, 2, 0);
        assertTrue(loaded.loaded());
        assertEquals(1, loaded.loadedChunks());
        assertEquals(2, loaded.entities());
        assertFalse(loaded.leakFreeAfterUnload());
        assertFalse(new WorldProvider.ResourceSnapshot(false, 0, 0, 1).leakFreeAfterUnload());
        assertFalse(new WorldProvider.ResourceSnapshot(false, 1, 0, 0).leakFreeAfterUnload());
        assertFalse(new WorldProvider.ResourceSnapshot(false, 0, 1, 0).leakFreeAfterUnload());
        assertThrows(IllegalArgumentException.class,
                () -> new WorldProvider.ResourceSnapshot(false, -1, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldProvider.ResourceSnapshot(false, 0, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldProvider.ResourceSnapshot(false, 0, 0, -1));
    }

    @Test void workerAndOwnerStepsRunOnSeparatedThreads() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, false, false);
        final WorldOperationResult result = started(provider).submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), null,
                Duration.ofSeconds(2))).completion().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(WorldOperationResult.Status.SUCCEEDED, result.status());
        assertNotNull(result.operation());
        assertNotNull(result.reason());
        assertEquals(2, result.completedSteps().size());
        assertTrue(result.rollbackComplete());
        assertTrue(result.resources().leakFreeAfterUnload());
        assertThrows(UnsupportedOperationException.class,
                () -> result.completedSteps().add(DefinitionId.of("zartra", "world/extra")));
        assertTrue(provider.workerThread.startsWith("world-test-"));
        assertEquals(Thread.currentThread().getName(), provider.ownerThread);
        assertNotEquals(provider.ownerThread, provider.workerThread);
    }

    @Test void providerFailureRollsBackCompletedSteps() throws Exception {
        final RecordingProvider provider = new RecordingProvider(true, false, false);
        final WorldOperationResult result = started(provider).submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), null,
                Duration.ofSeconds(2))).completion().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(WorldOperationResult.Status.FAILED, result.status());
        assertTrue(result.rollbackComplete());
        assertEquals(Collections.singletonList("worker"), provider.rollbacks);

        final RecordingProvider rollbackFailure = new RecordingProvider(true, false, false, true);
        final WorldOperationResult incomplete = started(rollbackFailure).submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("rollback_failure"), null,
                Duration.ofSeconds(2))).completion().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(WorldOperationResult.Status.FAILED, incomplete.status());
        assertFalse(incomplete.rollbackComplete());
    }

    @Test void cancellationIsTypedAndDrained() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, true, false);
        final WorldOrchestrator orchestrator = started(provider);
        final WorldOrchestrator.OperationHandle handle = orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), null,
                Duration.ofSeconds(5)));
        assertTrue(provider.entered.await(1, TimeUnit.SECONDS));
        assertTrue(orchestrator.drain(Duration.ofSeconds(1)).isSuccess());
        assertEquals(WorldOperationResult.Status.CANCELLED,
                handle.completion().toCompletableFuture().get(2, TimeUnit.SECONDS).status());
        assertFalse(handle.cancel());
        assertEquals(0, orchestrator.accounting().active());
    }

    @Test void schedulerDeadlineBecomesTypedWorldTimeout() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, false, true);
        final WorldOperationResult result = started(provider).submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("target"), null,
                Duration.ofMillis(20))).completion().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(WorldOperationResult.Status.TIMED_OUT, result.status());
    }

    @Test void admissionIsHardBoundedAndLifecycleDrainStopsAdmission() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, true, false);
        final WorldOrchestrator orchestrator = started(provider);
        final WorldOrchestrator.OperationHandle accepted = orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("one"), null, Duration.ofSeconds(5)));
        assertTrue(provider.entered.await(1, TimeUnit.SECONDS));
        final WorldOperationResult rejected = orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("two"), null, Duration.ofSeconds(5)))
                .completion().toCompletableFuture().get(1, TimeUnit.SECONDS);
        assertEquals(WorldOperationResult.Status.REJECTED, rejected.status());
        assertFalse(orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("three"), null, Duration.ofSeconds(5)))
                .cancel());
        assertEquals(io.zartra.bedwars.api.health.Health.Status.DEGRADED,
                orchestrator.snapshot().status());
        accepted.cancel();
        assertTrue(orchestrator.drain(Duration.ofSeconds(1)).isSuccess());
        assertFalse(orchestrator.accounting().accepting());
        assertEquals(io.zartra.bedwars.api.health.Health.Status.HEALTHY,
                orchestrator.snapshot().status());
        assertEquals(1, orchestrator.accounting().maximumInFlight());
        assertEquals(1, orchestrator.accounting().accepted());
        assertEquals(1, orchestrator.accounting().cancelled());
        assertEquals(2, orchestrator.accounting().rejected());
        assertEquals(0, orchestrator.accounting().completed());
        assertEquals(0, orchestrator.accounting().failed());
        assertNotNull(orchestrator.id());
        assertTrue(orchestrator.stop(Duration.ofSeconds(1)).isSuccess());
        assertTrue(orchestrator.forceStop().isSuccess());
    }

    @Test void invalidLifecycleAndPlanFailuresAreRejectedDeterministically() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, false, false);
        scheduler = new BoundedTaskScheduler(1, 4, "world-test",
                MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), ignored -> { });
        assertThrows(IllegalArgumentException.class, () -> new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), provider, TimeSource.FixedTimeSource.at(Instant.EPOCH), 0));
        assertThrows(IllegalArgumentException.class, () -> new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), provider, TimeSource.FixedTimeSource.at(Instant.EPOCH), 65));
        final WorldOrchestrator stopped = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), provider, TimeSource.FixedTimeSource.at(Instant.EPOCH), 1);
        assertEquals(WorldOperationResult.Status.REJECTED, stopped.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("stopped"), null, Duration.ofSeconds(1)))
                .completion().toCompletableFuture().get(1, TimeUnit.SECONDS).status());
        assertThrows(IllegalArgumentException.class, () -> stopped.start(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> stopped.drain(Duration.ofSeconds(-1)));
        assertThrows(NullPointerException.class, () -> stopped.stop(null));

        final WorldProvider throwing = new WorldProvider() {
            @Override public ProviderId id() { return ProviderId.of("zartra", "throwing_world"); }
            @Override public Plan plan(final WorldOperation operation) {
                throw new IllegalStateException("deterministic planner rejection");
            }
            @Override public ResourceSnapshot snapshot(final WorldKey world) {
                return new ResourceSnapshot(false, 0, 0, 0);
            }
        };
        final WorldOrchestrator started = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), throwing, TimeSource.FixedTimeSource.at(Instant.EPOCH), 1);
        started.start(Duration.ofSeconds(1));
        assertEquals(WorldOperationResult.Status.REJECTED, started.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("rejected"), null, Duration.ofSeconds(1)))
                .completion().toCompletableFuture().get(1, TimeUnit.SECONDS).status());
        assertEquals(1, started.accounting().rejected());
        assertTrue(started.drain(Duration.ofNanos(Long.MAX_VALUE)).isSuccess());
    }

    @Test void duplicateTargetIsRejectedWhileDifferentTargetCapacityRemains() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, true, false);
        scheduler = new BoundedTaskScheduler(2, 4, "world-test",
                MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), ignored -> { });
        final WorldOrchestrator orchestrator = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), provider, TimeSource.FixedTimeSource.at(Instant.EPOCH), 2);
        orchestrator.start(Duration.ofSeconds(1));
        final WorldOrchestrator.OperationHandle first = orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("same"), null, Duration.ofSeconds(5)));
        assertTrue(provider.entered.await(1, TimeUnit.SECONDS));
        assertEquals(WorldOperationResult.Status.REJECTED, orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("same"), null, Duration.ofSeconds(5)))
                .completion().toCompletableFuture().get(1, TimeUnit.SECONDS).status());
        assertTrue(first.cancel());
        assertEquals(WorldOperationResult.Status.CANCELLED,
                first.completion().toCompletableFuture().get(1, TimeUnit.SECONDS).status());
    }

    @Test void completedOperationCanChainTheSameWorldWithoutAdmissionRace() throws Exception {
        final RecordingProvider provider = new RecordingProvider(false, false, false);
        final WorldOrchestrator orchestrator = started(provider);
        final WorldOperationResult result = orchestrator.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("chained"), null, Duration.ofSeconds(2)))
                .completion().thenCompose(ignored -> orchestrator.submit(operation(
                        WorldOperation.Type.UNLOAD, WorldKey.of("chained"), null,
                        Duration.ofSeconds(2))).completion())
                .toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(WorldOperationResult.Status.SUCCEEDED, result.status());
        assertEquals(2, orchestrator.accounting().completed());
        assertEquals(0, orchestrator.accounting().rejected());
    }

    @Test void terminalResultRejectsNullCompletedStepAndExposesAllFields() {
        final WorldOperation operation = operation(WorldOperation.Type.UNLOAD, WorldKey.of("arena"),
                null, Duration.ofSeconds(1));
        final WorldProvider.ResourceSnapshot resources = new WorldProvider.ResourceSnapshot(false, 0, 0, 0);
        final DefinitionId reason = DefinitionId.of("zartra", "world/manual");
        final WorldOperationResult result = new WorldOperationResult(operation,
                WorldOperationResult.Status.FAILED, reason,
                Collections.singletonList(DefinitionId.of("zartra", "world/step")), false, resources);
        assertSame(operation, result.operation());
        assertEquals(WorldOperationResult.Status.FAILED, result.status());
        assertEquals(reason, result.reason());
        assertFalse(result.rollbackComplete());
        assertSame(resources, result.resources());
        assertThrows(IllegalArgumentException.class, () -> new WorldOperationResult(operation,
                WorldOperationResult.Status.FAILED, reason,
                Arrays.asList(DefinitionId.of("zartra", "world/step"), null), false, resources));
        assertThrows(NullPointerException.class, () -> new WorldOperationResult(operation,
                WorldOperationResult.Status.FAILED, reason, null, false, resources));
    }

    @Test void dispatcherFailureForceStopAndHugeDeadlineRemainTyped() throws Exception {
        final RecordingProvider ownerFailure = new RecordingProvider(false, false, false);
        scheduler = new BoundedTaskScheduler(1, 4, "world-test",
                MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), ignored -> { });
        final WorldOrchestrator failed = new WorldOrchestrator(scheduler,
                new FailingOwnerDispatcher(), ownerFailure,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), 1);
        failed.start(Duration.ofSeconds(1));
        assertEquals(WorldOperationResult.Status.FAILED, failed.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("owner_failure"), null,
                Duration.ofSeconds(2))).completion().toCompletableFuture().get(2, TimeUnit.SECONDS).status());

        final RecordingProvider blocking = new RecordingProvider(false, true, false);
        final WorldOrchestrator forced = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), blocking,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), 1);
        forced.start(Duration.ofSeconds(1));
        final WorldOrchestrator.OperationHandle handle = forced.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("forced"), null, Duration.ofSeconds(5)));
        assertTrue(blocking.entered.await(1, TimeUnit.SECONDS));
        assertTrue(forced.forceStop().isSuccess());
        assertEquals(WorldOperationResult.Status.CANCELLED,
                handle.completion().toCompletableFuture().get(1, TimeUnit.SECONDS).status());

        final RecordingProvider immediate = new RecordingProvider(false, false, false);
        final WorldOrchestrator huge = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), immediate,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), 1);
        huge.start(Duration.ofSeconds(1));
        assertEquals(WorldOperationResult.Status.SUCCEEDED, huge.submit(operation(
                WorldOperation.Type.LOAD, WorldKey.of("huge"), null,
                Duration.ofSeconds(Long.MAX_VALUE))).completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).status());
    }

    private WorldOrchestrator started(final RecordingProvider provider) {
        scheduler = new BoundedTaskScheduler(1, 4, "world-test",
                MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), ignored -> { });
        final WorldOrchestrator orchestrator = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), provider,
                TimeSource.FixedTimeSource.at(Instant.EPOCH), 1);
        assertTrue(orchestrator.start(Duration.ofSeconds(1)).isSuccess());
        return orchestrator;
    }

    private static WorldOperation operation(final WorldOperation.Type type, final WorldKey target,
                                            final WorldKey source, final Duration timeout) {
        return WorldOperation.of(TaskId.random(), CorrelationId.random(), type, target, source, timeout);
    }

    private static final class ImmediateOwnerDispatcher implements SchedulerPort.OwnerThreadDispatcher {
        @Override public boolean isOwnerThread(final DefinitionId ownerId) { return true; }
        @Override public SchedulerPort.TaskHandle<Void> dispatch(
                final TaskDescriptor descriptor, final Runnable mutation) {
            mutation.run();
            return new CompletedVoidHandle(descriptor.taskId());
        }
    }

    private static final class CompletedVoidHandle implements SchedulerPort.TaskHandle<Void> {
        private final TaskId taskId;
        private final CompletionStage<SchedulerPort.Outcome<Void>> completion =
                CompletableFuture.completedFuture(SchedulerPort.Outcome.successVoid());
        CompletedVoidHandle(final TaskId taskId) { this.taskId = taskId; }
        @Override public TaskId taskId() { return taskId; }
        @Override public CompletionStage<SchedulerPort.Outcome<Void>> completion() { return completion; }
        @Override public boolean cancel() { return false; }
    }

    private static final class FailingOwnerDispatcher implements SchedulerPort.OwnerThreadDispatcher {
        @Override public boolean isOwnerThread(final DefinitionId ownerId) { return false; }
        @Override public SchedulerPort.TaskHandle<Void> dispatch(
                final TaskDescriptor descriptor, final Runnable mutation) {
            final FailureReport report = FailureReport.of(
                    DefinitionId.of("zartra", "world/owner_dispatch_failure"), FailureKind.INTERNAL,
                    descriptor.correlationId(), "world.owner_dispatch.failure", false, Instant.EPOCH);
            return new SchedulerPort.TaskHandle<Void>() {
                @Override public TaskId taskId() { return descriptor.taskId(); }
                @Override public CompletionStage<SchedulerPort.Outcome<Void>> completion() {
                    return CompletableFuture.completedFuture(SchedulerPort.Outcome.failure(report));
                }
                @Override public boolean cancel() { return false; }
            };
        }
    }

    private static final class RecordingProvider implements WorldProvider {
        private final boolean failOwner;
        private final boolean block;
        private final boolean waitForTimeout;
        private final boolean failRollback;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final List<String> rollbacks = Collections.synchronizedList(new ArrayList<String>());
        private volatile String workerThread;
        private volatile String ownerThread;
        RecordingProvider(final boolean failOwner, final boolean block, final boolean waitForTimeout) {
            this(failOwner, block, waitForTimeout, false);
        }
        RecordingProvider(final boolean failOwner, final boolean block, final boolean waitForTimeout,
                          final boolean failRollback) {
            this.failOwner = failOwner;
            this.block = block;
            this.waitForTimeout = waitForTimeout;
            this.failRollback = failRollback;
        }
        @Override public ProviderId id() { return ProviderId.of("zartra", "fake_world"); }
        @Override public Plan plan(final WorldOperation operation) {
            return new Plan(operation, Arrays.<Step>asList(
                    new RecordingStep("worker", Affinity.WORKER),
                    new RecordingStep("owner", Affinity.OWNER)));
        }
        @Override public ResourceSnapshot snapshot(final WorldKey world) {
            return new ResourceSnapshot(false, 0, 0, 0);
        }
        private final class RecordingStep implements Step {
            private final String name;
            private final Affinity affinity;
            RecordingStep(final String name, final Affinity affinity) {
                this.name = name;
                this.affinity = affinity;
            }
            @Override public DefinitionId id() { return DefinitionId.of("zartra", "world/" + name); }
            @Override public Affinity affinity() { return affinity; }
            @Override public StepResult execute(final CancellationToken token) {
                if (affinity == Affinity.WORKER) {
                    workerThread = Thread.currentThread().getName();
                    entered.countDown();
                    while (block && !token.isCancellationRequested()) { pause(); }
                    while (waitForTimeout && !token.isCancellationRequested()) { pause(); }
                    return StepResult.success();
                }
                ownerThread = Thread.currentThread().getName();
                return failOwner ? StepResult.failure(
                        DefinitionId.of("zartra", "world/provider_failure")) : StepResult.success();
            }
            @Override public StepResult rollback(final CancellationToken token) {
                rollbacks.add(name);
                return failRollback ? StepResult.failure(
                        DefinitionId.of("zartra", "world/rollback_failure")) : StepResult.success();
            }
            private void pause() {
                try { Thread.sleep(2L); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }
        }
    }
}
