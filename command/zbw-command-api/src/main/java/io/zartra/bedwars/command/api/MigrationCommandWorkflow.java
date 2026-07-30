package io.zartra.bedwars.command.api;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.migration.MigrationApi;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Neutral audited workflow behind {@code /zbw admin migrate-layout}.
 *
 * <p>The command framework performs authorization before invoking this service. Presentation
 * adapters render the immutable result on their owner thread.</p>
 */
public final class MigrationCommandWorkflow {
    private final MigrationApi.Service migrations;
    private final AuditSink audit;

    /** Creates an audited workflow. */
    public MigrationCommandWorkflow(final MigrationApi.Service migrations, final AuditSink audit) {
        this.migrations = Objects.requireNonNull(migrations, "migrations");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** Executes validate, plan, apply, rollback or report routing. */
    public CompletionStage<Outcome> execute(final String actor, final Operation operation,
                                            final MigrationApi.Request request,
                                            final String migrationId) {
        final String checkedActor = token(actor, "actor");
        Objects.requireNonNull(operation, "operation");
        audit.before(checkedActor, operation, migrationId);
        try {
            final CompletionStage<Outcome> stage;
            if (operation == Operation.VALIDATE || operation == Operation.PLAN) {
                stage = migrations.plan(Objects.requireNonNull(request, "request"))
                        .thenApply(Outcome::planned);
            } else if (operation == Operation.APPLY) {
                if (Objects.requireNonNull(request, "request").mode() != MigrationApi.Mode.APPLY) {
                    throw new IllegalArgumentException("apply requires APPLY request mode");
                }
                stage = migrations.execute(request).thenApply(Outcome::reported);
            } else if (operation == Operation.ROLLBACK) {
                stage = migrations.rollback(token(migrationId, "migrationId"))
                        .thenApply(Outcome::reported);
            } else {
                stage = CompletableFuture.completedFuture(Outcome.reportRequested(
                        token(migrationId, "migrationId")));
            }
            return stage.whenComplete((outcome, failure) ->
                    audit.after(checkedActor, operation, migrationId,
                            failure == null ? outcome.status() : "failed"));
        } catch (RuntimeException failure) {
            audit.after(checkedActor, operation, migrationId, "failed");
            throw failure;
        }
    }

    /** Canonical migration command operation and permission. */
    public enum Operation {
        /** Validate source and mapping prerequisites. */ VALIDATE("validate"),
        /** Produce immutable dry-run plan. */ PLAN("plan"),
        /** Apply a backup-protected plan. */ APPLY("apply"),
        /** Restore a prior applied migration. */ ROLLBACK("rollback"),
        /** Fetch a persisted report through the presentation adapter. */ REPORT("report");
        private final PermissionNode permission;
        Operation(final String action) {
            permission = PermissionNode.of(
                    "zartrabedwars.admin.migrate-layout." + action);
        }
        /** @return canonical dotted permission */ public PermissionNode permission() {
            return permission;
        }
    }

    /** Immutable routing outcome. */
    public static final class Outcome {
        private final MigrationApi.Plan plan;
        private final MigrationApi.Report report;
        private final String status;
        private Outcome(final MigrationApi.Plan plan, final MigrationApi.Report report,
                        final String status) {
            this.plan = plan;
            this.report = report;
            this.status = status;
        }
        private static Outcome planned(final MigrationApi.Plan plan) {
            return new Outcome(Objects.requireNonNull(plan, "plan"), null, "planned");
        }
        private static Outcome reported(final MigrationApi.Report report) {
            final MigrationApi.Report value = Objects.requireNonNull(report, "report");
            return new Outcome(null, value,
                    value.status().name().toLowerCase(java.util.Locale.ROOT));
        }
        private static Outcome reportRequested(final String migrationId) {
            return new Outcome(null, null, "report-requested:" + migrationId);
        }
        /** @return plan, or null for report operations */ public MigrationApi.Plan planOrNull() {
            return plan;
        }
        /** @return report, or null for plan/report lookup operations */
        public MigrationApi.Report reportOrNull() { return report; }
        /** @return stable audit status */ public String status() { return status; }
    }

    /** Audit port; implementations persist sanitized before/after entries. */
    public interface AuditSink {
        /** Records intent before service invocation. */
        void before(String actor, Operation operation, String migrationId);
        /** Records completion or failure. */
        void after(String actor, Operation operation, String migrationId, String status);
    }

    private static String token(final String value, final String label) {
        final String checked = Objects.requireNonNull(value, label).trim();
        if (!checked.matches("[a-z0-9][a-z0-9_.:/-]{1,127}")) {
            throw new IllegalArgumentException("invalid " + label);
        }
        return checked;
    }
}
