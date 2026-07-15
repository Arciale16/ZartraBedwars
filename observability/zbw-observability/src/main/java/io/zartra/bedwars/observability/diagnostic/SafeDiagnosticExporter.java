package io.zartra.bedwars.observability.diagnostic;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.time.TimeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic allowlist-only exporter with final-boundary secret detection. */
public final class SafeDiagnosticExporter {
    private final int maximumContributors;
    private final int maximumFields;
    private final int maximumValueCharacters;
    private final Set<DefinitionId> allowlist;
    private final Diagnostics.Sanitizer sanitizer;
    private final TimeSource timeSource;
    private final Map<DefinitionId, Diagnostics.Contributor> contributors =
            new TreeMap<DefinitionId, Diagnostics.Contributor>();
    /** Creates an exporter with hard limits. */
    public SafeDiagnosticExporter(final int maximumContributors, final int maximumFields,
                                  final int maximumValueCharacters,
                                  final Collection<DefinitionId> allowlist,
                                  final Diagnostics.Sanitizer sanitizer,
                                  final TimeSource timeSource) {
        if (maximumContributors < 1 || maximumFields < 1 || maximumValueCharacters < 1) {
            throw new IllegalArgumentException("diagnostic bounds must be positive");
        }
        this.maximumContributors = maximumContributors;
        this.maximumFields = maximumFields;
        this.maximumValueCharacters = maximumValueCharacters;
        final Collection<DefinitionId> requiredAllowlist =
                Objects.requireNonNull(allowlist, "allowlist");
        for (DefinitionId id : requiredAllowlist) {
            if (id == null) { throw new IllegalArgumentException("null allowlist ID"); }
        }
        this.allowlist = Collections.unmodifiableSet(
                new TreeSet<DefinitionId>(requiredAllowlist));
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }
    /** Registers one unique contributor. */
    public synchronized void register(final Diagnostics.Contributor contributor) {
        Objects.requireNonNull(contributor, "contributor");
        final DefinitionId id = Objects.requireNonNull(contributor.id(), "contributor.id");
        if (contributors.containsKey(id)) { throw new IllegalArgumentException("duplicate contributor ID"); }
        if (contributors.size() >= maximumContributors) {
            throw new IllegalStateException("diagnostic contributor capacity exhausted");
        }
        contributors.put(id, contributor);
    }
    /** @return allowlisted, redacted and sorted export */
    public synchronized Diagnostics.Export export() {
        final List<Diagnostics.Field> exported = new ArrayList<Diagnostics.Field>();
        for (Diagnostics.Contributor contributor : contributors.values()) {
            final List<Diagnostics.Field> candidates = Objects.requireNonNull(
                    contributor.fields(), "contributor fields");
            if (candidates.size() > maximumFields) {
                throw new IllegalStateException("contributor field capacity exceeded");
            }
            for (Diagnostics.Field candidate : candidates) {
                Objects.requireNonNull(candidate, "field");
                if (candidate.classification() != Diagnostics.Classification.PUBLIC
                        || !allowlist.contains(candidate.id())) {
                    continue;
                }
                if (exported.size() >= maximumFields) {
                    throw new IllegalStateException("export field capacity exhausted");
                }
                final String sanitized = sanitizer.sanitize(candidate.value());
                if (sanitized == null || sanitized.length() > maximumValueCharacters
                        || sanitizer.containsSensitiveValue(sanitized)) {
                    throw new SecurityException("diagnostic sanitization rejected a value");
                }
                exported.add(new Diagnostics.Field(candidate.id(), sanitized,
                        Diagnostics.Classification.PUBLIC));
            }
        }
        return new Diagnostics.Export(timeSource.now(), exported);
    }
    /** Exact-seed sanitizer with zeroizable seed storage. */
    public static final class SeededSanitizer implements Diagnostics.Sanitizer, AutoCloseable {
        private static final String REDACTED = "[redacted]";
        private final List<char[]> seeds = new ArrayList<char[]>();
        /** Copies non-empty sensitive seeds. */
        public SeededSanitizer(final Collection<String> sensitiveSeeds) {
            for (String value : Objects.requireNonNull(sensitiveSeeds, "sensitiveSeeds")) {
                if (value == null || value.isEmpty()) {
                    throw new IllegalArgumentException("sensitive seeds must be non-empty");
                }
                seeds.add(value.toCharArray());
            }
        }
        @Override public synchronized String sanitize(final String candidate) {
            String result = Objects.requireNonNull(candidate, "candidate");
            for (char[] seed : seeds) { result = result.replace(new String(seed), REDACTED); }
            result = result.replaceAll("(?i)(password|token|secret|authorization)=\\S+",
                    "$1=[redacted]");
            return result.replaceAll("(?i)bearer\\s+\\S+", "Bearer [redacted]");
        }
        @Override public synchronized boolean containsSensitiveValue(final String candidate) {
            Objects.requireNonNull(candidate, "candidate");
            for (char[] seed : seeds) {
                if (candidate.contains(new String(seed))) { return true; }
            }
            return false;
        }
        @Override public synchronized void close() {
            for (char[] seed : seeds) { Arrays.fill(seed, '\0'); }
            seeds.clear();
        }
    }
}
