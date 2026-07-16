package io.zartra.bedwars.game.addon;

import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Exactly-once delayed-leave state machine with configurable cancellation signals. */
public final class LeaveDelayPolicy {
    private final Map<PlayerId, Session> sessions = new HashMap<PlayerId, Session>();

    /** Starts a delay, or returns the existing live session without extending it. */
    public synchronized Session begin(final PlayerId playerId, final State state,
                                      final Instant now, final Rules rules) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(rules, "rules");
        final Session existing = sessions.get(playerId);
        if (existing != null && existing.status == Status.PENDING) { return existing; }
        final Duration delay = rules.delay(state);
        final Session created = new Session(playerId, state, now, now.plus(delay), Status.PENDING);
        sessions.put(playerId, created);
        return created;
    }

    /** Cancels a pending delay only when its configured signal applies. */
    public synchronized Optional<Session> signal(final PlayerId playerId, final Signal signal,
                                                 final Rules rules) {
        final Session current = sessions.get(Objects.requireNonNull(playerId, "playerId"));
        if (current == null || current.status != Status.PENDING
                || !rules.cancels(Objects.requireNonNull(signal, "signal"))) {
            return Optional.empty();
        }
        final Session cancelled = current.withStatus(Status.CANCELLED);
        sessions.put(playerId, cancelled);
        return Optional.of(cancelled);
    }

    /** Completes at most once when the deadline has elapsed. */
    public synchronized Optional<Session> tick(final PlayerId playerId, final Instant now) {
        final Session current = sessions.get(Objects.requireNonNull(playerId, "playerId"));
        if (current == null || current.status != Status.PENDING
                || Objects.requireNonNull(now, "now").isBefore(current.deadline)) {
            return Optional.empty();
        }
        final Session completed = current.withStatus(Status.COMPLETED);
        sessions.put(playerId, completed);
        return Optional.of(completed);
    }

    /** Performs a permission-checked immediate leave and records a terminal state. */
    public synchronized Session bypass(final PlayerId playerId, final State state,
                                       final Instant now, final boolean authorized) {
        if (!authorized) { throw new SecurityException("leave-delay bypass denied"); }
        final Session completed = new Session(Objects.requireNonNull(playerId, "playerId"),
                Objects.requireNonNull(state, "state"), Objects.requireNonNull(now, "now"),
                now, Status.BYPASSED);
        sessions.put(playerId, completed);
        return completed;
    }

    /** @return current delay snapshot for diagnostics */
    public synchronized Optional<Session> inspect(final PlayerId playerId) {
        return Optional.ofNullable(sessions.get(Objects.requireNonNull(playerId, "playerId")));
    }

    /** Removes a terminal session after player restoration and feedback cleanup. */
    public synchronized boolean clear(final PlayerId playerId) {
        final Session session = sessions.get(Objects.requireNonNull(playerId, "playerId"));
        if (session == null || session.status == Status.PENDING) { return false; }
        sessions.remove(playerId);
        return true;
    }

    /** Immutable per-state delays and cancellation mask. */
    public static final class Rules {
        private final Map<State, Duration> delays;
        private final java.util.Set<Signal> cancellations;
        /** Creates rules requiring a positive bounded delay for every state. */
        public Rules(final Map<State, Duration> delays,
                     final java.util.Set<Signal> cancellations) {
            if (delays == null || delays.size() != State.values().length
                    || cancellations == null || cancellations.contains(null)) {
                throw new IllegalArgumentException("leave-delay rules are incomplete");
            }
            this.delays = new java.util.EnumMap<State, Duration>(State.class);
            for (State state : State.values()) {
                final Duration delay = delays.get(state);
                if (delay == null || delay.isNegative() || delay.compareTo(Duration.ofMinutes(5)) > 0) {
                    throw new IllegalArgumentException("leave delay is invalid");
                }
                this.delays.put(state, delay);
            }
            this.cancellations = java.util.Collections.unmodifiableSet(
                    new java.util.HashSet<Signal>(cancellations));
        }
        private Duration delay(final State state) { return delays.get(state); }
        private boolean cancels(final Signal signal) { return cancellations.contains(signal); }
    }

    /** Eligible player states. */
    public enum State { /** Waiting arena. */ WAITING, /** Active match. */ PLAYING, /** Spectating. */ SPECTATING }
    /** Cancellation signals translated by the primary Paper adapter. */
    public enum Signal { /** Movement. */ MOVEMENT, /** Damage received. */ DAMAGE, /** Combat activity. */ COMBAT, /** Death. */ DEATH, /** State transition. */ STATE_CHANGE, /** Conflicting command. */ COMMAND }
    /** Delay lifecycle status. */
    public enum Status { /** Counting down. */ PENDING, /** Cancelled by policy. */ CANCELLED, /** Deadline completed. */ COMPLETED, /** Authorized immediate exit. */ BYPASSED }

    /** Immutable remaining-time and terminal projection model. */
    public static final class Session {
        private final PlayerId playerId;
        private final State state;
        private final Instant startedAt;
        private final Instant deadline;
        private final Status status;
        private Session(final PlayerId playerId, final State state, final Instant startedAt,
                        final Instant deadline, final Status status) {
            this.playerId = playerId;
            this.state = state;
            this.startedAt = startedAt;
            this.deadline = deadline;
            this.status = status;
        }
        private Session withStatus(final Status value) { return new Session(playerId, state, startedAt, deadline, value); }
        /** @return player identity */ public PlayerId playerId() { return playerId; }
        /** @return source game state */ public State state() { return state; }
        /** @return terminal or pending status */ public Status status() { return status; }
        /** @return deadline */ public Instant deadline() { return deadline; }
        /** @return ceiling seconds remaining, never negative */
        public long remainingSeconds(final Instant now) {
            if (status != Status.PENDING || !now.isBefore(deadline)) { return 0L; }
            final long millis = Duration.between(now, deadline).toMillis();
            return (millis + 999L) / 1000L;
        }
    }
}
