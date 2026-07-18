package io.zartra.bedwars.shop.generator;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic M07 arena-to-M11 generator projection with validated overrides. */
public final class ArenaGeneratorPlan {
    private final List<GeneratorConfiguration> configurations;
    private ArenaGeneratorPlan(final ArenaDefinition arena, final Map<DefinitionId, Override> overrides) {
        final List<GeneratorConfiguration> values = new ArrayList<GeneratorConfiguration>();
        final Map<DefinitionId, Boolean> ids = new LinkedHashMap<DefinitionId, Boolean>();
        for (ArenaGenerator generator : Objects.requireNonNull(arena, "arena").generators()) {
            if (ids.put(generator.id(), Boolean.TRUE) != null) { throw new IllegalArgumentException("duplicate arena generator ID"); }
            final Override override = overrides.get(generator.id());
            final GeneratorConfiguration.Builder builder = GeneratorConfiguration.builder(
                    generator.id(), generator.type(), generator.resource())
                    .interval(override == null || override.interval == null ? generator.interval() : override.interval)
                    .capacity(override == null ? 64 : override.capacity)
                    .amount(override == null ? 1 : override.amount)
                    .enabled(override == null || override.enabled)
                    .deliveryRule(override == null ? GeneratorConfiguration.DeliveryRule.WORLD_DROP : override.deliveryRule);
            if (generator.teamId().isPresent()) { builder.teamOwned(generator.teamId().get()); }
            values.add(builder.build());
        }
        for (DefinitionId overrideId : overrides.keySet()) {
            if (!ids.containsKey(overrideId)) { throw new IllegalArgumentException("override references unknown generator"); }
        }
        Collections.sort(values);
        configurations = Collections.unmodifiableList(values);
    }
    /** @return validated plan using arena defaults plus explicit per-arena overrides */
    public static ArenaGeneratorPlan create(final ArenaDefinition arena, final Map<DefinitionId, Override> overrides) {
        return new ArenaGeneratorPlan(arena, Objects.requireNonNull(overrides, "overrides"));
    }
    /** @return immutable configurations */ public List<GeneratorConfiguration> configurations() { return configurations; }
    /** Immutable per-arena override. */
    public static final class Override {
        private final Duration interval;
        private final int capacity;
        private final int amount;
        private final boolean enabled;
        private final GeneratorConfiguration.DeliveryRule deliveryRule;
        private Override(final Duration interval, final int capacity, final int amount,
                         final boolean enabled, final GeneratorConfiguration.DeliveryRule deliveryRule) {
            this.interval = interval;
            this.capacity = capacity;
            this.amount = amount;
            this.enabled = enabled;
            this.deliveryRule = Objects.requireNonNull(deliveryRule, "deliveryRule");
        }
        /** @return explicit override; final validation occurs against its arena generator */
        public static Override of(final Duration interval, final int capacity, final int amount,
                                  final boolean enabled, final GeneratorConfiguration.DeliveryRule deliveryRule) {
            return new Override(interval, capacity, amount, enabled, deliveryRule);
        }
    }
}
