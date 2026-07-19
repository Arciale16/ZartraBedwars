package io.zartra.bedwars.progression.model;

import java.util.Objects;

/** Immutable prestige tier definition. */
public final class PrestigeDefinition {
    private final int prestige;
    private final int requiredLevel;
    private final String displayName;

    /** Creates a validated prestige definition. */
    public PrestigeDefinition(final int prestige, final int requiredLevel, final String displayName) {
        if (prestige < 0 || requiredLevel < 1) { throw new IllegalArgumentException("invalid prestige thresholds"); }
        if (displayName == null || displayName.trim().isEmpty() || displayName.length() > 64) { throw new IllegalArgumentException("displayName must contain 1..64 characters"); }
        this.prestige = prestige;
        this.requiredLevel = requiredLevel;
        this.displayName = displayName;
    }
    /** @return non-negative prestige tier */ public int prestige() { return prestige; }
    /** @return required level */ public int requiredLevel() { return requiredLevel; }
    /** @return original display label */ public String displayName() { return displayName; }
    @Override public int hashCode() { return Objects.hash(prestige, requiredLevel, displayName); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof PrestigeDefinition)) { return false; }
        final PrestigeDefinition that = (PrestigeDefinition) other;
        return prestige == that.prestige && requiredLevel == that.requiredLevel
                && displayName.equals(that.displayName);
    }
}
