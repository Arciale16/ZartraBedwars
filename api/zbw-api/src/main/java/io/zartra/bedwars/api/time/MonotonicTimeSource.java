package io.zartra.bedwars.api.time;

/**
 * Thread-safe monotonic time source for elapsed-time and deadline measurement.
 *
 * <p>Values have no relationship to UTC and may only be subtracted when produced by the same
 * source instance.</p>
 */
public interface MonotonicTimeSource {
    /** @return monotonic nanosecond reading */
    long readNanos();

    /** Process-local monotonic clock. */
    enum SystemMonotonicTimeSource implements MonotonicTimeSource {
        /** Shared stateless instance. */
        INSTANCE;
        @Override public long readNanos() { return System.nanoTime(); }
    }
}
