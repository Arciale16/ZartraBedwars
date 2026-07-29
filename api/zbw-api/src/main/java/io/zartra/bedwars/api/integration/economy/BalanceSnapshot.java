package io.zartra.bedwars.api.integration.economy;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Immutable observed balance; the authoritative ledger remains outside the provider. */
public final class BalanceSnapshot {
    private final PlayerId playerId;
    private final DefinitionId currencyId;
    private final BigDecimal balance;
    private final long version;
    private final Instant observedAt;

    /**
     * Creates a balance snapshot.
     *
     * @param playerId player identity
     * @param currencyId stable currency identity
     * @param balance finite non-negative balance
     * @param version non-negative authoritative version
     * @param observedAt observation timestamp
     */
    public BalanceSnapshot(final PlayerId playerId, final DefinitionId currencyId,
                           final BigDecimal balance, final long version,
                           final Instant observedAt) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
        if (balance.signum() < 0 || balance.precision() > 34 || version < 0) {
            throw new IllegalArgumentException("balance and version must be bounded and non-negative");
        }
        this.version = version;
    }

    /** @return player identity */
    public PlayerId playerId() { return playerId; }
    /** @return currency identity */
    public DefinitionId currencyId() { return currencyId; }
    /** @return observed balance */
    public BigDecimal balance() { return balance; }
    /** @return authoritative version */
    public long version() { return version; }
    /** @return observation timestamp */
    public Instant observedAt() { return observedAt; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof BalanceSnapshot)) { return false; }
        BalanceSnapshot that = (BalanceSnapshot) other;
        return version == that.version && playerId.equals(that.playerId)
                && currencyId.equals(that.currencyId) && balance.equals(that.balance)
                && observedAt.equals(that.observedAt);
    }

    @Override public int hashCode() {
        return Objects.hash(playerId, currencyId, balance, version, observedAt);
    }
}
