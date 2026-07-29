package io.zartra.bedwars.integration.decentholograms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.hologram.HologramProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** DecentHolograms presentation delegation test. */
final class DecentHologramsAdapterTest {
    @Test void delegatesBoundedPresentationDefinition() {
        HologramProvider.Definition definition = new HologramProvider.Definition(
                DefinitionId.of("zartra", "generator"),
                Collections.singletonList("Tier I"), Duration.ofMillis(250), 2);
        DecentHologramsAdapter adapter = new DecentHologramsAdapter(new Gateway(),
                OptionalProviderLifecycle.Probe.AVAILABLE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        adapter.start().toCompletableFuture().join();
        assertEquals(definition, adapter.upsert(definition)
                .toCompletableFuture().join().requireValue());
    }

    private static final class Gateway implements DecentHologramsAdapter.Gateway {
        @Override public CompletableFuture<HologramProvider.Definition> upsert(
                final HologramProvider.Definition value) {
            return CompletableFuture.completedFuture(value);
        }
        @Override public CompletableFuture<Boolean> remove(final DefinitionId id) {
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletableFuture<Boolean> render(
                final DefinitionId id, final PlayerId viewerId) {
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletableFuture<List<HologramProvider.Definition>> exportDefinitions() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
}
