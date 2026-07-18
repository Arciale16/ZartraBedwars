package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.PurchaseTransactionPort;
import io.zartra.bedwars.shop.api.RotationContracts;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.TenderRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ValidationBranchTest {
    @Test void rejectsEveryUnsafePriceAndRuleBoundary() {
        assertThrows(NullPointerException.class,
                () -> new ShopCatalog.ResourceAmount(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 1_000_000_001L));
        final ShopCatalog.ResourceAmount amount = new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 1);
        assertThrows(IllegalArgumentException.class, () -> amount.multiply(0));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopCatalog.Price(Collections.emptyList()));
        assertThrows(NullPointerException.class,
                () -> new ShopCatalog.Price(Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class, () -> rules(0, Duration.ZERO, 0));
        assertThrows(IllegalArgumentException.class, () -> rules(65, Duration.ZERO, 0));
        assertThrows(IllegalArgumentException.class,
                () -> rules(1, Duration.ofSeconds(-1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> rules(1, Duration.ofDays(8), 0));
        assertThrows(IllegalArgumentException.class, () -> rules(1, Duration.ZERO, -1));
        assertThrows(IllegalArgumentException.class,
                () -> rules(1, Duration.ZERO, 1_000_001));
    }

    @Test void rejectsMalformedPurchaseStateRequestAndQuote() {
        final Map<ResourceId, Long> balance = new LinkedHashMap<ResourceId, Long>();
        balance.put(TenderRegistry.IRON, 1L);
        assertThrows(IllegalArgumentException.class, () -> new PurchaseTransactionPort.State(
                -1, balance, true, 0, 0, 0, 0, Optional.empty()));
        balance.put(TenderRegistry.IRON, -1L);
        assertThrows(IllegalArgumentException.class, () -> new PurchaseTransactionPort.State(
                0, balance, true, 0, 0, 0, 0, Optional.empty()));
        final PurchaseContext context = context();
        assertThrows(IllegalArgumentException.class, () -> new PurchaseRequest(context,
                ShopIds.CatalogId.of("test", "main"), ShopIds.ItemId.of("test", "wool"),
                0, false, IdempotencyKey.of("test", "bad")));
        final PurchaseRequest request = new PurchaseRequest(context,
                ShopIds.CatalogId.of("test", "main"), ShopIds.ItemId.of("test", "wool"),
                1, false, IdempotencyKey.of("test", "ok"));
        final ShopCatalog.Price price = new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 1)));
        final Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseQuote(request, 0, 0, price, 1,
                        Collections.singleton(PermissionNode.of("zartrabedwars.shop.purchase")),
                        now, now.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseQuote(request, 1, 0, price, 0,
                        Collections.singleton(PermissionNode.of("zartrabedwars.shop.purchase")),
                        now, now.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseQuote(request, 1, 0, price, 1,
                        Collections.singleton(PermissionNode.of("zartrabedwars.shop.purchase")),
                        now, now));
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseQuote(request, 1, 0, price, 1,
                        Collections.emptyList(), now, now.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new PurchaseContext(
                AuthorizationSubject.of(AuthorizationSubject.Kind.SERVICE,
                        DefinitionId.of("test", "service/shop")), PlayerId.of(new UUID(0, 1)),
                MatchId.of(new UUID(0, 2)), ArenaId.of(new UUID(0, 3)),
                DefinitionId.of("test", "mode/standard"),
                DefinitionId.of("test", "team/red"), Optional.empty()));
    }

    @Test void rejectsUnsafeRotationBoundaries() {
        final ShopIds.RotationId id = ShopIds.RotationId.of("test", "weekly");
        final ShopIds.ItemId wool = ShopIds.ItemId.of("test", "wool");
        final RotationContracts.PoolEntry entry = new RotationContracts.PoolEntry(wool, 1);
        final Instant start = Instant.parse("2026-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> new RotationContracts.PoolEntry(wool, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RotationContracts.PoolEntry(wool, 1_000_001));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.of(start.minusSeconds(1)), 1, 0, Duration.ZERO,
                Collections.singletonList(entry)));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.empty(), 0, 0, Duration.ZERO, Collections.singletonList(entry)));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.empty(), 1, -1, Duration.ZERO, Collections.singletonList(entry)));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.empty(), 1, 129, Duration.ZERO, Collections.singletonList(entry)));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.empty(), 1, 0, Duration.ofSeconds(-1),
                Collections.singletonList(entry)));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.empty(), 1, 0, Duration.ofDays(8),
                Collections.singletonList(entry)));
        assertThrows(IllegalArgumentException.class, () -> rotation(id, start,
                Optional.empty(), 1, 0, Duration.ZERO, Arrays.asList(entry, entry)));
        assertThrows(IllegalArgumentException.class, () -> new RotationContracts.Snapshot(
                id, 0, start, start.plusSeconds(1), Collections.singletonList(wool)));
        assertThrows(IllegalArgumentException.class, () -> new RotationContracts.Snapshot(
                id, 1, start, start.plusSeconds(1), Collections.emptyList()));
    }

    private static ShopCatalog.PurchaseRules rules(final int bulk, final Duration cooldown,
                                                   final int limit) {
        return new ShopCatalog.PurchaseRules(false, bulk, cooldown, limit,
                0, 0, 0, Optional.empty(), Optional.empty());
    }

    private static RotationContracts.Definition rotation(final ShopIds.RotationId id,
            final Instant start, final Optional<Instant> end, final int slots,
            final int noRepeat, final Duration cooldown,
            final java.util.Collection<RotationContracts.PoolEntry> pool) {
        return new RotationContracts.Definition(id, ZoneId.of("UTC"), Duration.ofDays(7),
                start, end, slots, noRepeat, cooldown, Optional.empty(), pool);
    }

    private static PurchaseContext context() {
        return new PurchaseContext(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                DefinitionId.of("test", "player/alex")), PlayerId.of(new UUID(0, 1)),
                MatchId.of(new UUID(0, 2)), ArenaId.of(new UUID(0, 3)),
                DefinitionId.of("test", "mode/standard"),
                DefinitionId.of("test", "team/red"), Optional.empty());
    }
}
