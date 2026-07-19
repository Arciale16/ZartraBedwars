package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.util.Objects;

/** Immutable append-only experience mutation. */
public final class ExperienceLedgerEntry {
    private final TransactionId transactionId;
    private final PlayerProgressionId owner;
    private final long delta;
    private final ExperienceAmount resultingExperience;
    private final IdempotencyKey idempotencyKey;
    private final AuditMetadata audit;

    /** Creates a non-zero experience mutation. */
    public ExperienceLedgerEntry(final TransactionId transactionId,
                                 final PlayerProgressionId owner, final long delta,
                                 final ExperienceAmount resultingExperience,
                                 final IdempotencyKey idempotencyKey,
                                 final AuditMetadata audit) {
        if (delta == 0) { throw new IllegalArgumentException("delta must be non-zero"); }
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.delta = delta;
        this.resultingExperience = Objects.requireNonNull(resultingExperience, "resultingExperience");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return transaction identity */ public TransactionId transactionId() { return transactionId; }
    /** @return progression owner */ public PlayerProgressionId owner() { return owner; }
    /** @return signed experience change */ public long delta() { return delta; }
    /** @return experience after mutation */ public ExperienceAmount resultingExperience() { return resultingExperience; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
