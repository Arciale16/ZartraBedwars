package io.zartra.bedwars.progression.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable player prestige state. */
public final class PrestigeState {
    private final int prestige;
    private final Instant attainedAt;

    /** Creates a non-negative prestige state. */
    public PrestigeState(final int prestige, final Instant attainedAt) {
        if (prestige < 0) { throw new IllegalArgumentException("prestige must be non-negative"); }
        this.prestige = prestige;
        this.attainedAt = Objects.requireNonNull(attainedAt, "attainedAt");
    }
    /** @return prestige tier */ public int prestige() { return prestige; }
    /** @return attainment time */ public Instant attainedAt() { return attainedAt; }
    @Override public int hashCode() { return Objects.hash(prestige, attainedAt); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof PrestigeState)) { return false; }
        final PrestigeState that = (PrestigeState) other;
        return prestige == that.prestige && attainedAt.equals(that.attainedAt);
    }
}
