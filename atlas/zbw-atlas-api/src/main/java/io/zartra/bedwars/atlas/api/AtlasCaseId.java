package io.zartra.bedwars.atlas.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable collision-resistant Atlas case identity (ZBW-ATLAS-003, ZBW-ARC-009). */
public final class AtlasCaseId implements Comparable<AtlasCaseId> {
    private final UUID value;

    private AtlasCaseId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
    /** Creates a random case identity. */ public static AtlasCaseId random() { return new AtlasCaseId(UUID.randomUUID()); }
    /** Restores canonical UUID text. */ public static AtlasCaseId parse(final String value) {
        return new AtlasCaseId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }
    /** Creates an identity from a UUID. */ public static AtlasCaseId of(final UUID value) { return new AtlasCaseId(value); }
    /** Returns the UUID value. */ public UUID value() { return value; }
    @Override public int compareTo(final AtlasCaseId other) { return value.compareTo(other.value); }
    @Override public boolean equals(final Object other) {
        return other instanceof AtlasCaseId && value.equals(((AtlasCaseId) other).value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
