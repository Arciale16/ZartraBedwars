package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.identity.CaseId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import java.time.Instant;

/** Durable privacy retention, legal-hold and pseudonymous tombstone port. */
public interface RetentionRepository {
    /** Records or extends retention for a durable subject. */
    Result<Boolean> retain(UnitOfWork unitOfWork, RecordKey key, RetentionPolicy policy,
                           Instant expiresAt);
    /** Places an authorized legal hold without storing public identity in evidence records. */
    Result<Boolean> hold(UnitOfWork unitOfWork, CaseId caseId, RecordKey key,
                         PlayerId authorizedBy, Instant placedAt);
    /** Records authorized release; deletion remains a separate auditable transition. */
    Result<Boolean> release(UnitOfWork unitOfWork, CaseId caseId,
                            PlayerId authorizedBy, Instant releasedAt);
    /** Writes a non-content tombstone after deletion to make retries idempotent. */
    Result<Boolean> tombstone(UnitOfWork unitOfWork, RecordKey key, Instant deletedAt);
}
