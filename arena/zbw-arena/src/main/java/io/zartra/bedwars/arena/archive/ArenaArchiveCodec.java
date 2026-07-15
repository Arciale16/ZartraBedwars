package io.zartra.bedwars.arena.archive;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.model.ArenaBundle;
import java.time.Instant;

/** Versioned, bounded and deterministic arena metadata serialization boundary. */
public interface ArenaArchiveCodec {
    /** @return integrity-checked schema-versioned archive */
    Result<ArenaArchive> encode(DefinitionId archiveId, ArenaBundle bundle, Instant createdAt);
    /** @return fully validated immutable aggregate preserving arena/map identities */
    Result<ArenaBundle> decode(ArenaArchive archive);
}
