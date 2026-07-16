package io.zartra.bedwars.game.spi;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Optional;

/** Append-only security and operational audit boundary for privileged game actions. */
public interface GameAuditSink {
    /** Records one permission-checked action without retaining a platform principal. */
    void record(MatchId matchId, Optional<PlayerId> actor, DefinitionId action,
                boolean allowed, Instant occurredAt);
}
