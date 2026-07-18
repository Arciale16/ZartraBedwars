package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.PurchaseFailure;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.PurchaseResult;
import io.zartra.bedwars.shop.api.PurchaseTransactionPort;
import io.zartra.bedwars.shop.api.PurchaseValidator;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.TenderRegistry;
import io.zartra.bedwars.shop.application.PurchaseService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class PurchaseServiceTest {
    private static final ResourceId CRYSTAL = ResourceId.of("extension", "resource/crystal");
    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

    @Test void quotesAndAtomicallyExecutesMultipleTenderPurchase() {
        final MutableTime time = new MutableTime(NOW);
        final FakePort port = new FakePort(state(7, true, 0, 0, 0, 0,
                balances(20, 3), Optional.empty()));
        final ShopCatalog catalog = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD, rules(false, 4, Duration.ZERO, 64, 0, 0, 0,
                        Optional.empty()), Optional.empty(), Optional.empty(),
                new ShopCatalog.Price(Arrays.asList(
                        new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 4),
                        new ShopCatalog.ResourceAmount(CRYSTAL, 1))));
        final PurchaseService service = service(port, time, true, Collections.emptyList(), true);
        final PurchaseResult<PurchaseQuote> result = service.quote(catalog, request(catalog, 2, false));
        assertTrue(result.isSuccess());
        final PurchaseQuote quote = result.value().get();
        assertEquals(7, quote.stateRevision());
        assertEquals(2, quote.catalogRevision());
        assertEquals(4, quote.grantQuantity());
        assertEquals(8, amount(quote.price(), TenderRegistry.IRON));
        assertEquals(2, amount(quote.price(), CRYSTAL));
        final PurchaseResult<PurchaseOutcome> executed = service.execute(quote);
        assertTrue(executed.isSuccess());
        assertFalse(executed.value().get().duplicate());
        assertEquals(quote, executed.value().get().quote());
        assertEquals(NOW, executed.value().get().observedAt());
        assertEquals(1, port.commitCalls);
        assertEquals(quote, port.lastCommit.quote());
    }

    @Test void rejectsIdentityScopeVisibilityAndAuthorizationFailures() {
        final FakePort port = new FakePort(defaultState());
        final MutableTime time = new MutableTime(NOW);
        final ShopCatalog global = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD, defaultRules(), Optional.empty(), Optional.empty(),
                price(TenderRegistry.IRON, 4));
        final PurchaseRequest wrongCatalog = new PurchaseRequest(context(),
                ShopIds.CatalogId.of("test", "other"), ShopIds.ItemId.of("test", "wool"),
                1, false, IdempotencyKey.of("test", "wrong"));
        assertFailure(service(port, time, true, Collections.emptyList(), false)
                .quote(global, wrongCatalog), PurchaseFailure.Code.CATALOG_MISMATCH);
        final ShopCatalog scoped = catalog(ShopCatalog.Scope.of(
                Optional.of(DefinitionId.of("test", "mode/other")), Optional.empty(),
                Optional.empty(), Optional.empty()), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD, defaultRules(), Optional.empty(),
                Optional.empty(), price(TenderRegistry.IRON, 4));
        assertFailure(service(port, time, true, Collections.emptyList(), false)
                .quote(scoped, request(scoped, 1, false)), PurchaseFailure.Code.SCOPE_MISMATCH);
        final ShopCatalog hidden = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.HIDDEN,
                ShopCatalog.Availability.STANDARD, defaultRules(), Optional.empty(),
                Optional.empty(), price(TenderRegistry.IRON, 4));
        assertFailure(service(port, time, true, Collections.emptyList(), false)
                .quote(hidden, request(hidden, 1, false)), PurchaseFailure.Code.UNAVAILABLE);
        final ShopCatalog disabled = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.DISABLED, defaultRules(), Optional.empty(),
                Optional.empty(), price(TenderRegistry.IRON, 4));
        assertFailure(service(port, time, true, Collections.emptyList(), false)
                .quote(disabled, request(disabled, 1, false)), PurchaseFailure.Code.UNAVAILABLE);
        assertFailure(service(port, time, false, Collections.emptyList(), false)
                .quote(global, request(global, 1, false)), PurchaseFailure.Code.FORBIDDEN);
        final PurchaseRequest unknown = new PurchaseRequest(context(), global.id(),
                ShopIds.ItemId.of("test", "unknown"), 1, false,
                IdempotencyKey.of("test", "unknown"));
        assertFailure(service(port, time, true, Collections.emptyList(), false)
                .quote(global, unknown), PurchaseFailure.Code.UNKNOWN_ITEM);
    }

    @Test void validatesBulkConfirmationTenderBalanceAndCapacity() {
        final MutableTime time = new MutableTime(NOW);
        final ShopCatalog confirmed = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD,
                rules(true, 2, Duration.ZERO, 0, 0, 0, 0, Optional.empty()),
                Optional.empty(), Optional.empty(), price(TenderRegistry.IRON, 4));
        assertFailure(service(new FakePort(defaultState()), time, true, Collections.emptyList(), false)
                .quote(confirmed, request(confirmed, 1, false)), PurchaseFailure.Code.CONFIRMATION_REQUIRED);
        assertFailure(service(new FakePort(defaultState()), time, true, Collections.emptyList(), false)
                .quote(confirmed, request(confirmed, 3, true)), PurchaseFailure.Code.BULK_LIMIT);
        final ShopCatalog custom = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.CUSTOM, defaultRules(), Optional.empty(), Optional.empty(),
                price(CRYSTAL, 1));
        assertFailure(service(new FakePort(defaultState()), time, true, Collections.emptyList(), false)
                .quote(custom, request(custom, 1, false)), PurchaseFailure.Code.UNKNOWN_TENDER);
        assertFailure(service(new FakePort(state(1, true, 0, 0, 0, 0,
                        balances(3, 0), Optional.empty())), time, true,
                Collections.emptyList(), false).quote(confirmed, request(confirmed, 1, true)),
                PurchaseFailure.Code.INSUFFICIENT_RESOURCES);
        assertFailure(service(new FakePort(state(1, false, 0, 0, 0, 0,
                        balances(20, 0), Optional.empty())), time, true,
                Collections.emptyList(), false).quote(confirmed, request(confirmed, 1, true)),
                PurchaseFailure.Code.INVENTORY_FULL);
    }

    @Test void validatesInventoryPlayerTeamArenaLimitsAndCooldown() {
        final MutableTime time = new MutableTime(NOW);
        final ShopCatalog catalog = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD,
                rules(false, 4, Duration.ofSeconds(5), 4, 2, 3, 4, Optional.empty()),
                Optional.empty(), Optional.empty(), price(TenderRegistry.IRON, 1));
        assertFailure(service(new FakePort(state(1, true, 3, 0, 0, 0,
                        balances(20, 0), Optional.empty())), time, true,
                Collections.emptyList(), false).quote(catalog, request(catalog, 1, false)),
                PurchaseFailure.Code.INVENTORY_LIMIT);
        assertFailure(service(new FakePort(state(1, true, 0, 2, 0, 0,
                        balances(20, 0), Optional.empty())), time, true,
                Collections.emptyList(), false).quote(catalog, request(catalog, 1, false)),
                PurchaseFailure.Code.PLAYER_LIMIT);
        assertFailure(service(new FakePort(state(1, true, 0, 0, 3, 0,
                        balances(20, 0), Optional.empty())), time, true,
                Collections.emptyList(), false).quote(catalog, request(catalog, 1, false)),
                PurchaseFailure.Code.TEAM_LIMIT);
        assertFailure(service(new FakePort(state(1, true, 0, 0, 0, 4,
                        balances(20, 0), Optional.empty())), time, true,
                Collections.emptyList(), false).quote(catalog, request(catalog, 1, false)),
                PurchaseFailure.Code.ARENA_LIMIT);
        final PurchaseResult<PurchaseQuote> cooldown = service(new FakePort(state(1, true, 0, 0, 0, 0,
                        balances(20, 0), Optional.of(NOW.minusSeconds(2)))), time, true,
                Collections.emptyList(), false).quote(catalog, request(catalog, 1, false));
        assertFailure(cooldown, PurchaseFailure.Code.COOLDOWN);
        assertEquals(NOW.plusSeconds(3), cooldown.failure().get().retryAt().get());
    }

    @Test void requiresAndRunsConfiguredConditionsInStableOrder() {
        final DefinitionId condition = DefinitionId.of("test", "condition/ranked");
        final ShopCatalog catalog = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD, defaultRules(), Optional.empty(),
                Optional.of(condition), price(TenderRegistry.IRON, 1));
        final MutableTime time = new MutableTime(NOW);
        assertFailure(service(new FakePort(defaultState()), time, true,
                Collections.emptyList(), false).quote(catalog, request(catalog, 1, false)),
                PurchaseFailure.Code.CONDITION_REJECTED);
        final List<String> order = new ArrayList<String>();
        final PurchaseValidator global = validator("a_global", Optional.empty(), order, Optional.empty());
        final PurchaseValidator conditional = validator("b_condition", Optional.of(condition), order,
                Optional.of(PurchaseFailure.of(PurchaseFailure.Code.CONDITION_REJECTED)));
        assertFailure(service(new FakePort(defaultState()), time, true,
                Arrays.asList(conditional, global), false).quote(catalog, request(catalog, 1, false)),
                PurchaseFailure.Code.CONDITION_REJECTED);
        assertEquals(Arrays.asList("a_global", "b_condition"), order);
        assertThrows(IllegalArgumentException.class, () -> service(new FakePort(defaultState()),
                time, true, Arrays.asList(global, global), false));
    }

    @Test void mapsDuplicateStaleRejectedAndExpiredCommitOutcomes() {
        final MutableTime time = new MutableTime(NOW);
        final ShopCatalog catalog = catalog(ShopCatalog.Scope.global(), ShopCatalog.Visibility.VISIBLE,
                ShopCatalog.Availability.STANDARD, defaultRules(), Optional.empty(),
                Optional.empty(), price(TenderRegistry.IRON, 1));
        final FakePort port = new FakePort(defaultState());
        final PurchaseService service = service(port, time, true, Collections.emptyList(), false);
        final PurchaseQuote quote = service.quote(catalog, request(catalog, 1, false)).value().get();
        port.result = PurchaseTransactionPort.CommitResult.success(PurchaseTransactionPort.Status.DUPLICATE);
        assertTrue(service.execute(quote).value().get().duplicate());
        port.result = PurchaseTransactionPort.CommitResult.stale();
        assertFailure(service.execute(quote), PurchaseFailure.Code.STALE_QUOTE);
        port.result = PurchaseTransactionPort.CommitResult.rejected(
                PurchaseFailure.of(PurchaseFailure.Code.INVENTORY_FULL));
        assertFailure(service.execute(quote), PurchaseFailure.Code.INVENTORY_FULL);
        time.now = quote.expiresAt();
        assertFailure(service.execute(quote), PurchaseFailure.Code.QUOTE_EXPIRED);
        assertThrows(IllegalArgumentException.class, () -> PurchaseTransactionPort.CommitResult.success(
                PurchaseTransactionPort.Status.STALE));
        assertNotNull(PurchaseFailure.of(PurchaseFailure.Code.FORBIDDEN).message());
    }

    @Test void revalidatesEveryRequiredPermissionImmediatelyBeforeCommit() {
        final AtomicBoolean allowed = new AtomicBoolean(true);
        final AuthorizationService authorization = request -> allowed.get()
                ? AuthorizationDecision.allow(DefinitionId.of("test", "reason/allowed"))
                : AuthorizationDecision.deny(DefinitionId.of("test", "reason/revoked"));
        final FakePort port = new FakePort(defaultState());
        final PurchaseService service = new PurchaseService(authorization, port,
                TenderRegistry.nativeMatchResources(), new MutableTime(NOW),
                Duration.ofSeconds(2), Collections.emptyList());
        final PermissionNode itemPermission = PermissionNode.of("zartrabedwars.shop.item.wool");
        final ShopCatalog catalog = catalog(ShopCatalog.Scope.global(),
                ShopCatalog.Visibility.VISIBLE, ShopCatalog.Availability.STANDARD,
                rules(false, 1, Duration.ZERO, 0, 0, 0, 0,
                        Optional.of(itemPermission)), Optional.empty(), Optional.empty(),
                price(TenderRegistry.IRON, 1));
        final PurchaseQuote quote = service.quote(catalog, request(catalog, 1, false))
                .value().get();
        assertEquals(Arrays.asList(itemPermission, PurchaseService.PURCHASE_PERMISSION),
                quote.requiredPermissions());
        allowed.set(false);
        assertFailure(service.execute(quote), PurchaseFailure.Code.FORBIDDEN);
        assertEquals(0, port.commitCalls);
    }

    private static PurchaseValidator validator(final String path,
                                               final Optional<DefinitionId> supported,
                                               final List<String> order,
                                               final Optional<PurchaseFailure> result) {
        return new PurchaseValidator() {
            @Override public DefinitionId id() { return DefinitionId.of("test", "validator/" + path); }
            @Override public boolean supports(final Optional<DefinitionId> condition) {
                return supported.equals(condition);
            }
            @Override public Optional<PurchaseFailure> validate(final ShopCatalog catalog,
                    final ShopCatalog.ItemDefinition item, final PurchaseRequest request,
                    final PurchaseTransactionPort.State state,
                    final Optional<DefinitionId> condition) {
                order.add(path);
                return result;
            }
        };
    }

    private static PurchaseService service(final FakePort port, final TimeSource time,
                                           final boolean allowed,
                                           final Collection<PurchaseValidator> validators,
                                           final boolean customTender) {
        final AuthorizationService authorization = request -> allowed
                ? AuthorizationDecision.allow(DefinitionId.of("test", "reason/allowed"))
                : AuthorizationDecision.deny(DefinitionId.of("test", "reason/denied"));
        final List<TenderRegistry.TenderDefinition> definitions = new ArrayList<TenderRegistry.TenderDefinition>(
                TenderRegistry.nativeMatchResources().snapshot());
        if (customTender) {
            definitions.add(new TenderRegistry.TenderDefinition(CRYSTAL,
                    ProviderId.of("extension", "crystal"),
                    TenderRegistry.Kind.CUSTOM_MATCH_RESOURCE));
        }
        return new PurchaseService(authorization, port, new TenderRegistry(definitions),
                time, Duration.ofSeconds(2), validators);
    }

    private static PurchaseRequest request(final ShopCatalog catalog, final int batches,
                                           final boolean confirmed) {
        return new PurchaseRequest(context(), catalog.id(), ShopIds.ItemId.of("test", "wool"),
                batches, confirmed, IdempotencyKey.of("test", "purchase-" + batches + "-" + confirmed));
    }

    private static PurchaseContext context() {
        return new PurchaseContext(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                DefinitionId.of("test", "player/alex")), PlayerId.of(new UUID(0, 1)),
                MatchId.of(new UUID(0, 2)), ArenaId.of(new UUID(0, 3)),
                DefinitionId.of("test", "mode/standard"), DefinitionId.of("test", "team/red"),
                Optional.of(DefinitionId.of("test", "group/public")));
    }

    private static ShopCatalog catalog(final ShopCatalog.Scope scope,
                                       final ShopCatalog.Visibility categoryVisibility,
                                       final ShopCatalog.Availability availability,
                                       final ShopCatalog.PurchaseRules rules,
                                       final Optional<DefinitionId> categoryCondition,
                                       final Optional<DefinitionId> itemCondition,
                                       final ShopCatalog.Price price) {
        final ShopCatalog.Category category = new ShopCatalog.Category(
                ShopIds.CategoryId.of("test", "blocks"), MessageKey.of("category.blocks.name"),
                MessageKey.of("category.blocks.lore"), DefinitionId.of("test", "material/wool"),
                0, 0, categoryVisibility, Optional.empty(), categoryCondition);
        final ShopCatalog.PurchaseRules effective = new ShopCatalog.PurchaseRules(
                rules.confirmationRequired(), rules.maximumBulk(), rules.cooldown(),
                rules.inventoryLimit(), rules.playerLimit(), rules.teamLimit(), rules.arenaLimit(),
                rules.permission(), itemCondition);
        final ShopCatalog.ItemDefinition item = new ShopCatalog.ItemDefinition(
                ShopIds.ItemId.of("test", "wool"), category.id(),
                DefinitionId.of("test", "item/wool"), MessageKey.of("item.wool.name"),
                MessageKey.of("item.wool.lore"), 2, price, availability, effective,
                Collections.emptyList());
        return new ShopCatalog(ShopIds.CatalogId.of("test", "main"), 2, scope,
                DefinitionId.of("test", "shop/profile"), Collections.singletonList(category),
                Collections.singletonList(item));
    }

    private static ShopCatalog.PurchaseRules defaultRules() {
        return rules(false, 4, Duration.ZERO, 64, 0, 0, 0, Optional.empty());
    }

    private static ShopCatalog.PurchaseRules rules(final boolean confirmation, final int maxBulk,
                                                   final Duration cooldown, final int inventory,
                                                   final int player, final int team, final int arena,
                                                   final Optional<PermissionNode> permission) {
        return new ShopCatalog.PurchaseRules(confirmation, maxBulk, cooldown, inventory,
                player, team, arena, permission, Optional.empty());
    }

    private static ShopCatalog.Price price(final ResourceId resource, final long amount) {
        return new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(resource, amount)));
    }

    private static Map<ResourceId, Long> balances(final long iron, final long crystal) {
        final Map<ResourceId, Long> result = new LinkedHashMap<ResourceId, Long>();
        result.put(TenderRegistry.IRON, iron);
        result.put(CRYSTAL, crystal);
        return result;
    }

    private static PurchaseTransactionPort.State defaultState() {
        return state(1, true, 0, 0, 0, 0, balances(20, 0), Optional.empty());
    }

    private static PurchaseTransactionPort.State state(final long revision, final boolean capacity,
                                                        final int owned, final int player,
                                                        final int team, final int arena,
                                                        final Map<ResourceId, Long> balances,
                                                        final Optional<Instant> lastPurchase) {
        return new PurchaseTransactionPort.State(revision, balances, capacity, owned,
                player, team, arena, lastPurchase);
    }

    private static long amount(final ShopCatalog.Price price, final ResourceId resource) {
        return price.amounts().stream().filter(value -> value.resourceId().equals(resource))
                .findFirst().get().amount();
    }

    private static <T> void assertFailure(final PurchaseResult<T> result,
                                          final PurchaseFailure.Code code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.failure().get().code());
    }

    private static final class MutableTime implements TimeSource {
        private Instant now;
        private MutableTime(final Instant now) { this.now = now; }
        @Override public Instant now() { return now; }
    }

    private static final class FakePort implements PurchaseTransactionPort {
        private final State state;
        private CommitResult result = CommitResult.success(Status.APPLIED);
        private int commitCalls;
        private CommitRequest lastCommit;
        private FakePort(final State state) { this.state = state; }
        @Override public State inspect(final PurchaseRequest request,
                                       final ShopCatalog.ItemDefinition item) {
            return state;
        }
        @Override public CommitResult commit(final CommitRequest request) {
            commitCalls++;
            lastCommit = request;
            return result;
        }
    }
}
