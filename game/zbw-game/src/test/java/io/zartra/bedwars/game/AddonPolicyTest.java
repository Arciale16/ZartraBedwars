package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.addon.AntiDropPolicy;
import io.zartra.bedwars.game.addon.ArenaStartAnnouncementPolicy;
import io.zartra.bedwars.game.addon.DepositPolicy;
import io.zartra.bedwars.game.addon.HotbarPolicy;
import io.zartra.bedwars.game.addon.LeaveDelayPolicy;
import io.zartra.bedwars.game.addon.LobbyProjectionPolicy;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AddonPolicyTest {
    @Test void hotbarResolvesPrecedencePermissionsActionsAndLastKnownGood() {
        final HotbarPolicy.Slot global = slot(0, "global", null);
        final HotbarPolicy.Slot arena = slot(0, "arena", "zartrabedwars.hotbar.play");
        final List<HotbarPolicy.Layer> layers = Arrays.asList(
                new HotbarPolicy.Layer(DefinitionId.of("zartra", "hotbar/global"), HotbarPolicy.Scope.GLOBAL, null, Collections.singletonList(global)),
                new HotbarPolicy.Layer(DefinitionId.of("zartra", "hotbar/arena"), HotbarPolicy.Scope.ARENA, DefinitionId.of("zartra", "arena/one"), Collections.singletonList(arena)));
        final HotbarPolicy.Context context = new HotbarPolicy.Context(null, null, DefinitionId.of("zartra", "arena/one"));
        assertEquals(global.action(), HotbarPolicy.resolve(HotbarPolicy.State.WAITING, context, layers, Collections.<String>emptySet()).slots().get(0).action());
        final HotbarPolicy.Loadout resolved = HotbarPolicy.resolve(HotbarPolicy.State.WAITING, context, layers,
                Collections.singleton("zartrabedwars.hotbar.play"));
        assertEquals(arena.action(), resolved.interact(0, arena.action()).get().action());
        assertFalse(resolved.interact(0, global.action()).isPresent());
        final HotbarPolicy.Registry registry = new HotbarPolicy.Registry();
        assertEquals(1L, registry.publish(layers));
        assertThrows(IllegalArgumentException.class, () -> registry.publish(Arrays.asList(layers.get(0), layers.get(0))));
        assertEquals(1L, registry.revision());
        assertEquals(2, registry.definitions().size());
    }

    @Test void depositIsAtomicSupportsPartialAndRejectsProtectedCooldownAndState() {
        final Map<Integer, PlayerStateSnapshot.Item> sourceItems = new HashMap<Integer, PlayerStateSnapshot.Item>();
        sourceItems.put(0, GameFixtures.item(20));
        final PlayerStateSnapshot.Inventory source = new PlayerStateSnapshot.Inventory(9, sourceItems);
        final PlayerStateSnapshot.Inventory chest = PlayerStateSnapshot.Inventory.empty(1);
        final DepositPolicy.Rules rules = new DepositPolicy.Rules(Collections.singleton(GameFixtures.IRON),
                EnumSet.of(HotbarPolicy.State.PLAYING), Duration.ofSeconds(2), 100);
        final DepositPolicy.Request request = new DepositPolicy.Request(GameFixtures.IRON, 10,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW.minusSeconds(3), 0);
        final DepositPolicy.Outcome complete = DepositPolicy.plan(request, source, chest, rules, GameFixtures.NOW);
        assertEquals(DepositPolicy.Status.COMPLETE, complete.status());
        assertEquals(10, complete.transferred());
        assertEquals(10, complete.source().totalItems());
        assertEquals(10, complete.enderChest().totalItems());
        final Map<Integer, PlayerStateSnapshot.Item> full = new HashMap<Integer, PlayerStateSnapshot.Item>();
        full.put(0, GameFixtures.item(60));
        final DepositPolicy.Outcome partial = DepositPolicy.plan(request, source,
                new PlayerStateSnapshot.Inventory(1, full), rules, GameFixtures.NOW);
        assertEquals(DepositPolicy.Status.PARTIAL, partial.status());
        assertEquals(4, partial.transferred());
        assertEquals(DepositPolicy.Status.COOLDOWN, DepositPolicy.plan(
                new DepositPolicy.Request(GameFixtures.IRON, 1, HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0),
                source, chest, rules, GameFixtures.NOW).status());
        assertEquals(DepositPolicy.Status.STATE, DepositPolicy.plan(
                new DepositPolicy.Request(GameFixtures.IRON, 1, HotbarPolicy.State.WAITING, GameFixtures.NOW.minusSeconds(3), 0),
                source, chest, rules, GameFixtures.NOW).status());
        final Map<String, String> protectedMeta = Collections.singletonMap("zartra.protected", "true");
        final Map<Integer, PlayerStateSnapshot.Item> protectedItems = Collections.singletonMap(0,
                new PlayerStateSnapshot.Item(GameFixtures.IRON, 1, protectedMeta));
        assertEquals(DepositPolicy.Status.PROTECTED, DepositPolicy.plan(request,
                new PlayerStateSnapshot.Inventory(1, protectedItems), chest, rules, GameFixtures.NOW).status());
        assertThrows(IllegalStateException.class, () -> DepositPolicy.plan(
                new DepositPolicy.Request(GameFixtures.IRON, 1, HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0),
                source, chest, rules, GameFixtures.NOW).source());
    }

    @Test void announcementUsesThresholdCooldownRegressionAndEligibility() {
        final ArenaStartAnnouncementPolicy policy = new ArenaStartAnnouncementPolicy();
        final ArenaStartAnnouncementPolicy.Rules rules = new ArenaStartAnnouncementPolicy.Rules(2,
                Duration.ofSeconds(30), EnumSet.of(ArenaStartAnnouncementPolicy.Audience.LOCAL),
                EnumSet.of(ArenaStartAnnouncementPolicy.Channel.CHAT, ArenaStartAnnouncementPolicy.Channel.SOUND), null);
        assertFalse(policy.evaluate(observation(1, true, GameFixtures.NOW), rules).isPresent());
        final ArenaStartAnnouncementPolicy.Announcement announcement =
                policy.evaluate(observation(2, true, GameFixtures.NOW.plusSeconds(1)), rules).get();
        assertEquals(2, announcement.players());
        assertFalse(policy.evaluate(observation(2, true, GameFixtures.NOW.plusSeconds(2)), rules).isPresent());
        assertFalse(policy.evaluate(observation(1, true, GameFixtures.NOW.plusSeconds(3)), rules).isPresent());
        assertFalse(policy.evaluate(observation(2, true, GameFixtures.NOW.plusSeconds(4)), rules).isPresent());
        assertFalse(policy.evaluate(observation(1, true, GameFixtures.NOW.plusSeconds(31)), rules).isPresent());
        assertTrue(policy.evaluate(observation(2, true, GameFixtures.NOW.plusSeconds(32)), rules).isPresent());
        assertTrue(policy.reset(GameFixtures.arena(1)));
        assertFalse(policy.reset(GameFixtures.arena(1)));
    }

    @Test void antiDropFencesRacesAndSelectsFallbackRecipient() {
        final AntiDropPolicy policy = new AntiDropPolicy(16);
        final AntiDropPolicy.Rules rules = new AntiDropPolicy.Rules(Collections.singleton(GameFixtures.IRON),
                Arrays.asList(AntiDropPolicy.Recipient.OWNER, AntiDropPolicy.Recipient.KILLER), Duration.ofSeconds(30));
        final PlayerId owner = GameFixtures.player(1);
        final PlayerId killer = GameFixtures.player(2);
        final AntiDropPolicy.Observation observation = new AntiDropPolicy.Observation(
                IdempotencyKey.of("zartra", "capture/one"), GameFixtures.item(3), owner, killer,
                null, null, Collections.singleton(owner), Duration.ofSeconds(1), true, false, false);
        final AntiDropPolicy.Outcome pending = policy.capture(observation, rules, false);
        assertEquals(AntiDropPolicy.Status.PENDING_GRANT, pending.status());
        assertEquals(killer, pending.recipient().get());
        assertEquals(AntiDropPolicy.Status.DUPLICATE, policy.capture(observation, rules, true).status());
        assertEquals(1, policy.fenceSize());
        final AntiDropPolicy.Observation filtered = new AntiDropPolicy.Observation(
                IdempotencyKey.of("zartra", "capture/two"), GameFixtures.item(1), owner, null,
                null, null, Collections.<PlayerId>emptySet(), Duration.ofSeconds(31), true, false, false);
        assertEquals(AntiDropPolicy.Status.FILTERED, policy.capture(filtered, rules, true).status());
    }

    @Test void leaveDelayCancelsCompletesBypassesAndClearsExactlyOnce() {
        final LeaveDelayPolicy policy = new LeaveDelayPolicy();
        final Map<LeaveDelayPolicy.State, Duration> delays = new EnumMap<LeaveDelayPolicy.State, Duration>(LeaveDelayPolicy.State.class);
        for (LeaveDelayPolicy.State state : LeaveDelayPolicy.State.values()) { delays.put(state, Duration.ofSeconds(3)); }
        final LeaveDelayPolicy.Rules rules = new LeaveDelayPolicy.Rules(delays, EnumSet.of(LeaveDelayPolicy.Signal.MOVEMENT));
        final PlayerId player = GameFixtures.player(1);
        final LeaveDelayPolicy.Session first = policy.begin(player, LeaveDelayPolicy.State.PLAYING, GameFixtures.NOW, rules);
        assertEquals(3L, first.remainingSeconds(GameFixtures.NOW));
        assertFalse(policy.signal(player, LeaveDelayPolicy.Signal.DAMAGE, rules).isPresent());
        assertEquals(LeaveDelayPolicy.Status.CANCELLED, policy.signal(player, LeaveDelayPolicy.Signal.MOVEMENT, rules).get().status());
        assertTrue(policy.clear(player));
        policy.begin(player, LeaveDelayPolicy.State.PLAYING, GameFixtures.NOW, rules);
        assertFalse(policy.tick(player, GameFixtures.NOW.plusSeconds(2)).isPresent());
        assertEquals(LeaveDelayPolicy.Status.COMPLETED, policy.tick(player, GameFixtures.NOW.plusSeconds(3)).get().status());
        assertFalse(policy.tick(player, GameFixtures.NOW.plusSeconds(4)).isPresent());
        assertThrows(SecurityException.class, () -> policy.bypass(player, LeaveDelayPolicy.State.PLAYING, GameFixtures.NOW, false));
        assertEquals(LeaveDelayPolicy.Status.BYPASSED, policy.bypass(player, LeaveDelayPolicy.State.PLAYING, GameFixtures.NOW, true).status());
    }

    @Test void lobbyTabBossbarAndAdventureViewsAreDeterministicAndPrivate() {
        assertEquals(PlayerStateSnapshot.Mode.ADVENTURE, LobbyProjectionPolicy.gameMode(LobbyProjectionPolicy.PlayerView.WAITING, null));
        assertEquals(PlayerStateSnapshot.Mode.SURVIVAL, LobbyProjectionPolicy.gameMode(LobbyProjectionPolicy.PlayerView.PLAYING, null));
        assertEquals(PlayerStateSnapshot.Mode.SPECTATOR, LobbyProjectionPolicy.gameMode(LobbyProjectionPolicy.PlayerView.SPECTATING, null));
        assertEquals(PlayerStateSnapshot.Mode.CREATIVE, LobbyProjectionPolicy.gameMode(LobbyProjectionPolicy.PlayerView.RESTORE, PlayerStateSnapshot.Mode.CREATIVE));
        final LobbyProjectionPolicy.LobbyRules rules = new LobbyProjectionPolicy.LobbyRules(
                EnumSet.of(LobbyProjectionPolicy.InteractionType.BREAK), true, true);
        assertEquals(LobbyProjectionPolicy.InteractionDecision.DENY, LobbyProjectionPolicy.interaction(
                new LobbyProjectionPolicy.Interaction(LobbyProjectionPolicy.InteractionType.BREAK, true, false, true), rules));
        assertEquals(LobbyProjectionPolicy.InteractionDecision.RETURN_TO_SPAWN, LobbyProjectionPolicy.interaction(
                new LobbyProjectionPolicy.Interaction(LobbyProjectionPolicy.InteractionType.VOID_DAMAGE, true, false, true), rules));
        final PlayerId one = GameFixtures.player(1);
        final PlayerId two = GameFixtures.player(2);
        final List<LobbyProjectionPolicy.TabEntry> entries = Arrays.asList(
                new LobbyProjectionPolicy.TabEntry(two, LobbyProjectionPolicy.TabSection.TEAM, 1, 0, "Beta", "", true),
                new LobbyProjectionPolicy.TabEntry(one, LobbyProjectionPolicy.TabSection.STAFF, 0, 5, "Alpha", "admin", false));
        final LobbyProjectionPolicy.TabSnapshot hidden = LobbyProjectionPolicy.tab(entries,
                Collections.<PlayerId>emptySet(), 10, "head", "foot", GameFixtures.NOW);
        assertEquals(1, hidden.entries().size());
        final LobbyProjectionPolicy.TabSnapshot shown = LobbyProjectionPolicy.tab(entries,
                Collections.singleton(two), 10, "head2", "foot", GameFixtures.NOW.plusSeconds(1));
        final LobbyProjectionPolicy.TabDiff diff = LobbyProjectionPolicy.diff(hidden, shown);
        assertEquals(1, diff.upserts().size());
        assertTrue(diff.headerFooterChanged());
        assertTrue(LobbyProjectionPolicy.diff(shown, hidden).removals().contains(two));
        final LobbyProjectionPolicy.BossBarSnapshot bar = LobbyProjectionPolicy.bossBar(
                LobbyProjectionPolicy.BossBarState.WAITING, "bossbar.waiting", 0.5D,
                LobbyProjectionPolicy.BossBarColor.BLUE, LobbyProjectionPolicy.BossBarStyle.SOLID,
                Duration.ofSeconds(1), Collections.singleton(one));
        assertEquals(0.5D, bar.progress());
        assertEquals(1, bar.viewers().size());
        assertThrows(IllegalArgumentException.class, () -> LobbyProjectionPolicy.bossBar(
                LobbyProjectionPolicy.BossBarState.PLAYING, "bad key", 2.0D,
                LobbyProjectionPolicy.BossBarColor.RED, LobbyProjectionPolicy.BossBarStyle.SOLID,
                Duration.ZERO, Collections.singleton(one)));
    }

    private static HotbarPolicy.Slot slot(final int slot, final String name, final String permission) {
        return new HotbarPolicy.Slot(slot, GameFixtures.item(1), DefinitionId.of("zartra", "action/" + name),
                permission, EnumSet.of(HotbarPolicy.State.WAITING));
    }
    private static ArenaStartAnnouncementPolicy.Observation observation(
            final int players, final boolean eligible, final java.time.Instant instant) {
        return new ArenaStartAnnouncementPolicy.Observation(GameFixtures.arena(1),
                DefinitionId.of("zartra", "mode/standard"), DefinitionId.of("zartra", "group/default"),
                players, 4, 10, 4 - players, eligible, instant);
    }
}
