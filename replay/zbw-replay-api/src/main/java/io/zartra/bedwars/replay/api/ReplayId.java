package io.zartra.bedwars.replay.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable collision-resistant replay identity (ZBW-REPLAY-001, ZBW-ARC-009). */
public final class ReplayId implements Comparable<ReplayId> {
    private final UUID value;

    private ReplayId(final UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    /** Creates a new random replay identity. */
    public static ReplayId random() { return new ReplayId(UUID.randomUUID()); }

    /** Restores a replay identity from its canonical UUID text. */
    public static ReplayId parse(final String value) {
        return new ReplayId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }

    /** Creates a replay identity from an existing UUID. */
    public static ReplayId of(final UUID value) { return new ReplayId(value); }

    /** Returns the UUID value. */
    public UUID value() { return value; }

    @Override public int compareTo(final ReplayId other) { return value.compareTo(other.value); }
    @Override public boolean equals(final Object other) {
        return other instanceof ReplayId && value.equals(((ReplayId) other).value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
