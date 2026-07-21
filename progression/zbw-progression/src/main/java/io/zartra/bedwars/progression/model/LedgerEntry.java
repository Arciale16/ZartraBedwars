package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.util.Objects;

/** Immutable append-only persistent-currency ledger entry. */
public final class LedgerEntry {
    private final TransactionId transactionId;
    private final PlayerProgressionId owner;
    private final CurrencyId currencyId;
    private final long delta;
    private final long resultingBalance;
    private final IdempotencyKey idempotencyKey;
    private final AuditMetadata audit;

    /** Creates a validated ledger entry. */
    public LedgerEntry(final TransactionId transactionId, final PlayerProgressionId owner,
                       final CurrencyId currencyId, final long delta, final long resultingBalance,
                       final IdempotencyKey idempotencyKey, final AuditMetadata audit) {
        if (delta == 0) { throw new IllegalArgumentException("delta must be non-zero"); }
        if (resultingBalance < 0) { throw new IllegalArgumentException("resultingBalance must be non-negative"); }
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
        this.delta = delta;
        this.resultingBalance = resultingBalance;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return transaction identity */ public TransactionId transactionId() { return transactionId; }
    /** @return account owner */ public PlayerProgressionId owner() { return owner; }
    /** @return persistent currency */ public CurrencyId currencyId() { return currencyId; }
    /** @return signed balance change */ public long delta() { return delta; }
    /** @return balance after the transaction */ public long resultingBalance() { return resultingBalance; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
