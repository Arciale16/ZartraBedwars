package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.GameRules;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.game.model.PlayerSession;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelValidationTest {
    @Test void stateItemsInventoriesAndLocationsValidateAndCompare() {
        final Map<String, String> metadata = Collections.singletonMap("quality", "original");
        final PlayerStateSnapshot.Item item = new PlayerStateSnapshot.Item(GameFixtures.IRON, 2, metadata);
        assertEquals(item, item.withAmount(2));
        assertEquals(item.hashCode(), item.withAmount(2).hashCode());
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Item(GameFixtures.IRON, 0, metadata));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Item(GameFixtures.IRON, 1, Collections.singletonMap("BAD", "x")));
        final Map<Integer, PlayerStateSnapshot.Item> occupied = Collections.singletonMap(0, item);
        final PlayerStateSnapshot.Inventory inventory = new PlayerStateSnapshot.Inventory(9, occupied);
        assertEquals(2, inventory.totalItems());
        assertEquals(1, inventory.items().size());
        assertEquals(inventory, new PlayerStateSnapshot.Inventory(9, occupied));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Inventory(1, Collections.singletonMap(1, item)));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Location(
                DefinitionId.of("zartra", "world/one"), Double.NaN, 0, 0, 0, 0));
    }

    @Test void sessionsEnforceOwnershipReconnectAndRestoration() {
        final PlayerId player = GameFixtures.player(1);
        PlayerSession session = PlayerSession.waiting(GameFixtures.BLUE, GameFixtures.state(player));
        assertTrue(session.isParticipating());
        session = session.activate().disconnect(GameFixtures.NOW);
        assertTrue(session.disconnectedAt().isPresent());
        session = session.reconnect(GameFixtures.NOW.plusSeconds(1), Duration.ofSeconds(2));
        session = session.eliminate().beginRestoration().restored();
        assertEquals(PlayerSession.Status.RESTORED, session.status());
        final PlayerSession restored = session;
        assertThrows(IllegalStateException.class, restored::activate);
        assertThrows(IllegalStateException.class, () -> restored.disconnect(GameFixtures.NOW));
        assertEquals(GameFixtures.player(2), PlayerSession.waiting(GameFixtures.BLUE,
                new PlayerStateSnapshot(GameFixtures.player(2), PlayerStateSnapshot.Inventory.empty(1),
                        GameFixtures.state(player).location(), PlayerStateSnapshot.Mode.SURVIVAL, true))
                .playerId());
    }

    @Test void teamOperationsAreImmutableAndIdempotent() {
        final PlayerId player = GameFixtures.player(1);
        final TeamSnapshot empty = TeamSnapshot.empty(GameFixtures.BLUE, 1);
        final TeamSnapshot joined = empty.add(player);
        assertTrue(joined.contains(player));
        assertEquals(joined, joined.add(player));
        assertThrows(IllegalStateException.class, () -> joined.add(GameFixtures.player(2)));
        assertEquals(joined, joined.remove(GameFixtures.player(9)));
        assertTrue(!joined.destroyBed().bedPresent());
        assertTrue(joined.eliminate().eliminated());
        assertTrue(joined.reset().members().isEmpty());
    }

    @Test void rulesAndTransitionValidateBoundsAndNulls() {
        assertThrows(IllegalArgumentException.class, () -> new GameRules(0, 1, 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new GameRules(1, 1, 0,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new GameRules(1, 1, 1,
                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)));
        final io.zartra.bedwars.game.model.MatchSnapshot snapshot = GameFixtures.machine().snapshot();
        assertThrows(IllegalArgumentException.class, () -> new MatchTransition(snapshot, snapshot,
                Arrays.asList((MatchTransition.Fact) null), false));
        final MatchTransition.Fact fact = new MatchTransition.Fact(
                DefinitionId.of("zartra", "fact/test"), null, GameFixtures.BLUE);
        assertTrue(!fact.playerId().isPresent());
        assertEquals(GameFixtures.BLUE, fact.teamId().get());
    }
}
