package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import java.util.List;
import java.util.Optional;

/**
 * Repository for isolated setup sessions.
 *
 * <p>Implementations must compare the expected draft revision atomically. Durable implementations
 * may block and therefore run only on an M05 bounded worker.</p>
 */
public interface SetupSessionRepository {
    /** @return session when present */ Result<Optional<SetupSession>> find(SetupSessionId id);
    /** @return stable snapshot of active and terminal sessions retained by policy */ Result<List<SetupSession>> listSessions();
    /** @return saved session or a stale-revision failure */ Result<SetupSession> save(SetupSession session, long expectedDraftRevision);
    /** @return whether the exact-revision session was removed */ Result<Boolean> delete(SetupSessionId id, long expectedDraftRevision);
}
