package io.zartra.bedwars.game;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.GameRules;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

final class GameFixtures {
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    static final DefinitionId RED = DefinitionId.of("zartra", "team/red");
    static final DefinitionId BLUE = DefinitionId.of("zartra", "team/blue");
    static final DefinitionId IRON = DefinitionId.of("zartra", "item/iron");
    private GameFixtures() { }
    static PlayerId player(final int value) { return PlayerId.of(new UUID(0L, value)); }
    static MatchId match(final int value) { return MatchId.of(new UUID(1L, value)); }
    static ArenaId arena(final int value) { return ArenaId.of(new UUID(2L, value)); }
    static GameRules rules() {
        return new GameRules(2, 4, 2, Duration.ofSeconds(30),
                Duration.ofSeconds(5), Duration.ofSeconds(1));
    }
    static MatchStateMachine machine() {
        return new MatchStateMachine(match(1), arena(1), rules(), Arrays.asList(
                TeamSnapshot.empty(BLUE, 2), TeamSnapshot.empty(RED, 2)), NOW);
    }
    static PlayerStateSnapshot state(final PlayerId player) {
        return new PlayerStateSnapshot(player, PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(DefinitionId.of("zartra", "world/lobby"),
                        1.0D, 64.0D, 2.0D, 0.0F, 0.0F),
                PlayerStateSnapshot.Mode.SURVIVAL, true);
    }
    static PlayerStateSnapshot.Item item(final int amount) {
        return new PlayerStateSnapshot.Item(IRON, amount, Collections.<String, String>emptyMap());
    }
}
