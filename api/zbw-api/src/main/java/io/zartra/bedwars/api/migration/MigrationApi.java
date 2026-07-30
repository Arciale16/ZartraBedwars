package io.zartra.bedwars.api.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;

/**
 * Neutral contracts for deterministic, dry-run-first data migration.
 *
 * <p>Inputs are operator-authorized projections. Providers must not open files, execute imported
 * content or mutate an owning domain. A service invokes the target boundary only after a successful
 * plan and backup.</p>
 */
public final class MigrationApi {
    private MigrationApi() { throw new AssertionError("No instances"); }

    /** Execution mode. */
    public enum Mode { DRY_RUN, APPLY }

    /** Explicit target conflict policy. */
    public enum ConflictPolicy { FAIL, KEEP_EXISTING, REPLACE }

    /** Stable report status. */
    public enum Status { PLANNED, APPLIED, ROLLED_BACK, REJECTED, FAILED }

    /** Conversion classification. */
    public enum ConversionState { MAPPED, LOSSY, UNSUPPORTED }

    /** Immutable source or target record. */
    public static final class Record implements Comparable<Record> {
        private final String id;
        private final String kind;
        private final Map<String, String> attributes;

        /**
         * Creates a bounded data-only record.
         *
         * @param id canonical record identity
         * @param kind canonical record type
         * @param attributes scalar attributes, excluding secrets and executable content
         */
        public Record(final String id, final String kind, final Map<String, String> attributes) {
            this.id = token(id, "id");
            this.kind = token(kind, "kind");
            final TreeMap<String, String> copy = new TreeMap<String, String>();
            for (Map.Entry<String, String> entry
                    : Objects.requireNonNull(attributes, "attributes").entrySet()) {
                final String key = token(entry.getKey(), "attribute key");
                final String value = Objects.requireNonNull(entry.getValue(), "attribute value");
                if (value.length() > 4096) {
                    throw new IllegalArgumentException("attribute value exceeds 4096 characters");
                }
                copy.put(key, value);
            }
            if (copy.size() > 64) {
                throw new IllegalArgumentException("record exceeds 64 attributes");
            }
            this.attributes = Collections.unmodifiableMap(copy);
        }

        /** @return stable identity */ public String id() { return id; }
        /** @return record kind */ public String kind() { return kind; }
        /** @return sorted immutable attributes */ public Map<String, String> attributes() {
            return attributes;
        }
        @Override public int compareTo(final Record other) {
            final int idOrder = id.compareTo(other.id);
            return idOrder == 0 ? kind.compareTo(other.kind) : idOrder;
        }
        @Override public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (!(other instanceof Record)) { return false; }
            final Record that = (Record) other;
            return id.equals(that.id) && kind.equals(that.kind)
                    && attributes.equals(that.attributes);
        }
        @Override public int hashCode() { return Objects.hash(id, kind, attributes); }
    }

    /** Immutable migration request. */
    public static final class Request {
        private final String migrationId;
        private final String source;
        private final String provenance;
        private final Mode mode;
        private final ConflictPolicy conflictPolicy;
        private final List<Record> records;

        /** Creates a request from an already-confined and size-checked source adapter. */
        public Request(final String migrationId, final String source, final String provenance,
                       final Mode mode, final ConflictPolicy conflictPolicy,
                       final List<Record> records) {
            this.migrationId = token(migrationId, "migrationId");
            this.source = token(source, "source");
            this.provenance = text(provenance, "provenance", 1024);
            this.mode = Objects.requireNonNull(mode, "mode");
            this.conflictPolicy = Objects.requireNonNull(conflictPolicy, "conflictPolicy");
            this.records = immutableRecords(records);
            if (this.records.size() > 10000) {
                throw new IllegalArgumentException("migration exceeds 10000 source records");
            }
        }

        /** @return migration identity */ public String migrationId() { return migrationId; }
        /** @return neutral source adapter identity */ public String source() { return source; }
        /** @return operator-supplied provenance acknowledgement */ public String provenance() {
            return provenance;
        }
        /** @return execution mode */ public Mode mode() { return mode; }
        /** @return explicit conflict policy */ public ConflictPolicy conflictPolicy() {
            return conflictPolicy;
        }
        /** @return sorted immutable source records */ public List<Record> records() {
            return records;
        }
    }

    /** One deterministic provider conversion. */
    public static final class Conversion {
        private final ConversionState state;
        private final List<Record> records;
        private final String reason;

        /** Creates a conversion result. */
        public Conversion(final ConversionState state, final List<Record> records,
                          final String reason) {
            this.state = Objects.requireNonNull(state, "state");
            this.records = immutableRecords(records);
            this.reason = text(reason, "reason", 512);
            if (state == ConversionState.UNSUPPORTED && !this.records.isEmpty()) {
                throw new IllegalArgumentException("unsupported conversion cannot contain records");
            }
        }

        /** @return conversion state */ public ConversionState state() { return state; }
        /** @return sorted immutable target records */ public List<Record> records() {
            return records;
        }
        /** @return stable operator-safe reason */ public String reason() { return reason; }
    }

    /** Public conversion extension point. */
    public interface Provider {
        /** @return canonical provider identity */
        String id();
        /** @return whether this provider owns the source record kind */
        boolean supports(String sourceKind);
        /** @return deterministic data-only conversion */
        Conversion convert(Record source);
    }

    /** Immutable plan generated before any target mutation. */
    public static final class Plan {
        private final String migrationId;
        private final Mode mode;
        private final List<Record> targetRecords;
        private final List<String> findings;
        private final boolean applicable;

        /** Creates a deterministic migration plan. */
        public Plan(final String migrationId, final Mode mode, final List<Record> targetRecords,
                    final List<String> findings, final boolean applicable) {
            this.migrationId = token(migrationId, "migrationId");
            this.mode = Objects.requireNonNull(mode, "mode");
            this.targetRecords = immutableRecords(targetRecords);
            this.findings = immutableText(findings, "finding", 512);
            this.applicable = applicable;
        }

        /** @return migration identity */ public String migrationId() { return migrationId; }
        /** @return requested execution mode */ public Mode mode() { return mode; }
        /** @return deterministic target projection */ public List<Record> targetRecords() {
            return targetRecords;
        }
        /** @return sorted immutable findings */ public List<String> findings() { return findings; }
        /** @return whether apply is safe under the selected conflict policy */
        public boolean applicable() { return applicable; }
    }

    /** Immutable execution report. */
    public static final class Report {
        private final String migrationId;
        private final Status status;
        private final int sourceCount;
        private final int targetCount;
        private final List<String> findings;

        /** Creates an execution report. */
        public Report(final String migrationId, final Status status, final int sourceCount,
                      final int targetCount, final List<String> findings) {
            this.migrationId = token(migrationId, "migrationId");
            this.status = Objects.requireNonNull(status, "status");
            if (sourceCount < 0 || targetCount < 0) {
                throw new IllegalArgumentException("counts must be non-negative");
            }
            this.sourceCount = sourceCount;
            this.targetCount = targetCount;
            this.findings = immutableText(findings, "finding", 512);
        }

        /** @return migration identity */ public String migrationId() { return migrationId; }
        /** @return terminal status */ public Status status() { return status; }
        /** @return source record count */ public int sourceCount() { return sourceCount; }
        /** @return target record count */ public int targetCount() { return targetCount; }
        /** @return sorted immutable findings */ public List<String> findings() { return findings; }
    }

    /** Asynchronous migration service. Implementations execute only on bounded worker executors. */
    public interface Service {
        /** @return deterministic plan without mutation */
        CompletionStage<Plan> plan(Request request);
        /** @return dry-run or backup-protected application report */
        CompletionStage<Report> execute(Request request);
        /** @return rollback report for a previously applied migration */
        CompletionStage<Report> rollback(String migrationId);
    }

    private static List<Record> immutableRecords(final List<Record> records) {
        final List<Record> copy = new ArrayList<Record>(
                Objects.requireNonNull(records, "records"));
        if (copy.contains(null)) { throw new IllegalArgumentException("records contain null"); }
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    private static List<String> immutableText(final List<String> values, final String label,
                                              final int maximumLength) {
        final List<String> copy = new ArrayList<String>();
        for (String value : Objects.requireNonNull(values, label + "s")) {
            copy.add(text(value, label, maximumLength));
        }
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    private static String token(final String value, final String label) {
        final String checked = text(value, label, 128);
        if (!checked.matches("[a-z0-9][a-z0-9_.:/-]{0,127}")) {
            throw new IllegalArgumentException("invalid " + label);
        }
        return checked;
    }

    private static String text(final String value, final String label, final int maximumLength) {
        final String checked = Objects.requireNonNull(value, label).trim();
        if (checked.isEmpty() || checked.length() > maximumLength
                || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("invalid " + label);
        }
        return checked;
    }
}
