package io.zartra.bedwars.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlaceholderIntegrationEdgeCaseTest {

    @Test
    void missingPlayerReturnsFallback() {
        final PlaceholderApiLifecycle lifecycle = new PlaceholderApiLifecycle(PlaceholderApiProviders.fallback());
        final String value = lifecycle.resolve("stats_wins", null, "n/a");
        assertEquals("n/a", value);
    }

    @Test
    void unknownPlaceholderReturnsFallback() {
        final PlaceholderApiLifecycle lifecycle = new PlaceholderApiLifecycle(PlaceholderApiProviders.fallback());
        final UUID playerId = UUID.randomUUID();
        final String value = lifecycle.resolve("does_not_exist", playerId, "n/a");
        assertEquals("n/a", value);
    }

    @Test
    void resolverFailureFallsBack() {
        final PlaceholderApiLifecycle lifecycle = new PlaceholderApiLifecycle(PlaceholderApiProviders.fallback());
        lifecycle.registry().register(
                io.zartra.bedwars.integration.placeholderapi.api.PlaceholderId.of("boom"),
                context -> {
                    throw new IllegalStateException("boom");
                }
        );
        final String value = lifecycle.resolve("boom", UUID.randomUUID(), "-");
        assertEquals("-", value);
    }
}
