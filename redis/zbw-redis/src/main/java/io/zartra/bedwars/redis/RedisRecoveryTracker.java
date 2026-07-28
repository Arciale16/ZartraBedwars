package io.zartra.bedwars.redis;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Tracks cache-loss recovery against the five-minute M19 coordination RTO. */
public final class RedisRecoveryTracker {
    /** Required M19 coordination recovery objective. */
    public static final Duration RTO = Duration.ofMinutes(5);

    /** Recovery states; none authorizes a durable business mutation. */
    public enum State {
        HEALTHY,
        DEGRADED,
        REBUILDING,
        RECOVERED
    }

    private final Clock clock;
    private State state = State.HEALTHY;
    private Instant failureAt;
    private Instant recoveredAt;

    /** Creates a tracker driven by an injected monotonic wall clock. */
    public RedisRecoveryTracker(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Records Redis loss or partition and pauses unsafe cross-node work. */
    public synchronized void unavailable() {
        if (state == State.HEALTHY || state == State.RECOVERED) {
            failureAt = clock.instant();
            recoveredAt = null;
        }
        state = State.DEGRADED;
    }

    /** Records that metadata is being rebuilt from its durable owner. */
    public synchronized void rebuilding() {
        if (failureAt == null) {
            throw new IllegalStateException("rebuild requires an observed failure");
        }
        state = State.REBUILDING;
    }

    /** Records successful owner-backed rebuild and consumer restart. */
    public synchronized void recovered() {
        if (state != State.REBUILDING) {
            throw new IllegalStateException("recovery requires rebuild completion");
        }
        recoveredAt = clock.instant();
        state = State.RECOVERED;
    }

    /** @return current recovery state */ public synchronized State state() { return state; }

    /** @return elapsed outage duration, bounded by the current observation time */
    public synchronized Duration elapsed() {
        if (failureAt == null) {
            return Duration.ZERO;
        }
        final Instant end = recoveredAt == null ? clock.instant() : recoveredAt;
        return Duration.between(failureAt, end);
    }

    /** @return true only for a completed rebuild within the five-minute objective */
    public synchronized boolean metRto() {
        return state == State.RECOVERED && elapsed().compareTo(RTO) <= 0;
    }
}
