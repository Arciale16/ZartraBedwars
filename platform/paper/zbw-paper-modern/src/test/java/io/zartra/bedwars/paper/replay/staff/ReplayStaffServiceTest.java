package io.zartra.bedwars.paper.replay.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.paper.replay.PaperReplayService;
import io.zartra.bedwars.paper.replay.ReplayAudience;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerResult;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-001/005/006/007/008/009/010 staff-tool and audit tests. */
final class ReplayStaffServiceTest {
    private static final UUID ACTOR = new UUID(0L, 1709L);
    private static final UUID PLAYER = new UUID(0L, 1710L);
    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");
    private static final ReplayId FIRST =
            ReplayId.parse("00000000-0000-0000-0000-000000001709");
    private static final ReplayId SECOND =
            ReplayId.parse("00000000-0000-0000-0000-000000001710");

    @Test
    void permissionBoundariesDenyAndAuditSearchAndAdminActions() {
        final Harness harness = harness();
        final FakeAudience ordinary = new FakeAudience(Collections.emptySet());
        final FakeAudience staff = new FakeAudience(
                Collections.singleton(PaperReplayService.STAFF_PERMISSION));

        assertEquals(ReplayStaffResult.Status.FORBIDDEN,
                join(harness.service.search(ordinary, playerQuery())).status());
        assertEquals(ReplayStaffResult.Status.FORBIDDEN,
                join(harness.service.mark(staff, FIRST, true)).status());
        assertEquals(ReplayStaffResult.Status.FORBIDDEN,
                join(harness.service.archive(staff, FIRST)).status());
        assertEquals(3, harness.audit.records.size());
        assertEquals(ReplayStaffAction.SEARCH, harness.audit.records.get(0).action());
        assertEquals(ReplayStaffResult.Status.FORBIDDEN,
                harness.audit.records.get(2).outcome());
    }

    @Test
    void searchSupportsPlayerMatchDateDurationAndDeterministicOrdering() {
        final Harness harness = harness();
        final FakeAudience staff = staff();
        final ReplayStaffResult byPlayer = join(harness.service.search(staff, playerQuery()));
        assertEquals(ReplayStaffResult.Status.SUCCESS, byPlayer.status());
        assertEquals(List.of(FIRST, SECOND), ids(byPlayer.records()));

        final ReplayStaffQuery byMatch = new ReplayStaffQuery(null,
                MatchId.parse("00000000-0000-0000-0000-000000001710"),
                null, null, null, null, 10);
        assertEquals(List.of(SECOND), ids(join(harness.service.search(staff, byMatch)).records()));
        final ReplayStaffQuery byDateAndDuration = new ReplayStaffQuery(null, null,
                NOW.minusSeconds(1), NOW.plusSeconds(1), 20L, 20L, 1);
        assertEquals(List.of(FIRST),
                ids(join(harness.service.search(staff, byDateAndDuration)).records()));
    }

    @Test
    void inspectExposesImmutableOrderedEventsAndInvalidReplayIsNotFound() {
        final Harness harness = harness();
        final ReplayStaffResult inspected = join(harness.service.inspect(staff(), FIRST));
        assertEquals(ReplayStaffResult.Status.SUCCESS, inspected.status());
        assertEquals(2, inspected.record().orElseThrow().session().timeline().events().size());
        assertThrows(UnsupportedOperationException.class, () -> inspected.record().orElseThrow()
                .session().timeline().events().add(event(2L, 30L)));
        assertEquals(ReplayStaffResult.Status.NOT_FOUND,
                join(harness.service.inspect(staff(),
                        ReplayId.parse("00000000-0000-0000-0000-000000009999"))).status());
    }

    @Test
    void adminCanMarkArchiveAndRemoveOnlyFailedReplay() {
        final Harness harness = harness();
        final FakeAudience admin = admin();

        assertEquals(ReplayStaffResult.Status.SUCCESS,
                join(harness.service.mark(admin, FIRST, true)).status());
        assertTrue(harness.store.records.get(FIRST).marked());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                join(harness.service.archive(admin, FIRST)).status());
        assertEquals(ReplayState.ARCHIVED, harness.repository.sessions.get(FIRST).state());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                join(harness.service.removeInvalid(admin, SECOND)).status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                join(harness.service.removeInvalid(admin, harness.failedId)).status());
        assertFalse(harness.store.records.containsKey(harness.failedId));
    }

    @Test
    void auditRecordsAreMonotonicDeterministicAndFailureIsSanitized() {
        final Harness harness = harness();
        final FakeAudience staff = staff();
        join(harness.service.search(staff, playerQuery()));
        join(harness.service.inspect(staff, FIRST));
        join(harness.service.auditOpen(staff, FIRST, ReplayStaffResult.Status.SUCCESS));

        assertEquals(List.of(0L, 1L, 2L), List.of(
                harness.audit.records.get(0).sequence(),
                harness.audit.records.get(1).sequence(),
                harness.audit.records.get(2).sequence()));
        assertTrue(harness.audit.records.stream()
                .allMatch(record -> record.occurredAt().equals(NOW)));
        assertEquals(ReplayStaffAction.OPEN, harness.audit.records.get(2).action());

        harness.audit.fail = true;
        assertEquals(ReplayStaffResult.Status.FAILED,
                join(harness.service.inspect(staff, FIRST)).status());
    }

    @Test
    void searchQueryRejectsUnboundedOrInvertedFilters() {
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                null, null, null, null, null, null, 10));
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                PlayerId.of(PLAYER), null, NOW, NOW.minusSeconds(1), null, null, 10));
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                PlayerId.of(PLAYER), null, null, null, 5L, 4L, 10));
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                PlayerId.of(PLAYER), null, null, null, null, null, 101));
    }


    @Test
    void staffCommandRouterCoversSearchInspectionOpenAndAdminActions() {
        Harness harness = harness();
        ReplayStaffCommandRouter router = new ReplayStaffCommandRouter(
                harness.service, (actor, replayId) -> CompletableFuture.completedFuture(
                        ReplayViewerResult.Status.SUCCESS));
        final FakeAudience admin = admin();

        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "search", "player", PLAYER.toString()).status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "search", "match",
                        "00000000-0000-0000-0000-000000001709", "2").status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "search", "date",
                        "2026-07-26T23:59:59Z", "2026-07-27T00:00:02Z").status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "search", "duration", "10", "50").status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "inspect", FIRST.toString()).status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "open", FIRST.toString()).status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "mark", FIRST.toString(), "true").status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "archive", FIRST.toString()).status());
        assertEquals(ReplayStaffResult.Status.SUCCESS,
                route(router, admin, "remove-invalid", harness.failedId.toString()).status());

        assertEquals(ReplayStaffResult.Status.INVALID_STATE, route(router, admin).status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "search", "unknown", "x").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "mark", FIRST.toString(), "maybe").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "inspect", "bad").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "unknown").status());
    }

    @Test
    void repositoryAndProviderFailuresMapToStableStaffOutcomes() {
        final Harness harness = harness();
        final FakeAudience admin = admin();
        harness.repository.sessions.remove(FIRST);
        assertEquals(ReplayStaffResult.Status.NOT_FOUND,
                join(harness.service.archive(admin, FIRST)).status());
        assertEquals(ReplayStaffResult.Status.NOT_FOUND,
                join(harness.service.mark(admin,
                        ReplayId.parse("00000000-0000-0000-0000-000000009999"), true)).status());
        assertEquals(ReplayStaffResult.Status.NOT_FOUND,
                join(harness.service.removeInvalid(admin,
                        ReplayId.parse("00000000-0000-0000-0000-000000009999"))).status());
    }


    @Test
    void staffModelsCoverOptionalBoundsAndInvalidInputs() {
        final ReplayStaffQuery fromOnly = new ReplayStaffQuery(
                null, null, NOW, null, null, null, 1);
        final ReplayStaffQuery same = new ReplayStaffQuery(
                null, null, NOW, null, null, null, 1);
        assertEquals(fromOnly, same);
        assertEquals(fromOnly.hashCode(), same.hashCode());
        assertFalse(fromOnly.equals("query"));
        assertTrue(fromOnly.createdFrom().isPresent());
        assertFalse(fromOnly.createdTo().isPresent());
        assertFalse(fromOnly.playerId().isPresent());
        assertFalse(fromOnly.matchId().isPresent());
        assertFalse(fromOnly.minimumDurationMillis().isPresent());
        assertFalse(fromOnly.maximumDurationMillis().isPresent());
        assertEquals(1, fromOnly.limit());

        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                PlayerId.of(PLAYER), null, null, null, -1L, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                PlayerId.of(PLAYER), null, null, null, null, -1L, 1));
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffQuery(
                PlayerId.of(PLAYER), null, null, null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ReplayStaffAuditRecord(
                -1L, NOW, ACTOR, ReplayStaffAction.SEARCH, null,
                ReplayStaffResult.Status.SUCCESS));
        final ReplayStaffAuditRecord audit = new ReplayStaffAuditRecord(
                0L, NOW, ACTOR, ReplayStaffAction.SEARCH, null,
                ReplayStaffResult.Status.SUCCESS);
        assertFalse(audit.replayId().isPresent());
        assertEquals(ACTOR, audit.actorId());
        assertThrows(NullPointerException.class, () -> ReplayStaffResult.search(null));
        assertThrows(IllegalArgumentException.class, () ->
                ReplayStaffResult.search(Collections.singletonList(null)));
        assertThrows(NullPointerException.class, () -> ReplayStaffResult.record(null));
    }

    @Test
    void routerRejectsEveryMalformedArityAndFilterValue() {
        final Harness harness = harness();
        final ReplayStaffCommandRouter router = new ReplayStaffCommandRouter(
                harness.service, (actor, replayId) -> CompletableFuture.completedFuture(
                        ReplayViewerResult.Status.SUCCESS));
        final FakeAudience admin = admin();
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "search").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "search", "player", PLAYER.toString(), "101").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "search", "date", NOW.toString()).status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "search", "duration", "x", "1").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "open").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "archive").status());
        assertEquals(ReplayStaffResult.Status.INVALID_STATE,
                route(router, admin, "remove-invalid").status());
    }
    private static ReplayStaffResult route(final ReplayStaffCommandRouter router,
                                           final ReplayAudience actor,
                                           final String... tokens) {
        return router.route(actor, Arrays.asList(tokens)).toCompletableFuture().join();
    }
    private static Harness harness() {
        final ReplaySession first = completed(FIRST,
                MatchId.parse("00000000-0000-0000-0000-000000001709"), NOW, 20L);
        final ReplaySession second = completed(SECOND,
                MatchId.parse("00000000-0000-0000-0000-000000001710"),
                NOW.plusSeconds(1), 40L);
        final ReplayId failedId =
                ReplayId.parse("00000000-0000-0000-0000-000000001711");
        final ReplaySession failed = ReplaySession.create(metadata(failedId,
                MatchId.parse("00000000-0000-0000-0000-000000001711"),
                NOW.plusSeconds(2))).fail("corrupt");
        final FakeStore store = new FakeStore(first, second, failed);
        final FakeRepository repository = new FakeRepository(first, second, failed);
        final RecordingAudit audit = new RecordingAudit();
        return new Harness(new ReplayStaffService(store, repository, audit, () -> NOW),
                store, repository, audit, failedId);
    }

    private static ReplaySession completed(final ReplayId replayId, final MatchId matchId,
                                           final Instant createdAt, final long duration) {
        return ReplaySession.create(metadata(replayId, matchId, createdAt)).start()
                .record(event(0L, duration / 2L))
                .record(event(1L, duration)).complete();
    }

    private static ReplayMetadata metadata(final ReplayId replayId, final MatchId matchId,
                                           final Instant createdAt) {
        return new ReplayMetadata(replayId, matchId, createdAt, 1,
                Collections.singleton(PlayerId.of(PLAYER)), true);
    }

    private static ReplayEvent event(final long sequence, final long offset) {
        return new ReplayEvent("event-" + sequence, sequence, offset,
                NOW.plusMillis(offset), ReplayEvent.Source.GAME, "match.event",
                Collections.singletonMap("state", "RUNNING"));
    }

    private static ReplayStaffQuery playerQuery() {
        return new ReplayStaffQuery(PlayerId.of(PLAYER), null, null, null,
                1L, null, 10);
    }

    private static FakeAudience staff() {
        return new FakeAudience(EnumSet.of(Permission.STAFF));
    }

    private static FakeAudience admin() {
        return new FakeAudience(EnumSet.of(Permission.STAFF, Permission.ADMIN, Permission.VIEW));
    }

    private static List<ReplayId> ids(final List<ReplayStaffRecord> rows) {
        final List<ReplayId> result = new ArrayList<ReplayId>();
        for (ReplayStaffRecord row : rows) {
            result.add(row.session().metadata().replayId());
        }
        return result;
    }

    private static ReplayStaffResult join(final CompletionStage<ReplayStaffResult> result) {
        return result.toCompletableFuture().join();
    }

    private enum Permission { VIEW, STAFF, ADMIN }

    private static final class FakeAudience implements ReplayAudience {
        private final Set<?> permissions;
        private FakeAudience(final Set<?> permissions) { this.permissions = permissions; }
        @Override public UUID playerId() { return ACTOR; }
        @Override public boolean hasPermission(final String permission) {
            return PaperReplayService.VIEW_PERMISSION.equals(permission)
                    && permissions.contains(Permission.VIEW)
                    || PaperReplayService.STAFF_PERMISSION.equals(permission)
                    && permissions.contains(Permission.STAFF)
                    || ReplayStaffService.ADMIN_PERMISSION.equals(permission)
                    && permissions.contains(Permission.ADMIN);
        }
        @Override public Object enterSpectatorReplay() { return "restore"; }
        @Override public void leaveSpectatorReplay(final Object restoration) { }
    }

    private static final class RecordingAudit implements ReplayStaffAuditSink {
        private final List<ReplayStaffAuditRecord> records =
                new ArrayList<ReplayStaffAuditRecord>();
        private boolean fail;
        @Override public CompletionStage<Void> append(final ReplayStaffAuditRecord record) {
            if (fail) {
                final CompletableFuture<Void> failed = new CompletableFuture<Void>();
                failed.completeExceptionally(new IllegalStateException("audit unavailable"));
                return failed;
            }
            records.add(record);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FakeStore implements ReplayStaffStore {
        private final Map<ReplayId, ReplayStaffRecord> records =
                new HashMap<ReplayId, ReplayStaffRecord>();
        private FakeStore(final ReplaySession... sessions) {
            for (ReplaySession session : sessions) {
                records.put(session.metadata().replayId(),
                        new ReplayStaffRecord(session, false));
            }
        }
        @Override public CompletionStage<List<ReplayStaffRecord>> search(
                final ReplayStaffQuery query) {
            final List<ReplayStaffRecord> result = new ArrayList<ReplayStaffRecord>();
            for (ReplayStaffRecord row : records.values()) {
                final ReplaySession session = row.session();
                final long duration = row.durationMillis();
                if ((!query.playerId().isPresent()
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
                        || duration <= query.maximumDurationMillis().get())) {
                    result.add(row);
                }
            }
            Collections.reverse(result);
            return CompletableFuture.completedFuture(result);
        }
        @Override public CompletionStage<Optional<ReplayStaffRecord>> find(
                final ReplayId replayId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(records.get(replayId)));
        }
        @Override public CompletionStage<Boolean> mark(final ReplayId replayId,
                                                       final boolean marked) {
            final ReplayStaffRecord current = records.get(replayId);
            if (current == null) { return CompletableFuture.completedFuture(false); }
            records.put(replayId, new ReplayStaffRecord(current.session(), marked));
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletionStage<Boolean> removeInvalid(final ReplayId replayId) {
            return CompletableFuture.completedFuture(records.remove(replayId) != null);
        }
    }

    private static final class FakeRepository implements ReplaySessionRepository {
        private final Map<ReplayId, ReplaySession> sessions =
                new HashMap<ReplayId, ReplaySession>();
        private FakeRepository(final ReplaySession... initial) {
            for (ReplaySession session : initial) {
                sessions.put(session.metadata().replayId(), session);
            }
        }
        @Override public CompletionStage<Boolean> create(final ReplaySession session) {
            return CompletableFuture.completedFuture(
                    sessions.putIfAbsent(session.metadata().replayId(), session) == null);
        }
        @Override public CompletionStage<SaveResult> save(final ReplaySession session,
                                                          final ReplayState expectedState) {
            final ReplaySession current = sessions.get(session.metadata().replayId());
            if (current == null) {
                return CompletableFuture.completedFuture(SaveResult.NOT_FOUND);
            }
            if (current.state() != expectedState) {
                return CompletableFuture.completedFuture(SaveResult.CONFLICT);
            }
            sessions.put(session.metadata().replayId(), session);
            return CompletableFuture.completedFuture(SaveResult.UPDATED);
        }
        @Override public CompletionStage<Optional<ReplaySession>> findSession(
                final ReplayId replayId) {
            return CompletableFuture.completedFuture(
                    Optional.ofNullable(sessions.get(replayId)));
        }
    }

    private static final class Harness {
        private final ReplayStaffService service;
        private final FakeStore store;
        private final FakeRepository repository;
        private final RecordingAudit audit;
        private final ReplayId failedId;
        private Harness(final ReplayStaffService service, final FakeStore store,
                        final FakeRepository repository, final RecordingAudit audit,
                        final ReplayId failedId) {
            this.service = service;
            this.store = store;
            this.repository = repository;
            this.audit = audit;
            this.failedId = failedId;
        }
    }
}
