package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.util.Objects;

/** Immutable normal, bulk or confirmed purchase request. */
public final class PurchaseRequest {
    private final PurchaseContext context;
    private final ShopIds.CatalogId catalogId;
    private final ShopIds.ItemId itemId;
    private final int batches;
    private final boolean confirmed;
    private final IdempotencyKey idempotencyKey;

    /** Creates a request for one to 64 item batches. */
    public PurchaseRequest(final PurchaseContext context, final ShopIds.CatalogId catalogId,
                           final ShopIds.ItemId itemId, final int batches,
                           final boolean confirmed, final IdempotencyKey idempotencyKey) {
        this.context = Objects.requireNonNull(context, "context");
        this.catalogId = Objects.requireNonNull(catalogId, "catalogId");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        if (batches < 1 || batches > 64) { throw new IllegalArgumentException("batches is out of range"); }
        this.batches = batches;
        this.confirmed = confirmed;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }

    /** @return authenticated match context */ public PurchaseContext context() { return context; }
    /** @return selected catalog */ public ShopIds.CatalogId catalogId() { return catalogId; }
    /** @return selected item */ public ShopIds.ItemId itemId() { return itemId; }
    /** @return requested number of batches */ public int batches() { return batches; }
    /** @return whether the caller completed confirmation */ public boolean confirmed() { return confirmed; }
    /** @return retry-stable operation key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
}
