package io.zartra.bedwars.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlaceholderApiLifecycleTest {

    @Test
    void resolvesFamilyPlaceholders() {
        final PlaceholderApiProviders providers = new PlaceholderApiProviders(
                playerId -> Optional.of("10"),
                playerId -> Optional.of("Prestige I"),
                playerId -> Optional.of("250"),
                playerId -> Optional.of("1200"),
                playerId -> Optional.of("99"),
                playerId -> Optional.of("1"),
                playerId -> Optional.of("15"),
                playerId -> Optional.of("3"),
                playerId -> Optional.of("4"),
                playerId -> Optional.of("daily:1"),
                playerId -> Optional.of("ach:2"),
                playerId -> Optional.of("chal:3"),
                playerId -> Optional.of("bp:4"),
                playerId -> Optional.of("blade"),
                playerId -> Optional.of("public"),
                playerId -> Optional.of("summer"),
                playerId -> Optional.of("12")
        );
        final PlaceholderApiLifecycle lifecycle = new PlaceholderApiLifecycle(providers);
        final UUID playerId = UUID.randomUUID();
        final String level = lifecycle.resolve("progression_level", playerId, "-");
        final String wins = lifecycle.resolve("stats_wins", playerId, "-");
        final String challenge = lifecycle.resolve("content_challenges", playerId, "-");
        final String campaign = lifecycle.resolve("cosmetics_active_campaign", playerId, "-");

        assertEquals("10", level);
        assertEquals("99", wins);
        assertEquals("chal:3", challenge);
        assertEquals("summer", campaign);
        assertTrue(lifecycle.registry().size() >= 16);
    }
}
