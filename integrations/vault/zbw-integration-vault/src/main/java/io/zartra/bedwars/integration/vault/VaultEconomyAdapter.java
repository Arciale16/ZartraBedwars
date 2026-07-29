package io.zartra.bedwars.integration.vault;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.economy.BalanceSnapshot;
import io.zartra.bedwars.api.integration.economy.EconomyProvider;
import io.zartra.bedwars.api.integration.economy.TransactionIntent;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/** Optional Vault economy adapter; the authoritative Zartra ledger remains outside Vault. */
public final class VaultEconomyAdapter implements EconomyProvider {
    private static final DefinitionId UNAVAILABLE =
            DefinitionId.of("zartra", "provider/vault-unavailable");
    private final Gateway gateway;
    private final TimeSource timeSource;
    private final OptionalProviderLifecycle lifecycle;
    private final AtomicLong version = new AtomicLong();

    /**
     * Creates the adapter around an operator-supplied Vault binding.
     *
     * @param gateway nonblocking Vault boundary
     * @param probe plugin presence and API compatibility
     * @param timeSource observation clock
     */
    public VaultEconomyAdapter(final Gateway gateway,
                               final OptionalProviderLifecycle.Probe probe,
                               final TimeSource timeSource) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        lifecycle = new OptionalProviderLifecycle(ProviderId.of("zartra", "vault"),
                SemanticVersion.parse("1.7.1"),
                CapabilitySet.of(Collections.singletonList(
                        CapabilityId.of("zartra", "economy"))),
                timeSource, "provider.vault", probe);
    }

    @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
    @Override public Health health() { return lifecycle.health(); }
    @Override public CompletionStage<Result<LifecycleState>> start() { return lifecycle.start(); }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        return lifecycle.drain(deadline);
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() { return lifecycle.stop(); }

    @Override
    public CompletionStage<Result<BalanceSnapshot>> balance(
            final PlayerId playerId, final DefinitionId currencyId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(currencyId, "currencyId");
        if (!lifecycle.available()) { return unavailable(); }
        return gateway.balance(playerId, currencyId).handle((balance, failure) ->
                failure == null
                        ? Result.success(snapshot(playerId, currencyId, balance))
                        : failure());
    }

    @Override
    public CompletionStage<Result<BalanceSnapshot>> transact(final TransactionIntent intent) {
        Objects.requireNonNull(intent, "intent");
        if (!lifecycle.available()) { return unavailable(); }
        if (!intent.deadline().isAfter(timeSource.now())) {
            return CompletableFuture.completedFuture(Result.failure(ApiError.of(
                    DefinitionId.of("zartra", "provider/deadline-expired"),
                    "provider.deadline_expired", ApiError.RetryDisposition.PERMANENT)));
        }
        return gateway.transact(intent).handle((balance, failure) ->
                failure == null
                        ? Result.success(snapshot(intent.playerId(), intent.currencyId(), balance))
                        : failure());
    }

    private BalanceSnapshot snapshot(final PlayerId playerId,
                                     final DefinitionId currencyId,
                                     final BigDecimal balance) {
        return new BalanceSnapshot(playerId, currencyId, balance,
                version.incrementAndGet(), timeSource.now());
    }

    private static <T> CompletionStage<Result<T>> unavailable() {
        return CompletableFuture.completedFuture(failure());
    }

    private static <T> Result<T> failure() {
        return Result.failure(ApiError.of(UNAVAILABLE, "provider.vault_unavailable",
                ApiError.RetryDisposition.RETRYABLE));
    }

    /** Narrow runtime binding implemented against the operator-installed Vault service. */
    public interface Gateway {
        /** @return asynchronously observed balance */
        CompletionStage<BigDecimal> balance(PlayerId playerId, DefinitionId currencyId);
        /** @return asynchronously observed post-transaction balance */
        CompletionStage<BigDecimal> transact(TransactionIntent intent);
    }
}
