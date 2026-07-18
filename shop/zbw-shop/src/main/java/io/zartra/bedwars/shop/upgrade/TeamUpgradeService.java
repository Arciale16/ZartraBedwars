package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/** Synchronized application service for one isolated team's match-local upgrades and traps. */
public final class TeamUpgradeService {
    private final UpgradeCatalog catalog;
    private TeamUpgradeState state;
    /** Creates a service from empty or recovered state. */
    public TeamUpgradeService(final UpgradeCatalog catalog, final TeamUpgradeState state) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.state = Objects.requireNonNull(state, "state");
    }
    /** Validates and purchases exactly the next upgrade level through one atomic debit. */
    public synchronized UpgradePurchaseResult purchase(final MatchSnapshot match,
                                                        final DefinitionId upgradeId,
                                                        final IdempotencyKey key,
                                                        final UpgradeTransactionPort transaction) {
        Objects.requireNonNull(upgradeId, "upgradeId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(transaction, "transaction");
        final UpgradePurchaseResult invalid = validateMatch(match);
        if (invalid != null) { return invalid; }
        if (state.cleaned()) { return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.CLEANED, null); }
        if (state.completed(key)) { return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.DUPLICATE, state); }
        final Optional<UpgradeDefinition> selected = catalog.find(upgradeId);
        if (!selected.isPresent()) { return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.UNKNOWN_UPGRADE, null); }
        final UpgradeDefinition definition = selected.get();
        final int next = definition.kind() == UpgradeDefinition.Kind.TRAP
                ? state.traps().size() + 1 : state.level(upgradeId) + 1;
        if (next > definition.maximumLevel()) { return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.MAXIMUM_LEVEL, null); }
        final UpgradeDefinition.Level level = definition.level(next);
        for (java.util.Map.Entry<DefinitionId, Integer> dependency : level.dependencies().entrySet()) {
            if (state.level(dependency.getKey()) < dependency.getValue()) {
                return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.DEPENDENCY_MISSING, null);
            }
        }
        final UpgradeTransactionPort.Result committed = Objects.requireNonNull(
                transaction.commit(state, level.cost(), key), "transaction result");
        if (committed == UpgradeTransactionPort.Result.INSUFFICIENT_RESOURCES) {
            return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.INSUFFICIENT_RESOURCES, null);
        }
        if (committed == UpgradeTransactionPort.Result.REVISION_CONFLICT) {
            return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.REVISION_CONFLICT, null);
        }
        state = state.upgraded(definition, next, key);
        return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.PURCHASED, state);
    }
    /** Activates and consumes the oldest trap once for an enemy player. */
    public synchronized Optional<TeamEffectIntent> activateTrap(final MatchSnapshot match,
                                                                 final DefinitionId intruderTeam,
                                                                 final PlayerId intruder,
                                                                 final IdempotencyKey key) {
        Objects.requireNonNull(intruderTeam, "intruderTeam");
        Objects.requireNonNull(intruder, "intruder");
        Objects.requireNonNull(key, "key");
        if (validateMatch(match) != null || state.cleaned() || state.completed(key)
                || state.teamId().equals(intruderTeam) || state.traps().isEmpty()) {
            return Optional.empty();
        }
        final TeamUpgradeState.TrapCharge trap = state.traps().get(0);
        state = state.activatedTrap(key);
        return Optional.of(new TeamEffectIntent(key, TeamEffectIntent.Kind.TRAP_ACTIVATED,
                state.teamId(), trap.effect(), intruder, Collections.emptyMap()));
    }
    /** Returns a deterministic current passive effect intent when its upgrade is owned. */
    public synchronized Optional<TeamEffectIntent> passiveEffect(final DefinitionId upgradeId,
                                                                  final TeamEffectIntent.Kind kind,
                                                                  final IdempotencyKey key) {
        if (kind != TeamEffectIntent.Kind.HEAL_POOL && kind != TeamEffectIntent.Kind.DRAGON_BUFF) {
            throw new IllegalArgumentException("unsupported passive effect kind");
        }
        final Optional<UpgradeDefinition> definition = catalog.find(Objects.requireNonNull(upgradeId, "upgradeId"));
        final int level = state.level(upgradeId);
        if (!definition.isPresent() || level == 0 || state.cleaned()) { return Optional.empty(); }
        return Optional.of(new TeamEffectIntent(Objects.requireNonNull(key, "key"), kind,
                state.teamId(), definition.get().level(level).effect(), null, Collections.emptyMap()));
    }
    /** Returns the current generic team-effect intent for a purchased non-passive upgrade. */
    public synchronized Optional<TeamEffectIntent> upgradeEffect(final DefinitionId upgradeId,
                                                                  final IdempotencyKey key) {
        final Optional<UpgradeDefinition> definition = catalog.find(Objects.requireNonNull(upgradeId, "upgradeId"));
        final int level = state.level(upgradeId);
        if (!definition.isPresent() || level == 0 || state.cleaned()) { return Optional.empty(); }
        return Optional.of(new TeamEffectIntent(Objects.requireNonNull(key, "key"),
                TeamEffectIntent.Kind.UPGRADE_APPLIED, state.teamId(),
                definition.get().level(level).effect(), null, Collections.emptyMap()));
    }
    /** Clears match-local team systems when M08 leaves PLAYING. */
    public synchronized void observe(final MatchSnapshot match) {
        if (validateIdentity(match) && match.state() != MatchSnapshot.State.PLAYING && !state.cleaned()) {
            state = state.cleanup();
        }
    }
    /** @return immutable current/recovered state */ public synchronized TeamUpgradeState state() { return state; }
    private UpgradePurchaseResult validateMatch(final MatchSnapshot match) {
        if (!validateIdentity(match) || match.state() != MatchSnapshot.State.PLAYING) {
            return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.INVALID_MATCH, null);
        }
        if (!match.team(state.teamId()).isPresent()) {
            return UpgradePurchaseResult.of(UpgradePurchaseResult.Status.UNKNOWN_TEAM, null);
        }
        return null;
    }
    private boolean validateIdentity(final MatchSnapshot match) {
        return match != null && state.matchId().equals(match.matchId());
    }
}
