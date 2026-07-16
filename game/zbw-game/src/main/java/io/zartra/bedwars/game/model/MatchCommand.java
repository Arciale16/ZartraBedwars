package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;
import java.util.Optional;

/** Immutable command accepted by the serialized match state machine. */
public final class MatchCommand {
    private final Type type;
    private final PlayerId playerId;
    private final DefinitionId teamId;
    private final PlayerStateSnapshot capturedState;
    private final DefinitionId outcome;
    private final IdempotencyKey completionKey;

    private MatchCommand(final Type type, final PlayerId playerId,
                         final DefinitionId teamId, final PlayerStateSnapshot capturedState,
                         final DefinitionId outcome, final IdempotencyKey completionKey) {
        this.type = Objects.requireNonNull(type, "type");
        this.playerId = playerId;
        this.teamId = teamId;
        this.capturedState = capturedState;
        this.outcome = outcome;
        this.completionKey = completionKey;
    }

    /** @return admission command with exact assigned team and captured state */
    public static MatchCommand admit(final DefinitionId teamId,
                                     final PlayerStateSnapshot capturedState) {
        return new MatchCommand(Type.ADMIT, capturedState.playerId(), teamId,
                capturedState, null, null);
    }
    /** @return pre-game removal command */ public static MatchCommand remove(final PlayerId id) { return player(Type.REMOVE, id); }
    /** @return start-countdown command */ public static MatchCommand startCountdown() { return simple(Type.START_COUNTDOWN); }
    /** @return cancellation command */ public static MatchCommand cancelCountdown() { return simple(Type.CANCEL_COUNTDOWN); }
    /** @return one-second deterministic countdown tick */ public static MatchCommand tick() { return simple(Type.TICK); }
    /** @return authorized force-start command */ public static MatchCommand forceStart() { return simple(Type.FORCE_START); }
    /** @return disconnect command */ public static MatchCommand disconnect(final PlayerId id) { return player(Type.DISCONNECT, id); }
    /** @return reconnect command */ public static MatchCommand reconnect(final PlayerId id) { return player(Type.RECONNECT, id); }
    /** @return team-bed destruction command */
    public static MatchCommand destroyBed(final DefinitionId teamId) {
        return new MatchCommand(Type.DESTROY_BED, null, teamId, null, null, null);
    }
    /** @return player-elimination command */ public static MatchCommand eliminate(final PlayerId id) { return player(Type.ELIMINATE, id); }
    /** @return completion fence command */
    public static MatchCommand complete(final DefinitionId outcome,
                                        final IdempotencyKey completionKey) {
        return new MatchCommand(Type.COMPLETE, null, null, null,
                Objects.requireNonNull(outcome, "outcome"),
                Objects.requireNonNull(completionKey, "completionKey"));
    }
    /** @return authoritative completion commit command */
    public static MatchCommand commitCompletion(final IdempotencyKey completionKey) {
        return new MatchCommand(Type.COMMIT_COMPLETION, null, null, null, null,
                Objects.requireNonNull(completionKey, "completionKey"));
    }
    /** @return one player restoration completion command */ public static MatchCommand restore(final PlayerId id) { return player(Type.RESTORE_PLAYER, id); }
    /** @return final reset command */ public static MatchCommand finishReset() { return simple(Type.FINISH_RESET); }

    private static MatchCommand player(final Type type, final PlayerId id) {
        return new MatchCommand(type, Objects.requireNonNull(id, "playerId"), null,
                null, null, null);
    }
    private static MatchCommand simple(final Type type) {
        return new MatchCommand(type, null, null, null, null, null);
    }
    /** @return command type */ public Type type() { return type; }
    /** @return player argument */ public Optional<PlayerId> playerId() { return Optional.ofNullable(playerId); }
    /** @return team argument */ public Optional<DefinitionId> teamId() { return Optional.ofNullable(teamId); }
    /** @return captured pre-session state */ public Optional<PlayerStateSnapshot> capturedState() { return Optional.ofNullable(capturedState); }
    /** @return completion outcome */ public Optional<DefinitionId> outcome() { return Optional.ofNullable(outcome); }
    /** @return completion idempotency key */ public Optional<IdempotencyKey> completionKey() { return Optional.ofNullable(completionKey); }

    /** Supported deterministic state-machine commands. */
    public enum Type {
        /** Admit one assigned player. */ ADMIT,
        /** Remove one waiting player. */ REMOVE,
        /** Begin configured countdown. */ START_COUNTDOWN,
        /** Cancel a countdown. */ CANCEL_COUNTDOWN,
        /** Advance countdown by one second. */ TICK,
        /** Authorized immediate start. */ FORCE_START,
        /** Mark participant disconnected. */ DISCONNECT,
        /** Resume disconnected participant. */ RECONNECT,
        /** Destroy one team bed. */ DESTROY_BED,
        /** Eliminate one participant. */ ELIMINATE,
        /** Fence a terminal outcome. */ COMPLETE,
        /** Record atomic completion/outbox commit. */ COMMIT_COMPLETION,
        /** Record one owner-thread state restoration. */ RESTORE_PLAYER,
        /** Finish reset and reopen admission. */ FINISH_RESET
    }
}
