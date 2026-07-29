package io.zartra.bedwars.api.integration.economy;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.util.concurrent.CompletionStage;

/** Vendor-neutral asynchronous economy adapter without ledger ownership. */
public interface EconomyProvider extends Provider {
    /**
     * Reads a balance without blocking the caller.
     *
     * @param playerId player identity
     * @param currencyId currency identity
     * @return asynchronous typed balance result
     */
    CompletionStage<Result<BalanceSnapshot>> balance(PlayerId playerId, DefinitionId currencyId);

    /**
     * Requests one idempotent adapter transaction.
     *
     * @param intent immutable operation intent
     * @return asynchronous post-operation snapshot
     */
    CompletionStage<Result<BalanceSnapshot>> transact(TransactionIntent intent);
}
