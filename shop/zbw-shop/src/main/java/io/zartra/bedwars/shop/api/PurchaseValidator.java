package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Optional;

/** Deterministic extension point for custom conditions and global purchase restrictions. */
public interface PurchaseValidator {
    /** @return stable ordering and diagnostic identity */ DefinitionId id();
    /**
     * Returns whether this validator handles the supplied condition.
     *
     * @param condition empty for global restrictions, otherwise a configured condition ID
     * @return true when {@link #validate} must run
     */
    boolean supports(Optional<DefinitionId> condition);
    /** @return empty when accepted, otherwise a typed expected rejection */
    Optional<PurchaseFailure> validate(ShopCatalog catalog, ShopCatalog.ItemDefinition item,
                                       PurchaseRequest request, PurchaseTransactionPort.State state,
                                       Optional<DefinitionId> condition);
}
