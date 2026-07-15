package io.zartra.bedwars.api.doctor;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.scheduler.TaskContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Extensible platform-neutral Plugin Doctor contracts. */
public final class PluginDoctor {
    private PluginDoctor() { throw new AssertionError("No instances"); }
    /** One bounded diagnostic check contributed by core, an adapter or an extension. */
    public interface Check {
        /** @return unique stable check ID */ DefinitionId id();
        /** @param context scheduler context @return secret-safe result @throws Exception isolated failure */
        Result inspect(TaskContext context) throws Exception;
    }
    /** Immutable result of one check. */
    public static final class Result {
        private final DefinitionId checkId;
        private final Health.Status status;
        private final DefinitionId reasonCode;
        private final List<Diagnostics.Field> evidence;
        /** Creates a result. */
        public Result(final DefinitionId checkId, final Health.Status status,
                      final DefinitionId reasonCode, final List<Diagnostics.Field> evidence) {
            this.checkId = Objects.requireNonNull(checkId, "checkId");
            this.status = Objects.requireNonNull(status, "status");
            this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
            final List<Diagnostics.Field> copy = new ArrayList<Diagnostics.Field>(
                    Objects.requireNonNull(evidence, "evidence"));
            if (copy.contains(null)) { throw new IllegalArgumentException("evidence cannot contain null"); }
            Collections.sort(copy);
            this.evidence = Collections.unmodifiableList(copy);
        }
        /** @return check ID */ public DefinitionId checkId() { return checkId; }
        /** @return severity */ public Health.Status status() { return status; }
        /** @return stable reason */ public DefinitionId reasonCode() { return reasonCode; }
        /** @return bounded evidence */ public List<Diagnostics.Field> evidence() { return evidence; }
    }
    /** Immutable deterministic aggregate report. */
    public static final class Report {
        private final Instant createdAt;
        private final List<Result> results;
        /** Creates a report sorted by check ID. */
        public Report(final Instant createdAt, final List<Result> results) {
            this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
            final List<Result> copy = new ArrayList<Result>(Objects.requireNonNull(results, "results"));
            if (copy.contains(null)) { throw new IllegalArgumentException("results cannot contain null"); }
            Collections.sort(copy, new Comparator<Result>() {
                @Override public int compare(final Result left, final Result right) {
                    return left.checkId().compareTo(right.checkId());
                }
            });
            this.results = Collections.unmodifiableList(copy);
        }
        /** @return creation instant */ public Instant createdAt() { return createdAt; }
        /** @return sorted check results */ public List<Result> results() { return results; }
    }
}
