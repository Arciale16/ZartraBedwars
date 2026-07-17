package io.zartra.bedwars.game.spectator;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Bounded spectator session state machine with exact restoration and target validation. */
public final class SpectatorFramework {
    private SpectatorFramework() { throw new AssertionError("No instances"); }

    /** Opaque spectator-session identity. */
    public static final class SessionId implements Comparable<SessionId> {
        private final UUID value;
        private SessionId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return supplied identity */ public static SessionId of(final UUID value) { return new SessionId(value); }
        /** @return generated identity */ public static SessionId random() { return of(UUID.randomUUID()); }
        /** @return UUID */ public UUID value() { return value; }
        @Override public int compareTo(final SessionId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof SessionId && value.equals(((SessionId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Admission cause retained for policy and audit. */
    public enum EntryReason { /** Participant was eliminated. */ ELIMINATED, /** Player requested an external view. */ EXTERNAL, /** Authorized staff view. */ STAFF }

    /** Session lifecycle. */
    public enum State { /** Actively viewing. */ ACTIVE, /** Temporarily disconnected. */ DISCONNECTED, /** Restoration is pending owner-thread application. */ RESTORING, /** Terminal and cleaned. */ CLOSED }

    /** Immutable spectator preferences that may persist across sessions. */
    public static final class Preferences {
        private final int flightSpeedLevel;
        private final boolean nightVision;
        private final boolean showSpectators;
        private final boolean firstPerson;
        /** Creates bounded preferences. */
        public Preferences(final int flightSpeedLevel, final boolean nightVision,
                           final boolean showSpectators, final boolean firstPerson) {
            if (flightSpeedLevel < 0 || flightSpeedLevel > 10) {
                throw new IllegalArgumentException("flight speed level must be between 0 and 10");
            }
            this.flightSpeedLevel = flightSpeedLevel;
            this.nightVision = nightVision;
            this.showSpectators = showSpectators;
            this.firstPerson = firstPerson;
        }
        /** @return configured level */ public int flightSpeedLevel() { return flightSpeedLevel; }
        /** @return night-vision preference */ public boolean nightVision() { return nightVision; }
        /** @return other-spectator visibility */ public boolean showSpectators() { return showSpectators; }
        /** @return first-person camera preference */ public boolean firstPerson() { return firstPerson; }
    }

    /** Explicit allowlist restrictions for a spectator. */
    public static final class Restrictions {
        private final boolean flight;
        private final boolean teleport;
        private final boolean chat;
        private final Set<DefinitionId> commands;
        /** Creates immutable policy; drop, pickup, damage and world escape are always denied. */
        public Restrictions(final boolean flight, final boolean teleport, final boolean chat,
                            final Collection<DefinitionId> commands) {
            this.flight = flight;
            this.teleport = teleport;
            this.chat = chat;
            final Set<DefinitionId> copy = new LinkedHashSet<DefinitionId>();
            for (DefinitionId command : Objects.requireNonNull(commands, "commands")) {
                copy.add(Objects.requireNonNull(command, "command"));
            }
            if (copy.size() > 128) { throw new IllegalArgumentException("too many spectator commands"); }
            this.commands = Collections.unmodifiableSet(copy);
        }
        /** @return flight allowed */ public boolean flight() { return flight; }
        /** @return target teleport allowed */ public boolean teleport() { return teleport; }
        /** @return spectator chat allowed */ public boolean chat() { return chat; }
        /** @return command allowlist */ public Set<DefinitionId> commands() { return commands; }
        /** @return item drop is denied */ public boolean canDropItems() { return false; }
        /** @return item pickup is denied */ public boolean canPickupItems() { return false; }
        /** @return spectator damage is denied */ public boolean canTakeDamage() { return false; }
        /** @return arbitrary world change is denied */ public boolean canChangeWorld() { return false; }
    }

    /** Configured lifecycle limits and external-admission policy. */
    public static final class Policy {
        private final int maximumSessions;
        private final Duration reconnectGrace;
        private final boolean externalAdmission;
        private final Restrictions restrictions;
        /** Creates typed spectator policy. */
        public Policy(final int maximumSessions, final Duration reconnectGrace,
                      final boolean externalAdmission, final Restrictions restrictions) {
            if (maximumSessions < 1 || maximumSessions > 100000) { throw new IllegalArgumentException("invalid spectator capacity"); }
            if (reconnectGrace == null || reconnectGrace.isNegative() || reconnectGrace.isZero()) {
                throw new IllegalArgumentException("reconnectGrace must be positive");
            }
            this.maximumSessions = maximumSessions;
            this.reconnectGrace = reconnectGrace;
            this.externalAdmission = externalAdmission;
            this.restrictions = Objects.requireNonNull(restrictions, "restrictions");
        }
        /** @return session bound */ public int maximumSessions() { return maximumSessions; }
        /** @return reconnect grace */ public Duration reconnectGrace() { return reconnectGrace; }
        /** @return whether ordinary external admission is allowed */ public boolean externalAdmission() { return externalAdmission; }
        /** @return interaction policy */ public Restrictions restrictions() { return restrictions; }
    }

    /** Immutable potential navigation target. */
    public static final class Target {
        private final PlayerId playerId;
        private final MatchId matchId;
        private final DefinitionId teamId;
        private final boolean living;
        private final boolean visible;
        private final boolean vanished;
        private final boolean consent;
        private final int order;
        /** Creates target facts. */
        public Target(final PlayerId playerId, final MatchId matchId, final DefinitionId teamId,
                      final boolean living, final boolean visible, final boolean vanished,
                      final boolean consent, final int order) {
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            this.matchId = Objects.requireNonNull(matchId, "matchId");
            this.teamId = Objects.requireNonNull(teamId, "teamId");
            this.living = living;
            this.visible = visible;
            this.vanished = vanished;
            this.consent = consent;
            this.order = order;
        }
        /** @return player */ public PlayerId playerId() { return playerId; }
        /** @return match */ public MatchId matchId() { return matchId; }
        /** @return team */ public DefinitionId teamId() { return teamId; }
        /** @return configured order */ public int order() { return order; }
        /** @return safe target eligibility */ public boolean eligible() { return living && visible && !vanished && consent; }
    }

    /** Immutable spectator session. */
    public static final class Session {
        private final SessionId id;
        private final PlayerId playerId;
        private final MatchId matchId;
        private final EntryReason reason;
        private final PlayerStateSnapshot capturedState;
        private final Preferences preferences;
        private final State state;
        private final PlayerId target;
        private final long revision;
        private final Instant disconnectedAt;
        private Session(final SessionId id, final PlayerId playerId, final MatchId matchId,
                        final EntryReason reason, final PlayerStateSnapshot capturedState,
                        final Preferences preferences, final State state, final PlayerId target,
                        final long revision, final Instant disconnectedAt) {
            this.id = id;
            this.playerId = playerId;
            this.matchId = matchId;
            this.reason = reason;
            this.capturedState = capturedState;
            this.preferences = preferences;
            this.state = state;
            this.target = target;
            this.revision = revision;
            this.disconnectedAt = disconnectedAt;
        }
        /** @return session */ public SessionId id() { return id; }
        /** @return spectator */ public PlayerId playerId() { return playerId; }
        /** @return bound match */ public MatchId matchId() { return matchId; }
        /** @return admission reason */ public EntryReason reason() { return reason; }
        /** @return captured state for exact restoration */ public PlayerStateSnapshot capturedState() { return capturedState; }
        /** @return current preferences */ public Preferences preferences() { return preferences; }
        /** @return state */ public State state() { return state; }
        /** @return target when selected */ public Optional<PlayerId> target() { return Optional.ofNullable(target); }
        /** @return monotonic session revision */ public long revision() { return revision; }
        /** @return disconnect time */ public Optional<Instant> disconnectedAt() { return Optional.ofNullable(disconnectedAt); }
        private Session change(final Preferences nextPreferences, final State nextState,
                               final PlayerId nextTarget, final Instant disconnected) {
            return new Session(id, playerId, matchId, reason, capturedState, nextPreferences,
                    nextState, nextTarget, revision + 1L, disconnected);
        }
    }

    /** Exact restoration request to be applied once by a platform projection. */
    public static final class Restoration {
        private final SessionId sessionId;
        private final PlayerStateSnapshot capturedState;
        private final long sessionRevision;
        private Restoration(final SessionId sessionId, final PlayerStateSnapshot capturedState,
                            final long sessionRevision) {
            this.sessionId = sessionId;
            this.capturedState = capturedState;
            this.sessionRevision = sessionRevision;
        }
        /** @return session */ public SessionId sessionId() { return sessionId; }
        /** @return state to restore */ public PlayerStateSnapshot capturedState() { return capturedState; }
        /** @return revision fence */ public long sessionRevision() { return sessionRevision; }
    }

    /** Immutable audit event. */
    public static final class Event {
        private final Type type;
        private final SessionId sessionId;
        private final PlayerId playerId;
        private final long revision;
        private Event(final Type type, final Session session) {
            this.type = type;
            this.sessionId = session.id();
            this.playerId = session.playerId();
            this.revision = session.revision();
        }
        /** @return lifecycle type */ public Type type() { return type; }
        /** @return session */ public SessionId sessionId() { return sessionId; }
        /** @return player */ public PlayerId playerId() { return playerId; }
        /** @return resulting revision */ public long revision() { return revision; }
        /** Event types. */ public enum Type { /** Entered. */ ENTERED, /** Target changed. */ TARGET_CHANGED, /** Preference changed. */ PREFERENCE_CHANGED, /** Disconnected. */ DISCONNECTED, /** Reconnected. */ RECONNECTED, /** Restoration requested. */ RESTORING, /** Closed. */ CLOSED }
    }

    /** Non-blocking audit/event sink. */ public interface EventSink { /** Observes an immutable event. */ void publish(Event event); }

    /** Thread-safe bounded lifecycle service containing no platform mutations. */
    public static final class Service {
        private final Policy policy;
        private final TimeSource time;
        private final EventSink events;
        private final Map<PlayerId, Session> sessions = new LinkedHashMap<PlayerId, Session>();
        private final Map<PlayerId, Preferences> preferences = new LinkedHashMap<PlayerId, Preferences>();
        private long staleRejected;
        private long cleanupCount;
        /** Creates a spectator service. */
        public Service(final Policy policy, final TimeSource time, final EventSink events) {
            this.policy = Objects.requireNonNull(policy, "policy");
            this.time = Objects.requireNonNull(time, "time");
            this.events = Objects.requireNonNull(events, "events");
        }
        /** Admits an eliminated, external or staff spectator. Repeated admission is idempotent. */
        public synchronized Session enter(final MatchId matchId, final EntryReason reason,
                                          final PlayerStateSnapshot capturedState) {
            Objects.requireNonNull(matchId, "matchId");
            Objects.requireNonNull(reason, "reason");
            final PlayerStateSnapshot captured = Objects.requireNonNull(capturedState, "capturedState");
            final Session existing = sessions.get(captured.playerId());
            if (existing != null) {
                if (existing.matchId().equals(matchId) && existing.state() != State.CLOSED) { return existing; }
                throw new IllegalStateException("player already has another spectator session");
            }
            if (sessions.size() >= policy.maximumSessions()) { throw new IllegalStateException("spectator capacity reached"); }
            if (reason == EntryReason.EXTERNAL && !policy.externalAdmission()) {
                throw new SecurityException("external spectator admission disabled");
            }
            final Preferences selected = preferences.containsKey(captured.playerId())
                    ? preferences.get(captured.playerId()) : new Preferences(5, false, true, false);
            final Session created = new Session(SessionId.random(), captured.playerId(), matchId,
                    reason, captured, selected, State.ACTIVE, null, 0L, null);
            sessions.put(created.playerId(), created);
            events.publish(new Event(Event.Type.ENTERED, created));
            return created;
        }
        /** Updates allowed persistent preferences using a revision fence. */
        public synchronized Session preferences(final PlayerId playerId, final long revision,
                                                final Preferences updated) {
            final Session current = requireActive(playerId, revision);
            final Session changed = current.change(Objects.requireNonNull(updated, "updated"),
                    current.state(), current.target().orElse(null), current.disconnectedAt().orElse(null));
            sessions.put(playerId, changed);
            preferences.put(playerId, updated);
            events.publish(new Event(Event.Type.PREFERENCE_CHANGED, changed));
            return changed;
        }
        /** Selects a valid living, visible and consented target in the same match. */
        public synchronized Session target(final PlayerId spectator, final long revision,
                                           final Target target) {
            final Session current = requireActive(spectator, revision);
            final Target checked = Objects.requireNonNull(target, "target");
            if (!checked.matchId().equals(current.matchId()) || !checked.eligible()
                    || checked.playerId().equals(spectator)) {
                throw new IllegalArgumentException("invalid spectator target");
            }
            final Session changed = current.change(current.preferences(), current.state(),
                    checked.playerId(), null);
            sessions.put(spectator, changed);
            events.publish(new Event(Event.Type.TARGET_CHANGED, changed));
            return changed;
        }
        /** Navigates deterministically to the next or previous eligible target. */
        public synchronized Session navigate(final PlayerId spectator, final long revision,
                                             final Collection<Target> targets, final boolean next) {
            final Session current = requireActive(spectator, revision);
            final List<Target> eligible = eligible(current, targets);
            if (eligible.isEmpty()) { throw new IllegalStateException("no eligible spectator target"); }
            int index = -1;
            if (current.target().isPresent()) {
                for (int value = 0; value < eligible.size(); value++) {
                    if (eligible.get(value).playerId().equals(current.target().get())) { index = value;
                    break;
                    }
                }
            }
            final int selected = next ? (index + 1) % eligible.size()
                    : (index <= 0 ? eligible.size() - 1 : index - 1);
            return target(spectator, revision, eligible.get(selected));
        }
        /** Clears an unavailable target without closing the spectator session. */
        public synchronized Session targetUnavailable(final PlayerId spectator, final long revision,
                                                      final PlayerId unavailable) {
            final Session current = requireActive(spectator, revision);
            if (!current.target().isPresent() || !current.target().get().equals(unavailable)) { return current; }
            final Session changed = current.change(current.preferences(), current.state(), null, null);
            sessions.put(spectator, changed);
            events.publish(new Event(Event.Type.TARGET_CHANGED, changed));
            return changed;
        }
        /** Records disconnect while retaining recoverable state. */
        public synchronized Session disconnect(final PlayerId playerId, final long revision) {
            final Session current = requireActive(playerId, revision);
            final Session changed = current.change(current.preferences(), State.DISCONNECTED,
                    current.target().orElse(null), time.now());
            sessions.put(playerId, changed);
            events.publish(new Event(Event.Type.DISCONNECTED, changed));
            return changed;
        }
        /** Reconnects within grace and clears an invalid target supplied by current target facts. */
        public synchronized Session reconnect(final PlayerId playerId, final long revision,
                                              final Collection<Target> currentTargets) {
            final Session current = require(playerId);
            if (current.revision() != revision || current.state() != State.DISCONNECTED
                    || time.now().isAfter(current.disconnectedAt().get().plus(policy.reconnectGrace()))) {
                staleRejected++;
                throw new IllegalStateException("spectator reconnect is stale or expired");
            }
            PlayerId target = null;
            if (current.target().isPresent()) {
                for (Target candidate : eligible(current, currentTargets)) {
                    if (candidate.playerId().equals(current.target().get())) { target = candidate.playerId();
                    break;
                    }
                }
            }
            final Session changed = current.change(current.preferences(), State.ACTIVE, target, null);
            sessions.put(playerId, changed);
            events.publish(new Event(Event.Type.RECONNECTED, changed));
            return changed;
        }
        /** Begins exact state restoration once. */
        public synchronized Restoration leave(final PlayerId playerId, final long revision) {
            final Session current = require(playerId);
            if (current.revision() != revision || current.state() == State.RESTORING || current.state() == State.CLOSED) {
                staleRejected++;
                throw new IllegalStateException("spectator leave is stale or duplicate");
            }
            final Session changed = current.change(current.preferences(), State.RESTORING, null, null);
            sessions.put(playerId, changed);
            events.publish(new Event(Event.Type.RESTORING, changed));
            return new Restoration(changed.id(), changed.capturedState(), changed.revision());
        }
        /** Confirms owner-thread restoration and removes all target-bound state. */
        public synchronized boolean restored(final PlayerId playerId, final SessionId id,
                                             final long revision) {
            final Session current = sessions.get(Objects.requireNonNull(playerId, "playerId"));
            if (current == null || !current.id().equals(id) || current.revision() != revision
                    || current.state() != State.RESTORING) { staleRejected++;
                    return false;
                    }
            final Session closed = current.change(current.preferences(), State.CLOSED, null, null);
            sessions.remove(playerId);
            events.publish(new Event(Event.Type.CLOSED, closed));
            cleanupCount++;
            return true;
        }
        /** Begins restoration for every session bound to a completed match. */
        public synchronized List<Restoration> matchEnded(final MatchId matchId) {
            final List<PlayerId> players = new ArrayList<PlayerId>();
            for (Session session : sessions.values()) {
                if (session.matchId().equals(Objects.requireNonNull(matchId, "matchId"))) { players.add(session.playerId()); }
            }
            final List<Restoration> result = new ArrayList<Restoration>();
            for (PlayerId player : players) { result.add(leave(player, sessions.get(player).revision())); }
            return Collections.unmodifiableList(result);
        }
        /** Expires disconnected sessions into restoration intents. */
        public synchronized List<Restoration> cleanup() {
            final List<PlayerId> expired = new ArrayList<PlayerId>();
            for (Session session : sessions.values()) {
                if (session.state() == State.DISCONNECTED
                        && time.now().isAfter(session.disconnectedAt().get().plus(policy.reconnectGrace()))) {
                    expired.add(session.playerId());
                }
            }
            final List<Restoration> result = new ArrayList<Restoration>();
            for (PlayerId player : expired) { result.add(leave(player, sessions.get(player).revision())); }
            return Collections.unmodifiableList(result);
        }
        /** @return current session */ public synchronized Optional<Session> session(final PlayerId playerId) { return Optional.ofNullable(sessions.get(Objects.requireNonNull(playerId, "playerId"))); }
        /** @return active session count */ public synchronized int activeSessions() { return sessions.size(); }
        /** @return stale/duplicate rejection count */ public synchronized long staleRejected() { return staleRejected; }
        /** @return completed cleanup count */ public synchronized long cleanupCount() { return cleanupCount; }
        /** @return configured restrictions */ public Restrictions restrictions() { return policy.restrictions(); }
        private Session require(final PlayerId playerId) {
            final Session value = sessions.get(Objects.requireNonNull(playerId, "playerId"));
            if (value == null) { throw new IllegalArgumentException("unknown spectator session"); }
            return value;
        }
        private Session requireActive(final PlayerId playerId, final long revision) {
            final Session current = require(playerId);
            if (current.revision() != revision || current.state() != State.ACTIVE) {
                staleRejected++;
                throw new IllegalStateException("spectator action is stale or inactive");
            }
            return current;
        }
        private static List<Target> eligible(final Session session, final Collection<Target> values) {
            final List<Target> result = new ArrayList<Target>();
            for (Target target : Objects.requireNonNull(values, "targets")) {
                final Target checked = Objects.requireNonNull(target, "target");
                if (checked.matchId().equals(session.matchId()) && checked.eligible()
                        && !checked.playerId().equals(session.playerId())) { result.add(checked); }
            }
            result.sort(Comparator.comparingInt(Target::order).thenComparing(value -> value.playerId().toString()));
            return result;
        }
    }
}
