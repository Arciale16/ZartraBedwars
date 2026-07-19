package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.CurrencyAccount;
import io.zartra.bedwars.progression.model.CurrencyId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** Persistence port for persistent-currency accounts, never M11 match resources. */
public interface CurrencyAccountRepository {
    /** Finds an account in an active transaction. */ Result<Optional<CurrencyAccount>> find(UnitOfWork unitOfWork, PlayerProgressionId owner, CurrencyId currencyId);
    /** Saves with optimistic revision validation. */ Result<CurrencyAccount> save(UnitOfWork unitOfWork, CurrencyAccount account, RecordRevision expectedRevision);
}
