package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.ProgressionAccount;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** Persistence port for progression aggregates; methods may block and must run off owner threads. */
public interface ProgressionAccountRepository {
    /** Finds an aggregate in an active transaction. */ Result<Optional<ProgressionAccount>> find(UnitOfWork unitOfWork, PlayerProgressionId id);
    /** Saves with optimistic revision validation. */ Result<ProgressionAccount> save(UnitOfWork unitOfWork, ProgressionAccount account, RecordRevision expectedRevision);
}
