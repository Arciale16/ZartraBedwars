package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.ExperienceLedgerEntry;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.List;
import java.util.Optional;

/** Append-only experience ledger port. */
public interface ExperienceLedgerRepository {
    /** Appends exactly once for the idempotency key. */ Result<ExperienceLedgerEntry> append(UnitOfWork unitOfWork, ExperienceLedgerEntry entry);
    /** Finds a previously appended mutation. */ Result<Optional<ExperienceLedgerEntry>> findByIdempotencyKey(UnitOfWork unitOfWork, IdempotencyKey key);
    /** Reads a bounded chronological history. */ Result<List<ExperienceLedgerEntry>> history(UnitOfWork unitOfWork, PlayerProgressionId owner, int limit);
}
