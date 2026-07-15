package io.zartra.bedwars.compat.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityOutcome;
import io.zartra.bedwars.compat.api.SemanticKey;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class Paper121CompatibilityAdapterTest {
    @Test void exactPrimaryRuntimeClaimIsLocked() {
        final Paper121CompatibilityAdapter adapter = adapter();
        assertEquals("Paper", adapter.runtimeClaim().platform());
        assertEquals("1.21.1", adapter.runtimeClaim().minecraftVersion());
        assertEquals("133", adapter.runtimeClaim().build());
        assertEquals(Paper121CompatibilityAdapter.SERVER_SHA256,
                adapter.runtimeClaim().sha256());
    }

    @Test void everyPrimarySemanticMappingResolvesSupported() {
        final Paper121CompatibilityAdapter adapter = adapter();
        assertEquals(PrimarySemanticMappings.required().size(), adapter.mappings().mappings().size());
        PrimarySemanticMappings.required().forEach(key ->
                assertEquals(CompatibilityOutcome.State.SUPPORTED, adapter.resolve(key).state()));
    }

    @Test void unknownCapabilityUsesExplicitUnsupportedPath() {
        final CompatibilityOutcome outcome = adapter().resolve(SemanticKey.of(
                SemanticKey.Kind.PARTICLE, DefinitionId.of("zartra", "effect/unknown")));
        assertEquals(CompatibilityOutcome.State.UNSUPPORTED, outcome.state());
        assertFalse(outcome.gameplayPreserved());
        assertThrows(NullPointerException.class, () -> adapter().resolve(null));
    }

    @Test void lifecycleAndProviderCapabilitiesAreDeterministic() {
        final Paper121CompatibilityAdapter adapter = adapter();
        assertEquals(Provider.HealthStatus.DISABLED, adapter.health().status());
        assertTrue(adapter.start().toCompletableFuture().join().isSuccess());
        assertEquals(Provider.HealthStatus.HEALTHY, adapter.health().status());
        assertEquals(10, adapter.descriptor().capabilities().values().size());
        assertTrue(adapter.drain(Duration.ofSeconds(1)).toCompletableFuture().join().isSuccess());
        assertTrue(adapter.stop().toCompletableFuture().join().isSuccess());
        assertThrows(IllegalArgumentException.class, () -> adapter.drain(Duration.ZERO));
    }

    private static Paper121CompatibilityAdapter adapter() {
        return new Paper121CompatibilityAdapter(TimeSource.FixedTimeSource.at(Instant.EPOCH));
    }
}
