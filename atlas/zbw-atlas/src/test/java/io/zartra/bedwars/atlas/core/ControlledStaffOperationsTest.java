package io.zartra.bedwars.atlas.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** ZBW-ADDON-323..333 controlled-operation authorization and rollback evidence. */
class ControlledStaffOperationsTest {
    @Test void appliesAndAuditsOnlyConfirmedExactPermissionRequest() {
        List<AtlasAuditRecord> audit = new ArrayList<>();
        ControlledStaffOperations operations = operations(audit);
        ControlledStaffOperations.Result result = operations.execute(request(
                ControlledStaffOperations.Operation.REVIVE,
                ControlledStaffOperations.Operation.REVIVE.permission(), false, true)).toCompletableFuture().join();
        assertTrue(result.applied());
        assertEquals("rollback-1", result.detail());
        assertEquals("revive", audit.get(0).action());
        assertEquals("accepted:case-123", audit.get(0).result());
    }

    @Test void rejectsPermissionImmunityAndMissingConfirmationBeforeMutation() {
        List<AtlasAuditRecord> audit = new ArrayList<>();
        ControlledStaffOperations operations = operations(audit);
        assertFalse(operations.execute(request(ControlledStaffOperations.Operation.SET_TEAM,
                "zartrabedwars.atlas.admin", false, true)).toCompletableFuture().join().applied());
        assertFalse(operations.execute(request(ControlledStaffOperations.Operation.SET_TEAM,
                ControlledStaffOperations.Operation.SET_TEAM.permission(), true, true))
                .toCompletableFuture().join().applied());
        assertFalse(operations.execute(request(ControlledStaffOperations.Operation.SET_TEAM,
                ControlledStaffOperations.Operation.SET_TEAM.permission(), false, false))
                .toCompletableFuture().join().applied());
        assertEquals(3, audit.size());
    }

    @Test void rollbackRequiresDedicatedPermissionAndOpaqueToken() {
        List<AtlasAuditRecord> audit = new ArrayList<>();
        ControlledStaffOperations operations = operations(audit);
        ControlledStaffOperations.Request rollback = request(ControlledStaffOperations.Operation.ROLLBACK,
                ControlledStaffOperations.Operation.ROLLBACK.permission(), false, true);
        assertTrue(operations.rollback(rollback, "rollback-1").toCompletableFuture().join().applied());
        assertThrows(IllegalArgumentException.class, () -> operations.rollback(rollback, "bad token"));
    }

    private static ControlledStaffOperations operations(final List<AtlasAuditRecord> audit) {
        ControlledStaffOperations.Backend backend = new ControlledStaffOperations.Backend() {
            @Override public java.util.concurrent.CompletionStage<ControlledStaffOperations.Change> apply(
                    final ControlledStaffOperations.Request request) {
                return CompletableFuture.completedFuture(new ControlledStaffOperations.Change(
                        "state-before", "state-after", "rollback-1"));
            }
            @Override public java.util.concurrent.CompletionStage<ControlledStaffOperations.Change> rollback(
                    final ControlledStaffOperations.Request request, final String token) {
                return CompletableFuture.completedFuture(new ControlledStaffOperations.Change(
                        "state-after", "state-before", token));
            }
        };
        return new ControlledStaffOperations(backend, record -> {
            audit.add(record);
            return CompletableFuture.completedFuture(null);
        });
    }

    private static ControlledStaffOperations.Request request(
            final ControlledStaffOperations.Operation operation, final String permission,
            final boolean immune, final boolean confirmed) {
        return new ControlledStaffOperations.Request(operation, "staff-1", "player-1",
                permission, "case-123", immune, confirmed, Instant.EPOCH);
    }
}
