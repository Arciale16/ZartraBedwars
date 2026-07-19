package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.shop.generator.ArenaGeneratorPlan;
import io.zartra.bedwars.shop.generator.GeneratorFleet;
import io.zartra.bedwars.shop.generator.ResourceDeliveryPort;
import io.zartra.bedwars.shop.integration.M11MatchRuntime;
import io.zartra.bedwars.shop.item.ItemActionService;
import io.zartra.bedwars.shop.item.ItemActionPorts;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.AddonMechanics;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class M11MatchRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final MatchId MATCH = MatchId.of(new UUID(91L, 1L));
    private static final ArenaId ARENA = ArenaId.of(new UUID(92L, 1L));

    @Test void consumesM08LifecycleAndCleansEverySubsystemExactlyOnce() {
        final ArenaDefinition arena = ArenaDefinition.builder(ARENA,
                MapId.of(new UUID(93L, 1L)), "integration", NOW).build();
        final GeneratorFleet fleet = new GeneratorFleet(MATCH,
                ArenaGeneratorPlan.create(arena, Collections.emptyMap()));
        final ItemActionService items = new ItemActionService(
                AddonMechanics.starterCatalog(), request -> true,
                new ItemActionPorts.Transaction() {
                    @Override public ItemActionPorts.Outcome commit(final UtilityItemDefinition definition,
                            final ItemActionRequest request, final long revision) {
                        return ItemActionPorts.Outcome.COMMITTED;
                    }
                    @Override public void compensate(final UtilityItemDefinition definition,
                                                     final ItemActionRequest request) { }
                },
                new ItemActionPorts.Effect() {
                    @Override public boolean apply(final DefinitionId effect,
                            final UtilityItemDefinition definition, final ItemActionRequest request,
                            final io.zartra.bedwars.api.identity.IdempotencyKey key) { return true; }
                    @Override public void cleanup(final DefinitionId owner) { }
                },
                DefinitionId.of("zartra", "runtime/m11"));
        final ResourceDeliveryPort delivery = (configuration, batch) -> ResourceDeliveryPort.Result.DELIVERED;
        final M11MatchRuntime runtime = new M11MatchRuntime(fleet, delivery,
                Collections.emptyList(), items);

        assertEquals(0, runtime.onSnapshot(snapshot(MatchSnapshot.State.WAITING), NOW));
        assertFalse(runtime.started());
        assertFalse(runtime.cleaned());
        assertEquals(0, runtime.onSnapshot(snapshot(MatchSnapshot.State.COUNTDOWN), NOW));
        assertFalse(runtime.cleaned());
        assertEquals(0, runtime.onSnapshot(snapshot(MatchSnapshot.State.PLAYING), NOW));
        assertTrue(runtime.started());
        assertFalse(runtime.cleaned());
        assertEquals(0, runtime.onSnapshot(snapshot(MatchSnapshot.State.RESETTING), NOW.plusSeconds(1)));
        assertTrue(runtime.cleaned());
        runtime.cleanup();
        assertEquals(0, runtime.onSnapshot(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(2)));
    }

    private static MatchSnapshot snapshot(final MatchSnapshot.State state) {
        return new MatchSnapshot(MATCH, ARENA, 0, state, 0, Arrays.asList(
                TeamSnapshot.empty(DefinitionId.of("zartra", "team/red"), 1),
                TeamSnapshot.empty(DefinitionId.of("zartra", "team/blue"), 1)),
                Collections.emptyList(), null, null, false, NOW);
    }
}
