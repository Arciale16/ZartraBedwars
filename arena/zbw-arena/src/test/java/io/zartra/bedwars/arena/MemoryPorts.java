package io.zartra.bedwars.arena;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.event.ApiEvent;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.application.ArenaAuditRecord;
import io.zartra.bedwars.arena.application.ArenaEvents;
import io.zartra.bedwars.arena.application.ArenaFailures;
import io.zartra.bedwars.arena.archive.ArenaArchive;
import io.zartra.bedwars.arena.setup.MarkerProposal;
import io.zartra.bedwars.arena.setup.SetupMutation;
import io.zartra.bedwars.arena.setup.SetupPreview;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import io.zartra.bedwars.arena.spi.ArenaArchiveStore;
import io.zartra.bedwars.arena.spi.ArenaAuditSink;
import io.zartra.bedwars.arena.spi.ArenaEventSink;
import io.zartra.bedwars.arena.spi.ArenaIdentityFactory;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.spi.MarkerDiscoveryPort;
import io.zartra.bedwars.arena.spi.SetupCommitPort;
import io.zartra.bedwars.arena.spi.SetupSessionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class MemoryPorts implements ArenaRepository, SetupSessionRepository, SetupCommitPort,
        ArenaArchiveStore, AuthorizationService, ArenaAuditSink, ArenaEventSink,
        ArenaIdentityFactory, MarkerDiscoveryPort {
    private final Map<ArenaId, ArenaRepository.Record> arenas =
            new HashMap<ArenaId, ArenaRepository.Record>();
    private final Map<SetupSessionId, SetupSession> sessions =
            new HashMap<SetupSessionId, SetupSession>();
    private final Map<DefinitionId, ArenaArchive> archives =
            new HashMap<DefinitionId, ArenaArchive>();
    final List<ArenaAuditRecord> audits = new ArrayList<ArenaAuditRecord>();
    final List<ArenaEvents.Changed> events = new ArrayList<ArenaEvents.Changed>();
    boolean allow = true;
    boolean cancel;
    boolean failArenaReads;
    boolean failArenaLists;
    boolean failArenaWrites;
    boolean failSessionReads;
    boolean failSessionLists;
    boolean failSessionWrites;
    boolean failCommits;
    boolean failArchiveReads;
    boolean failArchiveWrites;
    SetupMutation markerMutation;
    private int identitySequence = 10;

    @Override public synchronized Result<Optional<ArenaRepository.Record>> find(final ArenaId id) {
        if (failArenaReads) { return Result.failure(ArenaFailures.INVALID); }
        return Result.success(Optional.ofNullable(arenas.get(id)));
    }
    @Override public synchronized Result<List<ArenaRepository.Record>> listRecords() {
        if (failArenaLists) { return Result.failure(ArenaFailures.INVALID); }
        final List<ArenaRepository.Record> result = new ArrayList<ArenaRepository.Record>(arenas.values());
        Collections.sort(result, Comparator.comparing(value -> value.bundle().arenaId().toString()));
        return Result.success(result);
    }
    @Override public synchronized Result<ArenaRepository.Record> save(final ArenaRepository.SaveRequest request) {
        if (failArenaWrites) { return Result.failure(ArenaFailures.INVALID); }
        final ArenaRepository.Record existing = arenas.get(request.bundle().arenaId());
        if (request.expectedRevision() == 0L) {
            if (existing != null || displayConflict(request)) { return Result.failure(ArenaFailures.CONFLICT); }
            final ArenaRepository.Record created = new ArenaRepository.Record(request.bundle(), 1L,
                    request.promoteLastKnownGood() ? Optional.of(request.bundle())
                            : Optional.<io.zartra.bedwars.arena.model.ArenaBundle>empty());
            arenas.put(request.bundle().arenaId(), created);
            return Result.success(created);
        }
        if (existing == null || existing.revision() != request.expectedRevision()) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        final Optional<io.zartra.bedwars.arena.model.ArenaBundle> good =
                request.promoteLastKnownGood() ? Optional.of(request.bundle())
                        : existing.lastKnownGood();
        final ArenaRepository.Record changed = new ArenaRepository.Record(request.bundle(),
                existing.revision() + 1L, good);
        arenas.put(request.bundle().arenaId(), changed);
        return Result.success(changed);
    }
    private boolean displayConflict(final ArenaRepository.SaveRequest request) {
        for (ArenaRepository.Record record : arenas.values()) {
            if (record.bundle().arena().displayName().equalsIgnoreCase(
                    request.bundle().arena().displayName())
                    || record.bundle().mapId().equals(request.bundle().mapId())) { return true; }
        }
        return false;
    }
    @Override public synchronized Result<Boolean> delete(final ArenaId id, final long expectedRevision) {
        if (failArenaWrites) { return Result.failure(ArenaFailures.INVALID); }
        final ArenaRepository.Record record = arenas.get(id);
        if (record == null || record.revision() != expectedRevision) {
            return Result.success(Boolean.FALSE);
        }
        arenas.remove(id);
        return Result.success(Boolean.TRUE);
    }
    @Override public synchronized Result<ArenaRepository.Record> restoreLastKnownGood(
            final ArenaId id, final long expectedRevision) {
        if (failArenaWrites) { return Result.failure(ArenaFailures.INVALID); }
        final ArenaRepository.Record record = arenas.get(id);
        if (record == null || record.revision() != expectedRevision
                || !record.lastKnownGood().isPresent()) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        final ArenaRepository.Record restored = new ArenaRepository.Record(
                record.lastKnownGood().get(), record.revision() + 1L, record.lastKnownGood());
        arenas.put(id, restored);
        return Result.success(restored);
    }

    @Override public synchronized Result<Optional<SetupSession>> find(final SetupSessionId id) {
        if (failSessionReads) { return Result.failure(ArenaFailures.INVALID); }
        return Result.success(Optional.ofNullable(sessions.get(id)));
    }
    @Override public synchronized Result<List<SetupSession>> listSessions() {
        if (failSessionLists) { return Result.failure(ArenaFailures.INVALID); }
        return Result.success(new ArrayList<SetupSession>(sessions.values()));
    }
    @Override public synchronized Result<SetupSession> save(final SetupSession session,
                                                            final long expectedDraftRevision) {
        if (failSessionWrites) { return Result.failure(ArenaFailures.INVALID); }
        final SetupSession existing = sessions.get(session.id());
        if (expectedDraftRevision == 0L && existing == null) {
            sessions.put(session.id(), session);
            return Result.success(session);
        }
        if (existing == null || existing.draftRevision() != expectedDraftRevision) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        sessions.put(session.id(), session);
        return Result.success(session);
    }
    @Override public synchronized Result<Boolean> delete(final SetupSessionId id,
                                                         final long expectedDraftRevision) {
        final SetupSession existing = sessions.get(id);
        if (existing == null || existing.draftRevision() != expectedDraftRevision) {
            return Result.success(Boolean.FALSE);
        }
        sessions.remove(id);
        return Result.success(Boolean.TRUE);
    }
    @Override public synchronized Result<SetupCommitPort.CommitResult> commit(
            final SetupSession session, final SetupPreview preview,
            final boolean promoteLastKnownGood) {
        if (failCommits) { return Result.failure(ArenaFailures.INVALID); }
        final ArenaRepository.Record arena = arenas.get(session.arenaId());
        final SetupSession stored = sessions.get(session.id());
        if (arena == null || stored == null || arena.revision() != session.baseRepositoryRevision()
                || stored.draftRevision() != session.draftRevision()
                || !preview.matches(session)) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        final ArenaRepository.Record changed = new ArenaRepository.Record(preview.bundle(),
                arena.revision() + 1L, promoteLastKnownGood ? Optional.of(preview.bundle())
                        : arena.lastKnownGood());
        final SetupSession committed = session.committed();
        arenas.put(session.arenaId(), changed);
        sessions.put(session.id(), committed);
        return Result.success(new SetupCommitPort.CommitResult(changed, committed));
    }

    @Override public synchronized Result<ArenaArchive> save(final ArenaArchive archive) {
        if (failArchiveWrites) { return Result.failure(ArenaFailures.INVALID); }
        if (archives.putIfAbsent(archive.archiveId(), archive) != null) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        return Result.success(archive);
    }
    @Override public synchronized Result<Optional<ArenaArchive>> find(final DefinitionId archiveId) {
        if (failArchiveReads) { return Result.failure(ArenaFailures.INVALID); }
        return Result.success(Optional.ofNullable(archives.get(archiveId)));
    }
    @Override public synchronized Result<List<ArenaArchive>> listArchives() {
        return Result.success(new ArrayList<ArenaArchive>(archives.values()));
    }

    @Override public AuthorizationDecision authorize(final AuthorizationRequest request) {
        return allow ? AuthorizationDecision.allow(ArenaTestFixture.id("policy/allowed"))
                : AuthorizationDecision.deny(ArenaTestFixture.id("policy/denied"));
    }
    @Override public void publish(final ArenaAuditRecord record) { audits.add(record); }
    @Override public ApiEvent.Decision before(final ArenaEvents.BeforeChange event) {
        return cancel ? ApiEvent.Decision.cancel(ArenaTestFixture.id("policy/cancelled"))
                : ApiEvent.Decision.proceed();
    }
    @Override public void after(final ArenaEvents.Changed event) { events.add(event); }
    @Override public ArenaId newArenaId() {
        return ArenaId.of(new UUID(3L, identitySequence++));
    }
    @Override public MapId newMapId() { return MapId.of(new UUID(4L, identitySequence++)); }
    @Override public Result<MarkerProposal> discover(final SetupSession session) {
        if (markerMutation == null) { return Result.failure(ArenaFailures.INVALID); }
        return Result.success(new MarkerProposal(session.id(), session.draftRevision(),
                Collections.singletonList(markerMutation)));
    }
}
