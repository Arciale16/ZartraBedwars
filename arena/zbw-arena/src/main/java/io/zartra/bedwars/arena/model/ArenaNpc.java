package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;
import java.util.Optional;

/** Immutable shop or team-upgrade NPC placement. */
public final class ArenaNpc implements Comparable<ArenaNpc> {
    private final DefinitionId id;
    private final Kind kind;
    private final DefinitionId teamId;
    private final ArenaLocation location;

    private ArenaNpc(final DefinitionId id, final Kind kind, final DefinitionId teamId,
                     final ArenaLocation location) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.teamId = teamId;
        this.location = Objects.requireNonNull(location, "location");
    }

    /** @return a validated NPC placement */
    public static ArenaNpc of(final DefinitionId id, final Kind kind,
                              final DefinitionId teamId, final ArenaLocation location) {
        return new ArenaNpc(id, kind, teamId, location);
    }
    /** @return NPC identity */ public DefinitionId id() { return id; }
    /** @return NPC purpose */ public Kind kind() { return kind; }
    /** @return optional owning team */ public Optional<DefinitionId> teamId() { return Optional.ofNullable(teamId); }
    /** @return location including orientation */ public ArenaLocation location() { return location; }
    @Override public int compareTo(final ArenaNpc other) { return id.compareTo(Objects.requireNonNull(other, "other").id); }
    @Override public int hashCode() { return Objects.hash(id, kind, teamId, location); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaNpc)) { return false; }
        final ArenaNpc that = (ArenaNpc) other;
        return Objects.deepEquals(new Object[] {id, kind, teamId, location},
                new Object[] {that.id, that.kind, that.teamId, that.location});
    }

    /** Supported NPC purposes without binding to a platform entity type. */
    public enum Kind { SHOP, TEAM_UPGRADE }
}
