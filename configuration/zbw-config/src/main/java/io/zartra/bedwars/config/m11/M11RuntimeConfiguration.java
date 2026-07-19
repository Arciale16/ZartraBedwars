package io.zartra.bedwars.config.m11;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Transactional last-known-good registry for all M11 configuration families.
 *
 * <p>Parsing and filesystem I/O are adapter concerns. This Java 8 boundary consumes immutable
 * versioned documents, validates and migrates them, prepares every participant, then publishes one
 * deterministic snapshot or rolls every participant back.</p>
 */
public final class M11RuntimeConfiguration {
    private final Map<Section, Schema> schemas;
    private final Map<Section, Participant> participants;
    private volatile Snapshot active;

    /** Creates a coordinator from a previously validated last-known-good snapshot. */
    public M11RuntimeConfiguration(final Collection<Schema> schemas,
                                   final Collection<Participant> participants,
                                   final Snapshot active) {
        this.schemas = uniqueSchemas(schemas);
        this.participants = uniqueParticipants(participants);
        this.active = Objects.requireNonNull(active, "active");
        validateComplete(active.documents(), this.schemas);
    }

    /** @return immutable last-known-good state */ public Snapshot active() { return active;
        }

    /** Validates, migrates, prepares and atomically activates a complete candidate. */
    public synchronized Activation activate(final Collection<Document> documents) {
        final Map<Section, Document> candidate = index(documents);
        final List<Issue> issues = new ArrayList<Issue>();
        final Map<Section, Document> migrated = new EnumMap<Section, Document>(Section.class);
        for (Section section : Section.values()) {
            final Document supplied = candidate.get(section);
            if (supplied == null) { issues.add(new Issue(section, "missing_document"));
        continue;
        }
            final Schema schema = schemas.get(section);
            if (schema == null) { issues.add(new Issue(section, "missing_schema"));
        continue;
        }
            final MigrationResult result = schema.migrate(supplied);
            if (!result.success()) { issues.add(new Issue(section, result.code()));
        continue;
        }
            final Optional<String> failure = schema.validate(result.document());
            if (failure.isPresent()) { issues.add(new Issue(section, failure.get()));
        continue;
        }
            migrated.put(section, result.document());
        }
        if (!issues.isEmpty()) { return Activation.failure("validation_failed", issues, active);
        }
        final Snapshot next = new Snapshot(active.revision() + 1L, migrated);
        final List<Prepared> prepared = new ArrayList<Prepared>();
        try {
            for (Section section : Section.values()) {
                final Participant participant = participants.get(section);
                if (participant != null) { prepared.add(Objects.requireNonNull(participant.prepare(active, next), "prepared"));
        }
            }
        } catch (RuntimeException failure) {
            rollback(prepared);
            return Activation.failure("prepare_failed", Collections.<Issue>emptyList(), active);
        }
        final List<Prepared> applied = new ArrayList<Prepared>();
        try {
            for (Prepared change : prepared) { applied.add(change);
        change.apply();
        }
        } catch (RuntimeException failure) {
            rollback(applied);
        rollbackRemaining(prepared, applied.size());
            return Activation.failure("apply_failed", Collections.<Issue>emptyList(), active);
        }
        active = next;
        return Activation.success(next);
    }

    /** Runs a pure deterministic balance simulation against the active snapshot. */
    public SimulationResult simulate(final BalanceSimulation simulation) {
        final SimulationResult first = Objects.requireNonNull(simulation.run(active), "simulation result");
        final SimulationResult second = Objects.requireNonNull(simulation.run(active), "simulation result");
        if (!first.equals(second)) { return SimulationResult.failure("nondeterministic_simulation");
        }
        return first;
    }

    /** M11 configuration families. */
    public enum Section { SHOPS, GENERATORS, UPGRADES, TRAPS, UTILITY_ITEMS, MODES, ROTATIONS, SCRIPTS }

    /** Immutable canonical document. */
    public static final class Document {
        private final Section section;
        private final int version;
        private final Map<DefinitionId, String> values;
        /** Creates a bounded document with identity-ordered values. */
        public Document(final Section section, final int version, final Map<DefinitionId, String> values) {
            this.section = Objects.requireNonNull(section, "section");
            if (version < 1) { throw new IllegalArgumentException("version must be positive");
        }
            this.version = version;
            final List<Map.Entry<DefinitionId, String>> entries = new ArrayList<Map.Entry<DefinitionId, String>>(Objects.requireNonNull(values, "values").entrySet());
            entries.sort(Comparator.comparing(Map.Entry::getKey));
            final Map<DefinitionId, String> canonical = new LinkedHashMap<DefinitionId, String>();
            for (Map.Entry<DefinitionId, String> entry : entries) {
                final String value = Objects.requireNonNull(entry.getValue(), "value");
                if (value.length() > 4096 || value.indexOf('\0') >= 0) { throw new IllegalArgumentException("unsafe configuration value");
        }
                canonical.put(Objects.requireNonNull(entry.getKey(), "key"), value);
            }
            if (canonical.size() > 16384) { throw new IllegalArgumentException("document exceeds entry limit");
        }
            this.values = Collections.unmodifiableMap(canonical);
        }
        /** @return section */ public Section section() { return section;
        }
        /** @return schema version */ public int version() { return version;
        }
        /** @return canonical values */ public Map<DefinitionId, String> values() { return values;
        }
    }

    /** Versioned schema, migration chain and semantic validator. */
    public static final class Schema {
        private final Section section;
        private final int current;
        private final Map<Integer, Migration> migrations;
        private final Validator validator;
        /** Creates a schema with consecutive migrations. */
        public Schema(final Section section, final int current, final Collection<Migration> migrations,
                      final Validator validator) {
            this.section = Objects.requireNonNull(section, "section");
            if (current < 1) { throw new IllegalArgumentException("current version must be positive");
        }
            this.current = current;
        this.validator = Objects.requireNonNull(validator, "validator");
            final Map<Integer, Migration> indexed = new LinkedHashMap<Integer, Migration>();
            for (Migration migration : Objects.requireNonNull(migrations, "migrations")) {
                final Migration checked = Objects.requireNonNull(migration, "migration");
                if (checked.toVersion() != checked.fromVersion() + 1 || indexed.put(checked.fromVersion(), checked) != null) {
                    throw new IllegalArgumentException("migrations must be unique consecutive steps");
                }
            }
            this.migrations = Collections.unmodifiableMap(indexed);
        }
        /** @return section */ public Section section() { return section;
        }
        private MigrationResult migrate(final Document source) {
            if (source.section() != section) { return MigrationResult.failure("section_mismatch", source);
        }
            if (source.version() > current) { return MigrationResult.failure("future_version", source);
        }
            Document value = source;
            while (value.version() < current) {
                final Migration step = migrations.get(value.version());
                if (step == null) { return MigrationResult.failure("migration_missing", source);
        }
                value = Objects.requireNonNull(step.migrate(value), "migrated document");
                if (value.section() != section || value.version() != step.toVersion()) { return MigrationResult.failure("migration_contract", source);
        }
            }
            return MigrationResult.success(value);
        }
        private Optional<String> validate(final Document document) { return validator.validate(document);
        }
    }

    /** Pure one-version migration. */ public interface Migration { int fromVersion();
        int toVersion();
        Document migrate(Document source);
        }
    /** Pure semantic validator. */ public interface Validator { Optional<String> validate(Document document);
        }
    /** Transactional activation participant. */ public interface Participant { Section section();
        Prepared prepare(Snapshot current, Snapshot candidate);
        }
    /** Reversible prepared activation. */ public interface Prepared { void apply();
        void rollback();
        }
    /** Pure balance simulation. */ public interface BalanceSimulation { SimulationResult run(Snapshot snapshot);
        }

    /** Immutable active snapshot. */
    public static final class Snapshot {
        private final long revision;
        private final Map<Section, Document> documents;
        /** Creates a complete snapshot. */
        public Snapshot(final long revision, final Map<Section, Document> documents) {
            if (revision < 0) { throw new IllegalArgumentException("revision cannot be negative");
        }
            this.revision = revision;
        this.documents = Collections.unmodifiableMap(new EnumMap<Section, Document>(Objects.requireNonNull(documents, "documents")));
            validateComplete(this.documents, null);
        }
        /** @return revision */ public long revision() { return revision;
        }
        /** @return documents */ public Map<Section, Document> documents() { return documents;
        }
        /** @return required document */ public Document document(final Section section) { return Objects.requireNonNull(documents.get(Objects.requireNonNull(section, "section")), "document");
        }
    }
    /** Activation issue. */ public static final class Issue { private final Section section;
        private final String code;
        private Issue(final Section section, final String code) { this.section = section;
        this.code = safeCode(code);
        } public Section section() { return section;
        } public String code() { return code;
        } }
    /** Activation outcome. */ public static final class Activation { private final boolean success;
        private final String code;
        private final List<Issue> issues;
        private final Snapshot snapshot;
        private Activation(final boolean success, final String code, final Collection<Issue> issues, final Snapshot snapshot) { this.success = success;
        this.code = code;
        this.issues = Collections.unmodifiableList(new ArrayList<Issue>(issues));
        this.snapshot = snapshot;
        } private static Activation success(final Snapshot snapshot) { return new Activation(true, "ok", Collections.<Issue>emptyList(), snapshot);
        } private static Activation failure(final String code, final Collection<Issue> issues, final Snapshot snapshot) { return new Activation(false, code, issues, snapshot);
        } public boolean success() { return success;
        } public String code() { return code;
        } public List<Issue> issues() { return issues;
        } public Snapshot snapshot() { return snapshot;
        } }
    /** Deterministic balance simulation result. */ public static final class SimulationResult { private final boolean success;
        private final String code;
        private final long score;
        private SimulationResult(final boolean success, final String code, final long score) { this.success = success;
        this.code = safeCode(code);
        this.score = score;
        } public static SimulationResult success(final long score) { return new SimulationResult(true, "ok", score);
        } public static SimulationResult failure(final String code) { return new SimulationResult(false, code, 0);
        } public boolean success() { return success;
        } public String code() { return code;
        } public long score() { return score;
        } @Override public int hashCode() { return Objects.hash(success, code, score);
        } @Override public boolean equals(final Object other) { if (this == other) { return true;
        } if (!(other instanceof SimulationResult)) { return false;
        } final SimulationResult that = (SimulationResult) other;
        return success == that.success && score == that.score && code.equals(that.code);
        } }
    private static final class MigrationResult { private final boolean success;
        private final String code;
        private final Document document;
        private MigrationResult(final boolean success, final String code, final Document document) { this.success = success;
        this.code = code;
        this.document = document;
        } private static MigrationResult success(final Document document) { return new MigrationResult(true, "ok", document);
        } private static MigrationResult failure(final String code, final Document document) { return new MigrationResult(false, code, document);
        } private boolean success() { return success;
        } private String code() { return code;
        } private Document document() { return document;
        } }

    private static Map<Section, Schema> uniqueSchemas(final Collection<Schema> source) {
        final Map<Section, Schema> result = new EnumMap<Section, Schema>(Section.class);
        for (Schema value : Objects.requireNonNull(source, "schemas")) {
        final Schema checked = Objects.requireNonNull(value, "schema");
        if (result.put(checked.section(), checked) != null) { throw new IllegalArgumentException("duplicate schema");
        } } if (result.size() != Section.values().length) { throw new IllegalArgumentException("all M11 schemas are required");

    }
        return Collections.unmodifiableMap(result);
        }
    private static Map<Section, Participant> uniqueParticipants(final Collection<Participant> source) {
        final Map<Section, Participant> result = new EnumMap<Section, Participant>(Section.class);
        for (Participant value : Objects.requireNonNull(source, "participants")) {
        final Participant checked = Objects.requireNonNull(value, "participant");
        if (result.put(checked.section(), checked) != null) { throw new IllegalArgumentException("duplicate participant");
        }
    }
        return Collections.unmodifiableMap(result);
        }
    private static Map<Section, Document> index(final Collection<Document> source) {
        final Map<Section, Document> result = new EnumMap<Section, Document>(Section.class);
        for (Document value : Objects.requireNonNull(source, "documents")) {
        final Document checked = Objects.requireNonNull(value, "document");
        if (result.put(checked.section(), checked) != null) { throw new IllegalArgumentException("duplicate document");
        }
    }
        return result;
        }
    private static void validateComplete(final Map<Section, Document> documents, final Map<Section, Schema> schemas) { for (Section section : Section.values()) {
        final Document document = documents.get(section);
        if (document == null || document.section() != section || schemas != null && !schemas.containsKey(section)) { throw new IllegalArgumentException("incomplete M11 snapshot");
        } } }
    private static void rollback(final List<Prepared> values) { for (int index = values.size() - 1;
        index >= 0;
        index--) { try { values.get(index).rollback();
        } catch (RuntimeException ignored) { return;
        } } }
    private static void rollbackRemaining(final List<Prepared> values, final int applied) { for (int index = values.size() - 1;
        index >= applied;
        index--) { try { values.get(index).rollback();
        } catch (RuntimeException ignored) { return;
        } } }
    private static String safeCode(final String code) { if (code == null || !code.matches("[a-z0-9_.-]{1,64}")) { throw new IllegalArgumentException("invalid code");

    }
        return code;
        }
}
