package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.authorization.PermissionNode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/** Deterministic item-rotation, override and state contracts. */
public final class RotationContracts {
    private RotationContracts() { }

    /** One weighted item in a configured rotation pool. */
    public static final class PoolEntry {
        private final ShopIds.ItemId itemId;
        private final int weight;
        /** Creates an entry with weight in 1..1,000,000. */
        public PoolEntry(final ShopIds.ItemId itemId, final int weight) {
            this.itemId = Objects.requireNonNull(itemId, "itemId");
            if (weight < 1 || weight > 1_000_000) { throw new IllegalArgumentException("weight is out of range"); }
            this.weight = weight;
        }
        /** @return item */ public ShopIds.ItemId itemId() { return itemId; }
        /** @return deterministic selection weight */ public int weight() { return weight; }
    }

    /** Immutable schedule with explicit time zone, bounds and no-repeat policy. */
    public static final class Definition {
        private final ShopIds.RotationId id;
        private final ZoneId zoneId;
        private final Duration period;
        private final Instant startsAt;
        private final Instant endsAt;
        private final int slots;
        private final int noRepeatPeriods;
        private final Duration purchaseCooldown;
        private final PermissionNode permission;
        private final List<PoolEntry> pool;

        /** Creates a validated rotation definition. */
        public Definition(final ShopIds.RotationId id, final ZoneId zoneId,
                          final Duration period, final Instant startsAt,
                          final Optional<Instant> endsAt, final int slots,
                          final int noRepeatPeriods, final Duration purchaseCooldown,
                          final Optional<PermissionNode> permission,
                          final Collection<PoolEntry> pool) {
            this.id = Objects.requireNonNull(id, "id");
            this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
            this.period = Objects.requireNonNull(period, "period");
            if (period.compareTo(Duration.ofMinutes(1)) < 0
                    || period.compareTo(Duration.ofDays(366)) > 0) {
                throw new IllegalArgumentException("rotation period is out of range");
            }
            this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
            this.endsAt = Objects.requireNonNull(endsAt, "endsAt").orElse(null);
            if (this.endsAt != null && !this.endsAt.isAfter(startsAt)) {
                throw new IllegalArgumentException("rotation end must follow start");
            }
            if (slots < 1 || slots > 54 || noRepeatPeriods < 0 || noRepeatPeriods > 128) {
                throw new IllegalArgumentException("rotation slots or no-repeat bound is invalid");
            }
            this.purchaseCooldown = Objects.requireNonNull(purchaseCooldown, "purchaseCooldown");
            if (purchaseCooldown.isNegative() || purchaseCooldown.compareTo(Duration.ofDays(7)) > 0) {
                throw new IllegalArgumentException("rotation cooldown is out of range");
            }
            this.slots = slots;
            this.noRepeatPeriods = noRepeatPeriods;
            this.permission = Objects.requireNonNull(permission, "permission").orElse(null);
            final List<PoolEntry> copy = new ArrayList<PoolEntry>();
            final Set<ShopIds.ItemId> unique = new LinkedHashSet<ShopIds.ItemId>();
            for (PoolEntry entry : Objects.requireNonNull(pool, "pool")) {
                final PoolEntry checked = Objects.requireNonNull(entry, "entry");
                if (!unique.add(checked.itemId())) { throw new IllegalArgumentException("duplicate rotation item"); }
                copy.add(checked);
            }
            if (copy.size() < slots) { throw new IllegalArgumentException("rotation pool is smaller than slot count"); }
            this.pool = Collections.unmodifiableList(copy);
        }
        /** @return stable schedule ID */ public ShopIds.RotationId id() { return id; }
        /** @return explicit schedule time zone */ public ZoneId zoneId() { return zoneId; }
        /** @return fixed period */ public Duration period() { return period; }
        /** @return inclusive schedule start */ public Instant startsAt() { return startsAt; }
        /** @return optional exclusive schedule end */ public Optional<Instant> endsAt() { return Optional.ofNullable(endsAt); }
        /** @return simultaneously active slots */ public int slots() { return slots; }
        /** @return prior periods excluded from selection */ public int noRepeatPeriods() { return noRepeatPeriods; }
        /** @return configured purchase cooldown */ public Duration purchaseCooldown() { return purchaseCooldown; }
        /** @return optional access permission */ public Optional<PermissionNode> permission() { return Optional.ofNullable(permission); }
        /** @return immutable weighted pool */ public List<PoolEntry> pool() { return pool; }
    }

    /** Immutable active rotation selected from a definition revision. */
    public static final class Snapshot {
        private final ShopIds.RotationId id;
        private final long revision;
        private final Instant periodStart;
        private final Instant periodEnd;
        private final List<ShopIds.ItemId> activeItems;
        /** Creates a bounded active snapshot. */
        public Snapshot(final ShopIds.RotationId id, final long revision,
                        final Instant periodStart, final Instant periodEnd,
                        final Collection<ShopIds.ItemId> activeItems) {
            this.id = Objects.requireNonNull(id, "id");
            if (revision < 1) { throw new IllegalArgumentException("revision must be positive"); }
            this.revision = revision;
            this.periodStart = Objects.requireNonNull(periodStart, "periodStart");
            this.periodEnd = Objects.requireNonNull(periodEnd, "periodEnd");
            if (!periodEnd.isAfter(periodStart)) { throw new IllegalArgumentException("period end must follow start"); }
            final Set<ShopIds.ItemId> unique = new LinkedHashSet<ShopIds.ItemId>();
            for (ShopIds.ItemId item : Objects.requireNonNull(activeItems, "activeItems")) {
                if (!unique.add(Objects.requireNonNull(item, "item"))) {
                    throw new IllegalArgumentException("duplicate active item");
                }
            }
            if (unique.isEmpty()) { throw new IllegalArgumentException("active rotation is empty"); }
            this.activeItems = Collections.unmodifiableList(new ArrayList<ShopIds.ItemId>(unique));
        }
        /** @return rotation ID */ public ShopIds.RotationId id() { return id; }
        /** @return optimistic state revision */ public long revision() { return revision; }
        /** @return period start */ public Instant periodStart() { return periodStart; }
        /** @return period end */ public Instant periodEnd() { return periodEnd; }
        /** @return immutable active items */ public List<ShopIds.ItemId> activeItems() { return activeItems; }
    }

    /** Asynchronous local persistence port; M19/M20 later coordinate distributed state. */
    public interface StatePort {
        /** Loads a local snapshot without blocking a tick or owner thread. */ CompletionStage<Optional<Snapshot>> load(ShopIds.RotationId id);
        /** Atomically stores a snapshot if the current revision equals {@code expectedRevision}. */
        CompletionStage<Snapshot> replace(Snapshot snapshot, long expectedRevision);
    }

    /** Non-blocking event sink for rotation changes and administrative overrides. */
    public interface EventSink {
        /** Observes an immutable committed snapshot; implementations must return promptly. */ void onChanged(Snapshot snapshot);
    }
}
