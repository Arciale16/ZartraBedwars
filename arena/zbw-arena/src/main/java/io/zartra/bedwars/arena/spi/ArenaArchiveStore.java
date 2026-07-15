package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.archive.ArenaArchive;
import java.util.List;
import java.util.Optional;

/** Durable backup archive store; implementations run only on a bounded application worker. */
public interface ArenaArchiveStore {
    /** @return durably stored archive */ Result<ArenaArchive> save(ArenaArchive archive);
    /** @return archive when present */ Result<Optional<ArenaArchive>> find(DefinitionId archiveId);
    /** @return bounded archive inventory for operator tooling */ Result<List<ArenaArchive>> listArchives();
}
