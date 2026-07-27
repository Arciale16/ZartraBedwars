package io.zartra.bedwars.atlas.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable collision-resistant Atlas review identity (ZBW-ATLAS-005). */
public final class AtlasReviewId implements Comparable<AtlasReviewId> {
    private final UUID value;
    private AtlasReviewId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
    /** Creates a random review identity. */ public static AtlasReviewId random() { return new AtlasReviewId(UUID.randomUUID()); }
    /** Restores canonical UUID text. */ public static AtlasReviewId parse(final String value) {
        return new AtlasReviewId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }
    /** Creates an identity from a UUID. */ public static AtlasReviewId of(final UUID value) { return new AtlasReviewId(value); }
    /** Returns the UUID value. */ public UUID value() { return value; }
    @Override public int compareTo(final AtlasReviewId other) { return value.compareTo(other.value); }
    @Override public boolean equals(final Object other) {
        return other instanceof AtlasReviewId && value.equals(((AtlasReviewId) other).value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
