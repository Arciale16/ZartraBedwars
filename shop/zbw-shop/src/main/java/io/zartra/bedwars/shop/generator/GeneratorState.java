package io.zartra.bedwars.shop.generator;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable observable state of one generator runtime. */
public final class GeneratorState {
    /** Lifecycle owned by the generator application, not the M08 match state machine. */
    public enum Lifecycle { STOPPED, RUNNING, CLEANED }
    private final Lifecycle lifecycle;
    private final long sequence;
    private final int pendingUnits;
    private final Instant nextGeneration;
    GeneratorState(final Lifecycle lifecycle, final long sequence, final int pendingUnits,
                   final Instant nextGeneration) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.sequence = sequence;
        this.pendingUnits = pendingUnits;
        this.nextGeneration = nextGeneration;
    }
    /** @return lifecycle */ public Lifecycle lifecycle() { return lifecycle; }
    /** @return last allocated generation sequence */ public long sequence() { return sequence; }
    /** @return units awaiting confirmed delivery */ public int pendingUnits() { return pendingUnits; }
    /** @return next logical generation time when running */ public Optional<Instant> nextGeneration() { return Optional.ofNullable(nextGeneration); }
}
