package io.zartra.bedwars.integration.placeholderapi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PlaceholderIdTest {

    @Test
    void normalizesIdentifier() {
        assertEquals("example", PlaceholderId.of("  ExAmPlE  ").value());
    }

    @Test
    void rejectsBlankIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> PlaceholderId.of("   "));
    }

    @Test
    void rejectsWhitespaceIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> PlaceholderId.of("bad value"));
    }
}
