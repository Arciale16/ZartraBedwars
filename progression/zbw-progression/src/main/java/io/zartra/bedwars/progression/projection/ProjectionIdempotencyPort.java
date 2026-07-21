package io.zartra.bedwars.progression.projection;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** M04-inbox-style checkpoint port used inside the same unit of work as projection mutations. */
public interface ProjectionIdempotencyPort {
    /** Finds a previously committed checkpoint. */ Result<Optional<ProjectionCheckpoint>> find(UnitOfWork unitOfWork, IdempotencyKey key);
    /** Records a checkpoint atomically; duplicate keys return a typed conflict. */ Result<ProjectionCheckpoint> record(UnitOfWork unitOfWork, ProjectionCheckpoint checkpoint);
}
