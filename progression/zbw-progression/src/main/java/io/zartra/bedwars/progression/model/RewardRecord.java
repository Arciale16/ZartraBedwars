package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.util.Objects;

/** Immutable reward registration state; delivery behavior belongs to a later M12 phase. */
public final class RewardRecord {
    private final RewardId rewardId;
    private final PlayerProgressionId recipient;
    private final IdempotencyKey idempotencyKey;
    private final AuditMetadata audit;

    /** Creates a reward registration without executing delivery. */
    public RewardRecord(final RewardId rewardId, final PlayerProgressionId recipient,
                        final IdempotencyKey idempotencyKey, final AuditMetadata audit) {
        this.rewardId = Objects.requireNonNull(rewardId, "rewardId");
        this.recipient = Objects.requireNonNull(recipient, "recipient");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return reward identity */ public RewardId rewardId() { return rewardId; }
    /** @return intended recipient */ public PlayerProgressionId recipient() { return recipient; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
