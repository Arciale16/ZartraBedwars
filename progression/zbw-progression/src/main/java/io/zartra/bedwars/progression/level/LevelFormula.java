package io.zartra.bedwars.progression.level;

import io.zartra.bedwars.progression.model.ExperienceAmount;
import io.zartra.bedwars.progression.model.LevelDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Versioned level formula with deterministic preview and migration support. */
public final class LevelFormula {
    private final int version;
    private final List<LevelDefinition> definitions;

    /** Creates a strictly increasing cumulative level table. */
    public LevelFormula(final int version, final List<LevelDefinition> definitions) {
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        final List<LevelDefinition> copy = new ArrayList<LevelDefinition>(Objects.requireNonNull(definitions, "definitions"));
        if (copy.isEmpty() || copy.contains(null)) { throw new IllegalArgumentException("definitions must not be empty or contain null"); }
        Collections.sort(copy, Comparator.comparingInt(LevelDefinition::level));
        int priorLevel = 0;
        long priorXp = -1;
        for (LevelDefinition definition : copy) {
            if (definition.level() != priorLevel + 1 || definition.requiredExperience().value() <= priorXp) {
                throw new IllegalArgumentException("levels and XP thresholds must be contiguous and increasing");
            }
            priorLevel = definition.level();
            priorXp = definition.requiredExperience().value();
        }
        this.version = version;
        this.definitions = Collections.unmodifiableList(copy);
    }

    /** Resolves the highest attained level. */
    public int levelFor(final ExperienceAmount lifetimeExperience) {
        Objects.requireNonNull(lifetimeExperience, "lifetimeExperience");
        int level = 1;
        for (LevelDefinition definition : definitions) {
            if (lifetimeExperience.compareTo(definition.requiredExperience()) >= 0) { level = definition.level(); }
            else { break; }
        }
        return level;
    }

    /** @return cumulative XP required for a level */
    public ExperienceAmount requiredExperience(final int level) {
        if (level < 1 || level > definitions.size()) { throw new IllegalArgumentException("unknown level"); }
        return definitions.get(level - 1).requiredExperience();
    }

    /** Recalculates a snapshot under this formula without mutating history. */
    public Preview preview(final ExperienceAmount lifetimeExperience) {
        final int level = levelFor(lifetimeExperience);
        final ExperienceAmount next = level == definitions.size() ? lifetimeExperience
                : requiredExperience(level + 1);
        return new Preview(version, level, lifetimeExperience, next);
    }

    /** @return formula version */ public int version() { return version; }
    /** @return immutable definitions */ public List<LevelDefinition> definitions() { return definitions; }

    /** Immutable level calculation preview. */
    public static final class Preview {
        private final int version;
        private final int level;
        private final ExperienceAmount lifetimeExperience;
        private final ExperienceAmount nextThreshold;
        private Preview(final int version, final int level, final ExperienceAmount lifetimeExperience,
                        final ExperienceAmount nextThreshold) {
            this.version = version;
            this.level = level;
            this.lifetimeExperience = lifetimeExperience;
            this.nextThreshold = nextThreshold;
        }
        /** @return formula version */ public int version() { return version; }
        /** @return calculated level */ public int level() { return level; }
        /** @return lifetime XP */ public ExperienceAmount lifetimeExperience() { return lifetimeExperience; }
        /** @return next cumulative threshold or lifetime XP at maximum */ public ExperienceAmount nextThreshold() { return nextThreshold; }
    }
}
