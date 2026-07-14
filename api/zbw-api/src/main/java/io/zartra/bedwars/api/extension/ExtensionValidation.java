package io.zartra.bedwars.api.extension;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Deterministic extension metadata validation contracts and values. */
public final class ExtensionValidation {
    private ExtensionValidation() { throw new AssertionError("No instances"); }

    /** Validator contract implemented by the public SDK. Implementations must be thread-safe. */
    public interface Validator {
        /** @return deterministic report for one metadata record and compatibility target */
        Report validate(ExtensionMetadata metadata, Target target);
        /** @return deterministic report including duplicate extension/dependency checks */
        Report validateCatalog(Collection<ExtensionMetadata> metadata, Target target);
    }

    /** Runtime versions against which metadata is checked without loading extension code. */
    public static final class Target {
        private final SemanticVersion apiVersion;
        private final SemanticVersion productVersion;
        private final MinecraftVersion minecraftVersion;
        private Target(final SemanticVersion apiVersion, final SemanticVersion productVersion,
                       final MinecraftVersion minecraftVersion) {
            this.apiVersion = Objects.requireNonNull(apiVersion, "apiVersion");
            this.productVersion = Objects.requireNonNull(productVersion, "productVersion");
            this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        }
        /** @return compatibility target */
        public static Target of(final SemanticVersion apiVersion, final SemanticVersion productVersion,
                                final MinecraftVersion minecraftVersion) {
            return new Target(apiVersion, productVersion, minecraftVersion);
        }
        /** @return current public API version */ public SemanticVersion apiVersion() { return apiVersion; }
        /** @return current product version */ public SemanticVersion productVersion() { return productVersion; }
        /** @return current Minecraft server version */ public MinecraftVersion minecraftVersion() { return minecraftVersion; }
    }

    /** Immutable validation issue with stable code and field path. */
    public static final class Issue {
        private final Severity severity;
        private final DefinitionId code;
        private final String field;
        private final String messageKey;
        private Issue(final Severity severity, final DefinitionId code, final String field, final String messageKey) {
            this.severity = Objects.requireNonNull(severity, "severity");
            this.code = Objects.requireNonNull(code, "code");
            if (field == null || !field.matches("[a-z0-9][a-z0-9_.\\[\\]-]{0,127}")) { throw new IllegalArgumentException("Invalid field path"); }
            if (messageKey == null || !messageKey.matches("[a-z0-9][a-z0-9_.-]{0,127}")) { throw new IllegalArgumentException("Invalid message key"); }
            this.field = field;
            this.messageKey = messageKey;
        }
        /** @return issue */ public static Issue of(final Severity severity, final DefinitionId code, final String field, final String messageKey) { return new Issue(severity, code, field, messageKey); }
        /** @return severity */ public Severity severity() { return severity; }
        /** @return stable issue code */ public DefinitionId code() { return code; }
        /** @return deterministic metadata field path */ public String field() { return field; }
        /** @return localizable message key */ public String messageKey() { return messageKey; }
    }

    /** Immutable, deterministically sorted validation report. */
    public static final class Report {
        private static final Comparator<Issue> ORDER = Comparator.comparing(Issue::field)
                .thenComparing(issue -> issue.code().toString()).thenComparing(issue -> issue.severity().name());
        private final List<Issue> issues;
        private Report(final Collection<Issue> issues) {
            final List<Issue> sorted = new ArrayList<Issue>();
            for (Issue issue : Objects.requireNonNull(issues, "issues")) { sorted.add(Objects.requireNonNull(issue, "issue")); }
            Collections.sort(sorted, ORDER);
            this.issues = Collections.unmodifiableList(sorted);
        }
        /** @return sorted immutable report */ public static Report of(final Collection<Issue> issues) { return new Report(issues); }
        /** @return successful empty report */ public static Report valid() { return new Report(Collections.<Issue>emptyList()); }
        /** @return immutable sorted issues */ public List<Issue> issues() { return issues; }
        /** @return whether no error-severity issue exists */ public boolean isValid() {
            for (Issue issue : issues) { if (issue.severity == Severity.ERROR) { return false; } }
            return true;
        }
    }

    /** Validation severity. */
    public enum Severity { ERROR, WARNING }
}
