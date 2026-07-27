package io.zartra.bedwars.atlas.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable reviewer identity, never used as a community-facing alias (ZBW-ATLAS-004/007). */
public final class AtlasReviewerId implements Comparable<AtlasReviewerId> {
    private final UUID value;
    private AtlasReviewerId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
    /** Creates a random reviewer identity. */ public static AtlasReviewerId random() { return new AtlasReviewerId(UUID.randomUUID()); }
    /** Restores canonical UUID text. */ public static AtlasReviewerId parse(final String value) {
        return new AtlasReviewerId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }
    /** Creates an identity from a UUID. */ public static AtlasReviewerId of(final UUID value) { return new AtlasReviewerId(value); }
    /** Returns the UUID value for authorized internal use. */ public UUID value() { return value; }
    @Override public int compareTo(final AtlasReviewerId other) { return value.compareTo(other.value); }
    @Override public boolean equals(final Object other) {
        return other instanceof AtlasReviewerId && value.equals(((AtlasReviewerId) other).value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
