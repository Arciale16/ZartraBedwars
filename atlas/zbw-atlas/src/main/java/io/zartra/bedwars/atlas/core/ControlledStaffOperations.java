package io.zartra.bedwars.atlas.core;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Guarded reversible ZBW-ADDON-323..333 staff-operation policy. */
public final class ControlledStaffOperations {
    private final Backend backend;
    private final AuditSink audit;

    public ControlledStaffOperations(final Backend backend, final AuditSink audit) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** Authorizes then delegates one typed operation; Paper never receives a raw mutation handle. */
    public CompletionStage<Result> execute(final Request request) {
        Objects.requireNonNull(request, "request");
        if (!request.permission().equals(request.operation().permission())) {
            return denied(request, "permission-denied");
        }
        if (request.targetImmune()) { return denied(request, "target-immune"); }
        if (!request.confirmed()) { return denied(request, "confirmation-required"); }
        return backend.apply(request).thenCompose(change -> audit.append(new AtlasAuditRecord(
                request.actorReference(), request.operation().name().toLowerCase(
                        java.util.Locale.ROOT), request.targetReference(), request.occurredAt(),
                "accepted:" + request.reason(), change.beforeReference(), change.afterReference()))
                .thenApply(ignored -> Result.applied(change.rollbackToken())));
    }

    /** Executes a previously issued opaque rollback token through the same guarded backend. */
    public CompletionStage<Result> rollback(final Request request, final String rollbackToken) {
        Objects.requireNonNull(request, "request");
        if (request.operation() != Operation.ROLLBACK) {
            return denied(request, "wrong-operation");
        }
        if (!request.permission().equals(Operation.ROLLBACK.permission())) {
            return denied(request, "permission-denied");
        }
        if (request.targetImmune()) { return denied(request, "target-immune"); }
        if (!request.confirmed()) { return denied(request, "confirmation-required"); }
        return backend.rollback(request, token(rollbackToken)).thenCompose(change ->
                audit.append(new AtlasAuditRecord(request.actorReference(), "rollback",
                        request.targetReference(), request.occurredAt(), "accepted:" + request.reason(),
                        change.beforeReference(), change.afterReference()))
                        .thenApply(ignored -> Result.applied(change.rollbackToken())));
    }

    private CompletionStage<Result> denied(final Request request, final String reason) {
        return audit.append(new AtlasAuditRecord(request.actorReference(),
                request.operation().name().toLowerCase(java.util.Locale.ROOT),
                request.targetReference(), request.occurredAt(), reason + ":" + request.reason(),
                "state:unchanged", "state:unchanged"))
                .thenApply(ignored -> Result.denied(reason));
    }

    private static String token(final String value) {
        if (value == null || value.isEmpty() || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("opaque token required");
        }
        return value;
    }

    /** Canonical controlled operations and exact dotted permission nodes. */
    public enum Operation {
        FORCE_JOIN("zartrabedwars.admin.game.forcejoin"),
        REVIVE("zartrabedwars.admin.game.revive"),
        SET_TEAM("zartrabedwars.admin.game.setteam"),
        BED_MUTATION("zartrabedwars.admin.game.bed"),
        ADVANCE_EVENT("zartrabedwars.admin.game.skipevent"),
        TOOL_EFFECT("zartrabedwars.admin.game.tool"),
        CAGE("zartrabedwars.admin.game.cage"),
        INSPECT("zartrabedwars.admin.game.inspect"),
        ROLLBACK("zartrabedwars.admin.game.rollback");
        private final String permission;
        Operation(final String permission) { this.permission = permission; }
        public String permission() { return permission; }
    }

    /** Immutable, pre-authorized adapter request with mandatory reason and confirmation. */
    public static final class Request {
        private final Operation operation;
        private final String actorReference;
        private final String targetReference;
        private final String permission;
        private final String reason;
        private final boolean targetImmune;
        private final boolean confirmed;
        private final Instant occurredAt;

        public Request(final Operation operation, final String actorReference,
                       final String targetReference, final String permission,
                       final String reason, final boolean targetImmune,
                       final boolean confirmed, final Instant occurredAt) {
            this.operation = Objects.requireNonNull(operation, "operation");
            this.actorReference = token(actorReference);
            this.targetReference = token(targetReference);
            this.permission = token(permission);
            this.reason = token(reason);
            this.targetImmune = targetImmune;
            this.confirmed = confirmed;
            this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
        public Operation operation() { return operation; }
        public String actorReference() { return actorReference; }
        public String targetReference() { return targetReference; }
        public String permission() { return permission; }
        public String reason() { return reason; }
        public boolean targetImmune() { return targetImmune; }
        public boolean confirmed() { return confirmed; }
        public Instant occurredAt() { return occurredAt; }
    }

    /** Opaque before/after and rollback result returned by the game-owned adapter. */
    public static final class Change {
        private final String beforeReference;
        private final String afterReference;
        private final String rollbackToken;
        public Change(final String beforeReference, final String afterReference,
                      final String rollbackToken) {
            this.beforeReference = token(beforeReference);
            this.afterReference = token(afterReference);
            this.rollbackToken = token(rollbackToken);
        }
        public String beforeReference() { return beforeReference; }
        public String afterReference() { return afterReference; }
        public String rollbackToken() { return rollbackToken; }
    }

    /** Sanitized operation result. */
    public static final class Result {
        private final boolean applied;
        private final String detail;
        private Result(final boolean applied, final String detail) {
            this.applied = applied;
            this.detail = detail;
        }
        public static Result applied(final String token) { return new Result(true, token(token)); }
        public static Result denied(final String reason) { return new Result(false, token(reason)); }
        public boolean applied() { return applied; }
        public String detail() { return detail; }
    }

    public interface Backend {
        CompletionStage<Change> apply(Request request);
        CompletionStage<Change> rollback(Request request, String rollbackToken);
    }
    public interface AuditSink {
        CompletionStage<Void> append(AtlasAuditRecord record);
    }
}
