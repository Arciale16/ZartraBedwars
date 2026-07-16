package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable complete state of one reusable match allocation. */
public final class MatchSnapshot {
    private final MatchId matchId;
    private final ArenaId arenaId;
    private final long revision;
    private final State state;
    private final int countdownRemaining;
    private final List<TeamSnapshot> teams;
    private final List<PlayerSession> sessions;
    private final DefinitionId outcome;
    private final IdempotencyKey completionKey;
    private final boolean completionCommitted;
    private final Instant updatedAt;

    /** Creates a validated immutable aggregate snapshot. */
    public MatchSnapshot(final MatchId matchId, final ArenaId arenaId, final long revision,
                         final State state, final int countdownRemaining,
                         final List<TeamSnapshot> teams, final List<PlayerSession> sessions,
                         final DefinitionId outcome, final IdempotencyKey completionKey,
                         final boolean completionCommitted, final Instant updatedAt) {
        if (revision < 0 || countdownRemaining < 0) {
            throw new IllegalArgumentException("revision and countdown must be non-negative");
        }
        final List<TeamSnapshot> teamCopy = new ArrayList<TeamSnapshot>(
                Objects.requireNonNull(teams, "teams"));
        final List<PlayerSession> sessionCopy = new ArrayList<PlayerSession>(
                Objects.requireNonNull(sessions, "sessions"));
        if (teamCopy.size() < 2 || teamCopy.contains(null) || sessionCopy.contains(null)) {
            throw new IllegalArgumentException("match requires teams and non-null sessions");
        }
        final Set<DefinitionId> teamIds = new HashSet<DefinitionId>();
        for (TeamSnapshot team : teamCopy) {
            if (!teamIds.add(team.teamId())) { throw new IllegalArgumentException("duplicate team ID"); }
        }
        final Set<PlayerId> playerIds = new HashSet<PlayerId>();
        for (PlayerSession session : sessionCopy) {
            if (!teamIds.contains(session.teamId()) || !playerIds.add(session.playerId())) {
                throw new IllegalArgumentException("session has duplicate player or unknown team");
            }
        }
        Collections.sort(teamCopy, new Comparator<TeamSnapshot>() {
            @Override public int compare(final TeamSnapshot left, final TeamSnapshot right) {
                return left.teamId().compareTo(right.teamId());
            }
        });
        Collections.sort(sessionCopy, new Comparator<PlayerSession>() {
            @Override public int compare(final PlayerSession left, final PlayerSession right) {
                return left.playerId().toString().compareTo(right.playerId().toString());
            }
        });
        if ((outcome == null) != (completionKey == null)) {
            throw new IllegalArgumentException("outcome and completion key must be present together");
        }
        if (completionCommitted && completionKey == null) {
            throw new IllegalArgumentException("committed completion requires a key");
        }
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
        this.revision = revision;
        this.state = Objects.requireNonNull(state, "state");
        this.countdownRemaining = countdownRemaining;
        this.teams = Collections.unmodifiableList(teamCopy);
        this.sessions = Collections.unmodifiableList(sessionCopy);
        this.outcome = outcome;
        this.completionKey = completionKey;
        this.completionCommitted = completionCommitted;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** @return match identity */ public MatchId matchId() { return matchId; }
    /** @return leased arena identity */ public ArenaId arenaId() { return arenaId; }
    /** @return optimistic aggregate revision */ public long revision() { return revision; }
    /** @return lifecycle state */ public State state() { return state; }
    /** @return whole seconds remaining */ public int countdownRemaining() { return countdownRemaining; }
    /** @return immutable team states */ public List<TeamSnapshot> teams() { return teams; }
    /** @return immutable player sessions */ public List<PlayerSession> sessions() { return sessions; }
    /** @return terminal match outcome when completion began */ public Optional<DefinitionId> outcome() { return Optional.ofNullable(outcome); }
    /** @return stable exactly-once completion key */ public Optional<IdempotencyKey> completionKey() { return Optional.ofNullable(completionKey); }
    /** @return whether authoritative completion/outbox transaction committed */ public boolean completionCommitted() { return completionCommitted; }
    /** @return last successful transition instant */ public Instant updatedAt() { return updatedAt; }
    /** @return player count that has not completed restoration */
    public int activeSessionCount() {
        int count = 0;
        for (PlayerSession session : sessions) {
            if (session.status() != PlayerSession.Status.RESTORED) { count++; }
        }
        return count;
    }
    /** @return session by identity */
    public Optional<PlayerSession> session(final PlayerId playerId) {
        for (PlayerSession session : sessions) {
            if (session.playerId().equals(playerId)) { return Optional.of(session); }
        }
        return Optional.empty();
    }
    /** @return team by identity */
    public Optional<TeamSnapshot> team(final DefinitionId teamId) {
        for (TeamSnapshot team : teams) {
            if (team.teamId().equals(teamId)) { return Optional.of(team); }
        }
        return Optional.empty();
    }

    /** Match lifecycle states owned by M08. */
    public enum State {
        /** Admission is open. */ WAITING,
        /** Start countdown is running. */ COUNTDOWN,
        /** Team gameplay is active. */ PLAYING,
        /** Completion is fenced but not yet durably committed. */ COMPLETING,
        /** Completion committed; restoration and reset are in progress. */ RESETTING
    }
}
