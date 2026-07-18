package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.shop.generator.ArenaGeneratorPlan;
import io.zartra.bedwars.shop.generator.GeneratorBatch;
import io.zartra.bedwars.shop.generator.GeneratorConfiguration;
import io.zartra.bedwars.shop.generator.GeneratorFleet;
import io.zartra.bedwars.shop.generator.GeneratorRuntime;
import io.zartra.bedwars.shop.generator.GeneratorSplitPolicy;
import io.zartra.bedwars.shop.generator.GeneratorState;
import io.zartra.bedwars.shop.generator.ResourceDeliveryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class GeneratorRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final MatchId MATCH = MatchId.of(new UUID(1L, 1L));
    private static final ArenaId ARENA = ArenaId.of(new UUID(2L, 1L));
    private static final DefinitionId GENERATOR = DefinitionId.of("zartra", "generator/iron");
    private static final ResourceId IRON = ResourceId.of("zartra", "iron");

    @Test
    void validatesDefinitionsOwnershipResourcesAndBounds() {
        final GeneratorConfiguration configuration = configuration(2, 8, 1, true);
        assertEquals(IRON, configuration.resource());
        assertEquals(GeneratorConfiguration.Ownership.ARENA, configuration.ownership());
        assertFalse(configuration.ownerId().isPresent());
        assertThrows(NullPointerException.class, () -> GeneratorConfiguration.builder(GENERATOR,
                GeneratorTypeId.of("zartra", "iron"), null).build());
        assertThrows(IllegalArgumentException.class, () -> GeneratorConfiguration.builder(GENERATOR,
                GeneratorTypeId.of("zartra", "iron"), IRON).interval(Duration.ZERO).build());
        assertThrows(IllegalArgumentException.class, () -> GeneratorConfiguration.builder(GENERATOR,
                GeneratorTypeId.of("zartra", "iron"), IRON).capacity(0).build());
        assertThrows(IllegalArgumentException.class, () -> GeneratorConfiguration.builder(GENERATOR,
                GeneratorTypeId.of("zartra", "iron"), IRON).capacity(2).amount(3).build());
    }

    @Test
    void timingCapacityRetryAndIdempotencyAreDeterministic() {
        final GeneratorRuntime runtime = new GeneratorRuntime(MATCH, configuration(2, 2, 1, true));
        runtime.start(snapshot(MatchSnapshot.State.PLAYING), NOW);
        final RecordingDelivery delivery = new RecordingDelivery(true);
        assertEquals(0, runtime.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(1), delivery));
        assertEquals(2, runtime.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(20), delivery));
        assertEquals(2, runtime.state().pendingUnits());
        delivery.retry = false;
        assertEquals(0, runtime.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(20), delivery));
        assertEquals(0, runtime.state().pendingUnits());
        assertEquals(2, delivery.keys.size());
        assertEquals(2, runtime.state().sequence());
    }

    @Test
    void supportsDisableEnableCleanupAndRecoveryWithoutLoss() {
        final GeneratorRuntime runtime = new GeneratorRuntime(MATCH, configuration(1, 4, 1, true));
        assertThrows(IllegalStateException.class, () -> runtime.start(snapshot(MatchSnapshot.State.WAITING), NOW));
        runtime.start(snapshot(MatchSnapshot.State.PLAYING), NOW);
        runtime.setRunning(false, NOW);
        assertEquals(0, runtime.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(4), new RecordingDelivery(false)));
        runtime.setRunning(true, NOW.plusSeconds(4));
        final RecordingDelivery delivery = new RecordingDelivery(false);
        assertEquals(1, runtime.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(5), delivery));
        assertEquals(GeneratorState.Lifecycle.CLEANED, runtimeAfterEnd(runtime, delivery).lifecycle());
        assertEquals(0, runtime.state().pendingUnits());
        assertThrows(IllegalStateException.class, () -> runtime.start(snapshot(MatchSnapshot.State.PLAYING), NOW));
    }

    @Test
    void boundsCatchupAndSerializesConcurrentTicks() throws Exception {
        final GeneratorRuntime runtime = new GeneratorRuntime(MATCH, configuration(1, 4096, 1, true));
        runtime.start(snapshot(MatchSnapshot.State.PLAYING), NOW);
        final RecordingDelivery delivery = new RecordingDelivery(false);
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        final CountDownLatch start = new CountDownLatch(1);
        for (int index = 0; index < 8; index++) {
            executor.submit(() -> {
                start.await();
                runtime.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(1000), delivery);
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1000, runtime.state().sequence());
        assertEquals(1000, delivery.keys.size());
    }

    @Test
    void splitIsFairStableAndRejectsInvalidRecipients() {
        final PlayerId first = PlayerId.of(new UUID(0L, 1L));
        final PlayerId second = PlayerId.of(new UUID(0L, 2L));
        final PlayerId third = PlayerId.of(new UUID(0L, 3L));
        final List<PlayerId> players = Arrays.asList(third, first, second);
        assertEquals(Integer.valueOf(2), GeneratorSplitPolicy.split(4, 1, players).get(first));
        assertEquals(Integer.valueOf(2), GeneratorSplitPolicy.split(4, 2, players).get(second));
        assertEquals(4, GeneratorSplitPolicy.split(4, 3, players).values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(GeneratorSplitPolicy.split(1, 1, Collections.<PlayerId>emptyList()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> GeneratorSplitPolicy.split(1, 1, Arrays.asList(first, first)));
    }

    @Test
    void arenaOverridesAndMultipleResourcesProduceIndependentFleet() {
        final ArenaGenerator gold = ArenaGenerator.of(DefinitionId.of("zartra", "generator/gold"),
                GeneratorTypeId.of("zartra", "gold"), ResourceId.of("zartra", "gold"), null,
                ArenaLocation.of(1, 64, 1, 0, 0), Duration.ofSeconds(5));
        final ArenaGenerator custom = ArenaGenerator.of(DefinitionId.of("example", "generator/crystal"),
                GeneratorTypeId.of("example", "crystal"), ResourceId.of("example", "crystal"), null,
                ArenaLocation.of(2, 64, 2, 0, 0), Duration.ofSeconds(7));
        final ArenaDefinition arena = ArenaDefinition.builder(ARENA, MapId.of(new UUID(3L, 1L)), "test", NOW)
                .generators(Arrays.asList(custom, gold)).build();
        final Map<DefinitionId, ArenaGeneratorPlan.Override> overrides = new HashMap<DefinitionId, ArenaGeneratorPlan.Override>();
        overrides.put(gold.id(), ArenaGeneratorPlan.Override.of(Duration.ofSeconds(1), 10, 2, true,
                GeneratorConfiguration.DeliveryRule.SPLIT));
        final ArenaGeneratorPlan plan = ArenaGeneratorPlan.create(arena, overrides);
        assertEquals(2, plan.configurations().size());
        assertEquals(Duration.ofSeconds(1), plan.configurations().get(1).interval());
        final GeneratorFleet fleet = new GeneratorFleet(MATCH, plan);
        fleet.start(snapshot(MatchSnapshot.State.PLAYING), NOW);
        final RecordingDelivery delivery = new RecordingDelivery(false);
        assertEquals(6, fleet.tick(snapshot(MatchSnapshot.State.PLAYING), NOW.plusSeconds(7), delivery));
        fleet.cleanup();
        assertTrue(fleet.generators().stream().allMatch(value -> value.state().lifecycle() == GeneratorState.Lifecycle.CLEANED));
        final Map<DefinitionId, ArenaGeneratorPlan.Override> unknown = new HashMap<DefinitionId, ArenaGeneratorPlan.Override>();
        unknown.put(DefinitionId.of("zartra", "generator/missing"), overrides.get(gold.id()));
        assertThrows(IllegalArgumentException.class, () -> ArenaGeneratorPlan.create(arena, unknown));
    }

    private static GeneratorState runtimeAfterEnd(final GeneratorRuntime runtime, final ResourceDeliveryPort delivery) {
        runtime.tick(snapshot(MatchSnapshot.State.RESETTING), NOW.plusSeconds(6), delivery);
        return runtime.state();
    }
    private static GeneratorConfiguration configuration(final int seconds, final int capacity,
                                                        final int amount, final boolean enabled) {
        return GeneratorConfiguration.builder(GENERATOR, GeneratorTypeId.of("zartra", "iron"), IRON)
                .interval(Duration.ofSeconds(seconds)).capacity(capacity).amount(amount).enabled(enabled).build();
    }
    private static MatchSnapshot snapshot(final MatchSnapshot.State state) {
        return new MatchSnapshot(MATCH, ARENA, 0, state, 0, Arrays.asList(
                TeamSnapshot.empty(DefinitionId.of("zartra", "team/blue"), 2),
                TeamSnapshot.empty(DefinitionId.of("zartra", "team/red"), 2)),
                Collections.emptyList(), null, null, false, NOW);
    }
    private static final class RecordingDelivery implements ResourceDeliveryPort {
        private final Set<String> keys = Collections.synchronizedSet(new HashSet<String>());
        private volatile boolean retry;
        private RecordingDelivery(final boolean retry) { this.retry = retry; }
        @Override public Result deliver(final GeneratorConfiguration configuration, final GeneratorBatch batch) {
            if (retry) { return Result.RETRY; }
            return keys.add(batch.key().toString()) ? Result.DELIVERED : Result.ALREADY_DELIVERED;
        }
    }
}
