package io.zartra.bedwars.integration.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** ZBW-INT-005 optional-provider lifecycle, fallback and isolation certification. */
final class WorldProviderAdaptersTest {
    private static final Instant NOW = Instant.parse("2026-07-29T12:00:00Z");
    private static final TimeSource TIME = TimeSource.FixedTimeSource.at(NOW);
    private static final WorldKey WORLD = WorldKey.of("arena-one");
    private static final WorldProvider.ResourceSnapshot GATEWAY_SNAPSHOT =
            new WorldProvider.ResourceSnapshot(true, 2, 3, 1);

    @Test void allProvidersDeclareExactIdentityVersionAndCapabilities() {
        final Gateway gateway = new Gateway();
        final WorldProvider fallback = new FallbackProvider();
        final List<Provider> providers = Arrays.<Provider>asList(
                new WorldEditWorldProvider(gateway,
                        OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME),
                new FaweWorldProvider(gateway,
                        OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME),
                new WorldGuardWorldProvider(gateway,
                        OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME),
                new SlimeWorldManagerWorldProvider(gateway,
                        OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME),
                new MultiverseWorldProvider(gateway,
                        OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME));
        assertEquals(Arrays.asList(
                        ProviderId.of("zartra", "worldedit"),
                        ProviderId.of("zartra", "fawe"),
                        ProviderId.of("zartra", "worldguard"),
                        ProviderId.of("zartra", "slimeworldmanager"),
                        ProviderId.of("zartra", "multiverse-core")),
                Arrays.asList(
                        providers.get(0).descriptor().id(),
                        providers.get(1).descriptor().id(),
                        providers.get(2).descriptor().id(),
                        providers.get(3).descriptor().id(),
                        providers.get(4).descriptor().id()));
        assertEquals("7.3.16", providers.get(0).descriptor().version().toString());
        assertEquals("2.15.1", providers.get(1).descriptor().version().toString());
        assertEquals("7.0.17", providers.get(2).descriptor().version().toString());
        assertEquals("5.1.0", providers.get(3).descriptor().version().toString());
        assertEquals("5.3.3", providers.get(4).descriptor().version().toString());
        for (Provider provider : providers) {
            assertTrue(provider.descriptor().capabilities().size() > 0);
        }
    }

    @Test void presentProviderChecksCompatibilityAsynchronouslyAndDelegates() {
        final Gateway gateway = new Gateway();
        gateway.compatibility = new CompletableFuture<Boolean>();
        final FallbackProvider fallback = new FallbackProvider();
        final WorldEditWorldProvider provider = new WorldEditWorldProvider(
                gateway, OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME);
        final CompletionStage<io.zartra.bedwars.api.result.Result<
                Provider.LifecycleState>> start = provider.start();
        assertFalse(start.toCompletableFuture().isDone());
        assertEquals(Provider.HealthStatus.DEGRADED, provider.health().status());
        gateway.compatibility.complete(Boolean.TRUE);
        assertEquals(Provider.LifecycleState.RUNNING,
                start.toCompletableFuture().join().requireValue());
        assertEquals(Provider.HealthStatus.HEALTHY, provider.health().status());
        assertEquals("gateway", provider.plan(operation()).steps().get(0).id().path());
        assertSame(GATEWAY_SNAPSHOT, provider.snapshot(WORLD));
        assertEquals(0, fallback.planCalls.get());
        assertEquals(Provider.LifecycleState.RUNNING,
                provider.start().toCompletableFuture().join().requireValue());
        assertEquals(Provider.LifecycleState.STOPPED,
                provider.drain(Duration.ZERO).toCompletableFuture().join().requireValue());
        assertEquals("fallback", provider.plan(operation()).steps().get(0).id().path());
        assertEquals(Provider.LifecycleState.STOPPED,
                provider.stop().toCompletableFuture().join().requireValue());
        assertEquals(Provider.LifecycleState.RUNNING,
                provider.start().toCompletableFuture().join().requireValue());
    }

    @Test void absenceAndIncompatibleVersionsFailClosedToNativeFallback() {
        final Gateway gateway = new Gateway();
        final FallbackProvider fallback = new FallbackProvider();
        final WorldGuardWorldProvider absent = new WorldGuardWorldProvider(
                gateway, OptionalProviderLifecycle.Probe.ABSENT, fallback, TIME);
        assertEquals(Provider.LifecycleState.STOPPED,
                absent.start().toCompletableFuture().join().requireValue());
        assertEquals(Provider.HealthStatus.DISABLED, absent.health().status());
        assertEquals("fallback", absent.plan(operation()).steps().get(0).id().path());
        assertSame(fallback.snapshot, absent.snapshot(WORLD));
        assertEquals(Provider.LifecycleState.STOPPED,
                absent.drain(Duration.ZERO).toCompletableFuture().join().requireValue());

        final SlimeWorldManagerWorldProvider incompatible =
                new SlimeWorldManagerWorldProvider(
                        gateway, OptionalProviderLifecycle.Probe.INCOMPATIBLE,
                        fallback, TIME);
        assertEquals(Provider.LifecycleState.FAILED,
                incompatible.start().toCompletableFuture().join().requireValue());
        assertEquals(Provider.HealthStatus.UNAVAILABLE,
                incompatible.health().status());
        assertEquals(0, gateway.compatibilityCalls.get());
    }

    @Test void failedAsyncProbeAndGatewayDefectsUseFallback() {
        final Gateway rejected = new Gateway();
        rejected.compatibility =
                CompletableFuture.completedFuture(Boolean.FALSE);
        final FallbackProvider fallback = new FallbackProvider();
        final FaweWorldProvider incompatible = new FaweWorldProvider(
                rejected, OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME);
        assertEquals(Provider.LifecycleState.FAILED,
                incompatible.start().toCompletableFuture().join().requireValue());
        assertEquals(Provider.HealthStatus.UNAVAILABLE,
                incompatible.health().status());

        final Gateway unknown = new Gateway();
        unknown.compatibility = CompletableFuture.completedFuture(null);
        final WorldGuardWorldProvider unknownVersion = new WorldGuardWorldProvider(
                unknown, OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME);
        assertEquals(Provider.LifecycleState.FAILED,
                unknownVersion.start().toCompletableFuture().join().requireValue());

        final Gateway defective = new Gateway();
        defective.throwCompatibility = true;
        final MultiverseWorldProvider failed = new MultiverseWorldProvider(
                defective, OptionalProviderLifecycle.Probe.AVAILABLE, fallback, TIME);
        assertEquals(Provider.LifecycleState.FAILED,
                failed.start().toCompletableFuture().join().requireValue());

        final Gateway runtimeFailure = new Gateway();
        final WorldEditWorldProvider provider = new WorldEditWorldProvider(
                runtimeFailure, OptionalProviderLifecycle.Probe.AVAILABLE,
                fallback, TIME);
        provider.start().toCompletableFuture().join();
        runtimeFailure.invalidPlan = true;
        assertEquals("fallback", provider.plan(operation()).steps().get(0).id().path());
        runtimeFailure.invalidPlan = false;
        runtimeFailure.throwSnapshot = true;
        assertSame(fallback.snapshot, provider.snapshot(WORLD));
        assertThrows(IllegalArgumentException.class,
                () -> provider.drain(Duration.ofSeconds(-1)));
    }

    @Test void duplicateIdentityIsStableAndVendorClassesAreNotLinked() {
        final Gateway gateway = new Gateway();
        final WorldProvider fallback = new FallbackProvider();
        final WorldEditWorldProvider first = new WorldEditWorldProvider(
                gateway, OptionalProviderLifecycle.Probe.ABSENT, fallback, TIME);
        final WorldEditWorldProvider duplicate = new WorldEditWorldProvider(
                gateway, OptionalProviderLifecycle.Probe.ABSENT, fallback, TIME);
        assertEquals(first.descriptor().id(), duplicate.descriptor().id());
        assertMissing("com.sk89q.worldedit.WorldEdit");
        assertMissing("com.fastasyncworldedit.core.Fawe");
        assertMissing("com.sk89q.worldguard.WorldGuard");
        assertMissing("com.grinderwolf.swm.api.SlimePlugin");
        assertMissing("org.mvplugins.multiverse.core.MultiverseCore");
    }

    private static void assertMissing(final String type) {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                type, false, WorldProviderAdaptersTest.class.getClassLoader()));
    }

    private static WorldOperation operation() {
        return WorldOperation.of(TaskId.of(new UUID(1L, 2L)),
                CorrelationId.of(new UUID(3L, 4L)),
                WorldOperation.Type.LOAD, WORLD, null, Duration.ofSeconds(2));
    }

    private static final class Gateway implements WorldProviderGateway {
        private CompletableFuture<Boolean> compatibility =
                CompletableFuture.completedFuture(Boolean.TRUE);
        private final AtomicInteger compatibilityCalls = new AtomicInteger();
        private boolean throwCompatibility;
        private boolean invalidPlan;
        private boolean throwSnapshot;

        @Override public CompletionStage<Boolean> compatible() {
            compatibilityCalls.incrementAndGet();
            if (throwCompatibility) {
                throw new IllegalStateException("gateway unavailable");
            }
            return compatibility;
        }

        @Override public WorldProvider.Plan plan(final WorldOperation operation) {
            final WorldOperation selected = invalidPlan
                    ? WorldOperation.of(TaskId.of(new UUID(5L, 6L)),
                            CorrelationId.of(new UUID(7L, 8L)),
                            operation.type(), operation.target(), null,
                            Duration.ofSeconds(2))
                    : operation;
            return new WorldProvider.Plan(selected,
                    Arrays.<WorldProvider.Step>asList(new MarkerStep("gateway")));
        }

        @Override public WorldProvider.ResourceSnapshot snapshot(
                final WorldKey world) {
            if (throwSnapshot) {
                throw new IllegalStateException("snapshot unavailable");
            }
            return GATEWAY_SNAPSHOT;
        }
    }

    private static final class FallbackProvider implements WorldProvider {
        private final AtomicInteger planCalls = new AtomicInteger();
        private final ResourceSnapshot snapshot =
                new ResourceSnapshot(false, 0, 0, 0);
        @Override public ProviderId id() {
            return ProviderId.of("zartra", "paper_native_world");
        }
        @Override public Plan plan(final WorldOperation operation) {
            planCalls.incrementAndGet();
            return new Plan(operation,
                    Arrays.<Step>asList(new MarkerStep("fallback")));
        }
        @Override public ResourceSnapshot snapshot(final WorldKey world) {
            return snapshot;
        }
    }

    private static final class MarkerStep implements WorldProvider.Step {
        private final DefinitionId id;
        private MarkerStep(final String id) {
            this.id = DefinitionId.of("zartra", id);
        }
        @Override public DefinitionId id() { return id; }
        @Override public WorldProvider.Affinity affinity() {
            return WorldProvider.Affinity.WORKER;
        }
        @Override public WorldProvider.StepResult execute(
                final CancellationToken cancellation) {
            return WorldProvider.StepResult.success();
        }
        @Override public WorldProvider.StepResult rollback(
                final CancellationToken cancellation) {
            return WorldProvider.StepResult.success();
        }
    }
}
