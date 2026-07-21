package io.zartra.bedwars.progression.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable current or historical player level state. */
public final class LevelState {
    private final int level;
    private final ExperienceAmount experience;
    private final Instant attainedAt;

    /** Creates a validated level state. */
    public LevelState(final int level, final ExperienceAmount experience, final Instant attainedAt) {
        if (level < 1) { throw new IllegalArgumentException("level must be positive"); }
        this.level = level;
        this.experience = Objects.requireNonNull(experience, "experience");
        this.attainedAt = Objects.requireNonNull(attainedAt, "attainedAt");
    }
    /** @return positive level */ public int level() { return level; }
    /** @return cumulative experience */ public ExperienceAmount experience() { return experience; }
    /** @return attainment time */ public Instant attainedAt() { return attainedAt; }
    @Override public int hashCode() { return Objects.hash(level, experience, attainedAt); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof LevelState)) { return false; }
        final LevelState that = (LevelState) other;
        return level == that.level && experience.equals(that.experience)
                && attainedAt.equals(that.attainedAt);
    }
}
