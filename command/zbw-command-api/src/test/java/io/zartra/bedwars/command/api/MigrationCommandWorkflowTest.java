package io.zartra.bedwars.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class MigrationCommandWorkflowTest {
    @Test
    void routesAllOperationsWithCanonicalPermissionsAndAudit() {
        final List<String> audit = new ArrayList<String>();
        final MigrationCommandWorkflow workflow = new MigrationCommandWorkflow(
                new Service(), sink(audit));
        final MigrationApi.Request dry = request(MigrationApi.Mode.DRY_RUN);
        final MigrationApi.Request apply = request(MigrationApi.Mode.APPLY);

        assertNotNull(workflow.execute("operator/one",
                MigrationCommandWorkflow.Operation.VALIDATE, dry, "migration/test")
                .toCompletableFuture().join().planOrNull());
        assertNotNull(workflow.execute("operator/one",
                MigrationCommandWorkflow.Operation.PLAN, dry, "migration/test")
                .toCompletableFuture().join().planOrNull());
        assertNotNull(workflow.execute("operator/one",
                MigrationCommandWorkflow.Operation.APPLY, apply, "migration/test")
                .toCompletableFuture().join().reportOrNull());
        assertNotNull(workflow.execute("operator/one",
                MigrationCommandWorkflow.Operation.ROLLBACK, null, "migration/test")
                .toCompletableFuture().join().reportOrNull());
        assertEquals("report-requested:migration/test", workflow.execute("operator/one",
                MigrationCommandWorkflow.Operation.REPORT, null, "migration/test")
                .toCompletableFuture().join().status());
        assertEquals("zartrabedwars.admin.migrate-layout.apply",
                MigrationCommandWorkflow.Operation.APPLY.permission().value());
        assertEquals(10, audit.size());
        assertThrows(IllegalArgumentException.class, () -> workflow.execute("operator/one",
                MigrationCommandWorkflow.Operation.APPLY, dry, "migration/test"));
        assertEquals(12, audit.size());
        assertEquals("after:APPLY:failed", audit.get(11));
    }

    private static MigrationApi.Request request(final MigrationApi.Mode mode) {
        return new MigrationApi.Request("migration/test", "operator-export", "lawful-input",
                mode, MigrationApi.ConflictPolicy.FAIL,
                Collections.<MigrationApi.Record>emptyList());
    }

    private static MigrationCommandWorkflow.AuditSink sink(final List<String> audit) {
        return new MigrationCommandWorkflow.AuditSink() {
            @Override public void before(final String actor,
                                         final MigrationCommandWorkflow.Operation operation,
                                         final String migrationId) {
                audit.add("before:" + operation);
            }
            @Override public void after(final String actor,
                                        final MigrationCommandWorkflow.Operation operation,
                                        final String migrationId, final String status) {
                audit.add("after:" + operation + ":" + status);
            }
        };
    }

    private static final class Service implements MigrationApi.Service {
        @Override public CompletionStage<MigrationApi.Plan> plan(
                final MigrationApi.Request request) {
            return CompletableFuture.completedFuture(new MigrationApi.Plan(
                    request.migrationId(), request.mode(),
                    Collections.<MigrationApi.Record>emptyList(),
                    Collections.<String>emptyList(), true));
        }
        @Override public CompletionStage<MigrationApi.Report> execute(
                final MigrationApi.Request request) {
            return CompletableFuture.completedFuture(new MigrationApi.Report(
                    request.migrationId(), MigrationApi.Status.APPLIED, 0, 0,
                    Collections.<String>emptyList()));
        }
        @Override public CompletionStage<MigrationApi.Report> rollback(
                final String migrationId) {
            return CompletableFuture.completedFuture(new MigrationApi.Report(
                    migrationId, MigrationApi.Status.ROLLED_BACK, 0, 0,
                    Collections.<String>emptyList()));
        }
    }
}
