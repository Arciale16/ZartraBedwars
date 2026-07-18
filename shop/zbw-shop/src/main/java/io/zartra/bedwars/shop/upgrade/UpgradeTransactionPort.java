package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.ResourceId;
import java.util.Map;

/** Atomic match-resource debit boundary for a team-upgrade purchase. */
public interface UpgradeTransactionPort {
    /** Transaction result; no resource may be removed for non-committed results. */
    enum Result { COMMITTED, INSUFFICIENT_RESOURCES, REVISION_CONFLICT }
    /**
     * Atomically validates the team state revision and debits the complete cost once.
     * Implementations must fence the idempotency key with the debit.
     */
    Result commit(TeamUpgradeState expected, Map<ResourceId, Integer> cost, IdempotencyKey key);
}
