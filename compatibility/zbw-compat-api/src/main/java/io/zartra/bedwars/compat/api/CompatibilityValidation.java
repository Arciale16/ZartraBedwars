package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Deterministic validation report for a candidate semantic mapping snapshot. */
public final class CompatibilityValidation {
    private final List<Issue> issues;

    /** Creates a sorted immutable report. */
    public CompatibilityValidation(final List<Issue> issues) {
        final List<Issue> copy = new ArrayList<Issue>(Objects.requireNonNull(issues, "issues"));
        if (copy.contains(null)) { throw new IllegalArgumentException("issues cannot contain null"); }
        Collections.sort(copy);
        this.issues = Collections.unmodifiableList(copy);
    }
    /** @return true only when activation is safe */ public boolean isValid() { return issues.isEmpty(); }
    /** @return sorted immutable issues */ public List<Issue> issues() { return issues; }

    /** One stable mapping validation issue. */
    public static final class Issue implements Comparable<Issue> {
        private final DefinitionId code;
        private final SemanticKey key;
        /** Creates a localization-safe issue. */
        public Issue(final DefinitionId code, final SemanticKey key) {
            this.code = Objects.requireNonNull(code, "code");
            this.key = Objects.requireNonNull(key, "key");
        }
        /** @return stable issue code */ public DefinitionId code() { return code; }
        /** @return affected semantic key */ public SemanticKey key() { return key; }
        @Override public int compareTo(final Issue other) {
            final int keyOrder = key.compareTo(Objects.requireNonNull(other, "other").key);
            return keyOrder == 0 ? code.toString().compareTo(other.code.toString()) : keyOrder;
        }
    }
}
