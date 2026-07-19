package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.RewardRecord;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** Registration port for reward records; full delivery belongs to a later M12 phase. */
public interface RewardRepository {
    /** Registers a reward exactly once without delivering it. */ Result<RewardRecord> register(UnitOfWork unitOfWork, RewardRecord reward);
    /** Finds a prior registration by duplicate-suppression key. */ Result<Optional<RewardRecord>> findByIdempotencyKey(UnitOfWork unitOfWork, IdempotencyKey key);
}
