package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Serialized deterministic match aggregate.
 *
 * <p>The object performs no I/O and retains no platform object. Every successful mutation
 * increments one optimistic revision; callers may provide an expected revision so concurrent
 * writers cannot both commit.</p>
 */
public final class MatchStateMachine {
    private static final ApiError INVALID_TRANSITION = error("invalid_transition", false);
    private static final ApiError REVISION_CONFLICT = error("revision_conflict", true);
    private static final ApiError PLAYER_NOT_FOUND = error("player_not_found", false);
    private static final ApiError TEAM_NOT_FOUND = error("team_not_found", false);
    private static final ApiError CAPACITY = error("capacity", true);
    private static final ApiError RECONNECT_EXPIRED = error("reconnect_expired", false);
    private static final ApiError COMPLETION_CONFLICT = error("completion_conflict", false);
    private static final DefinitionId ADMITTED = fact("player_admitted");
    private static final DefinitionId REMOVED = fact("player_removed");
    private static final DefinitionId COUNTDOWN_STARTED = fact("countdown_started");
    private static final DefinitionId COUNTDOWN_CANCELLED = fact("countdown_cancelled");
    private static final DefinitionId COUNTDOWN_TICKED = fact("countdown_ticked");
    private static final DefinitionId MATCH_STARTED = fact("match_started");
    private static final DefinitionId DISCONNECTED = fact("player_disconnected");
    private static final DefinitionId RECONNECTED = fact("player_reconnected");
    private static final DefinitionId BED_DESTROYED = fact("bed_destroyed");
    private static final DefinitionId PLAYER_ELIMINATED = fact("player_eliminated");
    private static final DefinitionId TEAM_ELIMINATED = fact("team_eliminated");
    private static final DefinitionId COMPLETION_FENCED = fact("completion_fenced");
    private static final DefinitionId COMPLETION_COMMITTED = fact("completion_committed");
    private static final DefinitionId PLAYER_RESTORED = fact("player_restored");
    private static final DefinitionId MATCH_RESET = fact("match_reset");

    private final GameRules rules;
    private MatchSnapshot current;

    /** Creates an empty reusable match with at least two uniquely identified teams. */
    public MatchStateMachine(final MatchId matchId, final ArenaId arenaId,
                             final GameRules rules, final List<TeamSnapshot> teams,
                             final Instant createdAt) {
        this.rules = Objects.requireNonNull(rules, "rules");
        validateInitialTeams(teams, rules.maximumPlayers());
        final List<TeamSnapshot> empty = new ArrayList<TeamSnapshot>();
        for (TeamSnapshot team : teams) { empty.add(team.reset()); }
        current = new MatchSnapshot(matchId, arenaId, 0L, MatchSnapshot.State.WAITING,
                0, empty, Collections.<PlayerSession>emptyList(), null, null, false,
                Objects.requireNonNull(createdAt, "createdAt"));
    }

    private MatchStateMachine(final MatchSnapshot snapshot, final GameRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
        validateInitialTeams(snapshot.teams(), rules.maximumPlayers());
        if (snapshot.sessions().size() > rules.maximumPlayers()) {
            throw new IllegalArgumentException("snapshot exceeds configured player capacity");
        }
        current = Objects.requireNonNull(snapshot, "snapshot");
    }

    /** Rehydrates an exact persisted snapshot without producing a transition. */
    public static MatchStateMachine recover(final MatchSnapshot snapshot, final GameRules rules) {
        return new MatchStateMachine(snapshot, rules);
    }

    /** @return current immutable aggregate state */
    public synchronized MatchSnapshot snapshot() { return current; }

    /** @return immutable rules used to validate this aggregate */
    public GameRules rules() { return rules; }

    /** Applies against the current revision. Serialized callers should prefer this overload. */
    public synchronized Result<MatchTransition> apply(final MatchCommand command,
                                                      final Instant now) {
        return apply(command, current.revision(), now);
    }

    /** Applies one command only if the supplied revision is still current. */
    public synchronized Result<MatchTransition> apply(final MatchCommand command,
                                                      final long expectedRevision,
                                                      final Instant now) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(now, "now");
        if (expectedRevision != current.revision()) {
            return Result.failure(REVISION_CONFLICT);
        }
        switch (command.type()) {
            case ADMIT: return admit(command, now);
            case REMOVE: return remove(command, now);
            case START_COUNTDOWN: return startCountdown(now);
            case CANCEL_COUNTDOWN: return cancelCountdown(now);
            case TICK: return tick(now);
            case FORCE_START: return start(true, now);
            case DISCONNECT: return disconnect(command, now);
            case RECONNECT: return reconnect(command, now);
            case DESTROY_BED: return destroyBed(command, now);
            case ELIMINATE: return eliminate(command, now);
            case COMPLETE: return complete(command, now);
            case COMMIT_COMPLETION: return commitCompletion(command, now);
            case RESTORE_PLAYER: return restore(command, now);
            case FINISH_RESET: return finishReset(now);
            default: return Result.failure(INVALID_TRANSITION);
        }
    }

    private Result<MatchTransition> admit(final MatchCommand command, final Instant now) {
        if (current.state() != MatchSnapshot.State.WAITING
                && current.state() != MatchSnapshot.State.COUNTDOWN) {
            return Result.failure(INVALID_TRANSITION);
        }
        final PlayerId playerId = command.playerId().get();
        if (current.session(playerId).isPresent()) { return duplicate(); }
        if (current.sessions().size() >= rules.maximumPlayers()) { return Result.failure(CAPACITY); }
        final DefinitionId teamId = command.teamId().get();
        final Optional<TeamSnapshot> selected = current.team(teamId);
        if (!selected.isPresent()) { return Result.failure(TEAM_NOT_FOUND); }
        if (!selected.get().hasCapacity() || selected.get().eliminated()) {
            return Result.failure(CAPACITY);
        }
        final List<TeamSnapshot> teams = replaceTeam(selected.get().add(playerId));
        final List<PlayerSession> sessions = new ArrayList<PlayerSession>(current.sessions());
        sessions.add(PlayerSession.waiting(teamId, command.capturedState().get()));
        return change(current.state(), current.countdownRemaining(), teams, sessions,
                null, null, false, now, one(ADMITTED, playerId, teamId));
    }

    private Result<MatchTransition> remove(final MatchCommand command, final Instant now) {
        if (current.state() != MatchSnapshot.State.WAITING
                && current.state() != MatchSnapshot.State.COUNTDOWN) {
            return Result.failure(INVALID_TRANSITION);
        }
        final PlayerId playerId = command.playerId().get();
        final Optional<PlayerSession> found = current.session(playerId);
        if (!found.isPresent()) { return Result.failure(PLAYER_NOT_FOUND); }
        final List<PlayerSession> sessions = new ArrayList<PlayerSession>(current.sessions());
        sessions.remove(found.get());
        final TeamSnapshot team = current.team(found.get().teamId()).get().remove(playerId);
        final MatchSnapshot.State nextState = current.state() == MatchSnapshot.State.COUNTDOWN
                && participating(sessions) < rules.minimumPlayers()
                ? MatchSnapshot.State.WAITING : current.state();
        final int countdown = nextState == MatchSnapshot.State.WAITING
                ? 0 : current.countdownRemaining();
        return change(nextState, countdown, replaceTeam(team), sessions, null, null, false,
                now, one(REMOVED, playerId, team.teamId()));
    }

    private Result<MatchTransition> startCountdown(final Instant now) {
        if (current.state() != MatchSnapshot.State.WAITING
                || participating(current.sessions()) < rules.minimumPlayers()) {
            return Result.failure(INVALID_TRANSITION);
        }
        return change(MatchSnapshot.State.COUNTDOWN, rules.countdownSeconds(),
                current.teams(), current.sessions(), null, null, false, now,
                one(COUNTDOWN_STARTED, null, null));
    }

    private Result<MatchTransition> cancelCountdown(final Instant now) {
        if (current.state() != MatchSnapshot.State.COUNTDOWN) {
            return Result.failure(INVALID_TRANSITION);
        }
        return change(MatchSnapshot.State.WAITING, 0, current.teams(), current.sessions(),
                null, null, false, now, one(COUNTDOWN_CANCELLED, null, null));
    }

    private Result<MatchTransition> tick(final Instant now) {
        if (current.state() != MatchSnapshot.State.COUNTDOWN) {
            return Result.failure(INVALID_TRANSITION);
        }
        if (participating(current.sessions()) < rules.minimumPlayers()) {
            return cancelCountdown(now);
        }
        if (current.countdownRemaining() <= 1) { return start(false, now); }
        return change(MatchSnapshot.State.COUNTDOWN, current.countdownRemaining() - 1,
                current.teams(), current.sessions(), null, null, false, now,
                one(COUNTDOWN_TICKED, null, null));
    }

    private Result<MatchTransition> start(final boolean forced, final Instant now) {
        final boolean eligible = current.state() == MatchSnapshot.State.COUNTDOWN
                || forced && current.state() == MatchSnapshot.State.WAITING;
        final int players = participating(current.sessions());
        if (!eligible || players == 0 || !forced && players < rules.minimumPlayers()) {
            return Result.failure(INVALID_TRANSITION);
        }
        final List<PlayerSession> sessions = new ArrayList<PlayerSession>();
        for (PlayerSession session : current.sessions()) {
            sessions.add(session.status() == PlayerSession.Status.WAITING
                    ? session.activate() : session);
        }
        return change(MatchSnapshot.State.PLAYING, 0, current.teams(), sessions,
                null, null, false, now, one(MATCH_STARTED, null, null));
    }

    private Result<MatchTransition> disconnect(final MatchCommand command, final Instant now) {
        if (current.state() == MatchSnapshot.State.RESETTING) {
            return Result.failure(INVALID_TRANSITION);
        }
        final PlayerId playerId = command.playerId().get();
        final Optional<PlayerSession> found = current.session(playerId);
        if (!found.isPresent()) { return Result.failure(PLAYER_NOT_FOUND); }
        if (found.get().status() == PlayerSession.Status.DISCONNECTED) { return duplicate(); }
        final PlayerSession changed;
        try { changed = found.get().disconnect(now); }
        catch (IllegalStateException failure) { return Result.failure(INVALID_TRANSITION); }
        return change(current.state(), current.countdownRemaining(), current.teams(),
                replaceSession(changed), current.outcome().orElse(null),
                current.completionKey().orElse(null), current.completionCommitted(), now,
                one(DISCONNECTED, playerId, changed.teamId()));
    }

    private Result<MatchTransition> reconnect(final MatchCommand command, final Instant now) {
        final PlayerId playerId = command.playerId().get();
        final Optional<PlayerSession> found = current.session(playerId);
        if (!found.isPresent()) { return Result.failure(PLAYER_NOT_FOUND); }
        if (found.get().status() != PlayerSession.Status.DISCONNECTED) { return duplicate(); }
        final PlayerSession changed;
        try { changed = found.get().reconnect(now, rules.reconnectGrace()); }
        catch (IllegalStateException failure) { return Result.failure(RECONNECT_EXPIRED); }
        return change(current.state(), current.countdownRemaining(), current.teams(),
                replaceSession(changed), current.outcome().orElse(null),
                current.completionKey().orElse(null), current.completionCommitted(), now,
                one(RECONNECTED, playerId, changed.teamId()));
    }

    private Result<MatchTransition> destroyBed(final MatchCommand command, final Instant now) {
        if (current.state() != MatchSnapshot.State.PLAYING) {
            return Result.failure(INVALID_TRANSITION);
        }
        final DefinitionId teamId = command.teamId().get();
        final Optional<TeamSnapshot> found = current.team(teamId);
        if (!found.isPresent()) { return Result.failure(TEAM_NOT_FOUND); }
        if (!found.get().bedPresent()) { return duplicate(); }
        return change(current.state(), 0, replaceTeam(found.get().destroyBed()),
                current.sessions(), null, null, false, now,
                one(BED_DESTROYED, null, teamId));
    }

    private Result<MatchTransition> eliminate(final MatchCommand command, final Instant now) {
        if (current.state() != MatchSnapshot.State.PLAYING) {
            return Result.failure(INVALID_TRANSITION);
        }
        final PlayerId playerId = command.playerId().get();
        final Optional<PlayerSession> found = current.session(playerId);
        if (!found.isPresent()) { return Result.failure(PLAYER_NOT_FOUND); }
        if (found.get().status() == PlayerSession.Status.ELIMINATED) { return duplicate(); }
        final PlayerSession changed;
        try { changed = found.get().eliminate(); }
        catch (IllegalStateException failure) { return Result.failure(INVALID_TRANSITION); }
        final List<PlayerSession> sessions = replaceSession(changed);
        TeamSnapshot team = current.team(changed.teamId()).get();
        final List<MatchTransition.Fact> facts = new ArrayList<MatchTransition.Fact>();
        facts.add(new MatchTransition.Fact(PLAYER_ELIMINATED, playerId, team.teamId()));
        if (!hasActiveMember(team.teamId(), sessions)) {
            team = team.eliminate();
            facts.add(new MatchTransition.Fact(TEAM_ELIMINATED, null, team.teamId()));
        }
        return change(current.state(), 0, replaceTeam(team), sessions,
                null, null, false, now, facts);
    }

    private Result<MatchTransition> complete(final MatchCommand command, final Instant now) {
        final DefinitionId outcome = command.outcome().get();
        final IdempotencyKey key = command.completionKey().get();
        if (current.state() == MatchSnapshot.State.COMPLETING
                || current.state() == MatchSnapshot.State.RESETTING) {
            return current.completionKey().get().equals(key) && current.outcome().get().equals(outcome)
                    ? duplicate() : Result.<MatchTransition>failure(COMPLETION_CONFLICT);
        }
        if (current.state() != MatchSnapshot.State.PLAYING) {
            return Result.failure(INVALID_TRANSITION);
        }
        return change(MatchSnapshot.State.COMPLETING, 0, current.teams(), current.sessions(),
                outcome, key, false, now, one(COMPLETION_FENCED, null, null));
    }

    private Result<MatchTransition> commitCompletion(final MatchCommand command,
                                                     final Instant now) {
        if (current.state() == MatchSnapshot.State.RESETTING && current.completionCommitted()
                && current.completionKey().get().equals(command.completionKey().get())) {
            return duplicate();
        }
        if (current.state() != MatchSnapshot.State.COMPLETING
                || !current.completionKey().get().equals(command.completionKey().get())) {
            return Result.failure(COMPLETION_CONFLICT);
        }
        final List<PlayerSession> sessions = new ArrayList<PlayerSession>();
        for (PlayerSession session : current.sessions()) { sessions.add(session.beginRestoration()); }
        return change(MatchSnapshot.State.RESETTING, 0, current.teams(), sessions,
                current.outcome().get(), current.completionKey().get(), true, now,
                one(COMPLETION_COMMITTED, null, null));
    }

    private Result<MatchTransition> restore(final MatchCommand command, final Instant now) {
        if (current.state() != MatchSnapshot.State.RESETTING || !current.completionCommitted()) {
            return Result.failure(INVALID_TRANSITION);
        }
        final PlayerId playerId = command.playerId().get();
        final Optional<PlayerSession> found = current.session(playerId);
        if (!found.isPresent()) { return Result.failure(PLAYER_NOT_FOUND); }
        if (found.get().status() == PlayerSession.Status.RESTORED) { return duplicate(); }
        final PlayerSession restored;
        try { restored = found.get().restored(); }
        catch (IllegalStateException failure) { return Result.failure(INVALID_TRANSITION); }
        return change(current.state(), 0, current.teams(), replaceSession(restored),
                current.outcome().get(), current.completionKey().get(), true, now,
                one(PLAYER_RESTORED, playerId, restored.teamId()));
    }

    private Result<MatchTransition> finishReset(final Instant now) {
        if (current.state() != MatchSnapshot.State.RESETTING || !allRestored()) {
            return Result.failure(INVALID_TRANSITION);
        }
        final List<TeamSnapshot> resetTeams = new ArrayList<TeamSnapshot>();
        for (TeamSnapshot team : current.teams()) { resetTeams.add(team.reset()); }
        return change(MatchSnapshot.State.WAITING, 0, resetTeams,
                Collections.<PlayerSession>emptyList(), null, null, false, now,
                one(MATCH_RESET, null, null));
    }

    private Result<MatchTransition> change(
            final MatchSnapshot.State state, final int countdown,
            final List<TeamSnapshot> teams, final List<PlayerSession> sessions,
            final DefinitionId outcome, final IdempotencyKey completionKey,
            final boolean completionCommitted, final Instant now,
            final List<MatchTransition.Fact> facts) {
        final MatchSnapshot before = current;
        current = new MatchSnapshot(before.matchId(), before.arenaId(), before.revision() + 1L,
                state, countdown, teams, sessions, outcome, completionKey,
                completionCommitted, now);
        return Result.success(new MatchTransition(before, current, facts, false));
    }

    private Result<MatchTransition> duplicate() {
        return Result.success(new MatchTransition(current, current,
                Collections.<MatchTransition.Fact>emptyList(), true));
    }

    private List<TeamSnapshot> replaceTeam(final TeamSnapshot replacement) {
        final List<TeamSnapshot> teams = new ArrayList<TeamSnapshot>();
        for (TeamSnapshot team : current.teams()) {
            teams.add(team.teamId().equals(replacement.teamId()) ? replacement : team);
        }
        return teams;
    }

    private List<PlayerSession> replaceSession(final PlayerSession replacement) {
        final List<PlayerSession> sessions = new ArrayList<PlayerSession>();
        for (PlayerSession session : current.sessions()) {
            sessions.add(session.playerId().equals(replacement.playerId()) ? replacement : session);
        }
        return sessions;
    }

    private boolean allRestored() {
        for (PlayerSession session : current.sessions()) {
            if (session.status() != PlayerSession.Status.RESTORED) { return false; }
        }
        return true;
    }

    private static int participating(final List<PlayerSession> sessions) {
        int count = 0;
        for (PlayerSession session : sessions) {
            if (session.isParticipating()) { count++; }
        }
        return count;
    }

    private static boolean hasActiveMember(final DefinitionId teamId,
                                           final List<PlayerSession> sessions) {
        for (PlayerSession session : sessions) {
            if (teamId.equals(session.teamId()) && (session.status() == PlayerSession.Status.ACTIVE
                    || session.status() == PlayerSession.Status.DISCONNECTED)) {
                return true;
            }
        }
        return false;
    }

    private static List<MatchTransition.Fact> one(final DefinitionId type,
                                                   final PlayerId playerId,
                                                   final DefinitionId teamId) {
        return Collections.singletonList(new MatchTransition.Fact(type, playerId, teamId));
    }

    private static void validateInitialTeams(final List<TeamSnapshot> teams,
                                             final int maximumPlayers) {
        Objects.requireNonNull(teams, "teams");
        if (teams.size() < 2 || teams.size() > 32 || teams.contains(null)) {
            throw new IllegalArgumentException("match requires between 2 and 32 teams");
        }
        final Set<DefinitionId> ids = new HashSet<DefinitionId>();
        int capacity = 0;
        for (TeamSnapshot team : teams) {
            if (!ids.add(team.teamId())) { throw new IllegalArgumentException("duplicate team ID"); }
            capacity += team.capacity();
        }
        if (capacity < maximumPlayers) {
            throw new IllegalArgumentException("team capacity is below maximumPlayers");
        }
    }

    private static ApiError error(final String path, final boolean retryable) {
        return ApiError.of(DefinitionId.of("zartra", "game/" + path), "game." + path,
                retryable ? ApiError.RetryDisposition.RETRYABLE
                        : ApiError.RetryDisposition.PERMANENT);
    }
    private static DefinitionId fact(final String path) {
        return DefinitionId.of("zartra", "game/fact/" + path);
    }
}
