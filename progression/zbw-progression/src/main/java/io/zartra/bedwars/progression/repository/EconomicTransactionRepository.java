package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.LedgerEntry;
import io.zartra.bedwars.progression.model.TransactionId;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** Append-only persistent economic transaction port. */
public interface EconomicTransactionRepository {
    /** Appends one transaction atomically with its account mutation. */ Result<LedgerEntry> append(UnitOfWork unitOfWork, LedgerEntry entry);
    /** Finds a transaction by identity. */ Result<Optional<LedgerEntry>> find(UnitOfWork unitOfWork, TransactionId id);
    /** Finds a transaction by duplicate-suppression key. */ Result<Optional<LedgerEntry>> findByIdempotencyKey(UnitOfWork unitOfWork, IdempotencyKey key);
}
