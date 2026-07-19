package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.game.model.MatchSnapshot;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Synchronized deterministic lifecycle for match-local utility actions. */
public final class ItemActionService {
    private final UtilityItemCatalog catalog;
    private final ItemActionPorts.Authorization authorization;
    private final ItemActionPorts.Transaction transaction;
    private final ItemActionPorts.Effect effect;
    private final DefinitionId owner;
    private final Set<IdempotencyKey> completed = new HashSet<IdempotencyKey>();
    private final Map<String, Instant> cooldowns = new HashMap<String, Instant>();
    private final Map<String, Integer> counts = new HashMap<String, Integer>();
    private long revision;
    private boolean cleaned;
    private boolean active;

    /** Creates an isolated action runtime for one match owner. */
    public ItemActionService(final UtilityItemCatalog catalog,
                             final ItemActionPorts.Authorization authorization,
                             final ItemActionPorts.Transaction transaction,
                             final ItemActionPorts.Effect effect, final DefinitionId owner) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /** Validates and executes one action atomically with respect to this match runtime. */
    public synchronized ItemActionResult execute(final MatchSnapshot match,
                                                  final ItemActionRequest request) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(request, "request");
        if (!match.matchId().equals(request.context().matchId())
                || match.state() != MatchSnapshot.State.PLAYING || cleaned
                || !match.session(request.context().playerId()).isPresent()) {
            return ItemActionResult.of(ItemActionResult.Status.INVALID_STATE);
        }
        active = true;
        if (completed.contains(request.key())) {
            return ItemActionResult.of(ItemActionResult.Status.DUPLICATE);
        }
        final UtilityItemDefinition definition = catalog.definition(request.actionId()).orElse(null);
        if (definition == null) { return ItemActionResult.of(ItemActionResult.Status.UNKNOWN_ACTION); }
        if (!authorization.allowed(AuthorizationRequest.of(request.context().subject(),
                definition.permission(), definition.id()))) {
            return ItemActionResult.of(ItemActionResult.Status.DENIED);
        }
        if (!validTarget(definition, request)) {
            return ItemActionResult.of(ItemActionResult.Status.INVALID_TARGET);
        }
        final String actorAction = request.context().playerId() + "/" + definition.id();
        final Instant retryAt = cooldowns.get(actorAction);
        if (retryAt != null && request.requestedAt().isBefore(retryAt)) {
            return ItemActionResult.cooldown(retryAt);
        }
        final int count = counts.containsKey(actorAction) ? counts.get(actorAction) : 0;
        if (count >= definition.perMatchLimit()) {
            return ItemActionResult.of(ItemActionResult.Status.LIMIT_REACHED);
        }
        final ItemActionPorts.Outcome outcome = transaction.commit(definition, request, revision);
        if (outcome == ItemActionPorts.Outcome.DUPLICATE) {
            completed.add(request.key());
            return ItemActionResult.of(ItemActionResult.Status.DUPLICATE);
        }
        if (outcome == ItemActionPorts.Outcome.INSUFFICIENT) {
            return ItemActionResult.of(ItemActionResult.Status.INSUFFICIENT_RESOURCES);
        }
        if (outcome == ItemActionPorts.Outcome.CONFLICT) {
            return ItemActionResult.of(ItemActionResult.Status.CONFLICT);
        }
        final DefinitionId effectId = DefinitionId.of("zartra", "item-effect/" + definition.kind().name().toLowerCase());
        if (!effect.apply(effectId, definition, request, request.key())) {
            transaction.compensate(definition, request);
            return ItemActionResult.of(ItemActionResult.Status.EFFECT_REJECTED);
        }
        revision++;
        completed.add(request.key());
        counts.put(actorAction, count + 1);
        cooldowns.put(actorAction, request.requestedAt().plus(definition.cooldown()));
        return ItemActionResult.executed(effectId);
    }

    private boolean validTarget(final UtilityItemDefinition definition,
                                final ItemActionRequest request) {
        final UtilityItemDefinition.TargetRule rule = definition.targetRule();
        if (rule == UtilityItemDefinition.TargetRule.NONE) { return !request.target().isPresent(); }
        if (!request.target().isPresent()) { return false; }
        final ItemActionRequest.Target target = request.target().get();
        if (!target.active()) { return false; }
        if (rule == UtilityItemDefinition.TargetRule.LOCATION) { return target.buildAllowed(); }
        if (rule == UtilityItemDefinition.TargetRule.SELF) {
            return target.player().filter(request.context().playerId()::equals).isPresent();
        }
        if (rule == UtilityItemDefinition.TargetRule.TEAMMATE || rule == UtilityItemDefinition.TargetRule.OWN_BED) {
            return target.team().filter(request.context().teamId()::equals).isPresent();
        }
        if (rule == UtilityItemDefinition.TargetRule.ENEMY || rule == UtilityItemDefinition.TargetRule.ENEMY_BED) {
            return target.team().isPresent() && !request.context().teamId().equals(target.team().get());
        }
        return rule == UtilityItemDefinition.TargetRule.GENERATOR && target.buildAllowed();
    }

    /** Cleans every match-owned effect and rejects future execution. */
    public synchronized void cleanup() {
        if (!cleaned) { effect.cleanup(owner); }
        completed.clear();
        cooldowns.clear();
        counts.clear();
        cleaned = true;
    }
    /** Consumes M08 lifecycle without recreating it. */
    public synchronized void synchronize(final MatchSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.state() == MatchSnapshot.State.PLAYING) { active = true; }
        else if (active) { cleanup(); }
    }
    /** @return local optimistic revision */ public synchronized long revision() { return revision; }
    /** @return terminal cleanup state */ public synchronized boolean cleaned() { return cleaned; }
}
