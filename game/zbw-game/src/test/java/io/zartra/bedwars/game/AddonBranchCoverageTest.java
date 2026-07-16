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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AddonBranchCoverageTest {
    @Test void hotbarValidationAndEveryOverrideScopeFailClosed() {
        final HotbarPolicy.Slot slot = slot(0, "one", null, EnumSet.of(HotbarPolicy.State.WAITING));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Slot(-1,
                GameFixtures.item(1), id("action/a"), null, EnumSet.of(HotbarPolicy.State.WAITING)));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Slot(9,
                GameFixtures.item(1), id("action/a"), null, EnumSet.of(HotbarPolicy.State.WAITING)));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Slot(0,
                GameFixtures.item(1), id("action/a"), "BAD NODE", EnumSet.of(HotbarPolicy.State.WAITING)));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Slot(0,
                GameFixtures.item(1), id("action/a"), null, Collections.<HotbarPolicy.State>emptySet()));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Layer(id("layer/bad"),
                HotbarPolicy.Scope.GLOBAL, id("selector/bad"), Collections.singletonList(slot)));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Layer(id("layer/bad"),
                HotbarPolicy.Scope.ARENA, null, Collections.singletonList(slot)));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Layer(id("layer/empty"),
                HotbarPolicy.Scope.GLOBAL, null, Collections.<HotbarPolicy.Slot>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new HotbarPolicy.Layer(id("layer/duplicate"),
                HotbarPolicy.Scope.GLOBAL, null, Arrays.asList(slot, slot)));
        final DefinitionId group = id("group/one");
        final DefinitionId mode = id("mode/one");
        final DefinitionId arena = id("arena/one");
        final List<HotbarPolicy.Layer> layers = Arrays.asList(
                new HotbarPolicy.Layer(id("layer/global-a"), HotbarPolicy.Scope.GLOBAL, null, Collections.singletonList(slot)),
                new HotbarPolicy.Layer(id("layer/global-b"), HotbarPolicy.Scope.GLOBAL, null,
                        Collections.singletonList(slot(1, "two", null, EnumSet.of(HotbarPolicy.State.WAITING)))),
                new HotbarPolicy.Layer(id("layer/group"), HotbarPolicy.Scope.GROUP, group,
                        Collections.singletonList(slot(2, "group", null, EnumSet.of(HotbarPolicy.State.WAITING)))),
                new HotbarPolicy.Layer(id("layer/mode"), HotbarPolicy.Scope.MODE, mode,
                        Collections.singletonList(slot(3, "mode", null, EnumSet.of(HotbarPolicy.State.WAITING)))),
                new HotbarPolicy.Layer(id("layer/arena"), HotbarPolicy.Scope.ARENA, arena,
                        Collections.singletonList(slot(4, "arena", null, EnumSet.of(HotbarPolicy.State.PLAYING)))));
        final HotbarPolicy.Loadout loadout = HotbarPolicy.resolve(HotbarPolicy.State.WAITING,
                new HotbarPolicy.Context(group, mode, arena), layers, Collections.<String>emptySet());
        assertEquals(4, loadout.slots().size());
        assertEquals(HotbarPolicy.State.WAITING, loadout.state());
        assertFalse(HotbarPolicy.resolve(HotbarPolicy.State.WAITING,
                new HotbarPolicy.Context(id("group/x"), id("mode/x"), id("arena/x")),
                layers, Collections.<String>emptySet()).slots().containsKey(2));
        final HotbarPolicy.Registry registry = new HotbarPolicy.Registry();
        assertThrows(IllegalArgumentException.class, () -> registry.publish(Collections.<HotbarPolicy.Layer>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> registry.publish(Arrays.asList((HotbarPolicy.Layer) null)));
    }

    @Test void depositCoversLimitsResourcesNoCapacityAndValidationBranches() {
        final Set<DefinitionId> resources = Collections.singleton(GameFixtures.IRON);
        final Set<HotbarPolicy.State> states = EnumSet.of(HotbarPolicy.State.PLAYING);
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Request(GameFixtures.IRON, 0,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Request(GameFixtures.IRON, 4097,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Request(GameFixtures.IRON, 1,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW, -1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                Collections.<DefinitionId>emptySet(), states, Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                resources, Collections.<HotbarPolicy.State>emptySet(), Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                resources, states, Duration.ofMinutes(6), 1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                resources, states, Duration.ZERO, 0));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                resources, states, Duration.ZERO, 100001));
        final DepositPolicy.Rules rules = new DepositPolicy.Rules(resources, states, Duration.ZERO, 1);
        final PlayerStateSnapshot.Inventory source = inventory(1, GameFixtures.item(1));
        final DepositPolicy.Request limited = new DepositPolicy.Request(GameFixtures.IRON, 1,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW, 1);
        assertEquals(DepositPolicy.Status.LIMIT, DepositPolicy.plan(limited, source,
                PlayerStateSnapshot.Inventory.empty(1), rules, GameFixtures.NOW).status());
        final DefinitionId other = id("item/other");
        final DepositPolicy.Request wrong = new DepositPolicy.Request(other, 1,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0);
        assertEquals(DepositPolicy.Status.NO_CAPACITY, DepositPolicy.plan(wrong, source,
                PlayerStateSnapshot.Inventory.empty(1), rules, GameFixtures.NOW).status());
        final DepositPolicy.Request accepted = new DepositPolicy.Request(GameFixtures.IRON, 1,
                HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0);
        assertEquals(DepositPolicy.Status.NO_CAPACITY, DepositPolicy.plan(accepted, source,
                inventory(1, GameFixtures.item(64)), rules, GameFixtures.NOW).status());
        final Map<Integer, PlayerStateSnapshot.Item> multiple = new HashMap<Integer, PlayerStateSnapshot.Item>();
        multiple.put(0, GameFixtures.item(64));
        multiple.put(1, GameFixtures.item(2));
        final DepositPolicy.Outcome moved = DepositPolicy.plan(
                new DepositPolicy.Request(GameFixtures.IRON, 66, HotbarPolicy.State.PLAYING, GameFixtures.NOW, 0),
                new PlayerStateSnapshot.Inventory(2, multiple), PlayerStateSnapshot.Inventory.empty(2),
                new DepositPolicy.Rules(resources, states, Duration.ZERO, 100), GameFixtures.NOW);
        assertEquals(66, moved.transferred());
        assertEquals(0, moved.unfulfilled());
    }

    @Test void announcementValidationAudienceAndJoinChecksAreExplicit() {
        final Set<ArenaStartAnnouncementPolicy.Audience> local =
                EnumSet.of(ArenaStartAnnouncementPolicy.Audience.LOCAL);
        final Set<ArenaStartAnnouncementPolicy.Channel> chat =
                EnumSet.of(ArenaStartAnnouncementPolicy.Channel.CHAT);
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(0, Duration.ZERO, local, chat, null));
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(257, Duration.ZERO, local, chat, null));
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(1, Duration.ofHours(2), local, chat, null));
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(1, Duration.ZERO,
                Collections.<ArenaStartAnnouncementPolicy.Audience>emptySet(), chat, null));
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(1, Duration.ZERO, local,
                Collections.<ArenaStartAnnouncementPolicy.Channel>emptySet(), null));
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(1, Duration.ZERO,
                EnumSet.of(ArenaStartAnnouncementPolicy.Audience.PERMISSION), chat, "BAD NODE"));
        assertThrows(IllegalArgumentException.class, () -> observation(-1, 4, 0, true));
        assertThrows(IllegalArgumentException.class, () -> observation(5, 4, 0, true));
        assertThrows(IllegalArgumentException.class, () -> observation(1, 4, 4, true));
        final ArenaStartAnnouncementPolicy.Rules rules = new ArenaStartAnnouncementPolicy.Rules(1,
                Duration.ZERO, EnumSet.of(ArenaStartAnnouncementPolicy.Audience.PERMISSION), chat,
                "zartrabedwars.announce.receive");
        final ArenaStartAnnouncementPolicy policy = new ArenaStartAnnouncementPolicy();
        assertFalse(policy.evaluate(observation(1, 4, 3, false), rules).isPresent());
        assertTrue(policy.reset(GameFixtures.arena(1)));
        assertFalse(policy.evaluate(observation(1, 4, 0, true), rules).isPresent());
        assertTrue(policy.reset(GameFixtures.arena(1)));
        final ArenaStartAnnouncementPolicy.Announcement value =
                policy.evaluate(observation(1, 4, 3, true), rules).get();
        assertTrue(value.permission().isPresent());
        assertEquals(4, value.capacity());
        assertEquals(10, value.countdown());
        assertEquals(id("mode/standard"), value.mode());
        assertEquals(id("group/default"), value.group());
        assertEquals(chat, value.channels());
    }

    @Test void antiDropFiltersEveryRaceAndRecipientPathWithBoundedEviction() {
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy(15));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy(100001));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.<DefinitionId>emptySet(), Collections.singletonList(AntiDropPolicy.Recipient.OWNER), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), Collections.<AntiDropPolicy.Recipient>emptyList(), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), Arrays.asList(AntiDropPolicy.Recipient.OWNER,
                AntiDropPolicy.Recipient.OWNER), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), Collections.singletonList(AntiDropPolicy.Recipient.OWNER), Duration.ofMinutes(11)));
        assertThrows(IllegalArgumentException.class, () -> observation("negative", GameFixtures.item(1),
                GameFixtures.player(1), null, null, null, Duration.ofSeconds(-1), true, false, false));
        final AntiDropPolicy policy = new AntiDropPolicy(16);
        final AntiDropPolicy.Rules rules = new AntiDropPolicy.Rules(Collections.singleton(GameFixtures.IRON),
                Arrays.asList(AntiDropPolicy.Recipient.OWNER, AntiDropPolicy.Recipient.TEAMMATE,
                        AntiDropPolicy.Recipient.NEAREST), Duration.ofSeconds(10));
        assertEquals(AntiDropPolicy.Status.FILTERED, policy.capture(observation("boundary", GameFixtures.item(1),
                GameFixtures.player(1), null, null, null, Duration.ZERO, false, false, false), rules, true).status());
        assertEquals(AntiDropPolicy.Status.FILTERED, policy.capture(observation("pickup", GameFixtures.item(1),
                GameFixtures.player(1), null, null, null, Duration.ZERO, true, true, false), rules, true).status());
        assertEquals(AntiDropPolicy.Status.FILTERED, policy.capture(observation("reset", GameFixtures.item(1),
                GameFixtures.player(1), null, null, null, Duration.ZERO, true, false, true), rules, true).status());
        final PlayerId teammate = GameFixtures.player(2);
        assertEquals(teammate, policy.capture(observation("team", GameFixtures.item(1), null,
                null, teammate, null, Duration.ZERO, true, false, false), rules, true).recipient().get());
        final PlayerId nearest = GameFixtures.player(3);
        assertEquals(nearest, policy.capture(observation("nearest", GameFixtures.item(1), null,
                null, null, nearest, Duration.ZERO, true, false, false), rules, true).recipient().get());
        assertEquals(AntiDropPolicy.Status.NO_RECIPIENT, policy.capture(observation("none", GameFixtures.item(1),
                null, null, null, null, Duration.ZERO, true, false, false), rules, true).status());
        for (int index = 0; index < 20; index++) {
            policy.capture(observation("evict/" + index, GameFixtures.item(1), GameFixtures.player(1),
                    null, null, null, Duration.ZERO, true, false, false), rules, true);
        }
        assertEquals(16, policy.fenceSize());
    }

    @Test void leaveDelayValidationAndNoSessionBranchesAreCovered() {
        final EnumMap<LeaveDelayPolicy.State, Duration> delays =
                new EnumMap<LeaveDelayPolicy.State, Duration>(LeaveDelayPolicy.State.class);
        delays.put(LeaveDelayPolicy.State.WAITING, Duration.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new LeaveDelayPolicy.Rules(delays,
                Collections.<LeaveDelayPolicy.Signal>emptySet()));
        for (LeaveDelayPolicy.State state : LeaveDelayPolicy.State.values()) { delays.put(state, Duration.ZERO); }
        delays.put(LeaveDelayPolicy.State.PLAYING, Duration.ofMinutes(6));
        assertThrows(IllegalArgumentException.class, () -> new LeaveDelayPolicy.Rules(delays,
                Collections.<LeaveDelayPolicy.Signal>emptySet()));
        delays.put(LeaveDelayPolicy.State.PLAYING, Duration.ZERO);
        final LeaveDelayPolicy.Rules rules = new LeaveDelayPolicy.Rules(delays,
                EnumSet.of(LeaveDelayPolicy.Signal.COMBAT));
        final LeaveDelayPolicy policy = new LeaveDelayPolicy();
        final PlayerId player = GameFixtures.player(1);
        assertFalse(policy.inspect(player).isPresent());
        assertFalse(policy.signal(player, LeaveDelayPolicy.Signal.COMBAT, rules).isPresent());
        assertFalse(policy.tick(player, GameFixtures.NOW).isPresent());
        assertFalse(policy.clear(player));
        final LeaveDelayPolicy.Session first = policy.begin(player, LeaveDelayPolicy.State.WAITING,
                GameFixtures.NOW, rules);
        assertEquals(first, policy.begin(player, LeaveDelayPolicy.State.WAITING,
                GameFixtures.NOW.plusSeconds(1), rules));
        assertEquals(0L, first.remainingSeconds(GameFixtures.NOW));
        assertTrue(policy.tick(player, GameFixtures.NOW).isPresent());
        assertTrue(policy.clear(player));
    }

    @Test void lobbyTabAndBossbarValidationSortingDiffAndInteractionBranchesAreCovered() {
        final LobbyProjectionPolicy.LobbyRules rules = new LobbyProjectionPolicy.LobbyRules(
                EnumSet.of(LobbyProjectionPolicy.InteractionType.BREAK), true, true);
        assertEquals(LobbyProjectionPolicy.InteractionDecision.ALLOW, LobbyProjectionPolicy.interaction(
                interaction(LobbyProjectionPolicy.InteractionType.BREAK, false, false, true), rules));
        assertEquals(LobbyProjectionPolicy.InteractionDecision.ALLOW_AUDITED, LobbyProjectionPolicy.interaction(
                interaction(LobbyProjectionPolicy.InteractionType.BREAK, true, true, true), rules));
        assertEquals(LobbyProjectionPolicy.InteractionDecision.DENY, LobbyProjectionPolicy.interaction(
                interaction(LobbyProjectionPolicy.InteractionType.DOUBLE_JUMP, true, false, false), rules));
        assertEquals(LobbyProjectionPolicy.InteractionDecision.APPLY_DOUBLE_JUMP, LobbyProjectionPolicy.interaction(
                interaction(LobbyProjectionPolicy.InteractionType.DOUBLE_JUMP, true, false, true), rules));
        assertEquals(LobbyProjectionPolicy.InteractionDecision.ALLOW, LobbyProjectionPolicy.interaction(
                interaction(LobbyProjectionPolicy.InteractionType.HUNGER, true, false, true), rules));
        assertThrows(IllegalArgumentException.class, () -> LobbyProjectionPolicy.tab(
                Collections.<LobbyProjectionPolicy.TabEntry>emptyList(), Collections.<PlayerId>emptySet(),
                0, "", "", GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> LobbyProjectionPolicy.tab(
                Arrays.asList((LobbyProjectionPolicy.TabEntry) null), Collections.<PlayerId>emptySet(),
                1, "", "", GameFixtures.NOW));
        assertThrows(IllegalArgumentException.class, () -> entry(GameFixtures.player(1),
                LobbyProjectionPolicy.TabSection.TEAM, -1, 0, "name", "", false));
        assertThrows(IllegalArgumentException.class, () -> entry(GameFixtures.player(1),
                LobbyProjectionPolicy.TabSection.TEAM, 0, -1, "name", "", false));
        assertThrows(IllegalArgumentException.class, () -> entry(GameFixtures.player(1),
                LobbyProjectionPolicy.TabSection.TEAM, 0, 0, "", "", false));
        final List<LobbyProjectionPolicy.TabEntry> entries = Arrays.asList(
                entry(GameFixtures.player(4), LobbyProjectionPolicy.TabSection.TEAM, 2, 0, "same", "", false),
                entry(GameFixtures.player(3), LobbyProjectionPolicy.TabSection.TEAM, 1, 0, "same", "", false),
                entry(GameFixtures.player(2), LobbyProjectionPolicy.TabSection.TEAM, 1, 1, "same", "", false),
                entry(GameFixtures.player(1), LobbyProjectionPolicy.TabSection.STAFF, 0, 0, "same", "", false));
        final LobbyProjectionPolicy.TabSnapshot snapshot = LobbyProjectionPolicy.tab(entries,
                Collections.<PlayerId>emptySet(), 3, "head", "foot", GameFixtures.NOW);
        assertEquals(3, snapshot.entries().size());
        assertEquals("head", snapshot.header());
        assertEquals("foot", snapshot.footer());
        assertEquals(GameFixtures.NOW, snapshot.generatedAt());
        final LobbyProjectionPolicy.TabDiff unchanged = LobbyProjectionPolicy.diff(snapshot, snapshot);
        assertTrue(unchanged.upserts().isEmpty());
        assertTrue(unchanged.removals().isEmpty());
        assertFalse(unchanged.headerFooterChanged());
        for (LobbyProjectionPolicy.BossBarColor color : LobbyProjectionPolicy.BossBarColor.values()) {
            for (LobbyProjectionPolicy.BossBarStyle style : LobbyProjectionPolicy.BossBarStyle.values()) {
                final LobbyProjectionPolicy.BossBarSnapshot bar = LobbyProjectionPolicy.bossBar(
                        LobbyProjectionPolicy.BossBarState.PLAYING, "bossbar.playing", 1.0D,
                        color, style, Duration.ofMillis(50), Collections.<PlayerId>emptySet());
                assertEquals(color, bar.color());
                assertEquals(style, bar.style());
                assertEquals(Duration.ofMillis(50), bar.cadence());
                assertEquals("bossbar.playing", bar.messageKey());
                assertEquals(LobbyProjectionPolicy.BossBarState.PLAYING, bar.state());
            }
        }
        assertThrows(IllegalArgumentException.class, () -> LobbyProjectionPolicy.bossBar(
                LobbyProjectionPolicy.BossBarState.WAITING, "bossbar.waiting", Double.NaN,
                LobbyProjectionPolicy.BossBarColor.BLUE, LobbyProjectionPolicy.BossBarStyle.SOLID,
                Duration.ofSeconds(1), Collections.<PlayerId>emptySet()));
        assertThrows(IllegalArgumentException.class, () -> new LobbyProjectionPolicy.LobbyRules(
                new HashSet<LobbyProjectionPolicy.InteractionType>(Arrays.asList((LobbyProjectionPolicy.InteractionType) null)),
                true, true));
    }

    private static DefinitionId id(final String path) { return DefinitionId.of("zartra", path); }
    private static HotbarPolicy.Slot slot(final int index, final String name, final String permission,
                                          final Set<HotbarPolicy.State> states) {
        return new HotbarPolicy.Slot(index, GameFixtures.item(1), id("action/" + name), permission, states);
    }
    private static PlayerStateSnapshot.Inventory inventory(final int size, final PlayerStateSnapshot.Item item) {
        return new PlayerStateSnapshot.Inventory(size, Collections.singletonMap(0, item));
    }
    private static ArenaStartAnnouncementPolicy.Observation observation(
            final int players, final int capacity, final int reservable, final boolean eligible) {
        return new ArenaStartAnnouncementPolicy.Observation(GameFixtures.arena(1), id("mode/standard"),
                id("group/default"), players, capacity, 10, reservable, eligible, GameFixtures.NOW);
    }
    private static AntiDropPolicy.Observation observation(final String key,
            final PlayerStateSnapshot.Item item, final PlayerId owner, final PlayerId killer,
            final PlayerId teammate, final PlayerId nearest, final Duration age,
            final boolean boundary, final boolean pickedUp, final boolean resetting) {
        return new AntiDropPolicy.Observation(IdempotencyKey.of("zartra", "capture/" + key), item,
                owner, killer, teammate, nearest, Collections.<PlayerId>emptySet(), age,
                boundary, pickedUp, resetting);
    }
    private static LobbyProjectionPolicy.Interaction interaction(
            final LobbyProjectionPolicy.InteractionType type, final boolean managed,
            final boolean bypass, final boolean cooled) {
        return new LobbyProjectionPolicy.Interaction(type, managed, bypass, cooled);
    }
    private static LobbyProjectionPolicy.TabEntry entry(final PlayerId player,
            final LobbyProjectionPolicy.TabSection section, final int team, final int rank,
            final String name, final String suffix, final boolean privateEntry) {
        return new LobbyProjectionPolicy.TabEntry(player, section, team, rank, name, suffix, privateEntry);
    }
}
