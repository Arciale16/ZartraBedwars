package io.zartra.bedwars.shop.application;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.shop.api.PurchaseFailure;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.PurchaseResult;
import io.zartra.bedwars.shop.api.PurchaseTransactionPort;
import io.zartra.bedwars.shop.api.PurchaseValidator;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.TenderRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Immutable quote and purchase coordinator for match-local resources.
 *
 * <p>All methods are synchronous because the live inventory port is an owner-thread boundary;
 * neither method performs storage or network I/O. Callers must invoke the service on the inventory
 * owner's thread. Expected validation failures are typed results. The service has no ambient state,
 * so retries are governed exclusively by the supplied idempotency key and atomic port.</p>
 */
public final class PurchaseService {
    /** Canonical central authorization node for every player purchase. */
    public static final PermissionNode PURCHASE_PERMISSION = PermissionNode.of("zartrabedwars.shop.purchase");

    private final AuthorizationService authorization;
    private final PurchaseTransactionPort transactionPort;
    private final TenderRegistry tenders;
    private final TimeSource timeSource;
    private final Duration quoteLifetime;
    private final List<PurchaseValidator> validators;

    /** Creates a deterministic service with a bounded quote lifetime and unique validators. */
    public PurchaseService(final AuthorizationService authorization,
                           final PurchaseTransactionPort transactionPort,
                           final TenderRegistry tenders, final TimeSource timeSource,
                           final Duration quoteLifetime,
                           final Collection<PurchaseValidator> validators) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.transactionPort = Objects.requireNonNull(transactionPort, "transactionPort");
        this.tenders = Objects.requireNonNull(tenders, "tenders");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        this.quoteLifetime = Objects.requireNonNull(quoteLifetime, "quoteLifetime");
        if (quoteLifetime.compareTo(Duration.ofMillis(1)) < 0
                || quoteLifetime.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("quote lifetime is out of range");
        }
        final Map<DefinitionId, PurchaseValidator> unique = new LinkedHashMap<DefinitionId, PurchaseValidator>();
        for (PurchaseValidator validator : Objects.requireNonNull(validators, "validators")) {
            final PurchaseValidator checked = Objects.requireNonNull(validator, "validator");
            if (unique.put(Objects.requireNonNull(checked.id(), "validator id"), checked) != null) {
                throw new IllegalArgumentException("duplicate purchase validator " + checked.id());
            }
        }
        final List<PurchaseValidator> sorted = new ArrayList<PurchaseValidator>(unique.values());
        sorted.sort(Comparator.comparing(PurchaseValidator::id));
        this.validators = Collections.unmodifiableList(sorted);
    }

    /** Validates a request against one immutable catalog and returns an expiring quote. */
    public PurchaseResult<PurchaseQuote> quote(final ShopCatalog catalog,
                                                final PurchaseRequest request) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(request, "request");
        if (!catalog.id().equals(request.catalogId())) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.CATALOG_MISMATCH));
        }
        if (!catalog.scope().matches(request.context().modeId(), request.context().arenaId(),
                request.context().groupId(), request.context().teamId())) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.SCOPE_MISMATCH));
        }
        final Optional<ShopCatalog.ItemDefinition> found = catalog.item(request.itemId());
        if (!found.isPresent()) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.UNKNOWN_ITEM));
        }
        final ShopCatalog.ItemDefinition item = found.get();
        final ShopCatalog.Category category = catalog.category(item.categoryId()).get();
        if (item.availability() == ShopCatalog.Availability.DISABLED
                || item.availability() == ShopCatalog.Availability.HIDDEN
                || category.visibility() != ShopCatalog.Visibility.VISIBLE) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.UNAVAILABLE));
        }
        final List<PermissionNode> requiredPermissions = requiredPermissions(category, item);
        for (PermissionNode permission : requiredPermissions) {
            if (!authorized(request, permission)) {
                return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.FORBIDDEN));
            }
        }
        if (request.batches() > item.rules().maximumBulk()) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.BULK_LIMIT));
        }
        if (item.rules().confirmationRequired() && !request.confirmed()) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.CONFIRMATION_REQUIRED));
        }
        for (ShopCatalog.ResourceAmount amount : item.price().amounts()) {
            if (!tenders.find(amount.resourceId()).isPresent()) {
                return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.UNKNOWN_TENDER));
            }
        }
        final ShopCatalog.Price price = item.price().multiply(request.batches());
        final int grantQuantity = item.grantQuantity() * request.batches();
        final PurchaseTransactionPort.State state = transactionPort.inspect(request, item);
        final Optional<PurchaseFailure> builtIn = validateState(item, request, price, grantQuantity, state);
        if (builtIn.isPresent()) { return PurchaseResult.failure(builtIn.get()); }
        final Optional<PurchaseFailure> custom = validateExtensions(catalog, item, request, state,
                category.condition(), item.rules().condition());
        if (custom.isPresent()) { return PurchaseResult.failure(custom.get()); }
        final Instant issuedAt = timeSource.now();
        return PurchaseResult.success(new PurchaseQuote(request, catalog.revision(), state.revision(),
                price, grantQuantity, requiredPermissions, issuedAt, issuedAt.plus(quoteLifetime)));
    }

    /** Commits an accepted quote through exactly one atomic inventory mutation. */
    public PurchaseResult<PurchaseOutcome> execute(final PurchaseQuote quote) {
        Objects.requireNonNull(quote, "quote");
        final Instant now = timeSource.now();
        if (!now.isBefore(quote.expiresAt())) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.QUOTE_EXPIRED));
        }
        for (PermissionNode permission : quote.requiredPermissions()) {
            if (!authorized(quote.request(), permission)) {
                return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.FORBIDDEN));
            }
        }
        final PurchaseTransactionPort.CommitResult committed = Objects.requireNonNull(
                transactionPort.commit(new PurchaseTransactionPort.CommitRequest(quote)),
                "commit result");
        if (committed.status() == PurchaseTransactionPort.Status.APPLIED
                || committed.status() == PurchaseTransactionPort.Status.DUPLICATE) {
            return PurchaseResult.success(new PurchaseOutcome(quote,
                    committed.status() == PurchaseTransactionPort.Status.DUPLICATE, now));
        }
        if (committed.status() == PurchaseTransactionPort.Status.STALE) {
            return PurchaseResult.failure(PurchaseFailure.of(PurchaseFailure.Code.STALE_QUOTE));
        }
        return PurchaseResult.failure(committed.failure().orElse(
                PurchaseFailure.of(PurchaseFailure.Code.TRANSACTION_REJECTED)));
    }

    private boolean authorized(final PurchaseRequest request, final PermissionNode permission) {
        final AuthorizationDecision decision = authorization.authorize(AuthorizationRequest.of(
                request.context().subject(), permission, request.itemId().value()));
        return Objects.requireNonNull(decision, "authorization decision").isAllowed();
    }

    private static List<PermissionNode> requiredPermissions(
            final ShopCatalog.Category category, final ShopCatalog.ItemDefinition item) {
        final TreeSet<PermissionNode> result = new TreeSet<PermissionNode>();
        result.add(PURCHASE_PERMISSION);
        if (category.permission().isPresent()) { result.add(category.permission().get()); }
        if (item.rules().permission().isPresent()) { result.add(item.rules().permission().get()); }
        return Collections.unmodifiableList(new ArrayList<PermissionNode>(result));
    }

    private Optional<PurchaseFailure> validateState(final ShopCatalog.ItemDefinition item,
                                                     final PurchaseRequest request,
                                                     final ShopCatalog.Price price,
                                                     final int grantQuantity,
                                                     final PurchaseTransactionPort.State state) {
        if (!state.canReceive()) { return failed(PurchaseFailure.Code.INVENTORY_FULL); }
        for (ShopCatalog.ResourceAmount amount : price.amounts()) {
            if (state.balances().getOrDefault(amount.resourceId(), 0L) < amount.amount()) {
                return failed(PurchaseFailure.Code.INSUFFICIENT_RESOURCES);
            }
        }
        final ShopCatalog.PurchaseRules rules = item.rules();
        if (exceeds(state.ownedUnits(), grantQuantity, rules.inventoryLimit())) {
            return failed(PurchaseFailure.Code.INVENTORY_LIMIT);
        }
        if (exceeds(state.playerPurchases(), request.batches(), rules.playerLimit())) {
            return failed(PurchaseFailure.Code.PLAYER_LIMIT);
        }
        if (exceeds(state.teamPurchases(), request.batches(), rules.teamLimit())) {
            return failed(PurchaseFailure.Code.TEAM_LIMIT);
        }
        if (exceeds(state.arenaPurchases(), request.batches(), rules.arenaLimit())) {
            return failed(PurchaseFailure.Code.ARENA_LIMIT);
        }
        if (!rules.cooldown().isZero() && state.lastPurchaseAt().isPresent()) {
            final Instant retryAt = state.lastPurchaseAt().get().plus(rules.cooldown());
            if (timeSource.now().isBefore(retryAt)) { return Optional.of(PurchaseFailure.retryAt(retryAt)); }
        }
        return Optional.empty();
    }

    private Optional<PurchaseFailure> validateExtensions(final ShopCatalog catalog,
                                                          final ShopCatalog.ItemDefinition item,
                                                          final PurchaseRequest request,
                                                          final PurchaseTransactionPort.State state,
                                                          final Optional<DefinitionId> categoryCondition,
                                                          final Optional<DefinitionId> itemCondition) {
        final List<Optional<DefinitionId>> conditions = new ArrayList<Optional<DefinitionId>>();
        conditions.add(Optional.empty());
        if (categoryCondition.isPresent()) { conditions.add(categoryCondition); }
        if (itemCondition.isPresent() && !itemCondition.equals(categoryCondition)) { conditions.add(itemCondition); }
        for (Optional<DefinitionId> condition : conditions) {
            boolean handled = !condition.isPresent();
            for (PurchaseValidator validator : validators) {
                if (validator.supports(condition)) {
                    handled = true;
                    final Optional<PurchaseFailure> result = validator.validate(
                            catalog, item, request, state, condition);
                    if (result.isPresent()) { return result; }
                }
            }
            if (!handled) { return failed(PurchaseFailure.Code.CONDITION_REJECTED); }
        }
        return Optional.empty();
    }

    private static boolean exceeds(final int current, final int added, final int limit) {
        return limit > 0 && current > limit - added;
    }

    private static Optional<PurchaseFailure> failed(final PurchaseFailure.Code code) {
        return Optional.of(PurchaseFailure.of(code));
    }
}
