package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.PurchaseTransactionPort;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.TenderRegistry;
import io.zartra.bedwars.shop.application.AtomicInventoryPurchasePort;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class AtomicInventoryPurchasePortTest {
    @Test void completeQuoteIsForwardedAsOneAtomicMutation() {
        final AtomicReference<AtomicInventoryPurchasePort.Mutation> seen = new AtomicReference<>();
        final PurchaseTransactionPort.State state = state(3);
        final AtomicInventoryPurchasePort port = new AtomicInventoryPurchasePort(
                new AtomicInventoryPurchasePort.Inventory() {
                    @Override public PurchaseTransactionPort.State inspect(
                            final io.zartra.bedwars.shop.api.PurchaseRequest request,
                            final io.zartra.bedwars.shop.api.ShopCatalog.ItemDefinition item) {
                        return state;
                    }
                    @Override public AtomicInventoryPurchasePort.MutationResult commit(
                            final AtomicInventoryPurchasePort.Mutation mutation) {
                        seen.set(mutation);
                        return AtomicInventoryPurchasePort.MutationResult.of(
                                AtomicInventoryPurchasePort.MutationResult.Status.APPLIED);
                    }
                });
        assertSame(state, port.inspect(request(), item()));
        final PurchaseTransactionPort.CommitResult result = port.commit(
                new PurchaseTransactionPort.CommitRequest(quote(3)));
        assertEquals(PurchaseTransactionPort.Status.APPLIED, result.status());
        assertEquals(request().idempotencyKey(), seen.get().key());
        assertEquals(request().itemId(), seen.get().itemId());
        assertEquals(3, seen.get().expectedRevision());
    }

    @Test void duplicateAndEveryRejectionRemainTyped() {
        for (AtomicInventoryPurchasePort.MutationResult.Status status
                : AtomicInventoryPurchasePort.MutationResult.Status.values()) {
            final AtomicInventoryPurchasePort port = new AtomicInventoryPurchasePort(
                    new FixedInventory(status));
            final PurchaseTransactionPort.CommitResult result = port.commit(
                    new PurchaseTransactionPort.CommitRequest(quote(0)));
            if (status == AtomicInventoryPurchasePort.MutationResult.Status.APPLIED) {
                assertEquals(PurchaseTransactionPort.Status.APPLIED, result.status());
            } else if (status == AtomicInventoryPurchasePort.MutationResult.Status.DUPLICATE) {
                assertEquals(PurchaseTransactionPort.Status.DUPLICATE, result.status());
            } else if (status == AtomicInventoryPurchasePort.MutationResult.Status.STALE) {
                assertEquals(PurchaseTransactionPort.Status.STALE, result.status());
            } else {
                assertEquals(PurchaseTransactionPort.Status.REJECTED, result.status());
            }
        }
    }

    private static final class FixedInventory implements AtomicInventoryPurchasePort.Inventory {
        private final AtomicInventoryPurchasePort.MutationResult.Status status;
        private FixedInventory(final AtomicInventoryPurchasePort.MutationResult.Status status) {
            this.status = status;
        }
        @Override public PurchaseTransactionPort.State inspect(
                final io.zartra.bedwars.shop.api.PurchaseRequest request,
                final io.zartra.bedwars.shop.api.ShopCatalog.ItemDefinition item) {
            return state(0);
        }
        @Override public AtomicInventoryPurchasePort.MutationResult commit(
                final AtomicInventoryPurchasePort.Mutation mutation) {
            return AtomicInventoryPurchasePort.MutationResult.of(status);
        }
    }

    private static PurchaseTransactionPort.State state(final long revision) {
        final Map<io.zartra.bedwars.api.identity.ResourceId, Long> balances = new LinkedHashMap<>();
        balances.put(TenderRegistry.IRON, 20L);
        return new PurchaseTransactionPort.State(revision, balances, true, 0, 0, 0, 0,
                Optional.empty());
    }

    private static PurchaseQuote quote(final long revision) {
        return new PurchaseQuote(request(), 1, revision, price(), 2,
                Collections.singleton(PermissionNode.of("zartrabedwars.shop.purchase")),
                Instant.parse("2026-07-18T10:00:00Z"),
                Instant.parse("2026-07-18T10:00:02Z"));
    }

    private static PurchaseRequest request() {
        final PurchaseContext context = new PurchaseContext(
                AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                        DefinitionId.of("test", "player/alex")),
                PlayerId.of(new UUID(0, 1)), MatchId.of(new UUID(0, 2)),
                ArenaId.of(new UUID(0, 3)), DefinitionId.of("test", "mode/standard"),
                DefinitionId.of("test", "team/red"), Optional.empty());
        return new PurchaseRequest(context, ShopIds.CatalogId.of("test", "main"),
                ShopIds.ItemId.of("test", "wool"), 1, false,
                IdempotencyKey.of("test", "purchase-one"));
    }

    private static ShopCatalog.ItemDefinition item() {
        return new ShopCatalog.ItemDefinition(ShopIds.ItemId.of("test", "wool"),
                ShopIds.CategoryId.of("test", "blocks"), DefinitionId.of("test", "item/wool"),
                MessageKey.of("item.wool.name"), MessageKey.of("item.wool.lore"), 2, price(),
                ShopCatalog.Availability.STANDARD,
                new ShopCatalog.PurchaseRules(false, 1, Duration.ZERO, 0, 0, 0, 0,
                        Optional.empty(), Optional.empty()), Collections.emptyList());
    }

    private static ShopCatalog.Price price() {
        return new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 4)));
    }
}
