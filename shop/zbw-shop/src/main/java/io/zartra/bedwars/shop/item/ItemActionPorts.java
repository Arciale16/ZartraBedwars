package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;

/** Atomic boundaries required by the neutral utility-item runtime. */
public final class ItemActionPorts {
    private ItemActionPorts() { }
    /** Exact-node central authorization boundary. */
    public interface Authorization {
        /** @return whether the complete action request is permitted */ boolean allowed(AuthorizationRequest request);
    }
    /** Atomic match-local debit and owned-item consumption boundary. */
    public interface Transaction {
        /** Commits cost and inventory consumption once for the key. */
        Outcome commit(UtilityItemDefinition definition, ItemActionRequest request, long expectedRevision);
        /** Compensates a committed debit when the owner-thread effect rejects before publication. */
        void compensate(UtilityItemDefinition definition, ItemActionRequest request);
    }
    /** Owner-thread effect translation boundary; contains no action policy. */
    public interface Effect {
        /** Applies an already validated bounded effect. */
        boolean apply(DefinitionId effect, UtilityItemDefinition definition,
                      ItemActionRequest request, IdempotencyKey key);
        /** Removes all effects owned by a match action runtime. */ void cleanup(DefinitionId owner);
    }
    /** Atomic transaction outcomes. */
    public enum Outcome { /** Committed. */ COMMITTED, /** Already committed. */ DUPLICATE,
        /** Cost or item unavailable. */ INSUFFICIENT, /** Revision changed. */ CONFLICT }
}
