package io.zartra.bedwars.api.diagnostic;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Secret-safe diagnostic contribution and export contracts. */
public final class Diagnostics {
    private Diagnostics() { throw new AssertionError("No instances"); }
    /** Sensitivity classification; only PUBLIC fields may enter standard exports. */
    public enum Classification {
        /** Explicitly safe operational metadata. */ PUBLIC,
        /** Restricted operational metadata. */ PRIVATE,
        /** Credentials, endpoints or personal data. */ SECRET
    }
    /** Immutable candidate field. */
    public static final class Field implements Comparable<Field> {
        private final DefinitionId id;
        private final String value;
        private final Classification classification;
        /** Creates a candidate field that still requires exporter sanitization. */
        public Field(final DefinitionId id, final String value,
                     final Classification classification) {
            this.id = Objects.requireNonNull(id, "id");
            if (value == null || value.length() > 4096) {
                throw new IllegalArgumentException("diagnostic value is null or too long");
            }
            this.value = value;
            this.classification = Objects.requireNonNull(classification, "classification");
        }
        /** @return field ID */ public DefinitionId id() { return id; }
        /** @return untrusted candidate value */ public String value() { return value; }
        /** @return classification */ public Classification classification() { return classification; }
        @Override public int compareTo(final Field other) { return id.compareTo(other.id); }
    }
    /** Thread-safe bounded provider that performs no blocking I/O. */
    public interface Contributor {
        /** @return stable contributor ID */ DefinitionId id();
        /** @return bounded candidate fields */ List<Field> fields();
    }
    /** Final-boundary sanitizer. */
    public interface Sanitizer {
        /** @return redacted value */ String sanitize(String candidate);
        /** @return whether configured sensitive data remains */ boolean containsSensitiveValue(String candidate);
    }
    /** Immutable deterministic export. */
    public static final class Export {
        private final Instant createdAt;
        private final List<Field> fields;
        /** Creates an already-sanitized export. */
        public Export(final Instant createdAt, final List<Field> fields) {
            this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
            final List<Field> copy = new ArrayList<Field>(Objects.requireNonNull(fields, "fields"));
            if (copy.contains(null)) { throw new IllegalArgumentException("fields cannot contain null"); }
            Collections.sort(copy);
            this.fields = Collections.unmodifiableList(copy);
        }
        /** @return creation instant */ public Instant createdAt() { return createdAt; }
        /** @return sorted public fields */ public List<Field> fields() { return fields; }
    }
}
