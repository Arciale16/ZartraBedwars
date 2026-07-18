package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable team-upgrade definition with ordered levels, costs and dependencies. */
public final class UpgradeDefinition implements Comparable<UpgradeDefinition> {
    /** Built-in semantic families; CUSTOM remains extension-safe. */
    public enum Kind { SHARPNESS, PROTECTION, HASTE, FORGE, HEAL_POOL, DRAGON_BUFF, TRAP, CUSTOM }
    private final DefinitionId id;
    private final Kind kind;
    private final List<Level> levels;

    /** Creates a validated definition whose levels must be contiguous from one. */
    public UpgradeDefinition(final DefinitionId id, final Kind kind, final List<Level> levels) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        final List<Level> copy = new ArrayList<Level>(Objects.requireNonNull(levels, "levels"));
        if (copy.isEmpty() || copy.size() > 32 || copy.contains(null)) {
            throw new IllegalArgumentException("upgrade requires between one and 32 levels");
        }
        Collections.sort(copy);
        for (int index = 0; index < copy.size(); index++) {
            if (copy.get(index).number() != index + 1) {
                throw new IllegalArgumentException("upgrade levels must be contiguous from one");
            }
        }
        this.levels = Collections.unmodifiableList(copy);
    }
    /** @return identity */ public DefinitionId id() { return id; }
    /** @return semantic family */ public Kind kind() { return kind; }
    /** @return ordered levels */ public List<Level> levels() { return levels; }
    /** @return level or throws when outside this definition */
    public Level level(final int number) {
        if (number < 1 || number > levels.size()) { throw new IllegalArgumentException("unknown upgrade level"); }
        return levels.get(number - 1);
    }
    /** @return maximum level */ public int maximumLevel() { return levels.size(); }
    @Override public int compareTo(final UpgradeDefinition other) { return id.compareTo(Objects.requireNonNull(other, "other").id); }

    /** Immutable level price, dependency and effect metadata. */
    public static final class Level implements Comparable<Level> {
        private final int number;
        private final Map<ResourceId, Integer> cost;
        private final Map<DefinitionId, Integer> dependencies;
        private final DefinitionId effect;
        private final Duration forgeInterval;
        private Level(final int number, final Map<ResourceId, Integer> cost,
                      final Map<DefinitionId, Integer> dependencies, final DefinitionId effect,
                      final Duration forgeInterval) {
            if (number < 1 || number > 32) { throw new IllegalArgumentException("level is outside bounds"); }
            this.number = number;
            this.cost = positiveResources(cost, "cost", false);
            this.dependencies = positiveDefinitions(dependencies);
            this.effect = Objects.requireNonNull(effect, "effect");
            if (forgeInterval != null && (forgeInterval.isZero() || forgeInterval.isNegative()
                    || forgeInterval.compareTo(Duration.ofHours(1)) > 0)) {
                throw new IllegalArgumentException("forge interval must be positive and bounded");
            }
            this.forgeInterval = forgeInterval;
        }
        /** @return validated level */
        public static Level of(final int number, final Map<ResourceId, Integer> cost,
                               final Map<DefinitionId, Integer> dependencies,
                               final DefinitionId effect, final Duration forgeInterval) {
            return new Level(number, cost, dependencies, effect, forgeInterval);
        }
        /** @return one-based level */ public int number() { return number; }
        /** @return immutable resource cost */ public Map<ResourceId, Integer> cost() { return cost; }
        /** @return minimum required levels by upgrade */ public Map<DefinitionId, Integer> dependencies() { return dependencies; }
        /** @return semantic runtime effect */ public DefinitionId effect() { return effect; }
        /** @return forge interval, or null for non-forge effects */ public Duration forgeInterval() { return forgeInterval; }
        @Override public int compareTo(final Level other) { return Integer.compare(number, Objects.requireNonNull(other, "other").number); }
    }

    private static Map<ResourceId, Integer> positiveResources(final Map<ResourceId, Integer> source,
                                                               final String label,
                                                               final boolean emptyAllowed) {
        final Map<ResourceId, Integer> copy = new TreeMap<ResourceId, Integer>();
        for (Map.Entry<ResourceId, Integer> entry : Objects.requireNonNull(source, label).entrySet()) {
            final int amount = Objects.requireNonNull(entry.getValue(), label + " amount");
            if (amount < 1 || amount > 1000000) { throw new IllegalArgumentException(label + " amount is invalid"); }
            copy.put(Objects.requireNonNull(entry.getKey(), label + " resource"), amount);
        }
        if (!emptyAllowed && copy.isEmpty() || copy.size() > 16) { throw new IllegalArgumentException(label + " has invalid size"); }
        return Collections.unmodifiableMap(copy);
    }
    private static Map<DefinitionId, Integer> positiveDefinitions(final Map<DefinitionId, Integer> source) {
        final Map<DefinitionId, Integer> copy = new TreeMap<DefinitionId, Integer>();
        for (Map.Entry<DefinitionId, Integer> entry : Objects.requireNonNull(source, "dependencies").entrySet()) {
            final int level = Objects.requireNonNull(entry.getValue(), "dependency level");
            if (level < 1 || level > 32) { throw new IllegalArgumentException("dependency level is invalid"); }
            copy.put(Objects.requireNonNull(entry.getKey(), "dependency ID"), level);
        }
        if (copy.size() > 16) { throw new IllegalArgumentException("too many dependencies"); }
        return Collections.unmodifiableMap(copy);
    }
}
