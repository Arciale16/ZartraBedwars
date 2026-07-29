package io.zartra.bedwars.observability.doctor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.scheduler.TaskContext;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** ZBW-INT-002/003/006/007/008/009 provider certification state coverage. */
final class ProviderCompatibilityCheckTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test
    void reportsPresentAbsentIncompatibleAndDuplicateDeterministically() {
        final Provider present = provider("vault", OptionalProviderLifecycle.Probe.AVAILABLE);
        final Provider absent = provider("luckperms", OptionalProviderLifecycle.Probe.ABSENT);
        final Provider incompatible =
                provider("citizens", OptionalProviderLifecycle.Probe.INCOMPATIBLE);
        present.start().toCompletableFuture().join();
        absent.start().toCompletableFuture().join();
        incompatible.start().toCompletableFuture().join();

        final ProviderCompatibilityCheck check = new ProviderCompatibilityCheck(
                Arrays.asList(id("vault"), id("luckperms"), id("citizens"), id("grim")),
                Arrays.asList(incompatible, absent, present),
                Collections.singleton(id("grim")));

        assertEquals(ProviderCompatibilityCheck.State.PRESENT, check.state(id("vault")));
        assertEquals(ProviderCompatibilityCheck.State.ABSENT, check.state(id("luckperms")));
        assertEquals(ProviderCompatibilityCheck.State.INCOMPATIBLE, check.state(id("citizens")));
        assertEquals(ProviderCompatibilityCheck.State.DUPLICATE, check.state(id("grim")));
        final PluginDoctor.Result result = check.inspect(context());
        assertEquals(Health.Status.UNAVAILABLE, result.status());
        assertEquals(4, result.evidence().size());
        assertEquals("incompatible", result.evidence().get(0).value());
        assertEquals("duplicate", result.evidence().get(1).value());
        assertEquals("absent", result.evidence().get(2).value());
        assertEquals("present", result.evidence().get(3).value());
    }

    @Test
    void canonicalInventoryContainsEveryCertifiedProvider() {
        final List<ProviderId> ids = ProviderCompatibilityCheck.m21ProviderIds();
        assertEquals(9, ids.size());
        assertEquals(id("alessiodp-parties"), ids.get(0));
        assertEquals(id("znpcsplus"), ids.get(8));
    }

    private static TaskContext context() {
        return new TaskContext(TaskDescriptor.of(
                TaskId.random(), DefinitionId.of("zartra", "provider-certification"),
                DefinitionId.of("zartra", "plugin-doctor"), CorrelationId.random(),
                Duration.ofSeconds(1), true), new CancellationToken() {
                    @Override public boolean isCancellationRequested() { return false; }
                });
    }

    private static ProviderId id(final String path) {
        return ProviderId.of("zartra", path);
    }

    private static Provider provider(
            final String path, final OptionalProviderLifecycle.Probe probe) {
        final OptionalProviderLifecycle lifecycle = new OptionalProviderLifecycle(
                id(path), SemanticVersion.parse("1.0.0"), CapabilitySet.empty(),
                TimeSource.FixedTimeSource.at(NOW), "provider.test", probe);
        return new Provider() {
            @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
            @Override public Health health() { return lifecycle.health(); }
            @Override public CompletionStage<Result<LifecycleState>> start() {
                return lifecycle.start();
            }
            @Override public CompletionStage<Result<LifecycleState>> drain(
                    final Duration deadline) {
                return lifecycle.drain(deadline);
            }
            @Override public CompletionStage<Result<LifecycleState>> stop() {
                return lifecycle.stop();
            }
        };
    }
}
