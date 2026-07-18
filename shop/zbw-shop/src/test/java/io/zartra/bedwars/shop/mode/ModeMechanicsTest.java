package io.zartra.bedwars.shop.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.game.mode.ModeFramework;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.PlayerSession;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.shop.api.RotationContracts;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.mode.ModeMechanics.Ability;
import io.zartra.bedwars.shop.mode.ModeMechanics.Binding;
import io.zartra.bedwars.shop.mode.ModeMechanics.Configuration;
import io.zartra.bedwars.shop.mode.ModeMechanics.Feature;
import io.zartra.bedwars.shop.mode.ModeMechanics.Limits;
import io.zartra.bedwars.shop.mode.ModeMechanics.LuckyOutcome;
import io.zartra.bedwars.shop.mode.ModeMechanics.Runtime;
import io.zartra.bedwars.shop.mode.ModeMechanics.UltimateAbility;
import io.zartra.bedwars.shop.mode.ModeMechanics.Weapon;
import io.zartra.bedwars.shop.mode.ModeMechanics.WeaponArchetype;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Deterministic contract tests for the M11.1 Phase 2 mode/addon mechanics. */
final class ModeMechanicsTest {
    private static final MatchId MATCH = MatchId.of(UUID.fromString(
            "00000000-0000-0000-0000-000000000111"));
    private static final ArenaId ARENA = ArenaId.of(UUID.fromString(
            "00000000-0000-0000-0000-000000000222"));
    private static final PlayerId PLAYER = PlayerId.of(UUID.fromString(
            "00000000-0000-0000-0000-000000000333"));
    private static final DefinitionId RED = DefinitionId.of("team", "red");
    private static final DefinitionId BLUE = DefinitionId.of("team", "blue");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test void rejectsIncompleteOrUnallocatedBindings() {
        assertThrows(IllegalArgumentException.class, () -> new Binding(
                definition(Collections.<String>emptyList()), Feature.ARMED,
                Collections.singleton("ZBW-ADDON-010")));
        assertThrows(IllegalArgumentException.class, () -> runtime(Collections.singletonList(
                binding(Feature.ARMED, "ZBW-ADDON-010")), new Ports()));
        assertEquals(Feature.ARMED,
                binding(Feature.ARMED, "ZBW-ADDON-010").feature());
        assertThrows(IllegalArgumentException.class, () -> new Binding(
                definition(Collections.singletonList("ZBW-ADDON-010")), Feature.ARMED,
                Collections.singleton("invalid")));
        assertThrows(IllegalArgumentException.class, () -> new Binding(
                definition(Collections.singletonList("ZBW-ADDON-010")), Feature.ARMED,
                Arrays.asList("ZBW-ADDON-010", "ZBW-ADDON-010")));
        final List<Binding> duplicated = bindings();
        duplicated.add(binding(Feature.ARMED, "ZBW-ADDON-010"));
        assertThrows(IllegalArgumentException.class, () -> runtime(duplicated, new Ports()));
    }

    @Test void armedModeEnforcesRangeCadenceAmmoReloadAndIdempotency() {
        final Ports ports = new Ports();
        final Runtime runtime = runtime(bindings(), ports);
        runtime.observe(snapshot(MatchSnapshot.State.PLAYING));
        final DefinitionId weapon = DefinitionId.of("weapon", "rapid");
        assertFalse(runtime.fire(PLAYER, weapon, 101, false, NOW, key("range")).success());
        assertTrue(runtime.fire(PLAYER, weapon, 10, true, NOW, key("shot")).success());
        assertFalse(runtime.fire(PLAYER, weapon, 10, false, NOW, key("cadence")).success());
        assertFalse(runtime.fire(PLAYER, weapon, 10, false, NOW.plusSeconds(1), key("shot")).success());
        assertTrue(runtime.reload(PLAYER, weapon, NOW.plusSeconds(1), key("reload")).success());
        assertEquals(2, ports.commits);
    }

    @Test void luckyBlocksAreDeterministicBoundedAndIdempotent() {
        final Ports first = new Ports();
        final Runtime one = runtime(bindings(), first);
        one.observe(snapshot(MatchSnapshot.State.PLAYING));
        final ModeMechanics.Result result = one.openLuckyBlock(PLAYER, 77L, NOW, key("lucky"));
        assertTrue(result.success());
        final Runtime two = runtime(bindings(), new Ports());
        two.observe(snapshot(MatchSnapshot.State.PLAYING));
        final ModeMechanics.Result repeated = two.openLuckyBlock(
                PLAYER, 77L, NOW, key("lucky-repeat"));
        assertEquals(result.intents().get(0).action(), repeated.intents().get(0).action());
        assertFalse(one.openLuckyBlock(PLAYER, 77L, NOW, key("lucky")).success());
    }

    @Test void swappageRotatesEveryConfiguredTeamWithoutFixedIndexes() {
        for (int teamCount : Arrays.asList(2, 4, 8)) {
            final Ports ports = new Ports();
            final Runtime runtime = runtime(bindings(), ports);
            runtime.observe(snapshot(teamCount));
            final ModeMechanics.Result result = runtime.swap(NOW, key("swap-" + teamCount));
            assertTrue(result.success());
            assertEquals(teamCount, result.intents().size());
            assertEquals(teamCount, result.intents().stream()
                    .map(intent -> intent.owner()).distinct().count());
        }
    }

    @Test void everyUltimateCanBeSelectedAndActivatedDeterministically() {
        for (UltimateAbility ability : UltimateAbility.values()) {
            final Runtime runtime = runtime(bindings(), new Ports());
            runtime.observe(snapshot(MatchSnapshot.State.PLAYING));
            assertTrue(runtime.selectUltimate(PLAYER, ability, NOW,
                    key("select-" + ability)).success());
            assertTrue(runtime.activateUltimate(PLAYER, NOW,
                    key("activate-" + ability)).success());
            assertFalse(runtime.activateUltimate(PLAYER, NOW,
                    key("cooldown-" + ability)).success());
        }
    }

    @Test void boundedWorldMechanicsValidateTargetsAndCleanupAtMatchEnd() {
        final Ports ports = new Ports();
        final Runtime runtime = runtime(bindings(), ports);
        runtime.observe(snapshot(MatchSnapshot.State.PLAYING));
        assertTrue(runtime.voidless(PLAYER, true, 3, NOW, key("voidless")).success());
        assertTrue(runtime.rush(PLAYER, ModeMechanics.Runtime.RushAction.BRIDGE,
                4, NOW, key("rush")).success());
        assertTrue(runtime.sponge(PLAYER, 2, NOW, key("sponge")).success());
        assertTrue(runtime.popupTower(PLAYER, 8, NOW, key("tower")).success());
        assertFalse(runtime.popupTower(PLAYER, 200, NOW, key("tower-large")).success());
        runtime.observe(snapshot(MatchSnapshot.State.RESETTING));
        assertEquals(1, ports.cleanups);
        assertFalse(runtime.rush(PLAYER, ModeMechanics.Runtime.RushAction.GENERATOR,
                1, NOW, key("after-cleanup")).success());
    }

    @Test void bedStealIsTeamIsolatedAndRequiresEnemyBedsAndTokens() {
        final Runtime runtime = runtime(bindings(), new Ports());
        runtime.observe(snapshot(MatchSnapshot.State.PLAYING));
        assertFalse(runtime.bedDestroyed(PLAYER, RED, NOW, key("own-bed")).success());
        assertTrue(runtime.bedDestroyed(PLAYER, BLUE, NOW, key("enemy-bed")).success());
        assertEquals(1, runtime.bedState(RED).tokens());
        assertTrue(runtime.upgradeBed(PLAYER, NOW, key("upgrade")).success());
        assertEquals(1, runtime.bedState(RED).level());
        assertEquals(0, runtime.bedState(BLUE).level());
        assertFalse(runtime.upgradeBed(PLAYER, NOW, key("no-token")).success());
    }

    @Test void colourChangerPreservesCanonicalDataAndHonoursDenylist() {
        final Runtime runtime = runtime(bindings(), new Ports());
        final DefinitionId white = DefinitionId.of("colour", "white");
        final DefinitionId red = DefinitionId.of("colour", "red");
        final ModeMechanics.ColouredItem wool = new ModeMechanics.ColouredItem(
                DefinitionId.of("item", "wool"), white, 12, false,
                Collections.singletonMap("name", "original"));
        final ModeMechanics.ColouredItem converted = runtime.convertColour(wool, red);
        assertEquals(red, converted.colour());
        assertEquals(wool.amount(), converted.amount());
        assertEquals(wool.metadata(), converted.metadata());
        final ModeMechanics.ColouredItem denied = new ModeMechanics.ColouredItem(
                DefinitionId.of("item", "protected"), white, 1, false,
                Collections.<String, String>emptyMap());
        assertEquals(white, runtime.convertColour(denied, red).colour());
    }

    @Test void itemRotationIsStableLocalAndTimeBounded() {
        final Runtime runtime = runtime(bindings(), new Ports());
        final RotationContracts.Definition definition = new RotationContracts.Definition(
                ShopIds.RotationId.of("test", "weekly"), ZoneId.of("UTC"),
                Duration.ofDays(7), NOW, Optional.empty(), 2, 1, Duration.ZERO,
                Optional.empty(), Arrays.asList(
                new RotationContracts.PoolEntry(ShopIds.ItemId.of("item", "one"), 10),
                new RotationContracts.PoolEntry(ShopIds.ItemId.of("item", "two"), 5),
                new RotationContracts.PoolEntry(ShopIds.ItemId.of("item", "three"), 1)));
        final RotationContracts.Snapshot first = runtime.rotate(definition, NOW);
        assertEquals(first.activeItems(), runtime.rotate(definition, NOW.plusSeconds(1)).activeItems());
        assertNotEquals(first.revision(), runtime.rotate(definition, NOW.plus(Duration.ofDays(7))).revision());
        assertThrows(IllegalArgumentException.class,
                () -> runtime.rotate(definition, NOW.minusSeconds(1)));
    }

    @Test void scriptHookIsAllowlistedByFeatureAndLifecycleIsM08Owned() {
        final Ports ports = new Ports();
        final Runtime runtime = runtime(bindings(), ports);
        assertTrue(runtime.invokeScript(Feature.LUCKY_BLOCK,
                io.zartra.bedwars.scripting.api.ScriptId.of("test", "outcome"),
                Collections.singletonMap("seed", "1")));
        assertEquals(1, ports.scripts);
        runtime.observe(snapshot(MatchSnapshot.State.WAITING));
        assertEquals(1, ports.cleanups);
    }

    @Test void configurationRejectsMalformedAndIncompleteContent() {
        assertThrows(IllegalArgumentException.class,
                () -> new Limits(true, -1, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Limits(true, 1, 0, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Limits(true, 1, 1, Duration.ofHours(2)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                0, 0, 1, 1, 1, 1, 0, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                1, 4097, 1, 1, 1, 1, 0, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                1, 0, 33, 1, 1, 1, 0, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                1, 0, 1, 101, 1, 1, 0, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                1, 0, 1, 1, 257, 1, 0, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                1, 0, 1, 1, 1, 101, 0, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new Weapon(
                DefinitionId.of("weapon", "bad"), WeaponArchetype.RAPID,
                1, 0, 1, 1, 1, 1, 101, 100,
                Duration.ofMillis(100), Duration.ofMillis(100)));
        assertThrows(IllegalArgumentException.class, () -> new LuckyOutcome(
                DefinitionId.of("lucky", "bad"), LuckyOutcome.Kind.ENTITY,
                0, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new LuckyOutcome(
                DefinitionId.of("lucky", "bad"), LuckyOutcome.Kind.ENTITY,
                1, 65, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Ability(
                UltimateAbility.BUILDER, Duration.ZERO, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Ability(
                UltimateAbility.BUILDER, Duration.ZERO, 1, 1001, 1));
        assertThrows(IllegalArgumentException.class, () -> new Ability(
                UltimateAbility.BUILDER, Duration.ZERO, 1, 1, 65));
        assertThrows(IllegalArgumentException.class, () -> new ModeMechanics.ColouredItem(
                DefinitionId.of("item", "bad"), RED, 0, false,
                Collections.<String, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> new Configuration(
                Collections.<Feature, Limits>emptyMap(), Collections.<Weapon>emptyList(),
                Collections.<LuckyOutcome>emptyList(), Collections.<Ability>emptyList(),
                Collections.<DefinitionId>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ModeMechanics.EffectIntent(
                ModeMechanics.EffectType.STATUS, Feature.RUSH, DefinitionId.of("action", "bad"),
                RED, -1, Collections.<String, String>emptyMap()));
        final ModeMechanics.ColouredItem protectedItem = new ModeMechanics.ColouredItem(
                DefinitionId.of("item", "signed"), RED, 1, true,
                Collections.<String, String>emptyMap());
        assertEquals(protectedItem, runtime(bindings(), new Ports())
                .convertColour(protectedItem, BLUE));
    }

    @Test void runtimeRejectsInvalidStateCooldownCapacityAndPlatformFailure() {
        final Ports ports = new Ports();
        final Runtime runtime = runtime(bindings(), ports);
        assertFalse(runtime.fire(PLAYER, DefinitionId.of("weapon", "rapid"), 1,
                false, NOW, key("not-started")).success());
        runtime.observe(snapshot(MatchSnapshot.State.PLAYING));
        ports.accept = false;
        assertFalse(runtime.sponge(PLAYER, 1, NOW, key("rejected")).success());
        ports.accept = true;
        assertFalse(runtime.sponge(PLAYER, 0, NOW, key("zero")).success());
        assertThrows(IllegalArgumentException.class,
                () -> runtime.observe(new MatchSnapshot(MatchId.of(UUID.fromString(
                        "00000000-0000-0000-0000-000000000999")), ARENA,
                        0, MatchSnapshot.State.PLAYING, 0,
                        Arrays.asList(TeamSnapshot.empty(RED, 1), TeamSnapshot.empty(BLUE, 1)),
                        Collections.<PlayerSession>emptyList(), null, null, false, NOW)));
        assertThrows(NullPointerException.class,
                () -> runtime.convertColour(new ModeMechanics.ColouredItem(
                        DefinitionId.of("item", "wool"), RED, 1, false,
                        Collections.<String, String>emptyMap()), null));
    }

    @Test void runtimeCoversResourceExhaustionAndOutcomeFamilies() {
        final Runtime armed = runtime(bindings(), new Ports());
        armed.observe(snapshot(MatchSnapshot.State.PLAYING));
        final DefinitionId weapon = DefinitionId.of("weapon", "rapid");
        assertFalse(armed.reload(PLAYER, weapon, NOW, key("reload-full")).success());
        assertTrue(armed.fire(PLAYER, weapon, 1, false, NOW, key("round-one")).success());
        assertTrue(armed.fire(PLAYER, weapon, 1, false, NOW.plusSeconds(1),
                key("round-two")).success());
        assertFalse(armed.fire(PLAYER, weapon, 1, false, NOW.plusSeconds(2),
                key("empty")).success());
        final Runtime lucky = runtime(bindings(), new Ports());
        lucky.observe(snapshot(MatchSnapshot.State.PLAYING));
        boolean entity = false;
        boolean resource = false;
        for (int seed = 0; seed < 16; seed++) {
            final ModeMechanics.Result result = lucky.openLuckyBlock(
                    PLAYER, seed, NOW, key("family-" + seed));
            if (result.success()) {
                entity |= result.intents().get(0).type() == ModeMechanics.EffectType.ENTITY;
                resource |= result.intents().get(0).type() == ModeMechanics.EffectType.RESOURCE;
            }
        }
        assertTrue(entity);
        assertTrue(resource);
        assertFalse(lucky.activateUltimate(PLAYER, NOW, key("unselected")).success());
    }

    private static Runtime runtime(final List<Binding> bindings, final Ports ports) {
        return new Runtime(MATCH, bindings, configuration(), ports, ports, ports);
    }

    private static Configuration configuration() {
        final Map<Feature, Limits> limits = new EnumMap<Feature, Limits>(Feature.class);
        for (Feature feature : Feature.values()) {
            limits.put(feature, new Limits(true, 128, 64, Duration.ZERO));
        }
        final List<Weapon> weapons = new ArrayList<Weapon>();
        for (WeaponArchetype archetype : WeaponArchetype.values()) {
            weapons.add(new Weapon(DefinitionId.of("weapon", archetype.name().toLowerCase()),
                    archetype, 2, 8, 1, 10, 100, 90, 20, 150,
                    Duration.ofMillis(100), Duration.ofMillis(500)));
        }
        final List<LuckyOutcome> outcomes = Arrays.asList(
                new LuckyOutcome(DefinitionId.of("lucky", "resource"),
                        LuckyOutcome.Kind.BENEFICIAL, 2, 1, Duration.ZERO),
                new LuckyOutcome(DefinitionId.of("lucky", "entity"),
                        LuckyOutcome.Kind.ENTITY, 1, 2, Duration.ZERO));
        final List<Ability> abilities = new ArrayList<Ability>();
        for (UltimateAbility type : UltimateAbility.values()) {
            abilities.add(new Ability(type, Duration.ofSeconds(1), 2, 2, 3));
        }
        return new Configuration(limits, weapons, outcomes, abilities,
                Collections.singleton(DefinitionId.of("item", "protected")));
    }

    private static List<Binding> bindings() {
        final List<Binding> result = new ArrayList<Binding>();
        for (Feature feature : Feature.values()) {
            result.add(binding(feature, requirement(feature)));
        }
        return result;
    }

    private static Binding binding(final Feature feature, final String requirement) {
        return new Binding(definition(Collections.singletonList(requirement)), feature,
                Collections.singleton(requirement));
    }

    private static ModeFramework.Definition definition(final List<String> requirements) {
        final List<ModeFramework.DeferredBinding> deferred = new ArrayList<ModeFramework.DeferredBinding>();
        for (String requirement : requirements) {
            deferred.add(new ModeFramework.DeferredBinding(requirement, "M11"));
        }
        return new ModeFramework.Definition(ModeFramework.ModeId.of("test", "phase-two"),
                MessageKey.of("mode.name"), MessageKey.of("mode.description"),
                new ModeFramework.Version(1, 0), true, 2, 8, 2, 64,
                Collections.<DefinitionId>emptyList(),
                Collections.<ModeFramework.ConfigField>emptyList(), deferred);
    }

    private static String requirement(final Feature feature) {
        switch (feature) {
            case ARMED: return "ZBW-ADDON-010";
            case LUCKY_BLOCK: return "ZBW-ADDON-061";
            case SPONGE: return "ZBW-ADDON-141";
            case POPUP_TOWER: return "ZBW-ADDON-184";
            case SWAPPAGE: return "ZBW-ADDON-236";
            case ULTIMATE: return "ZBW-ADDON-300";
            case VOIDLESS: return "ZBW-ADDON-315";
            case RUSH: return "ZBW-ADDON-341";
            case PER_ARENA_GENERATOR: return "ZBW-ADDON-363";
            case ITEM_ROTATION: return "ZBW-ADDON-379";
            case COLOR_CHANGER: return "ZBW-ADDON-389";
            case BED_STEAL: return "ZBW-ADDON-438";
            default: throw new AssertionError(feature);
        }
    }

    private static MatchSnapshot snapshot(final MatchSnapshot.State state) {
        final PlayerStateSnapshot captured = new PlayerStateSnapshot(PLAYER,
                PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(DefinitionId.of("world", "arena"),
                        0, 64, 0, 0, 0), PlayerStateSnapshot.Mode.SURVIVAL, true);
        return new MatchSnapshot(MATCH, ARENA, 0, state, 0,
                Arrays.asList(TeamSnapshot.empty(RED, 4), TeamSnapshot.empty(BLUE, 4)),
                Collections.singletonList(PlayerSession.waiting(RED, captured).activate()),
                null, null, false, NOW);
    }

    private static MatchSnapshot snapshot(final int teamCount) {
        final List<TeamSnapshot> teams = new ArrayList<TeamSnapshot>();
        for (int index = 0; index < teamCount; index++) {
            teams.add(TeamSnapshot.empty(DefinitionId.of("team", "team-" + index), 4));
        }
        return new MatchSnapshot(MATCH, ARENA, 0, MatchSnapshot.State.PLAYING, 0,
                teams, Collections.<PlayerSession>emptyList(), null, null, false, NOW);
    }

    private static IdempotencyKey key(final String value) {
        return IdempotencyKey.of("test", value.toLowerCase().replace('_', '-'));
    }

    private static final class Ports implements ModeMechanics.EffectPort,
            ModeMechanics.ScriptHook, ModeMechanics.AuditSink {
        private int commits;
        private int cleanups;
        private int scripts;
        private boolean accept = true;
        @Override public boolean apply(final MatchId match, final IdempotencyKey key,
                                       final List<ModeMechanics.EffectIntent> intents) {
            commits++;
            return accept;
        }
        @Override public void cleanup(final MatchId match) { cleanups++; }
        @Override public boolean invoke(final io.zartra.bedwars.scripting.api.ScriptId script,
                                        final Map<String, String> input) {
            scripts++;
            return true;
        }
        @Override public void record(final ModeMechanics.AuditRecord record) { }
    }
}
