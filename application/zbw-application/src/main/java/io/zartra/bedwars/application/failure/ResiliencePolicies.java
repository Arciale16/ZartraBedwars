package io.zartra.bedwars.application.failure;

import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.time.MonotonicTimeSource;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Deterministic bounded retry and circuit-breaker policies for idempotent operations. */
public final class ResiliencePolicies {
    private ResiliencePolicies() { throw new AssertionError("No instances"); }
    /** Immutable exponential retry limits without implicit sleeping. */
    public static final class RetryPolicy {
        private final int maximumAttempts;
        private final Duration initialDelay;
        private final Duration maximumDelay;
        /** Creates a policy capped at sixteen attempts. */
        public RetryPolicy(final int maximumAttempts, final Duration initialDelay,
                           final Duration maximumDelay) {
            if (maximumAttempts < 1 || maximumAttempts > 16) {
                throw new IllegalArgumentException("maximumAttempts must be 1 through 16");
            }
            positive(initialDelay, "initialDelay");
            positive(maximumDelay, "maximumDelay");
            if (initialDelay.compareTo(maximumDelay) > 0) {
                throw new IllegalArgumentException("initialDelay cannot exceed maximumDelay");
            }
            this.maximumAttempts = maximumAttempts;
            this.initialDelay = initialDelay;
            this.maximumDelay = maximumDelay;
        }
        /** @return total attempt cap */ public int maximumAttempts() { return maximumAttempts; }
        /** @return whether another retry is allowed */
        public boolean permits(final int completedAttempts, final FailureKind kind,
                               final boolean idempotent) {
            Objects.requireNonNull(kind, "kind");
            return idempotent && completedAttempts >= 1 && completedAttempts < maximumAttempts
                    && (kind == FailureKind.UNAVAILABLE || kind == FailureKind.TIMEOUT
                    || kind == FailureKind.CONFLICT);
        }
        /** @return bounded exponential delay */
        public Duration delayAfter(final int completedAttempts) {
            if (completedAttempts < 1) {
                throw new IllegalArgumentException("completedAttempts must be positive");
            }
            Duration delay = initialDelay;
            for (int index = 1; index < completedAttempts
                    && delay.compareTo(maximumDelay) < 0; index++) {
                try { delay = delay.multipliedBy(2L); }
                catch (ArithmeticException exception) { return maximumDelay; }
            }
            return delay.compareTo(maximumDelay) > 0 ? maximumDelay : delay;
        }
    }
    /** Thread-safe closed/open/half-open circuit with one recovery probe. */
    public static final class CircuitBreaker {
        /** Circuit state. */
        public enum State {
            /** Calls admitted. */ CLOSED,
            /** Calls fail fast. */ OPEN,
            /** One recovery probe admitted. */ HALF_OPEN
        }
        private final int failureThreshold;
        private final long openNanos;
        private final MonotonicTimeSource clock;
        private final AtomicReference<State> state = new AtomicReference<State>(State.CLOSED);
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicLong openedAt = new AtomicLong();
        private final AtomicBoolean probe = new AtomicBoolean();
        /** Creates a breaker. */
        public CircuitBreaker(final int failureThreshold, final Duration openDuration,
                              final MonotonicTimeSource clock) {
            if (failureThreshold < 1) { throw new IllegalArgumentException("threshold must be positive"); }
            positive(openDuration, "openDuration");
            this.failureThreshold = failureThreshold;
            this.openNanos = nanos(openDuration);
            this.clock = Objects.requireNonNull(clock, "clock");
        }
        /** @return whether one call may begin */
        public boolean tryAcquire() {
            final State current = state.get();
            if (current == State.CLOSED) { return true; }
            if (current == State.OPEN) {
                if (clock.readNanos() - openedAt.get() < openNanos
                        || !state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    return false;
                }
            }
            return probe.compareAndSet(false, true);
        }
        /** Closes after successful admitted work. */
        public void onSuccess() {
            failures.set(0);
            probe.set(false);
            state.set(State.CLOSED);
        }
        /** Opens after threshold or a failed half-open probe. */
        public void onFailure() {
            probe.set(false);
            if (state.get() == State.HALF_OPEN || failures.incrementAndGet() >= failureThreshold) {
                openedAt.set(clock.readNanos());
                state.set(State.OPEN);
            }
        }
        /** @return state */ public State state() { return state.get(); }
        /** @return consecutive failures */ public int consecutiveFailures() { return failures.get(); }
    }
    private static void positive(final Duration duration, final String label) {
        Objects.requireNonNull(duration, label);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }
    private static long nanos(final Duration duration) {
        try { return duration.toNanos(); }
        catch (ArithmeticException exception) { return Long.MAX_VALUE; }
    }
}
