package io.zartra.bedwars.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class PaperFoundationSettingsTest {
    @Test void directSettingsAreBoundedAndImmutable() {
        final PaperFoundationSettings settings = PaperFoundationSettings.of(
                2, 32, 8, 64, Duration.ofSeconds(120));
        assertEquals(2, settings.workers());
        assertEquals(32, settings.queueCapacity());
        assertEquals(8, settings.maximumInFlightWorlds());
        assertEquals(64, settings.maximumTrackedWorlds());
        assertEquals(Duration.ofSeconds(120), settings.operationTimeout());
        assertThrows(IllegalArgumentException.class, () -> PaperFoundationSettings.of(
                0, 32, 8, 64, Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> PaperFoundationSettings.of(
                2, 32, 9, 8, Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> PaperFoundationSettings.of(
                9, 32, 8, 64, Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> PaperFoundationSettings.of(
                2, 32, 8, 64, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> PaperFoundationSettings.of(
                2, 32, 8, 64, Duration.ofSeconds(301)));
        assertThrows(IllegalArgumentException.class, () -> PaperFoundationSettings.of(
                2, 32, 8, 64, Duration.ofMillis(1500)));
    }

    @Test void primaryBootstrapDescriptorDeclaresPresentationAndReplayPermissions() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            final String descriptor = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(descriptor.contains("ZartraBedWarsPlugin"));
            assertTrue(descriptor.contains("api-version: '1.21'"));
            assertTrue(descriptor.contains("load: STARTUP"));
            assertTrue(descriptor.contains("commands:"));
            assertTrue(descriptor.contains("  zbw:"));
            assertTrue(descriptor.contains("  deposit:"));
            assertTrue(descriptor.contains("permissions:"));
            assertTrue(descriptor.contains("  zartrabedwars.replay.view:"));
            assertTrue(descriptor.contains("  zartrabedwars.replay.staff:"));
            assertTrue(descriptor.contains("    default: true"));
            assertTrue(descriptor.contains("    default: op"));
        }
    }
}
