package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.addon.AntiDropPolicy;
import io.zartra.bedwars.game.addon.ArenaStartAnnouncementPolicy;
import io.zartra.bedwars.game.addon.DepositPolicy;
import io.zartra.bedwars.game.addon.HotbarPolicy;
import io.zartra.bedwars.game.addon.LeaveDelayPolicy;
import io.zartra.bedwars.game.addon.LobbyProjectionPolicy;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValueObjectBranchCoverageTest {
    @Test void itemMetadataBoundsAndEveryEqualityDimensionAreCovered() {
        final Map<String, String> tooMany = new HashMap<String, String>();
        for (int index = 0; index < 33; index++) { tooMany.put("key" + index, "value"); }
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Item(
                GameFixtures.IRON, 1, tooMany));
        final Map<String, String> nullKey = new HashMap<String, String>();
        nullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Item(
                GameFixtures.IRON, 1, nullKey));
        final Map<String, String> nullValue = new HashMap<String, String>();
        nullValue.put("key", null);
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Item(
                GameFixtures.IRON, 1, nullValue));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Item(
                GameFixtures.IRON, 1, Collections.singletonMap("key", repeat('x', 257))));
        final PlayerStateSnapshot.Item item = new PlayerStateSnapshot.Item(GameFixtures.IRON, 1,
                Collections.singletonMap("key", "value"));
        assertTrue(item.equals(item));
        assertFalse(item.equals("item"));
        assertFalse(item.equals(item.withAmount(2)));
        assertFalse(item.equals(new PlayerStateSnapshot.Item(DefinitionId.of("zartra", "item/gold"),
                1, item.metadata())));
        assertFalse(item.equals(new PlayerStateSnapshot.Item(GameFixtures.IRON, 1,
                Collections.singletonMap("key", "other"))));
    }

    @Test void inventoryBoundsNullEntriesAndEveryEqualityDimensionAreCovered() {
        assertThrows(IllegalArgumentException.class, () -> PlayerStateSnapshot.Inventory.empty(0));
        assertThrows(IllegalArgumentException.class, () -> PlayerStateSnapshot.Inventory.empty(129));
        final Map<Integer, PlayerStateSnapshot.Item> nullKey = new HashMap<Integer, PlayerStateSnapshot.Item>();
        nullKey.put(null, GameFixtures.item(1));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Inventory(1, nullKey));
        final Map<Integer, PlayerStateSnapshot.Item> nullValue = new HashMap<Integer, PlayerStateSnapshot.Item>();
        nullValue.put(0, null);
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Inventory(1, nullValue));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Inventory(1,
                Collections.singletonMap(-1, GameFixtures.item(1))));
        final PlayerStateSnapshot.Inventory inventory = new PlayerStateSnapshot.Inventory(2,
                Collections.singletonMap(0, GameFixtures.item(1)));
        assertTrue(inventory.equals(inventory));
        assertFalse(inventory.equals("inventory"));
        assertFalse(inventory.equals(PlayerStateSnapshot.Inventory.empty(1)));
        assertFalse(inventory.equals(PlayerStateSnapshot.Inventory.empty(2)));
    }

    @Test void everyLocationFiniteGuardIsCovered() {
        final DefinitionId world = DefinitionId.of("zartra", "world/one");
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Location(
                world, 0, Double.POSITIVE_INFINITY, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Location(
                world, 0, 0, Double.NEGATIVE_INFINITY, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Location(
                world, 0, 0, 0, Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerStateSnapshot.Location(
                world, 0, 0, 0, 0, Float.POSITIVE_INFINITY));
    }

    @Test void tabEntryValidationAndEveryEqualityDimensionAreCovered() {
        final PlayerId one = GameFixtures.player(1);
        assertThrows(IllegalArgumentException.class, () -> entry(one, 0, 0, null, "", false));
        assertThrows(IllegalArgumentException.class, () -> entry(one, 0, 0, repeat('x', 65), "", false));
        assertThrows(IllegalArgumentException.class, () -> entry(one, 0, 0, "name", null, false));
        assertThrows(IllegalArgumentException.class, () -> entry(one, 0, 0, "name", repeat('x', 129), false));
        final LobbyProjectionPolicy.TabEntry base = entry(one, 1, 1, "name", "suffix", false);
        assertTrue(base.equals(base));
        assertFalse(base.equals("entry"));
        assertFalse(base.equals(entry(one, 2, 1, "name", "suffix", false)));
        assertFalse(base.equals(entry(one, 1, 2, "name", "suffix", false)));
        assertFalse(base.equals(entry(one, 1, 1, "name", "suffix", true)));
        assertFalse(base.equals(entry(GameFixtures.player(2), 1, 1, "name", "suffix", false)));
        assertFalse(base.equals(new LobbyProjectionPolicy.TabEntry(one,
                LobbyProjectionPolicy.TabSection.LOBBY, 1, 1, "name", "suffix", false)));
        assertFalse(base.equals(entry(one, 1, 1, "other", "suffix", false)));
        assertFalse(base.equals(entry(one, 1, 1, "name", "other", false)));
        assertTrue(base.hashCode() != 0);
    }

    @Test void bossBarRejectsEveryMalformedFieldIndependently() {
        final Set<PlayerId> viewers = Collections.emptySet();
        assertBossFailure(null, 0.5D, Duration.ofSeconds(1), viewers);
        assertBossFailure("BAD KEY", 0.5D, Duration.ofSeconds(1), viewers);
        assertBossFailure("bossbar.ok", -0.1D, Duration.ofSeconds(1), viewers);
        assertBossFailure("bossbar.ok", 1.1D, Duration.ofSeconds(1), viewers);
        assertBossFailure("bossbar.ok", 0.5D, null, viewers);
        assertBossFailure("bossbar.ok", 0.5D, Duration.ofSeconds(-1), viewers);
        assertBossFailure("bossbar.ok", 0.5D, Duration.ofSeconds(11), viewers);
        assertBossFailure("bossbar.ok", 0.5D, Duration.ofSeconds(1), null);
        final Set<PlayerId> nullViewer = new HashSet<PlayerId>();
        nullViewer.add(null);
        assertBossFailure("bossbar.ok", 0.5D, Duration.ofSeconds(1), nullViewer);
    }

    @Test void ruleNullAndNullElementBranchesAreCovered() {
        final Set<HotbarPolicy.State> states = EnumSet.of(HotbarPolicy.State.PLAYING);
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(null, states, Duration.ZERO, 1));
        final Set<DefinitionId> nullResource = new HashSet<DefinitionId>();
        nullResource.add(null);
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(nullResource, states, Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), null, Duration.ZERO, 1));
        final Set<HotbarPolicy.State> nullState = new HashSet<HotbarPolicy.State>();
        nullState.add(null);
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), nullState, Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), states, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new DepositPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), states, Duration.ofSeconds(-1), 1));

        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(null,
                Collections.singletonList(AntiDropPolicy.Recipient.OWNER), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), null, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), Collections.singletonList(AntiDropPolicy.Recipient.OWNER), null));
        assertThrows(IllegalArgumentException.class, () -> new AntiDropPolicy.Rules(
                Collections.singleton(GameFixtures.IRON), Collections.singletonList(AntiDropPolicy.Recipient.OWNER), Duration.ofSeconds(-1)));

        final Set<ArenaStartAnnouncementPolicy.Audience> audience =
                EnumSet.of(ArenaStartAnnouncementPolicy.Audience.LOCAL);
        final Set<ArenaStartAnnouncementPolicy.Channel> channel =
                EnumSet.of(ArenaStartAnnouncementPolicy.Channel.CHAT);
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(
                1, null, audience, channel, null));
        assertThrows(IllegalArgumentException.class, () -> new ArenaStartAnnouncementPolicy.Rules(
                1, Duration.ofSeconds(-1), audience, channel, null));

        final EnumMap<LeaveDelayPolicy.State, Duration> delays =
                new EnumMap<LeaveDelayPolicy.State, Duration>(LeaveDelayPolicy.State.class);
        for (LeaveDelayPolicy.State state : LeaveDelayPolicy.State.values()) { delays.put(state, Duration.ZERO); }
        assertThrows(IllegalArgumentException.class, () -> new LeaveDelayPolicy.Rules(delays, null));
        final Set<LeaveDelayPolicy.Signal> nullSignal = new HashSet<LeaveDelayPolicy.Signal>();
        nullSignal.add(null);
        assertThrows(IllegalArgumentException.class, () -> new LeaveDelayPolicy.Rules(delays, nullSignal));
        delays.put(LeaveDelayPolicy.State.PLAYING, null);
        assertThrows(IllegalArgumentException.class, () -> new LeaveDelayPolicy.Rules(delays,
                Collections.<LeaveDelayPolicy.Signal>emptySet()));
    }

    private static LobbyProjectionPolicy.TabEntry entry(final PlayerId player, final int team,
            final int rank, final String name, final String suffix, final boolean privateEntry) {
        return new LobbyProjectionPolicy.TabEntry(player, LobbyProjectionPolicy.TabSection.TEAM,
                team, rank, name, suffix, privateEntry);
    }
    private static void assertBossFailure(final String key, final double progress,
            final Duration cadence, final Set<PlayerId> viewers) {
        assertThrows(IllegalArgumentException.class, () -> LobbyProjectionPolicy.bossBar(
                LobbyProjectionPolicy.BossBarState.WAITING, key, progress,
                LobbyProjectionPolicy.BossBarColor.BLUE, LobbyProjectionPolicy.BossBarStyle.SOLID,
                cadence, viewers));
    }
    private static String repeat(final char value, final int count) {
        final StringBuilder text = new StringBuilder(count);
        for (int index = 0; index < count; index++) { text.append(value); }
        return text.toString();
    }
}
