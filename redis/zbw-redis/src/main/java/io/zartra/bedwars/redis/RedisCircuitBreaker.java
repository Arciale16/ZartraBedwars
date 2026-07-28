package io.zartra.bedwars.redis;


import java.time.Clock;

import java.time.Duration;

import java.util.Objects;


/** Small synchronized fail-fast circuit breaker for Redis commands. */
public final class RedisCircuitBreaker {
    /** Circuit states. */ public enum State { CLOSED, OPEN, HALF_OPEN }
    private final Clock clock;

    private final int failureThreshold;

    private final Duration resetAfter;

    private int failures;

    private long openedAt;

    private State state = State.CLOSED;

    /** Creates a bounded breaker. */
    public RedisCircuitBreaker(final Clock clock, final int failureThreshold, final Duration resetAfter) {
        this.clock = Objects.requireNonNull(clock, "clock");

        this.resetAfter = Objects.requireNonNull(resetAfter, "resetAfter");

        if (failureThreshold < 1 || resetAfter.isNegative() || resetAfter.isZero()) { throw new IllegalArgumentException("invalid breaker policy");
 }
        this.failureThreshold = failureThreshold;

    }
    /** Returns whether a command may start. */
    public synchronized boolean allowRequest() {
        if (state == State.OPEN && clock.millis() - openedAt >= resetAfter.toMillis()) { state = State.HALF_OPEN;
 }
        return state != State.OPEN;

    }
    /** Records successful recovery. */ public synchronized void success() { failures = 0;
 state = State.CLOSED;
 }
    /** Records a failure and opens at threshold. */
    public synchronized void failure() {
        failures++;

        if (failures >= failureThreshold) { state = State.OPEN;
 openedAt = clock.millis();
 }
    }
    /** Returns current state. */ public synchronized State state() { return state;
 }
}
