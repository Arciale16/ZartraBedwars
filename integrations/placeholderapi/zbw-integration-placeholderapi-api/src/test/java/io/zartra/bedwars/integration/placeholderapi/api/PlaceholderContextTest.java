package io.zartra.bedwars.integration.placeholderapi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlaceholderContextTest {

    @Test
    void nullFallbackDefaultsToDash() {
        final PlaceholderContext context = PlaceholderContext.of(
                "zbw",
                UUID.randomUUID(),
                "stats_wins",
                null,
                true
        );
        assertEquals("-", context.fallback());
    }

    @Test
    void rejectsNullNamespace() {
        assertThrows(NullPointerException.class, () -> PlaceholderContext.of(
                null,
                UUID.randomUUID(),
                "stats_wins",
                "-",
                true
        ));
    }
}
