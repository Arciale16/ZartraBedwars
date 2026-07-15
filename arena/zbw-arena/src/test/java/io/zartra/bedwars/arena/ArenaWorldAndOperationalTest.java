package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.application.scheduler.BoundedTaskScheduler;
import io.zartra.bedwars.arena.application.ArenaFailures;
import io.zartra.bedwars.arena.application.ArenaOperationalView;
import io.zartra.bedwars.arena.application.ArenaPolicy;
import io.zartra.bedwars.arena.application.ArenaWorldLifecycleService;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import io.zartra.bedwars.world.api.WorldProvider;
import io.zartra.bedwars.world.orchestration.WorldOrchestrator;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArenaWorldAndOperationalTest {
    private BoundedTaskScheduler scheduler;
    private CountDownLatch entered;
    private CountDownLatch release;

    @BeforeEach void setUp() {
        entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
        scheduler = new BoundedTaskScheduler(1, 4, "arena-world-test",
                MonotonicTimeSource.SystemMonotonicTimeSource.INSTANCE,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW), report -> { });
    }

    @AfterEach void tearDown() throws InterruptedException {
        release.countDown();
        scheduler.shutdown(Duration.ofSeconds(2), Duration.ofSeconds(1));
    }

    @Test void resetUsesWorldOrchestratorAndRejectsConcurrentSameWorldOperation() throws Exception {
        final MemoryPorts ports = new MemoryPorts();
        ports.save(new ArenaRepository.SaveRequest(ArenaTestFixture.complete(), 0, true));
        final WorldOrchestrator orchestrator = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), new BlockingProvider(entered, release),
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW), 2);
        assertTrue(orchestrator.start(Duration.ofSeconds(1)).isSuccess());
        final ArenaWorldLifecycleService service = new ArenaWorldLifecycleService(ports,
                orchestrator, ArenaPolicy.of(10, 2, Duration.ofSeconds(10),
                Duration.ofSeconds(5)), ports, ports, ports,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
        final ArenaWorldLifecycleService.Handle first = service.reset(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), CorrelationId.random()).requireValue();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        final ArenaWorldLifecycleService.Handle second = service.recover(
                ArenaTestFixture.ARENA_ID, ArenaTestFixture.actor(),
                CorrelationId.random()).requireValue();
        assertEquals(ArenaFailures.WORLD, second.completion().toCompletableFuture().get(2,
                TimeUnit.SECONDS).error().get());
        release.countDown();
        final Result<WorldOperationResult> completed = first.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS);
        assertTrue(completed.isSuccess());
        assertEquals(WorldOperation.Type.RESET, completed.requireValue().operation().type());
    }

    @Test void cloneCancellationAndAuthorizationAreTyped() throws Exception {
        final MemoryPorts ports = new MemoryPorts();
        ports.save(new ArenaRepository.SaveRequest(ArenaTestFixture.complete(), 0, true));
        final WorldOrchestrator orchestrator = new WorldOrchestrator(scheduler,
                new ImmediateOwnerDispatcher(), new BlockingProvider(entered, release),
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW), 2);
        orchestrator.start(Duration.ofSeconds(1));
        final ArenaWorldLifecycleService service = new ArenaWorldLifecycleService(ports,
                orchestrator, ArenaPolicy.of(10, 2, Duration.ofSeconds(10),
                Duration.ofSeconds(5)), ports, ports, ports,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
        final ArenaWorldLifecycleService.Handle clone = service.duplicateWorld(
                ArenaTestFixture.ARENA_ID, WorldKey.of("cloned_world"),
                ArenaTestFixture.actor(), CorrelationId.random()).requireValue();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        assertTrue(clone.cancel());
        assertFalse(clone.cancel());
        assertEquals(ArenaFailures.WORLD, clone.completion().toCompletableFuture()
                .get(2, TimeUnit.SECONDS).error().get());
        ports.allow = false;
        assertEquals(ArenaFailures.FORBIDDEN, service.reset(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), CorrelationId.random()).error().get());
    }

    @Test void operationalProjectionIsBoundedPublicAndNonBlocking() {
        final ArenaOperationalView view = new ArenaOperationalView(
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
        view.observeInventory(4, 0);
        view.observeActiveSetups(2);
        assertEquals(io.zartra.bedwars.api.health.Health.Status.HEALTHY,
                view.snapshot().status());
        view.observeInventory(4, 1);
        assertEquals(io.zartra.bedwars.api.health.Health.Status.DEGRADED,
                view.snapshot().status());
        assertEquals(4, view.fields().size());
        assertFalse(view.isInvalid(new ArenaValidation.DefaultValidator().validate(
                ArenaTestFixture.complete())));
        final io.zartra.bedwars.world.api.WorldOperation operation =
                io.zartra.bedwars.world.api.WorldOperation.create(WorldOperation.Type.RESET,
                        WorldKey.of("operational"), WorldKey.of("operational_template"),
                        Duration.ofSeconds(1));
        view.observeWorld(new WorldOperationResult(operation,
                WorldOperationResult.Status.SUCCEEDED, ArenaTestFixture.id("world/succeeded"),
                Collections.<DefinitionId>emptyList(), true,
                new WorldProvider.ResourceSnapshot(false, 0, 0, 0)));
        view.observeWorld(new WorldOperationResult(operation,
                WorldOperationResult.Status.FAILED, ArenaTestFixture.id("world/failure"),
                Collections.<DefinitionId>emptyList(), true,
                new WorldProvider.ResourceSnapshot(false, 0, 0, 0)));
        assertTrue(view.isInvalid(new ArenaValidation.DefaultValidator().validate(
                new io.zartra.bedwars.arena.model.ArenaBundle(
                        ArenaTestFixture.complete().arena().toBuilder()
                                .teams(Collections.emptyList()).build(),
                        ArenaTestFixture.complete().map()))));
        assertThrowsForOperationalBounds(view);
    }

    private static void assertThrowsForOperationalBounds(final ArenaOperationalView view) {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> view.observeInventory(1, 2));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> view.observeInventory(-1, 0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> view.observeInventory(1, -1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> view.observeActiveSetups(-1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> view.observeActiveSetups(257));
    }

    private static final class BlockingProvider implements WorldProvider {
        private final CountDownLatch entered;
        private final CountDownLatch release;
        private BlockingProvider(final CountDownLatch entered, final CountDownLatch release) {
            this.entered = entered;
            this.release = release;
        }
        @Override public ProviderId id() { return ProviderId.of("zartra", "test_world"); }
        @Override public Plan plan(final WorldOperation operation) {
            return new Plan(operation, Collections.<Step>singletonList(new Step() {
                @Override public DefinitionId id() { return ArenaTestFixture.id("world/test_step"); }
                @Override public Affinity affinity() { return Affinity.WORKER; }
                @Override public StepResult execute(final CancellationToken cancellation) {
                    entered.countDown();
                    try { release.await(2, TimeUnit.SECONDS); }
                    catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
                    return cancellation.isCancellationRequested()
                            ? StepResult.failure(ArenaTestFixture.id("world/cancelled"))
                            : StepResult.success();
                }
                @Override public StepResult rollback(final CancellationToken cancellation) {
                    return StepResult.success();
                }
            }));
        }
        @Override public ResourceSnapshot snapshot(final WorldKey world) {
            return new ResourceSnapshot(false, 0, 0, 0);
        }
    }

    private static final class ImmediateOwnerDispatcher implements SchedulerPort.OwnerThreadDispatcher {
        @Override public boolean isOwnerThread(final DefinitionId ownerId) { return true; }
        @Override public SchedulerPort.TaskHandle<Void> dispatch(
                final io.zartra.bedwars.api.scheduler.TaskDescriptor descriptor,
                final Runnable mutation) {
            mutation.run();
            return new CompletedVoidHandle(descriptor.taskId());
        }
    }

    private static final class CompletedVoidHandle implements SchedulerPort.TaskHandle<Void> {
        private final io.zartra.bedwars.api.identity.TaskId id;
        private final CompletableFuture<SchedulerPort.Outcome<Void>> completion;
        private CompletedVoidHandle(final io.zartra.bedwars.api.identity.TaskId id) {
            this.id = id;
            completion = CompletableFuture.completedFuture(SchedulerPort.Outcome.successVoid());
        }
        @Override public io.zartra.bedwars.api.identity.TaskId taskId() { return id; }
        @Override public java.util.concurrent.CompletionStage<SchedulerPort.Outcome<Void>> completion() { return completion; }
        @Override public boolean cancel() { return false; }
    }
}
