package io.zartra.bedwars.integration.placeholderapi.api;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaceholderRegistryTest {

    @Test
    void resolvesKnownPlaceholder() {
        final PlaceholderRegistry registry = new PlaceholderRegistry();
        registry.register(PlaceholderId.of("hello"), context -> PlaceholderResult.found("world"));
        final PlaceholderContext context = PlaceholderContext.of(
                "zbw",
                UUID.randomUUID(),
                "hello",
                "fallback",
                false
        );
        final String value = registry.resolve(context).value();
        assertEquals("world", value);
    }

    @Test
    void fallbackIfMissingPlaceholder() {
        final PlaceholderRegistry registry = new PlaceholderRegistry();
        final PlaceholderContext context = PlaceholderContext.of(
                "zbw",
                UUID.randomUUID(),
                "missing",
                "fallback",
                false
        );
        final PlaceholderResult result = registry.resolve(context);
        assertFalse(result.found());
        assertTrue(result.fallback());
        assertEquals("fallback", result.value());
    }
}
