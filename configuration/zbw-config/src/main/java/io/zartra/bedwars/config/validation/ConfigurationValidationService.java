package io.zartra.bedwars.config.validation;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.InitialCatalog;
import io.zartra.bedwars.config.schema.ConfigurationModel.Issue;
import io.zartra.bedwars.config.schema.ConfigurationModel.LogicalFile;
import io.zartra.bedwars.config.schema.ConfigurationModel.Schema;
import io.zartra.bedwars.config.schema.ConfigurationModel.Severity;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValidatedConfiguration;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValidationReport;
import io.zartra.bedwars.config.schema.ConfigurationModel.Validator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministic startup/manual validation use case over injected source and environment ports. */
public final class ConfigurationValidationService {
    private static final int MAX_OPTIONS_PER_FILE = 10000;
    private final Validator validator;
    private final Map<LogicalFile, Schema> schemas;
    private final List<CrossDocumentRule> crossDocumentRules;

    /** @param validator strict validator @param crossDocumentRules deterministic global rules */
    public ConfigurationValidationService(final Validator validator,
                                          final Collection<CrossDocumentRule> crossDocumentRules) {
        this.validator = Objects.requireNonNull(validator, "validator");
        final Map<LogicalFile, Schema> collected = new EnumMap<LogicalFile, Schema>(LogicalFile.class);
        for (Schema schema : InitialCatalog.schemas()) { collected.put(schema.file(), schema); }
        schemas = Collections.unmodifiableMap(collected);
        final List<CrossDocumentRule> rules = new ArrayList<CrossDocumentRule>();
        for (CrossDocumentRule rule : Objects.requireNonNull(crossDocumentRules, "crossDocumentRules")) {
            rules.add(Objects.requireNonNull(rule, "crossDocumentRule"));
        }
        this.crossDocumentRules = Collections.unmodifiableList(rules);
    }

    /** Validates every logical file plus injected Java/platform/provider/filesystem facts. */
    public AggregateReport validateStartup(final DocumentSource source,
                                           final Collection<ExternalCheck> externalChecks) {
        Objects.requireNonNull(source, "source");
        final Map<LogicalFile, ValidationReport> reports =
                new EnumMap<LogicalFile, ValidationReport>(LogicalFile.class);
        for (LogicalFile file : LogicalFile.values()) {
            reports.put(file, validateLoaded(file, load(source, file)));
        }
        final List<Issue> global = new ArrayList<Issue>();
        for (ExternalCheck check : Objects.requireNonNull(externalChecks, "externalChecks")) {
            final ExternalCheck checked = Objects.requireNonNull(check, "externalCheck");
            if (!checked.success()) {
                global.add(Issue.of(Severity.ERROR, checked.code(), null, checked.action()));
            }
        }
        final Map<LogicalFile, ValidatedConfiguration> valid = validConfigurations(reports);
        if (valid.size() == LogicalFile.values().length) {
            for (CrossDocumentRule rule : crossDocumentRules) {
                global.addAll(Objects.requireNonNull(rule.validate(Collections.unmodifiableMap(valid)),
                        "crossDocumentIssues"));
            }
        }
        return new AggregateReport(Mode.STARTUP, reports, global);
    }

    /** Validates one logical file for an operator command or GUI action. */
    public AggregateReport validateManual(final LogicalFile file, final Document document) {
        final Map<LogicalFile, ValidationReport> reports =
                new EnumMap<LogicalFile, ValidationReport>(LogicalFile.class);
        reports.put(Objects.requireNonNull(file, "file"), validateLoaded(file,
                LoadResult.success(Objects.requireNonNull(document, "document"))));
        return new AggregateReport(Mode.MANUAL, reports, Collections.<Issue>emptyList());
    }

    private LoadResult load(final DocumentSource source, final LogicalFile file) {
        try {
            final Optional<Document> document = Objects.requireNonNull(source.load(file), "sourceResult");
            return document.isPresent() ? LoadResult.success(document.get())
                    : LoadResult.failure(DefinitionId.of("zartra", "validation/file_missing"));
        } catch (RuntimeException exception) {
            return LoadResult.failure(DefinitionId.of("zartra", "validation/source_failed"));
        }
    }
    private ValidationReport validateLoaded(final LogicalFile file, final LoadResult loaded) {
        if (!loaded.document().isPresent()) {
            return ValidationReport.invalid(Collections.singletonList(Issue.of(Severity.ERROR,
                    loaded.failure().get(), null,
                    "Restore the logical configuration file from a validated generated reference.")));
        }
        final Document document = loaded.document().get();
        if (document.values().size() > MAX_OPTIONS_PER_FILE) {
            return ValidationReport.invalid(Collections.singletonList(Issue.of(Severity.ERROR,
                    DefinitionId.of("zartra", "validation/option_limit"), null,
                    "Reduce the logical file below the documented option limit.")));
        }
        return validator.validate(schemas.get(file), document);
    }
    private static Map<LogicalFile, ValidatedConfiguration> validConfigurations(
            final Map<LogicalFile, ValidationReport> reports) {
        final Map<LogicalFile, ValidatedConfiguration> valid =
                new EnumMap<LogicalFile, ValidatedConfiguration>(LogicalFile.class);
        for (Map.Entry<LogicalFile, ValidationReport> entry : reports.entrySet()) {
            if (entry.getValue().isValid()) {
                valid.put(entry.getKey(), entry.getValue().configuration().get());
            }
        }
        return valid;
    }

    /** Input port; filesystem and runtime adapters implement loading outside this module. */
    public interface DocumentSource {
        /** @return logical document, or empty when absent */ Optional<Document> load(LogicalFile file);
    }

    /** Deterministic cross-file reference, duplicate-ID or compatibility rule. */
    public interface CrossDocumentRule {
        /** @return secret-free global issues over complete validated snapshots */
        Collection<Issue> validate(Map<LogicalFile, ValidatedConfiguration> configurations);
    }

    /** External platform/provider/filesystem fact supplied by an adapter. */
    public static final class ExternalCheck {
        private final DefinitionId code;
        private final boolean success;
        private final String action;
        private ExternalCheck(final DefinitionId code, final boolean success, final String action) {
            this.code = Objects.requireNonNull(code, "code");
            this.success = success;
            if (action == null || action.trim().isEmpty()) {
                throw new IllegalArgumentException("Actionable guidance is required");
            }
            this.action = action;
        }
        /** @return adapter-supplied validation fact */
        public static ExternalCheck of(final DefinitionId code, final boolean success,
                                       final String action) {
            return new ExternalCheck(code, success, action);
        }
        /** @return stable check code */ public DefinitionId code() { return code; }
        /** @return check outcome */ public boolean success() { return success; }
        /** @return secret-free corrective action */ public String action() { return action; }
    }

    /** Validation invocation mode. */
    public enum Mode {
        /** Full startup gate. */ STARTUP,
        /** Explicit operator validation. */ MANUAL
    }

    /** Immutable aggregate validation result. */
    public static final class AggregateReport {
        private final Mode mode;
        private final Map<LogicalFile, ValidationReport> reports;
        private final List<Issue> globalIssues;
        private AggregateReport(final Mode mode, final Map<LogicalFile, ValidationReport> reports,
                                final Collection<Issue> globalIssues) {
            this.mode = mode;
            this.reports = Collections.unmodifiableMap(new EnumMap<LogicalFile, ValidationReport>(reports));
            final List<Issue> issues = new ArrayList<Issue>(globalIssues);
            Collections.sort(issues);
            this.globalIssues = Collections.unmodifiableList(issues);
        }
        /** @return invocation mode */ public Mode mode() { return mode; }
        /** @return deterministic per-file reports */ public Map<LogicalFile, ValidationReport> reports() { return reports; }
        /** @return environment and cross-document issues */ public List<Issue> globalIssues() { return globalIssues; }
        /** @return whether every file and external/cross-document check passed */
        public boolean isValid() {
            if (!globalIssues.isEmpty()) { return false; }
            for (ValidationReport report : reports.values()) {
                if (!report.isValid()) { return false; }
            }
            return true;
        }
    }

    private static final class LoadResult {
        private final Document document;
        private final DefinitionId failure;
        private LoadResult(final Document document, final DefinitionId failure) {
            this.document = document;
            this.failure = failure;
        }
        private static LoadResult success(final Document document) { return new LoadResult(document, null); }
        private static LoadResult failure(final DefinitionId failure) { return new LoadResult(null, failure); }
        private Optional<Document> document() { return Optional.ofNullable(document); }
        private Optional<DefinitionId> failure() { return Optional.ofNullable(failure); }
    }
}
