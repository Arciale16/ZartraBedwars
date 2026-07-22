package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.cosmetic.CosmeticLoadout;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** M12-storage-boundary port for M14 cosmetic state. */
public interface CosmeticStateRepository {
    /** Reads a loadout inside the caller-owned transaction. */
    Result<Optional<CosmeticLoadout>> find(UnitOfWork unitOfWork, PlayerProgressionId owner);
    /** Saves with optimistic revision and duplicate suppression. */
    Result<CosmeticLoadout> save(UnitOfWork unitOfWork, CosmeticLoadout loadout,
                                 RecordRevision expectedRevision, IdempotencyKey idempotencyKey);
}
