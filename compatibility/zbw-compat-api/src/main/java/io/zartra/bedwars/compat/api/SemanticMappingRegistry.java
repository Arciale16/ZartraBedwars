package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Thread-safe, last-known-good semantic mapping registry with deterministic validation. */
public final class SemanticMappingRegistry {
    private static final DefinitionId DUPLICATE = DefinitionId.of("zartra", "compat/duplicate");
    private static final DefinitionId MISSING = DefinitionId.of("zartra", "compat/missing");
    private volatile Snapshot active;

    /** Creates a registry from a fully validated initial snapshot. */
    public SemanticMappingRegistry(final Collection<CompatibilityMapping> initial,
                                   final Set<SemanticKey> required) {
        final Activation activation = evaluate(initial, required, 1L);
        if (!activation.validation().isValid()) {
            throw new IllegalArgumentException("initial mappings must satisfy every required key");
        }
        active = activation.candidate();
    }

    /**
     * Atomically activates a valid candidate and retains the previous snapshot on every error.
     *
     * @return activation result including whether the last-known-good snapshot was retained
     */
    public synchronized Activation activate(final Collection<CompatibilityMapping> candidate,
                                            final Set<SemanticKey> required) {
        final Activation evaluated = evaluate(candidate, required, active.version() + 1L);
        if (!evaluated.validation().isValid()) {
            return new Activation(evaluated.validation(), active, false);
        }
        active = evaluated.candidate();
        return new Activation(evaluated.validation(), active, true);
    }

    /** @return current immutable last-known-good snapshot */ public Snapshot snapshot() { return active; }

    private static Activation evaluate(final Collection<CompatibilityMapping> candidate,
                                       final Set<SemanticKey> required, final long version) {
        Objects.requireNonNull(candidate, "candidate");
        final Set<SemanticKey> mandatory = new TreeSet<SemanticKey>();
        for (SemanticKey key : Objects.requireNonNull(required, "required")) {
            mandatory.add(Objects.requireNonNull(key, "required key"));
        }
        final Map<SemanticKey, CompatibilityMapping> mappings = new TreeMap<SemanticKey, CompatibilityMapping>();
        final List<CompatibilityValidation.Issue> issues = new ArrayList<CompatibilityValidation.Issue>();
        for (CompatibilityMapping mapping : candidate) {
            Objects.requireNonNull(mapping, "candidate mapping");
            if (mappings.put(mapping.semanticKey(), mapping) != null) {
                issues.add(new CompatibilityValidation.Issue(DUPLICATE, mapping.semanticKey()));
            }
        }
        for (SemanticKey key : mandatory) {
            if (!mappings.containsKey(key)) {
                issues.add(new CompatibilityValidation.Issue(MISSING, key));
            }
        }
        final CompatibilityValidation validation = new CompatibilityValidation(issues);
        return new Activation(validation, new Snapshot(version, mappings), validation.isValid());
    }

    /** Immutable registry activation decision. */
    public static final class Activation {
        private final CompatibilityValidation validation;
        private final Snapshot candidate;
        private final boolean activated;
        private Activation(final CompatibilityValidation validation, final Snapshot candidate,
                           final boolean activated) {
            this.validation = validation;
            this.candidate = candidate;
            this.activated = activated;
        }
        /** @return deterministic validation */ public CompatibilityValidation validation() { return validation; }
        /** @return active snapshot after the attempt */ public Snapshot candidate() { return candidate; }
        /** @return true only when candidate replaced last-known-good state */ public boolean activated() { return activated; }
    }

    /** Immutable sorted semantic mapping snapshot. */
    public static final class Snapshot {
        private final long version;
        private final Map<SemanticKey, CompatibilityMapping> mappings;
        private Snapshot(final long version, final Map<SemanticKey, CompatibilityMapping> mappings) {
            if (version < 1L) { throw new IllegalArgumentException("version must be positive"); }
            this.version = version;
            this.mappings = Collections.unmodifiableMap(
                    new LinkedHashMap<SemanticKey, CompatibilityMapping>(mappings));
        }
        /** @return monotonically increasing active version */ public long version() { return version; }
        /** @return selected mapping or null-free absence */
        public java.util.Optional<CompatibilityMapping> find(final SemanticKey key) {
            return java.util.Optional.ofNullable(mappings.get(Objects.requireNonNull(key, "key")));
        }
        /** @return immutable sorted map */ public Map<SemanticKey, CompatibilityMapping> mappings() { return mappings; }
    }
}
