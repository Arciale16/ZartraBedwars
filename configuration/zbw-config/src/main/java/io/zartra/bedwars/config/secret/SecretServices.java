package io.zartra.bedwars.config.secret;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.secret.SecretRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Namespace for injected secret sources, scoped leases and sanitized diagnostic exports. */
public final class SecretServices {
    private SecretServices() { throw new AssertionError("No instances"); }

    /** Secret source adapter; environment and protected-file access live behind this port. */
    public interface SecretSource {
        /** @return the one source class implemented by this adapter */ SecretRef.Source source();
        /** @return newly allocated secret characters, or empty when unavailable */
        Optional<char[]> resolve(String key);
    }

    /** Callback receiving leased secret characters; returning them or retaining them is forbidden. */
    public interface SecretOperation<T> {
        /** @return non-secret derived result */ T apply(char[] secret);
    }

    /** Resolver that owns source priority and never returns secret text directly. */
    public static final class Resolver {
        private final Map<SecretRef.Source, SecretSource> sources;
        /** @param sources unique injected source adapters */
        public Resolver(final Collection<SecretSource> sources) {
            final Map<SecretRef.Source, SecretSource> collected =
                    new EnumMap<SecretRef.Source, SecretSource>(SecretRef.Source.class);
            for (SecretSource source : Objects.requireNonNull(sources, "sources")) {
                final SecretSource checked = Objects.requireNonNull(source, "source");
                if (collected.put(checked.source(), checked) != null) {
                    throw new IllegalArgumentException("Duplicate secret source");
                }
            }
            this.sources = Collections.unmodifiableMap(collected);
        }
        /** @return resolution for the explicitly referenced source */
        public Resolution resolve(final SecretRef reference) {
            Objects.requireNonNull(reference, "reference");
            final SecretSource source = sources.get(reference.source());
            if (source == null) {
                return Resolution.failure(DefinitionId.of("zartra", "secret/source_unavailable"));
            }
            return fromSource(source, reference.key());
        }
        /**
         * Resolves an operator key in approved provider, environment, protected-file order.
         *
         * @return first successful resolution or a stable unavailable result
         */
        public Resolution resolvePreferred(final String key) {
            if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9_./-]{1,127}")) {
                throw new IllegalArgumentException("Invalid secret lookup key");
            }
            for (SecretRef.Source sourceKind : Arrays.asList(SecretRef.Source.PROVIDER,
                    SecretRef.Source.ENVIRONMENT, SecretRef.Source.PROTECTED_FILE)) {
                final SecretSource source = sources.get(sourceKind);
                if (source != null) {
                    final Resolution result = fromSource(source, key);
                    if (result.isResolved()) { return result; }
                }
            }
            return Resolution.failure(DefinitionId.of("zartra", "secret/not_found"));
        }
        private static Resolution fromSource(final SecretSource source, final String key) {
            final Optional<char[]> resolved;
            try { resolved = Objects.requireNonNull(source.resolve(key), "sourceResult"); }
            catch (RuntimeException exception) {
                return Resolution.failure(DefinitionId.of("zartra", "secret/resolution_failed"));
            }
            if (!resolved.isPresent()) {
                return Resolution.failure(DefinitionId.of("zartra", "secret/not_found"));
            }
            final char[] material = Objects.requireNonNull(resolved.get(), "secretMaterial");
            if (material.length == 0) {
                Arrays.fill(material, '\0');
                return Resolution.failure(DefinitionId.of("zartra", "secret/empty"));
            }
            final SecretLease lease = new SecretLease(material);
            Arrays.fill(material, '\0');
            return Resolution.success(lease);
        }
    }

    /** Single-use, closeable lease that clears its private secret copy. */
    public static final class SecretLease implements AutoCloseable {
        private char[] secret;
        private SecretLease(final char[] secret) { this.secret = Arrays.copyOf(secret, secret.length); }
        /**
         * Runs an operation with a fresh temporary copy and clears that copy immediately.
         *
         * @return non-secret derived result
         */
        public synchronized <T> T use(final SecretOperation<T> operation) {
            Objects.requireNonNull(operation, "operation");
            if (secret == null) { throw new IllegalStateException("Secret lease is closed"); }
            final char[] temporary = Arrays.copyOf(secret, secret.length);
            try { return operation.apply(temporary); }
            finally { Arrays.fill(temporary, '\0'); }
        }
        /** Clears the private material; repeated calls are safe. */
        @Override public synchronized void close() {
            if (secret != null) {
                Arrays.fill(secret, '\0');
                secret = null;
            }
        }
        @Override public String toString() { return "SecretLease[REDACTED]"; }
    }

    /** Immutable resolution outcome carrying a lease or a stable secret-free error. */
    public static final class Resolution {
        private final SecretLease lease;
        private final DefinitionId failure;
        private Resolution(final SecretLease lease, final DefinitionId failure) {
            this.lease = lease;
            this.failure = failure;
        }
        private static Resolution success(final SecretLease lease) { return new Resolution(lease, null); }
        private static Resolution failure(final DefinitionId failure) { return new Resolution(null, failure); }
        /** @return whether a lease is available */ public boolean isResolved() { return lease != null; }
        /** @return lease; empty after a failed resolution */ public Optional<SecretLease> lease() { return Optional.ofNullable(lease); }
        /** @return stable failure code */ public Optional<DefinitionId> failure() { return Optional.ofNullable(failure); }
    }

    /** Closeable redactor seeded with exact secret character sequences. */
    public static final class Redactor implements AutoCloseable {
        private final List<char[]> secrets;
        private boolean closed;
        /** @param secrets secret values to remove from diagnostic text */
        public Redactor(final Collection<char[]> secrets) {
            final List<char[]> copy = new ArrayList<char[]>();
            for (char[] secret : Objects.requireNonNull(secrets, "secrets")) {
                final char[] checked = Arrays.copyOf(Objects.requireNonNull(secret, "secret"), secret.length);
                if (checked.length > 0) { copy.add(checked); }
            }
            Collections.sort(copy, new Comparator<char[]>() {
                @Override public int compare(final char[] left, final char[] right) {
                    return Integer.compare(right.length, left.length);
                }
            });
            this.secrets = copy;
        }
        /** @return text with every seeded secret occurrence replaced */
        public synchronized String redact(final String text) {
            Objects.requireNonNull(text, "text");
            if (closed) { throw new IllegalStateException("Redactor is closed"); }
            String result = text;
            for (char[] secret : secrets) { result = redactOne(result, secret); }
            return result;
        }
        private static String redactOne(final String text, final char[] secret) {
            final StringBuilder output = new StringBuilder();
            int index = 0;
            while (index < text.length()) {
                if (matches(text, index, secret)) {
                    output.append("[REDACTED]");
                    index += secret.length;
                } else {
                    output.append(text.charAt(index));
                    index++;
                }
            }
            return output.toString();
        }
        private static boolean matches(final String text, final int start, final char[] secret) {
            if (start + secret.length > text.length()) { return false; }
            for (int offset = 0; offset < secret.length; offset++) {
                if (text.charAt(start + offset) != secret[offset]) { return false; }
            }
            return true;
        }
        /** Clears every seeded secret copy. */
        @Override public synchronized void close() {
            if (!closed) {
                for (char[] secret : secrets) { Arrays.fill(secret, '\0'); }
                closed = true;
            }
        }
    }

    /** Diagnostic field sensitivity classification. */
    public enum Classification {
        /** Safe public health metadata. */ PUBLIC,
        /** Safe only for an authorized operator export. */ OPERATOR,
        /** Must never be exported. */ SENSITIVE
    }

    /** Immutable diagnostic field before allowlist and redaction policy. */
    public static final class DiagnosticField {
        private final DefinitionId id;
        private final Classification classification;
        private final String value;
        private DiagnosticField(final DefinitionId id, final Classification classification,
                                final String value) {
            this.id = Objects.requireNonNull(id, "id");
            this.classification = Objects.requireNonNull(classification, "classification");
            this.value = Objects.requireNonNull(value, "value");
        }
        /** @return diagnostic field */
        public static DiagnosticField of(final DefinitionId id, final Classification classification,
                                         final String value) {
            return new DiagnosticField(id, classification, value);
        }
        /** @return field identity */ public DefinitionId id() { return id; }
        /** @return sensitivity */ public Classification classification() { return classification; }
        /** @return untrusted value passed only to the exporter */ public String value() { return value; }
    }

    /** Allowlist-only deterministic diagnostic exporter. */
    public static final class DiagnosticExporter {
        private static final Set<String> FORBIDDEN_TOKENS = Collections.unmodifiableSet(
                new HashSet<String>(Arrays.asList("secret", "token", "password", "credential",
                        "private-key", "webhook-url", "endpoint-url")));
        private final Set<DefinitionId> allowlist;
        private final Redactor redactor;
        /** @param allowlist exact field IDs @param redactor seeded redactor */
        public DiagnosticExporter(final Collection<DefinitionId> allowlist, final Redactor redactor) {
            this.allowlist = Collections.unmodifiableSet(new HashSet<DefinitionId>(
                    Objects.requireNonNull(allowlist, "allowlist")));
            this.redactor = Objects.requireNonNull(redactor, "redactor");
        }
        /** @return canonical lines for allowed non-sensitive fields */
        public ExportResult export(final Collection<DiagnosticField> fields) {
            final List<DiagnosticField> sorted = new ArrayList<DiagnosticField>(
                    Objects.requireNonNull(fields, "fields"));
            Collections.sort(sorted, new Comparator<DiagnosticField>() {
                @Override public int compare(final DiagnosticField left, final DiagnosticField right) {
                    return left.id().compareTo(right.id());
                }
            });
            final StringBuilder output = new StringBuilder();
            for (DiagnosticField field : sorted) {
                if (field == null || !allowlist.contains(field.id())
                        || field.classification() == Classification.SENSITIVE
                        || hasForbiddenToken(field.id().path())) {
                    return ExportResult.failure(DefinitionId.of("zartra", "diagnostic/field_denied"));
                }
                output.append(field.id()).append('=').append(redactor.redact(field.value())).append('\n');
            }
            return ExportResult.success(output.toString());
        }
        private static boolean hasForbiddenToken(final String path) {
            for (String token : FORBIDDEN_TOKENS) {
                if (path.contains(token)) { return true; }
            }
            return false;
        }
    }

    /** Immutable diagnostic export outcome. */
    public static final class ExportResult {
        private final String content;
        private final DefinitionId failure;
        private ExportResult(final String content, final DefinitionId failure) {
            this.content = content;
            this.failure = failure;
        }
        private static ExportResult success(final String content) { return new ExportResult(content, null); }
        private static ExportResult failure(final DefinitionId failure) { return new ExportResult(null, failure); }
        /** @return whether export passed policy */ public boolean isSuccess() { return failure == null; }
        /** @return sanitized content */ public Optional<String> content() { return Optional.ofNullable(content); }
        /** @return stable denial code */ public Optional<DefinitionId> failure() { return Optional.ofNullable(failure); }
    }
}
