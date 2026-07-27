package io.zartra.bedwars.atlas.api;

import java.time.Instant;
import java.util.Objects;

/** Immutable, identity-free case classification and origin metadata (ZBW-ATLAS-003/004). */
public final class AtlasCaseMetadata {
    private final AtlasCaseSource source;
    private final Instant createdAt;
    private final String category;
    private final String sourceReference;
    private final int priority;
    private final int schemaVersion;

    /** Creates validated metadata containing no player name, UUID or free-form evidence payload. */
    public AtlasCaseMetadata(final AtlasCaseSource source, final Instant createdAt,
                             final String category, final String sourceReference,
                             final int priority, final int schemaVersion) {
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority must be between 0 and 100");
        }
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        this.source = Objects.requireNonNull(source, "source");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.category = requireToken(category, "category", 80);
        this.sourceReference = requireToken(sourceReference, "sourceReference", 160);
        this.priority = priority;
        this.schemaVersion = schemaVersion;
    }

    private static String requireToken(final String value, final String name, final int maximum) {
        if (value == null || value.trim().isEmpty() || value.length() > maximum
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(name + " must be an opaque token of at most " + maximum);
        }
        return value;
    }

    /** Returns the source family. */ public AtlasCaseSource source() { return source; }
    /** Returns the immutable creation instant. */ public Instant createdAt() { return createdAt; }
    /** Returns a stable, non-sensitive category token. */ public String category() { return category; }
    /** Returns an opaque source-system reference, never raw report content. */
    public String sourceReference() { return sourceReference; }
    /** Returns queue priority from zero through one hundred. */ public int priority() { return priority; }
    /** Returns the positive serialized contract version. */ public int schemaVersion() { return schemaVersion; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasCaseMetadata)) { return false; }
        final AtlasCaseMetadata that = (AtlasCaseMetadata) other;
        return priority == that.priority && schemaVersion == that.schemaVersion
                && source == that.source && createdAt.equals(that.createdAt)
                && category.equals(that.category) && sourceReference.equals(that.sourceReference);
    }
    @Override public int hashCode() {
        return Objects.hash(source, createdAt, category, sourceReference, priority, schemaVersion);
    }
}
