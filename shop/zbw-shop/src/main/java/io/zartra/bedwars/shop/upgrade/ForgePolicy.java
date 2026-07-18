package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable forge output policy indexed by the purchased forge upgrade level. */
public final class ForgePolicy {
    private final DefinitionId upgradeId;
    private final Map<Integer, Level> levels;
    /** Creates a policy with contiguous levels from one. */
    public ForgePolicy(final DefinitionId upgradeId, final Map<Integer, Level> levels) {
        this.upgradeId = Objects.requireNonNull(upgradeId, "upgradeId");
        final Map<Integer, Level> copy = new TreeMap<Integer, Level>(Objects.requireNonNull(levels, "levels"));
        if (copy.isEmpty() || copy.size() > 32) { throw new IllegalArgumentException("forge levels are invalid"); }
        int expected = 1;
        for (Map.Entry<Integer, Level> entry : copy.entrySet()) {
            if (entry.getKey() != expected++ || entry.getValue() == null) {
                throw new IllegalArgumentException("forge levels must be contiguous from one");
            }
        }
        this.levels = Collections.unmodifiableMap(copy);
    }
    /** @return forge upgrade ID */ public DefinitionId upgradeId() { return upgradeId; }
    /** @return policy for a purchased level */
    public Level level(final int number) {
        final Level level = levels.get(number);
        if (level == null) { throw new IllegalArgumentException("unknown forge level"); }
        return level;
    }
    /** Immutable interval and multi-resource yield. */
    public static final class Level {
        private final Duration interval;
        private final Map<ResourceId, Integer> resources;
        /** Creates a bounded forge level. */
        public Level(final Duration interval, final Map<ResourceId, Integer> resources) {
            this.interval = Objects.requireNonNull(interval, "interval");
            if (interval.isZero() || interval.isNegative() || interval.compareTo(Duration.ofHours(1)) > 0) {
                throw new IllegalArgumentException("forge interval is invalid");
            }
            final Map<ResourceId, Integer> copy = new TreeMap<ResourceId, Integer>();
            for (Map.Entry<ResourceId, Integer> entry : Objects.requireNonNull(resources, "resources").entrySet()) {
                final int amount = Objects.requireNonNull(entry.getValue(), "amount");
                if (amount < 1 || amount > 4096) { throw new IllegalArgumentException("forge amount is invalid"); }
                copy.put(Objects.requireNonNull(entry.getKey(), "resource"), amount);
            }
            if (copy.isEmpty() || copy.size() > 16) { throw new IllegalArgumentException("forge resources are invalid"); }
            this.resources = Collections.unmodifiableMap(copy);
        }
        /** @return generation interval */ public Duration interval() { return interval; }
        /** @return generated native/custom resources */ public Map<ResourceId, Integer> resources() { return resources; }
    }
}
