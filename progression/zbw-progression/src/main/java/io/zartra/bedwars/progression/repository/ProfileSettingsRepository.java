package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.profile.ProfileSettings;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** M12-storage-boundary port for M14 private profile settings. */
public interface ProfileSettingsRepository {
    /** Reads settings inside the caller-owned transaction. */
    Result<Optional<ProfileSettings>> find(UnitOfWork unitOfWork, PlayerProgressionId owner);
    /** Saves with optimistic revision and duplicate suppression. */
    Result<ProfileSettings> save(UnitOfWork unitOfWork, ProfileSettings settings,
                                 RecordRevision expectedRevision, IdempotencyKey idempotencyKey);
}
