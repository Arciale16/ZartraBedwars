package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;

/** Typed identity for one player's progression aggregate. */
public final class PlayerProgressionId {
    private final PlayerId playerId;
    private PlayerProgressionId(final PlayerId playerId) { this.playerId = Objects.requireNonNull(playerId, "playerId"); }
    /** @return progression identity for a player */ public static PlayerProgressionId of(final PlayerId playerId) { return new PlayerProgressionId(playerId); }
    /** @return underlying player identity */ public PlayerId playerId() { return playerId; }
    @Override public int hashCode() { return playerId.hashCode(); }
    @Override public boolean equals(final Object other) { return other instanceof PlayerProgressionId && playerId.equals(((PlayerProgressionId) other).playerId); }
    @Override public String toString() { return playerId.toString(); }
}
