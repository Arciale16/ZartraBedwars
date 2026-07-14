package io.zartra.bedwars.api.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable Semantic Versioning 2.0.0 value. */
public final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern FORMAT = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?");
    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> preRelease;
    private final String build;

    private SemanticVersion(final int major, final int minor, final int patch,
                            final List<String> preRelease, final String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = Collections.unmodifiableList(new ArrayList<String>(preRelease));
        this.build = build;
    }

    /**
     * Parses a SemVer 2.0.0 representation.
     *
     * @param value version text
     * @return parsed version
     * @throws VersionFormatException when malformed or outside integer bounds
     */
    public static SemanticVersion parse(final String value) {
        if (value == null) {
            throw new VersionFormatException("Semantic version must not be null");
        }
        final Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new VersionFormatException("Semantic version is malformed");
        }
        final List<String> prerelease = splitIdentifiers(matcher.group(4));
        for (String identifier : prerelease) {
            if (identifier.matches("[0-9]+") && identifier.length() > 1 && identifier.charAt(0) == '0') {
                throw new VersionFormatException("Numeric prerelease identifier has a leading zero");
            }
        }
        try {
            return new SemanticVersion(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                    prerelease, matcher.group(5));
        } catch (NumberFormatException exception) {
            throw new VersionFormatException("Semantic version component exceeds integer bounds", exception);
        }
    }

    private static List<String> splitIdentifiers(final String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        final List<String> identifiers = new ArrayList<String>();
        Collections.addAll(identifiers, value.split("\\."));
        return identifiers;
    }

    /** @return major component */
    public int major() { return major; }
    /** @return minor component */
    public int minor() { return minor; }
    /** @return patch component */
    public int patch() { return patch; }
    /** @return immutable prerelease identifiers */
    public List<String> preRelease() { return preRelease; }
    /** @return build metadata, or an empty string when absent */
    public String build() { return build == null ? "" : build; }
    /** @return whether this version is a prerelease */
    public boolean isPreRelease() { return !preRelease.isEmpty(); }

    @Override
    public int compareTo(final SemanticVersion other) {
        Objects.requireNonNull(other, "other");
        int comparison = Integer.compare(major, other.major);
        if (comparison == 0) { comparison = Integer.compare(minor, other.minor); }
        if (comparison == 0) { comparison = Integer.compare(patch, other.patch); }
        if (comparison != 0) { return comparison; }
        if (preRelease.isEmpty() || other.preRelease.isEmpty()) {
            if (preRelease.isEmpty() && other.preRelease.isEmpty()) { return 0; }
            return preRelease.isEmpty() ? 1 : -1;
        }
        final int common = Math.min(preRelease.size(), other.preRelease.size());
        for (int index = 0; index < common; index++) {
            comparison = compareIdentifier(preRelease.get(index), other.preRelease.get(index));
            if (comparison != 0) { return comparison; }
        }
        return Integer.compare(preRelease.size(), other.preRelease.size());
    }

    private static int compareIdentifier(final String left, final String right) {
        final boolean leftNumeric = left.matches("[0-9]+");
        final boolean rightNumeric = right.matches("[0-9]+");
        if (leftNumeric && rightNumeric) {
            if (left.length() != right.length()) {
                return Integer.compare(left.length(), right.length());
            }
            return left.compareTo(right);
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    @Override
    public String toString() {
        final StringBuilder value = new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
        if (!preRelease.isEmpty()) { value.append('-').append(String.join(".", preRelease)); }
        if (build != null) { value.append('+').append(build); }
        return value.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof SemanticVersion)) { return false; }
        final SemanticVersion that = (SemanticVersion) other;
        return major == that.major && minor == that.minor && patch == that.patch
                && preRelease.equals(that.preRelease) && Objects.equals(build, that.build);
    }

    @Override
    public int hashCode() { return Objects.hash(major, minor, patch, preRelease, build); }

    /** Typed parse failure for a semantic version boundary. */
    public static final class VersionFormatException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        /** @param message safe diagnostic text */
        public VersionFormatException(final String message) { super(message); }
        /** @param message safe diagnostic text @param cause parse cause */
        public VersionFormatException(final String message, final Throwable cause) { super(message, cause); }
    }
}
