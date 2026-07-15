package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.setup.SetupPreview;
import io.zartra.bedwars.arena.setup.SetupSession;
import java.util.Objects;

/**
 * Atomic setup publication boundary implemented by the M04 storage adapter.
 *
 * <p>The implementation must revision-check the arena and session, save the complete aggregate,
 * retain the prior last-known-good image, mark the session committed and publish its outbox fact
 * in one durable transaction. Any failure leaves both records unchanged.</p>
 */
public interface SetupCommitPort {
    /** @return committed arena/session evidence or a typed conflict/failure */
    Result<CommitResult> commit(SetupSession session, SetupPreview preview,
                                boolean promoteLastKnownGood);

    /** Immutable atomic commit evidence. */
    final class CommitResult {
        private final ArenaRepository.Record arena;
        private final SetupSession session;
        /** Creates evidence only for a terminal committed session. */
        public CommitResult(final ArenaRepository.Record arena, final SetupSession session) {
            this.arena = Objects.requireNonNull(arena, "arena");
            this.session = Objects.requireNonNull(session, "session");
            if (session.state() != SetupSession.State.COMMITTED) {
                throw new IllegalArgumentException("session is not committed");
            }
        }
        /** @return durable arena record */ public ArenaRepository.Record arena() { return arena; }
        /** @return durable terminal setup session */ public SetupSession session() { return session; }
    }
}
