package io.zartra.bedwars.progression.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.RewardId;
import java.util.Objects;

/** Exactly-once M13 request for the completed M12 reward engine. */
public final class M13RewardIntent {
    private final RewardId rewardId;
    private final PlayerProgressionId recipient;
    private final IdempotencyKey idempotencyKey;

    /** Creates an immutable reward request. */
    public M13RewardIntent(final RewardId rewardId, final PlayerProgressionId recipient,
                           final IdempotencyKey idempotencyKey) {
        this.rewardId = Objects.requireNonNull(rewardId, "rewardId");
        this.recipient = Objects.requireNonNull(recipient, "recipient");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
    /** @return M12 reward identity */ public RewardId rewardId() { return rewardId; }
    /** @return recipient */ public PlayerProgressionId recipient() { return recipient; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
}
