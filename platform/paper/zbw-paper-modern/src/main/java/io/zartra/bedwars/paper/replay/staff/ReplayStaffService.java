package io.zartra.bedwars.paper.replay.staff;

import io.zartra.bedwars.paper.replay.PaperReplayService;
import io.zartra.bedwars.paper.replay.ReplayAudience;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Async permission-gated replay search, moderation and deterministic audit service. */
public final class ReplayStaffService {
    /** Destructive replay-management permission. */
    public static final String ADMIN_PERMISSION = "zartrabedwars.replay.admin";

    private static final Comparator<ReplayStaffRecord> ORDER =
            Comparator.comparing((ReplayStaffRecord value) ->
                    value.session().metadata().createdAt())
                    .thenComparing(value ->
                            value.session().metadata().replayId().toString());

    private final ReplayStaffStore store;
    private final ReplaySessionRepository sessions;
    private final ReplayStaffAuditSink audit;
    private final Supplier<Instant> time;
    private final AtomicLong auditSequence = new AtomicLong();

    /** Creates a service over non-blocking authoritative ports. */
    public ReplayStaffService(final ReplayStaffStore store,
                              final ReplaySessionRepository sessions,
                              final ReplayStaffAuditSink audit,
                              final Supplier<Instant> time) {
        this.store = Objects.requireNonNull(store, "store");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.time = Objects.requireNonNull(time, "time");
    }

    /** Searches replay metadata with bounded deterministic output. */
    public CompletionStage<ReplayStaffResult> search(final ReplayAudience actor,
                                                     final ReplayStaffQuery query) {
        Objects.requireNonNull(query, "query");
        if (!staff(actor)) { return audited(actor, ReplayStaffAction.SEARCH, null,
                ReplayStaffResult.of(ReplayStaffResult.Status.FORBIDDEN)); }
        return protect(store.search(query)).thenCompose(rows -> {
            if (!rows.isPresent()) {
                return audited(actor, ReplayStaffAction.SEARCH, null,
                        ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
            }
            final List<ReplayStaffRecord> accepted = new ArrayList<ReplayStaffRecord>();
            for (ReplayStaffRecord row : rows.get()) {
                if (row == null || !matches(row, query)) {
                    return audited(actor, ReplayStaffAction.SEARCH, null,
                            ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
                }
                accepted.add(row);
            }
            accepted.sort(ORDER);
            if (accepted.size() > query.limit()) {
                accepted.subList(query.limit(), accepted.size()).clear();
            }
            return audited(actor, ReplayStaffAction.SEARCH, null,
                    ReplayStaffResult.search(accepted));
        });
    }

    /** Loads metadata and ordered events for one staff-authorized replay. */
    public CompletionStage<ReplayStaffResult> inspect(final ReplayAudience actor,
                                                      final ReplayId replayId) {
        Objects.requireNonNull(replayId, "replayId");
        if (!staff(actor)) { return audited(actor, ReplayStaffAction.INSPECT, replayId,
                ReplayStaffResult.of(ReplayStaffResult.Status.FORBIDDEN)); }
        return find(actor, ReplayStaffAction.INSPECT, replayId);
    }

    /** Idempotently marks or unmarks a replay for staff review. */
    public CompletionStage<ReplayStaffResult> mark(final ReplayAudience actor,
                                                   final ReplayId replayId,
                                                   final boolean marked) {
        Objects.requireNonNull(replayId, "replayId");
        if (!admin(actor)) { return audited(actor, ReplayStaffAction.MARK, replayId,
                ReplayStaffResult.of(ReplayStaffResult.Status.FORBIDDEN)); }
        return protect(store.mark(replayId, marked)).thenCompose(updated -> audited(
                actor, ReplayStaffAction.MARK, replayId, updated.isPresent() && updated.get()
                        ? ReplayStaffResult.of(ReplayStaffResult.Status.SUCCESS)
                        : ReplayStaffResult.of(updated.isPresent()
                                ? ReplayStaffResult.Status.NOT_FOUND
                                : ReplayStaffResult.Status.FAILED)));
    }

    /** Archives one completed replay using compare-state persistence. */
    public CompletionStage<ReplayStaffResult> archive(final ReplayAudience actor,
                                                      final ReplayId replayId) {
        Objects.requireNonNull(replayId, "replayId");
        if (!admin(actor)) { return audited(actor, ReplayStaffAction.ARCHIVE, replayId,
                ReplayStaffResult.of(ReplayStaffResult.Status.FORBIDDEN)); }
        return protect(sessions.findSession(replayId)).thenCompose(found -> {
            if (!found.isPresent()) {
                return audited(actor, ReplayStaffAction.ARCHIVE, replayId,
                        ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
            }
            if (!found.get().isPresent()) {
                return audited(actor, ReplayStaffAction.ARCHIVE, replayId,
                        ReplayStaffResult.of(ReplayStaffResult.Status.NOT_FOUND));
            }
            final ReplaySession session = found.get().get();
            if (session.state() != ReplayState.COMPLETED) {
                return audited(actor, ReplayStaffAction.ARCHIVE, replayId,
                        ReplayStaffResult.of(ReplayStaffResult.Status.INVALID_STATE));
            }
            return protect(sessions.save(session.archive(), ReplayState.COMPLETED))
                    .thenCompose(saved -> audited(actor, ReplayStaffAction.ARCHIVE, replayId,
                            saveResult(saved)));
        });
    }

    /** Removes provider-validated invalid data; valid recordings are never deleted. */
    public CompletionStage<ReplayStaffResult> removeInvalid(final ReplayAudience actor,
                                                            final ReplayId replayId) {
        Objects.requireNonNull(replayId, "replayId");
        if (!admin(actor)) { return audited(actor, ReplayStaffAction.REMOVE_INVALID, replayId,
                ReplayStaffResult.of(ReplayStaffResult.Status.FORBIDDEN)); }
        return protect(store.find(replayId)).thenCompose(found -> {
            if (!found.isPresent()) {
                return audited(actor, ReplayStaffAction.REMOVE_INVALID, replayId,
                        ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
            }
            if (!found.get().isPresent()) {
                return audited(actor, ReplayStaffAction.REMOVE_INVALID, replayId,
                        ReplayStaffResult.of(ReplayStaffResult.Status.NOT_FOUND));
            }
            if (found.get().get().session().state() != ReplayState.FAILED) {
                return audited(actor, ReplayStaffAction.REMOVE_INVALID, replayId,
                        ReplayStaffResult.of(ReplayStaffResult.Status.INVALID_STATE));
            }
            return protect(store.removeInvalid(replayId)).thenCompose(removed -> audited(
                    actor, ReplayStaffAction.REMOVE_INVALID, replayId,
                    removed.isPresent() && removed.get()
                            ? ReplayStaffResult.of(ReplayStaffResult.Status.SUCCESS)
                            : ReplayStaffResult.of(removed.isPresent()
                                    ? ReplayStaffResult.Status.NOT_FOUND
                                    : ReplayStaffResult.Status.FAILED)));
        });
    }

    /** Audits the existing viewer-open result without duplicating playback behavior. */
    public CompletionStage<ReplayStaffResult> auditOpen(final ReplayAudience actor,
                                                        final ReplayId replayId,
                                                        final ReplayStaffResult.Status outcome) {
        if (!staff(actor)) {
            return audited(actor, ReplayStaffAction.OPEN, replayId,
                    ReplayStaffResult.of(ReplayStaffResult.Status.FORBIDDEN));
        }
        return audited(actor, ReplayStaffAction.OPEN, replayId,
                ReplayStaffResult.of(Objects.requireNonNull(outcome, "outcome")));
    }

    private CompletionStage<ReplayStaffResult> find(final ReplayAudience actor,
                                                    final ReplayStaffAction action,
                                                    final ReplayId replayId) {
        return protect(store.find(replayId)).thenCompose(found -> {
            final ReplayStaffResult result;
            if (!found.isPresent()) {
                result = ReplayStaffResult.of(ReplayStaffResult.Status.FAILED);
            } else if (!found.get().isPresent()) {
                result = ReplayStaffResult.of(ReplayStaffResult.Status.NOT_FOUND);
            } else {
                result = ReplayStaffResult.record(found.get().get());
            }
            return audited(actor, action, replayId, result);
        });
    }

    private CompletionStage<ReplayStaffResult> audited(final ReplayAudience actor,
                                                       final ReplayStaffAction action,
                                                       final ReplayId replayId,
                                                       final ReplayStaffResult result) {
        Objects.requireNonNull(actor, "actor");
        final ReplayStaffAuditRecord record = new ReplayStaffAuditRecord(
                auditSequence.getAndIncrement(), Objects.requireNonNull(time.get(), "time"),
                actor.playerId(), action, replayId, result.status());
        final CompletionStage<Void> appended;
        try {
            appended = audit.append(record);
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(
                    ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
        }
        if (appended == null) {
            return CompletableFuture.completedFuture(
                    ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
        }
        return appended.handle((ignored, failure) -> failure == null ? result
                : ReplayStaffResult.of(ReplayStaffResult.Status.FAILED));
    }

    private static ReplayStaffResult saveResult(
            final Optional<ReplaySessionRepository.SaveResult> saved) {
        if (!saved.isPresent()) { return ReplayStaffResult.of(ReplayStaffResult.Status.FAILED); }
        switch (saved.get()) {
            case UPDATED: return ReplayStaffResult.of(ReplayStaffResult.Status.SUCCESS);
            case NOT_FOUND: return ReplayStaffResult.of(ReplayStaffResult.Status.NOT_FOUND);
            case CONFLICT: return ReplayStaffResult.of(ReplayStaffResult.Status.INVALID_STATE);
            default: return ReplayStaffResult.of(ReplayStaffResult.Status.FAILED);
        }
    }

    private static boolean matches(final ReplayStaffRecord row, final ReplayStaffQuery query) {
        final ReplaySession session = row.session();
        final long duration = row.durationMillis();
        return (!query.playerId().isPresent()
                || session.metadata().participants().contains(query.playerId().get()))
                && (!query.matchId().isPresent()
                || session.metadata().matchId().equals(query.matchId().get()))
                && (!query.createdFrom().isPresent()
                || !session.metadata().createdAt().isBefore(query.createdFrom().get()))
                && (!query.createdTo().isPresent()
                || !session.metadata().createdAt().isAfter(query.createdTo().get()))
                && (!query.minimumDurationMillis().isPresent()
                || duration >= query.minimumDurationMillis().get())
                && (!query.maximumDurationMillis().isPresent()
                || duration <= query.maximumDurationMillis().get());
    }

    private static boolean staff(final ReplayAudience actor) {
        Objects.requireNonNull(actor, "actor");
        return actor.hasPermission(PaperReplayService.STAFF_PERMISSION);
    }

    private static boolean admin(final ReplayAudience actor) {
        Objects.requireNonNull(actor, "actor");
        return actor.hasPermission(ADMIN_PERMISSION);
    }

    private static <T> CompletionStage<Optional<T>> protect(final CompletionStage<T> stage) {
        if (stage == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return stage.handle((value, failure) -> failure == null && value != null
                ? Optional.of(value) : Optional.empty());
    }
}
