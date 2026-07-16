package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable player membership and reconnect/restoration state for one match. */
public final class PlayerSession {
    private final PlayerId playerId;
    private final DefinitionId teamId;
    private final PlayerStateSnapshot capturedState;
    private final Status status;
    private final Status resumeStatus;
    private final Instant disconnectedAt;

    private PlayerSession(final PlayerId playerId, final DefinitionId teamId,
                          final PlayerStateSnapshot capturedState, final Status status,
                          final Status resumeStatus, final Instant disconnectedAt) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.teamId = Objects.requireNonNull(teamId, "teamId");
        this.capturedState = Objects.requireNonNull(capturedState, "capturedState");
        if (!playerId.equals(capturedState.playerId())) {
            throw new IllegalArgumentException("captured state belongs to another player");
        }
        this.status = Objects.requireNonNull(status, "status");
        this.resumeStatus = Objects.requireNonNull(resumeStatus, "resumeStatus");
        this.disconnectedAt = disconnectedAt;
        if (status == Status.DISCONNECTED && disconnectedAt == null) {
            throw new IllegalArgumentException("disconnected session requires timestamp");
        }
    }

    /** @return newly admitted waiting session */
    public static PlayerSession waiting(final DefinitionId teamId,
                                        final PlayerStateSnapshot capturedState) {
        return new PlayerSession(capturedState.playerId(), teamId, capturedState,
                Status.WAITING, Status.WAITING, null);
    }
    /** @return player identity */ public PlayerId playerId() { return playerId; }
    /** @return assigned team identity */ public DefinitionId teamId() { return teamId; }
    /** @return state captured before arena mutation */ public PlayerStateSnapshot capturedState() { return capturedState; }
    /** @return current lifecycle state */ public Status status() { return status; }
    /** @return disconnect instant when disconnected */ public Optional<Instant> disconnectedAt() { return Optional.ofNullable(disconnectedAt); }
    /** @return whether gameplay or waiting state remains eligible */
    public boolean isParticipating() {
        return status == Status.WAITING || status == Status.ACTIVE || status == Status.DISCONNECTED;
    }
    /** @return copy activated for gameplay */
    public PlayerSession activate() {
        if (status != Status.WAITING) { throw new IllegalStateException("only waiting player can activate"); }
        return new PlayerSession(playerId, teamId, capturedState, Status.ACTIVE, Status.ACTIVE, null);
    }
    /** @return copy recording a disconnect; repeated disconnect is idempotent */
    public PlayerSession disconnect(final Instant instant) {
        Objects.requireNonNull(instant, "instant");
        if (status == Status.DISCONNECTED) { return this; }
        if (status == Status.RESTORED || status == Status.RESTORING) {
            throw new IllegalStateException("restoration state cannot disconnect");
        }
        return new PlayerSession(playerId, teamId, capturedState, Status.DISCONNECTED,
                status, instant);
    }
    /** @return copy resumed within the configured grace period */
    public PlayerSession reconnect(final Instant now, final Duration grace) {
        if (status != Status.DISCONNECTED) { return this; }
        if (now.isAfter(disconnectedAt.plus(Objects.requireNonNull(grace, "grace")))) {
            throw new IllegalStateException("reconnect grace expired");
        }
        return new PlayerSession(playerId, teamId, capturedState, resumeStatus,
                resumeStatus, null);
    }
    /** @return copy marked eliminated */
    public PlayerSession eliminate() {
        if (status == Status.ELIMINATED) { return this; }
        if (status != Status.ACTIVE && status != Status.DISCONNECTED) {
            throw new IllegalStateException("only active participant can be eliminated");
        }
        return new PlayerSession(playerId, teamId, capturedState, Status.ELIMINATED,
                Status.ELIMINATED, disconnectedAt);
    }
    /** @return copy ready for an owner-thread restoration effect */
    public PlayerSession beginRestoration() {
        if (status == Status.RESTORED || status == Status.RESTORING) { return this; }
        return new PlayerSession(playerId, teamId, capturedState, Status.RESTORING,
                Status.RESTORING, null);
    }
    /** @return copy recording exactly-once restoration completion */
    public PlayerSession restored() {
        if (status == Status.RESTORED) { return this; }
        if (status != Status.RESTORING) {
            throw new IllegalStateException("restoration was not started");
        }
        return new PlayerSession(playerId, teamId, capturedState, Status.RESTORED,
                Status.RESTORED, null);
    }

    /** Player lifecycle states within a match. */
    public enum Status {
        /** Admitted and waiting. */ WAITING,
        /** Active team participant. */ ACTIVE,
        /** Temporarily disconnected and rejoinable. */ DISCONNECTED,
        /** Eliminated viewer. */ ELIMINATED,
        /** Owner-thread restoration requested. */ RESTORING,
        /** Captured state restored exactly once. */ RESTORED
    }
}
