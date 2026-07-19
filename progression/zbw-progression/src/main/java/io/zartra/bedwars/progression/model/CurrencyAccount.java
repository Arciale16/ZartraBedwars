package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.Objects;

/** Immutable persistent balance account, distinct from M11 match tenders. */
public final class CurrencyAccount {
    private final PlayerProgressionId owner;
    private final CurrencyId currencyId;
    private final long balance;
    private final RecordRevision revision;
    private final AuditMetadata audit;

    /** Creates a non-negative persistent balance snapshot. */
    public CurrencyAccount(final PlayerProgressionId owner, final CurrencyId currencyId,
                           final long balance, final RecordRevision revision,
                           final AuditMetadata audit) {
        if (balance < 0) { throw new IllegalArgumentException("balance must be non-negative"); }
        this.owner = Objects.requireNonNull(owner, "owner");
        this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
        this.balance = balance;
        this.revision = Objects.requireNonNull(revision, "revision");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return account owner */ public PlayerProgressionId owner() { return owner; }
    /** @return persistent currency */ public CurrencyId currencyId() { return currencyId; }
    /** @return current non-negative balance */ public long balance() { return balance; }
    /** @return optimistic revision */ public RecordRevision revision() { return revision; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
