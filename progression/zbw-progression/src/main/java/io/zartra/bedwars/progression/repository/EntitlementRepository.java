package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.EntitlementGrant;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;
import java.util.Set;

/** Persistence port for idempotent entitlement grants. */
public interface EntitlementRepository {
    /** Records a grant exactly once. */ Result<EntitlementGrant> grant(UnitOfWork unitOfWork, EntitlementGrant grant);
    /** Finds a prior grant by duplicate-suppression key. */ Result<Optional<EntitlementGrant>> findByIdempotencyKey(UnitOfWork unitOfWork, IdempotencyKey key);
    /** Reads the immutable entitlement identity set for an owner. */ Result<Set<EntitlementId>> findAll(UnitOfWork unitOfWork, PlayerProgressionId owner);
}
