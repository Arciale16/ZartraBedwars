package io.zartra.bedwars.storage.api;

import java.util.Objects;

/** Immutable schema migration descriptor with a verified SHA-256 checksum. */
public final class Migration implements Comparable<Migration> {
    private final int version;
    private final String description;
    private final String checksum;
    private final boolean unsafeDdl;

    private Migration(final int version, final String description, final String checksum,
                      final boolean unsafeDdl) {
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        if (description == null || !description.matches("[a-z][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("description must be a stable lowercase token");
        }
        if (checksum == null || !checksum.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("checksum must be lowercase SHA-256");
        }
        this.version = version;
        this.description = description;
        this.checksum = checksum;
        this.unsafeDdl = unsafeDdl;
    }

    /** @return validated migration descriptor */
    public static Migration of(final int version, final String description, final String checksum,
                               final boolean unsafeDdl) {
        return new Migration(version, description, checksum, unsafeDdl);
    }
    /** @return monotonically increasing schema version */ public int version() { return version; }
    /** @return stable description */ public String description() { return description; }
    /** @return canonical content checksum */ public String checksum() { return checksum; }
    /** @return whether restore-based rollback evidence is mandatory */ public boolean unsafeDdl() { return unsafeDdl; }
    @Override public int compareTo(final Migration other) { return Integer.compare(version, other.version); }
    @Override public int hashCode() { return Objects.hash(version, description, checksum, unsafeDdl); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof Migration)) { return false; }
        final Migration that = (Migration) other;
        return version == that.version && unsafeDdl == that.unsafeDdl
                && description.equals(that.description) && checksum.equals(that.checksum);
    }
}
