package io.zartra.bedwars.shop.generator;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Immutable, platform-neutral configuration of one match-local resource generator. */
public final class GeneratorConfiguration implements Comparable<GeneratorConfiguration> {
    /** Ownership scope used for routing and split eligibility. */
    public enum Ownership { ARENA, TEAM }
    /** Delivery semantics requested from the platform delivery adapter. */
    public enum DeliveryRule { WORLD_DROP, DIRECT, SPLIT }

    private final DefinitionId id;
    private final GeneratorTypeId type;
    private final ResourceId resource;
    private final Ownership ownership;
    private final DefinitionId ownerId;
    private final Duration interval;
    private final int capacity;
    private final int amount;
    private final boolean enabled;
    private final DeliveryRule deliveryRule;

    private GeneratorConfiguration(final Builder builder) {
        id = Objects.requireNonNull(builder.id, "id");
        type = Objects.requireNonNull(builder.type, "type");
        resource = Objects.requireNonNull(builder.resource, "resource");
        ownership = Objects.requireNonNull(builder.ownership, "ownership");
        ownerId = builder.ownerId;
        interval = Objects.requireNonNull(builder.interval, "interval");
        deliveryRule = Objects.requireNonNull(builder.deliveryRule, "deliveryRule");
        if (interval.isZero() || interval.isNegative() || interval.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("interval must be positive and at most one hour");
        }
        if (builder.capacity < 1 || builder.capacity > 4096 || builder.amount < 1
                || builder.amount > builder.capacity) {
            throw new IllegalArgumentException("amount and capacity must be positive and bounded");
        }
        if ((ownership == Ownership.TEAM) != (ownerId != null)) {
            throw new IllegalArgumentException("team ownership requires exactly one owner ID");
        }
        capacity = builder.capacity;
        amount = builder.amount;
        enabled = builder.enabled;
    }

    /** @return builder with safe defaults */
    public static Builder builder(final DefinitionId id, final GeneratorTypeId type,
                                  final ResourceId resource) {
        return new Builder(id, type, resource);
    }
    /** @return stable generator identity */ public DefinitionId id() { return id; }
    /** @return registered generator type */ public GeneratorTypeId type() { return type; }
    /** @return native or custom generated resource */ public ResourceId resource() { return resource; }
    /** @return ownership scope */ public Ownership ownership() { return ownership; }
    /** @return team owner, absent for arena-owned generators */ public Optional<DefinitionId> ownerId() { return Optional.ofNullable(ownerId); }
    /** @return interval between generation instants */ public Duration interval() { return interval; }
    /** @return maximum undelivered resource units */ public int capacity() { return capacity; }
    /** @return resource units generated per interval */ public int amount() { return amount; }
    /** @return whether generation is administratively enabled */ public boolean enabled() { return enabled; }
    /** @return requested delivery behavior */ public DeliveryRule deliveryRule() { return deliveryRule; }
    @Override public int compareTo(final GeneratorConfiguration other) { return id.compareTo(Objects.requireNonNull(other, "other").id); }

    /** Mutable construction helper; the resulting value is immutable. */
    public static final class Builder {
        private final DefinitionId id;
        private final GeneratorTypeId type;
        private final ResourceId resource;
        private Ownership ownership = Ownership.ARENA;
        private DefinitionId ownerId;
        private Duration interval = Duration.ofSeconds(1);
        private int capacity = 64;
        private int amount = 1;
        private boolean enabled = true;
        private DeliveryRule deliveryRule = DeliveryRule.WORLD_DROP;
        private Builder(final DefinitionId id, final GeneratorTypeId type, final ResourceId resource) {
            this.id = id;
            this.type = type;
            this.resource = resource;
        }
        /** Assigns arena ownership. */ public Builder arenaOwned() {
            ownership = Ownership.ARENA;
            ownerId = null;
            return this;
        }
        /** Assigns team ownership. */ public Builder teamOwned(final DefinitionId teamId) {
            ownership = Ownership.TEAM;
            ownerId = Objects.requireNonNull(teamId, "teamId");
            return this;
        }
        /** Sets the generation interval. */ public Builder interval(final Duration value) {
            interval = value;
            return this;
        }
        /** Sets maximum pending units. */ public Builder capacity(final int value) {
            capacity = value;
            return this;
        }
        /** Sets units per interval. */ public Builder amount(final int value) {
            amount = value;
            return this;
        }
        /** Sets administrative state. */ public Builder enabled(final boolean value) {
            enabled = value;
            return this;
        }
        /** Sets delivery behavior. */ public Builder deliveryRule(final DeliveryRule value) {
            deliveryRule = value;
            return this;
        }
        /** @return validated immutable configuration */ public GeneratorConfiguration build() { return new GeneratorConfiguration(this); }
    }
}
