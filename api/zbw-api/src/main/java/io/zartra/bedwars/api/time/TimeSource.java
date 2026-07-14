package io.zartra.bedwars.api.time;

import java.time.Instant;
import java.util.Objects;

/** Thread-safe source of UTC wall-clock instants. */
public interface TimeSource {
    /** @return current UTC instant */
    Instant now();

    /**
     * Process wall-clock implementation. Use only at composition boundaries; inject the interface
     * elsewhere so tests and replayable policies remain deterministic.
     */
    enum SystemTimeSource implements TimeSource {
        /** Shared stateless instance. */
        INSTANCE;
        @Override public Instant now() { return Instant.now(); }
    }

    /** Immutable deterministic time source for tests, migrations and snapshot evaluation. */
    final class FixedTimeSource implements TimeSource {
        private final Instant instant;
        private FixedTimeSource(final Instant instant) { this.instant = Objects.requireNonNull(instant, "instant"); }
        /** @return a source that always returns {@code instant} */
        public static FixedTimeSource at(final Instant instant) { return new FixedTimeSource(instant); }
        @Override public Instant now() { return instant; }
    }
}
