package io.zartra.bedwars.shop.application;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.shop.api.PurchaseFailure;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.PurchaseTransactionPort;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import java.time.Instant;
import java.util.Objects;

/**
 * Owner-thread purchase adapter that exposes one indivisible platform inventory mutation.
 *
 * <p>The platform delegate is responsible for comparing the revision and idempotency key before
 * debiting every tender and granting the semantic item. It must either publish the whole mutation
 * or publish nothing. Overflow is represented explicitly and never silently discards an item.</p>
 */
public final class AtomicInventoryPurchasePort implements PurchaseTransactionPort {
    private final Inventory inventory;

    /** Creates an adapter around one non-blocking owner-thread inventory boundary. */
    public AtomicInventoryPurchasePort(final Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override public State inspect(final PurchaseRequest request,
                                   final ShopCatalog.ItemDefinition item) {
        return Objects.requireNonNull(inventory.inspect(
                Objects.requireNonNull(request, "request"),
                Objects.requireNonNull(item, "item")), "inventory state");
    }

    @Override public CommitResult commit(final CommitRequest request) {
        final PurchaseQuote quote = Objects.requireNonNull(request, "request").quote();
        final Mutation mutation = new Mutation(quote.request(), quote.stateRevision(),
                quote.price(), quote.request().itemId(), quote.grantQuantity(),
                quote.request().idempotencyKey(), quote.issuedAt());
        final MutationResult result = Objects.requireNonNull(inventory.commit(mutation),
                "mutation result");
        switch (result.status()) {
            case APPLIED: return CommitResult.success(Status.APPLIED);
            case DUPLICATE: return CommitResult.success(Status.DUPLICATE);
            case STALE: return CommitResult.stale();
            case OVERFLOW:
                return CommitResult.rejected(PurchaseFailure.of(
                        PurchaseFailure.Code.INVENTORY_FULL));
            case INSUFFICIENT_TENDER:
                return CommitResult.rejected(PurchaseFailure.of(
                        PurchaseFailure.Code.INSUFFICIENT_RESOURCES));
            default:
                return CommitResult.rejected(PurchaseFailure.of(
                        PurchaseFailure.Code.TRANSACTION_REJECTED));
        }
    }

    /** Closed, owner-thread platform inventory surface. */
    public interface Inventory {
        /** Inspects a complete inventory state without mutation or blocking I/O. */
        State inspect(PurchaseRequest request, ShopCatalog.ItemDefinition item);
        /** Atomically applies or rejects one complete mutation. */
        MutationResult commit(Mutation mutation);
    }

    /** Immutable debit, grant and audit mutation. */
    public static final class Mutation {
        private final PurchaseRequest request;
        private final long expectedRevision;
        private final ShopCatalog.Price debit;
        private final ShopIds.ItemId itemId;
        private final int quantity;
        private final IdempotencyKey key;
        private final Instant committedAt;

        private Mutation(final PurchaseRequest request, final long expectedRevision,
                         final ShopCatalog.Price debit, final ShopIds.ItemId itemId,
                         final int quantity, final IdempotencyKey key,
                         final Instant committedAt) {
            this.request = Objects.requireNonNull(request, "request");
            if (expectedRevision < 0) {
                throw new IllegalArgumentException("expected revision must not be negative");
            }
            this.expectedRevision = expectedRevision;
            this.debit = Objects.requireNonNull(debit, "debit");
            this.itemId = Objects.requireNonNull(itemId, "itemId");
            if (quantity < 1) { throw new IllegalArgumentException("quantity must be positive"); }
            this.quantity = quantity;
            this.key = Objects.requireNonNull(key, "key");
            this.committedAt = Objects.requireNonNull(committedAt, "committedAt");
        }
        /** @return purchase context and scope */ public PurchaseRequest request() { return request; }
        /** @return optimistic inventory revision */ public long expectedRevision() { return expectedRevision; }
        /** @return exact multi-tender debit */ public ShopCatalog.Price debit() { return debit; }
        /** @return semantic catalogue item */ public ShopIds.ItemId itemId() { return itemId; }
        /** @return exact granted units */ public int quantity() { return quantity; }
        /** @return retry-stable mutation key */ public IdempotencyKey key() { return key; }
        /** @return deterministic audit instant */ public Instant committedAt() { return committedAt; }
    }

    /** Platform mutation outcome; every rejection guarantees no partial debit or grant. */
    public static final class MutationResult {
        /** Supported atomic outcomes. */
        public enum Status { APPLIED, DUPLICATE, STALE, OVERFLOW, INSUFFICIENT_TENDER, REJECTED }
        private final Status status;
        private MutationResult(final Status status) { this.status = status; }
        /** Creates an explicit outcome. */ public static MutationResult of(final Status status) {
            return new MutationResult(Objects.requireNonNull(status, "status"));
        }
        /** @return explicit atomic outcome */ public Status status() { return status; }
    }
}
