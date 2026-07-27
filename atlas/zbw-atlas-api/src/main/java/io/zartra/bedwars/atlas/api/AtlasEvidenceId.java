package io.zartra.bedwars.atlas.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable evidence-reference identity; it does not identify or own replay payloads. */
public final class AtlasEvidenceId implements Comparable<AtlasEvidenceId> {
    private final UUID value;
    private AtlasEvidenceId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
    /** Creates a random evidence identity. */ public static AtlasEvidenceId random() { return new AtlasEvidenceId(UUID.randomUUID()); }
    /** Restores canonical UUID text. */ public static AtlasEvidenceId parse(final String value) {
        return new AtlasEvidenceId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }
    /** Creates an identity from a UUID. */ public static AtlasEvidenceId of(final UUID value) { return new AtlasEvidenceId(value); }
    /** Returns the UUID value. */ public UUID value() { return value; }
    @Override public int compareTo(final AtlasEvidenceId other) { return value.compareTo(other.value); }
    @Override public boolean equals(final Object other) {
        return other instanceof AtlasEvidenceId && value.equals(((AtlasEvidenceId) other).value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
