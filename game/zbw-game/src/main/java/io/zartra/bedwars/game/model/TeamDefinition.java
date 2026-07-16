package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.domain.team.TeamLayoutLimits;
import java.util.Objects;

/** Immutable runtime team identity, presentation metadata and player capacity. */
public final class TeamDefinition {
    private static final DefinitionId NEUTRAL_COLOR =
            DefinitionId.of("zartra", "color/neutral");

    private final DefinitionId id;
    private final String displayName;
    private final DefinitionId color;
    private final int capacity;

    private TeamDefinition(final DefinitionId id, final String displayName,
                           final DefinitionId color, final int capacity) {
        this.id = Objects.requireNonNull(id, "id");
        if (displayName == null || displayName.trim().isEmpty() || displayName.length() > 48
                || displayName.indexOf('\r') >= 0 || displayName.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("displayName is blank, unsafe or too long");
        }
        this.displayName = displayName;
        this.color = Objects.requireNonNull(color, "color");
        this.capacity = TeamLayoutLimits.requireTeamCapacity(capacity);
    }

    /** @return a validated arena-derived team definition */
    public static TeamDefinition of(final DefinitionId id, final String displayName,
                                    final DefinitionId color, final int capacity) {
        return new TeamDefinition(id, displayName, color, capacity);
    }

    static TeamDefinition compatibility(final DefinitionId id, final int capacity) {
        return new TeamDefinition(id, "Configured Team", NEUTRAL_COLOR, capacity);
    }

    /** @return stable semantic team identity */ public DefinitionId id() { return id; }
    /** @return configured display label */ public String displayName() { return displayName; }
    /** @return configured semantic color identity */ public DefinitionId color() { return color; }
    /** @return configured player capacity */ public int capacity() { return capacity; }

    @Override public int hashCode() { return Objects.hash(id, displayName, color, capacity); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof TeamDefinition)) { return false; }
        final TeamDefinition that = (TeamDefinition) other;
        return capacity == that.capacity && id.equals(that.id)
                && displayName.equals(that.displayName) && color.equals(that.color);
    }
}
