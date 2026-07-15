package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.result.Result;
import java.util.Optional;

/** Aggregate-record persistence port; callers supply an active unit of work. */
public interface StorageRepository {
    /** @return record when present, or a typed failure */
    Result<Optional<StoredRecord>> find(UnitOfWork unitOfWork, RecordKey key);
    /** @return record with its new durable revision, or a conflict failure */
    Result<StoredRecord> save(UnitOfWork unitOfWork, StoredRecord record,
                              RecordRevision expectedRevision);
    /** @return whether a matching record was deleted, or a conflict failure */
    Result<Boolean> delete(UnitOfWork unitOfWork, RecordKey key,
                           RecordRevision expectedRevision);
}
