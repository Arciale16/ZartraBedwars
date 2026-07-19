package io.zartra.bedwars.progression.model;

import java.util.Objects;

/** Immutable threshold definition for one progression level. */
public final class LevelDefinition {
    private final int level;
    private final ExperienceAmount requiredExperience;

    /** Creates a positive level with a cumulative experience threshold. */
    public LevelDefinition(final int level, final ExperienceAmount requiredExperience) {
        if (level < 1) { throw new IllegalArgumentException("level must be positive"); }
        this.level = level;
        this.requiredExperience = Objects.requireNonNull(requiredExperience, "requiredExperience");
    }
    /** @return positive level */ public int level() { return level; }
    /** @return cumulative experience threshold */ public ExperienceAmount requiredExperience() { return requiredExperience; }
    @Override public int hashCode() { return Objects.hash(level, requiredExperience); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof LevelDefinition)) { return false; }
        final LevelDefinition that = (LevelDefinition) other;
        return level == that.level && requiredExperience.equals(that.requiredExperience);
    }
}
