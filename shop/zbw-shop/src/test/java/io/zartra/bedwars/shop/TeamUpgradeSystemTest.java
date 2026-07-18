package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.shop.upgrade.ForgePolicy;
import io.zartra.bedwars.shop.upgrade.ForgeRuntime;
import io.zartra.bedwars.shop.upgrade.TeamEffectIntent;
import io.zartra.bedwars.shop.upgrade.TeamUpgradeService;
import io.zartra.bedwars.shop.upgrade.TeamUpgradeState;
import io.zartra.bedwars.shop.upgrade.UpgradeCatalog;
import io.zartra.bedwars.shop.upgrade.UpgradeDefinition;
import io.zartra.bedwars.shop.upgrade.UpgradePurchaseResult;
import io.zartra.bedwars.shop.upgrade.UpgradeTransactionPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamUpgradeSystemTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final MatchId MATCH = MatchId.of(new UUID(1L, 40L));
    private static final ArenaId ARENA = ArenaId.of(new UUID(2L, 40L));
    private static final DefinitionId RED = DefinitionId.of("zartra", "team/red");
    private static final DefinitionId BLUE = DefinitionId.of("zartra", "team/blue");
    private static final DefinitionId SHARPNESS = DefinitionId.of("zartra", "upgrade/sharpness");
    private static final DefinitionId PROTECTION = DefinitionId.of("zartra", "upgrade/protection");
    private static final DefinitionId FORGE = DefinitionId.of("zartra", "upgrade/forge");
    private static final DefinitionId HEAL = DefinitionId.of("zartra", "upgrade/heal_pool");
    private static final DefinitionId DRAGON = DefinitionId.of("zartra", "upgrade/dragon_buff");
    private static final DefinitionId TRAP = DefinitionId.of("zartra", "upgrade/alarm_trap");
    private static final ResourceId DIAMOND = ResourceId.of("zartra", "diamond");
    private static final ResourceId IRON = ResourceId.of("zartra", "iron");
    private static final ResourceId GOLD = ResourceId.of("zartra", "gold");

    @Test
    void purchaseLevelsCostsAndEffectsAreDeterministic() {
        final TeamUpgradeService service = service(RED);
        final BalanceTransaction transaction = new BalanceTransaction(20);
        assertEquals(UpgradePurchaseResult.Status.PURCHASED,
                service.purchase(playing(), SHARPNESS, key("sharp/1"), transaction).status());
        assertEquals(1, service.state().level(SHARPNESS));
        assertEquals(18, transaction.balance);
        assertEquals(DefinitionId.of("zartra", "effect/sharpness_1"),
                service.upgradeEffect(SHARPNESS, key("effect/sharp")).get().effect());
        assertEquals(UpgradePurchaseResult.Status.PURCHASED,
                service.purchase(playing(), SHARPNESS, key("sharp/2"), transaction).status());
        assertEquals(2, service.state().level(SHARPNESS));
        assertEquals(UpgradePurchaseResult.Status.MAXIMUM_LEVEL,
                service.purchase(playing(), SHARPNESS, key("sharp/3"), transaction).status());
    }

    @Test
    void rejectsInvalidInsufficientUnknownAndMissingDependencyPurchases() {
        final TeamUpgradeService service = service(RED);
        final BalanceTransaction poor = new BalanceTransaction(0);
        assertEquals(UpgradePurchaseResult.Status.INSUFFICIENT_RESOURCES,
                service.purchase(playing(), SHARPNESS, key("poor"), poor).status());
        assertEquals(UpgradePurchaseResult.Status.DEPENDENCY_MISSING,
                service.purchase(playing(), PROTECTION, key("dependency"), new BalanceTransaction(20)).status());
        assertEquals(UpgradePurchaseResult.Status.UNKNOWN_UPGRADE,
                service.purchase(playing(), DefinitionId.of("example", "missing"), key("missing"), poor).status());
        assertEquals(UpgradePurchaseResult.Status.INVALID_MATCH,
                service.purchase(snapshot(MatchSnapshot.State.WAITING), SHARPNESS, key("waiting"), poor).status());
        assertEquals(UpgradePurchaseResult.Status.REVISION_CONFLICT,
                service.purchase(playing(), SHARPNESS, key("conflict"), new ConflictTransaction()).status());
    }

    @Test
    void duplicatePurchaseDoesNotDebitTwice() {
        final TeamUpgradeService service = service(RED);
        final BalanceTransaction transaction = new BalanceTransaction(20);
        final IdempotencyKey key = key("duplicate");
        assertEquals(UpgradePurchaseResult.Status.PURCHASED,
                service.purchase(playing(), SHARPNESS, key, transaction).status());
        assertEquals(UpgradePurchaseResult.Status.DUPLICATE,
                service.purchase(playing(), SHARPNESS, key, transaction).status());
        assertEquals(1, transaction.commits);
        assertEquals(18, transaction.balance);
    }

    @Test
    void trapQueueActivatesOnceForEnemyAndPreservesTeamIsolation() {
        final TeamUpgradeService red = service(RED);
        final TeamUpgradeService blue = service(BLUE);
        final BalanceTransaction redBalance = new BalanceTransaction(20);
        assertEquals(UpgradePurchaseResult.Status.PURCHASED,
                red.purchase(playing(), TRAP, key("trap/1"), redBalance).status());
        assertEquals(UpgradePurchaseResult.Status.PURCHASED,
                red.purchase(playing(), TRAP, key("trap/2"), redBalance).status());
        assertEquals(2, red.state().traps().size());
        assertFalse(red.activateTrap(playing(), RED, PlayerId.of(new UUID(9L, 1L)), key("friendly")).isPresent());
        final IdempotencyKey activation = key("trap/activate");
        assertEquals(TeamEffectIntent.Kind.TRAP_ACTIVATED, red.activateTrap(playing(), BLUE,
                PlayerId.of(new UUID(9L, 2L)), activation).get().kind());
        assertFalse(red.activateTrap(playing(), BLUE, PlayerId.of(new UUID(9L, 2L)), activation).isPresent());
        assertEquals(1, red.state().traps().size());
        assertTrue(blue.state().traps().isEmpty());
    }

    @Test
    void healPoolAndDragonBuffRemainTeamScoped() {
        final TeamUpgradeService red = service(RED);
        final BalanceTransaction transaction = new BalanceTransaction(20);
        red.purchase(playing(), HEAL, key("heal"), transaction);
        red.purchase(playing(), DRAGON, key("dragon"), transaction);
        assertEquals(TeamEffectIntent.Kind.HEAL_POOL,
                red.passiveEffect(HEAL, TeamEffectIntent.Kind.HEAL_POOL, key("heal/effect")).get().kind());
        assertEquals(TeamEffectIntent.Kind.DRAGON_BUFF,
                red.passiveEffect(DRAGON, TeamEffectIntent.Kind.DRAGON_BUFF, key("dragon/effect")).get().kind());
        assertThrows(IllegalArgumentException.class, () -> red.passiveEffect(HEAL,
                TeamEffectIntent.Kind.UPGRADE_APPLIED, key("invalid/effect")));
        assertFalse(service(BLUE).passiveEffect(HEAL, TeamEffectIntent.Kind.HEAL_POOL,
                key("blue/heal")).isPresent());
    }

    @Test
    void forgeProducesConfiguredResourcesWithBoundedDeterministicTiming() {
        final TeamUpgradeService service = service(RED);
        service.purchase(playing(), FORGE, key("forge/1"), new BalanceTransaction(20));
        final ForgeRuntime forge = new ForgeRuntime(forgePolicy());
        assertTrue(forge.tick(playing(), service.state(), NOW).isEmpty());
        final List<TeamEffectIntent> first = forge.tick(playing(), service.state(), NOW.plusSeconds(3));
        assertEquals(1, first.size());
        assertEquals(Integer.valueOf(2), first.get(0).resources().get(IRON));
        assertEquals(Integer.valueOf(1), first.get(0).resources().get(GOLD));
        final List<TeamEffectIntent> bounded = forge.tick(playing(), service.state(), NOW.plusSeconds(1000));
        assertEquals(ForgeRuntime.MAX_EMISSIONS, bounded.size());
        assertEquals(ForgeRuntime.MAX_EMISSIONS + 1L, forge.sequence());
    }

    @Test
    void forgeUpgradeChangesTimingAndCustomOutputPolicy() {
        final TeamUpgradeService service = service(RED);
        final BalanceTransaction transaction = new BalanceTransaction(20);
        service.purchase(playing(), FORGE, key("forge/change/1"), transaction);
        final ForgeRuntime forge = new ForgeRuntime(forgePolicy());
        forge.tick(playing(), service.state(), NOW);
        service.purchase(playing(), FORGE, key("forge/change/2"), transaction);
        assertTrue(forge.tick(playing(), service.state(), NOW.plusSeconds(1)).isEmpty());
        assertEquals(1, forge.tick(playing(), service.state(), NOW.plusSeconds(2)).size());
    }

    @Test
    void cleanupReconnectAndRecoveryPreserveOnlyValidMatchLocalState() {
        final TeamUpgradeService original = service(RED);
        original.purchase(playing(), SHARPNESS, key("recover/purchase"), new BalanceTransaction(20));
        final TeamUpgradeState recovered = TeamUpgradeState.restore(MATCH, RED,
                original.state().revision(), original.state().levels(), original.state().traps(),
                Collections.singleton(key("recover/purchase")));
        final TeamUpgradeService resumed = new TeamUpgradeService(catalog(), recovered);
        assertEquals(1, resumed.state().level(SHARPNESS));
        assertEquals(UpgradePurchaseResult.Status.DUPLICATE,
                resumed.purchase(playing(), SHARPNESS, key("recover/purchase"), new BalanceTransaction(20)).status());
        assertEquals(1, resumed.state().level(SHARPNESS));
        resumed.observe(snapshot(MatchSnapshot.State.RESETTING));
        assertTrue(resumed.state().cleaned());
        assertTrue(resumed.state().levels().isEmpty());
        assertEquals(UpgradePurchaseResult.Status.INVALID_MATCH,
                resumed.purchase(snapshot(MatchSnapshot.State.RESETTING), SHARPNESS,
                        key("after/cleanup"), new BalanceTransaction(20)).status());
    }

    @Test
    void definitionsAndCatalogRejectMalformedLevelsCostsAndDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeDefinition(SHARPNESS,
                UpgradeDefinition.Kind.SHARPNESS, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> level(1, 0,
                Collections.<DefinitionId, Integer>emptyMap(), "bad"));
        final UpgradeDefinition broken = new UpgradeDefinition(PROTECTION,
                UpgradeDefinition.Kind.PROTECTION, Collections.singletonList(level(1, 2,
                Collections.singletonMap(DefinitionId.of("example", "unknown"), 1), "broken")));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeCatalog(Collections.singletonList(broken)));
        assertThrows(IllegalArgumentException.class, () -> new ForgePolicy.Level(Duration.ZERO,
                Collections.singletonMap(IRON, 1)));
    }

    private static TeamUpgradeService service(final DefinitionId team) {
        return new TeamUpgradeService(catalog(), TeamUpgradeState.empty(MATCH, team));
    }
    private static UpgradeCatalog catalog() {
        final UpgradeDefinition sharpness = new UpgradeDefinition(SHARPNESS,
                UpgradeDefinition.Kind.SHARPNESS, Arrays.asList(level(1, 2,
                Collections.<DefinitionId, Integer>emptyMap(), "sharpness_1"), level(2, 4,
                Collections.<DefinitionId, Integer>emptyMap(), "sharpness_2")));
        final UpgradeDefinition protection = new UpgradeDefinition(PROTECTION,
                UpgradeDefinition.Kind.PROTECTION, Collections.singletonList(level(1, 3,
                Collections.singletonMap(SHARPNESS, 1), "protection_1")));
        final UpgradeDefinition forge = new UpgradeDefinition(FORGE, UpgradeDefinition.Kind.FORGE,
                Arrays.asList(level(1, 2, Collections.<DefinitionId, Integer>emptyMap(), "forge_1"),
                        level(2, 4, Collections.<DefinitionId, Integer>emptyMap(), "forge_2")));
        final UpgradeDefinition heal = new UpgradeDefinition(HEAL, UpgradeDefinition.Kind.HEAL_POOL,
                Collections.singletonList(level(1, 1, Collections.<DefinitionId, Integer>emptyMap(), "heal_pool")));
        final UpgradeDefinition dragon = new UpgradeDefinition(DRAGON, UpgradeDefinition.Kind.DRAGON_BUFF,
                Collections.singletonList(level(1, 1, Collections.<DefinitionId, Integer>emptyMap(), "dragon_buff")));
        final UpgradeDefinition trap = new UpgradeDefinition(TRAP, UpgradeDefinition.Kind.TRAP,
                Arrays.asList(level(1, 1, Collections.<DefinitionId, Integer>emptyMap(), "alarm_trap"),
                        level(2, 2, Collections.<DefinitionId, Integer>emptyMap(), "alarm_trap")));
        return new UpgradeCatalog(Arrays.asList(sharpness, protection, forge, heal, dragon, trap));
    }
    private static UpgradeDefinition.Level level(final int number, final int cost,
                                                  final Map<DefinitionId, Integer> dependencies,
                                                  final String effect) {
        return UpgradeDefinition.Level.of(number, Collections.singletonMap(DIAMOND, cost), dependencies,
                DefinitionId.of("zartra", "effect/" + effect), null);
    }
    private static ForgePolicy forgePolicy() {
        final Map<Integer, ForgePolicy.Level> levels = new HashMap<Integer, ForgePolicy.Level>();
        final Map<ResourceId, Integer> first = new HashMap<ResourceId, Integer>();
        first.put(IRON, 2);
        first.put(GOLD, 1);
        levels.put(1, new ForgePolicy.Level(Duration.ofSeconds(3), first));
        levels.put(2, new ForgePolicy.Level(Duration.ofSeconds(1), Collections.singletonMap(GOLD, 2)));
        return new ForgePolicy(FORGE, levels);
    }
    private static MatchSnapshot playing() { return snapshot(MatchSnapshot.State.PLAYING); }
    private static MatchSnapshot snapshot(final MatchSnapshot.State state) {
        return new MatchSnapshot(MATCH, ARENA, 0, state, 0,
                Arrays.asList(TeamSnapshot.empty(BLUE, 2), TeamSnapshot.empty(RED, 2)),
                Collections.emptyList(), null, null, false, NOW);
    }
    private static IdempotencyKey key(final String path) { return IdempotencyKey.of("test", path); }

    private static final class BalanceTransaction implements UpgradeTransactionPort {
        private int balance;
        private int commits;
        private final Set<IdempotencyKey> keys = new HashSet<IdempotencyKey>();
        private BalanceTransaction(final int balance) { this.balance = balance; }
        @Override public Result commit(final TeamUpgradeState expected,
                                       final Map<ResourceId, Integer> cost,
                                       final IdempotencyKey key) {
            if (!keys.add(key)) { return Result.COMMITTED; }
            final int required = cost.get(DIAMOND);
            if (balance < required) {
                keys.remove(key);
                return Result.INSUFFICIENT_RESOURCES;
            }
            balance -= required;
            commits++;
            return Result.COMMITTED;
        }
    }
    private static final class ConflictTransaction implements UpgradeTransactionPort {
        @Override public Result commit(final TeamUpgradeState expected,
                                       final Map<ResourceId, Integer> cost,
                                       final IdempotencyKey key) {
            return Result.REVISION_CONFLICT;
        }
    }
}
