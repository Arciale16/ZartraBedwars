package io.zartra.bedwars.api.version;

import java.util.Objects;

/** Immutable half-open semantic version range {@code [minimum, maximumExclusive)}. */
public final class VersionRange {
    private final SemanticVersion minimum;
    private final SemanticVersion maximumExclusive;

    private VersionRange(final SemanticVersion minimum, final SemanticVersion maximumExclusive) {
        if (minimum.compareTo(maximumExclusive) >= 0) {
            throw new IllegalArgumentException("minimum must be lower than maximumExclusive");
        }
        this.minimum = minimum;
        this.maximumExclusive = maximumExclusive;
    }

    /** @return a half-open range @throws NullPointerException if a boundary is null */
    public static VersionRange between(final SemanticVersion minimum, final SemanticVersion maximumExclusive) {
        return new VersionRange(Objects.requireNonNull(minimum, "minimum"),
                Objects.requireNonNull(maximumExclusive, "maximumExclusive"));
    }

    /** @return parsed range in canonical {@code [min,max)} form */
    public static VersionRange parse(final String value) {
        if (value == null || !value.startsWith("[") || !value.endsWith(")")) {
            throw new SemanticVersion.VersionFormatException("Version range must use [min,max) form");
        }
        final String[] bounds = value.substring(1, value.length() - 1).split(",", -1);
        if (bounds.length != 2) {
            throw new SemanticVersion.VersionFormatException("Version range must contain two boundaries");
        }
        return between(SemanticVersion.parse(bounds[0]), SemanticVersion.parse(bounds[1]));
    }

    /** @return whether the supplied version is inside the range */
    public boolean contains(final SemanticVersion value) {
        Objects.requireNonNull(value, "value");
        return minimum.compareTo(value) <= 0 && value.compareTo(maximumExclusive) < 0;
    }

    /** @return inclusive minimum */
    public SemanticVersion minimum() { return minimum; }
    /** @return exclusive maximum */
    public SemanticVersion maximumExclusive() { return maximumExclusive; }
    @Override public String toString() { return '[' + minimum.toString() + ',' + maximumExclusive.toString() + ')'; }
    @Override public int hashCode() { return Objects.hash(minimum, maximumExclusive); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof VersionRange)) { return false; }
        final VersionRange that = (VersionRange) other;
        return minimum.equals(that.minimum) && maximumExclusive.equals(that.maximumExclusive);
    }
}
