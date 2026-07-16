package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.addon.LobbyProjectionPolicy;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;

/** Owner-thread scoreboard, tab-list and boss-bar projection with stale-state cleanup. */
public final class PaperLobbyProjection {
    private final Map<PlayerId, Object> ownedScoreboards = new HashMap<PlayerId, Object>();
    private final Map<PlayerId, Set<PlayerId>> ownedTabEntries = new HashMap<PlayerId, Set<PlayerId>>();
    private final Map<MatchId, Object> bars = new HashMap<MatchId, Object>();

    /** Applies a complete sidebar, replacing only a scoreboard previously owned by this projector. */
    public void scoreboard(final PlayerId viewerId, final String title, final List<String> lines) {
        requireOwner();
        if (title == null || title.length() > 64 || lines == null || lines.size() > 15
                || lines.contains(null)) { throw new IllegalArgumentException("scoreboard projection is invalid"); }
        final Object viewer = requirePlayer(viewerId);
        final Object manager = manager();
        final Object board = PaperReflection.invoke(manager, "getNewScoreboard", new Class<?>[0]);
        final Object objective = PaperReflection.invoke(board, "registerNewObjective",
                new Class<?>[] {String.class, String.class, String.class}, "zbw", "dummy", title);
        final Class<?> displaySlot = type("org.bukkit.scoreboard.DisplaySlot");
        PaperReflection.invoke(objective, "setDisplaySlot", new Class<?>[] {displaySlot},
                PaperReflection.constant(displaySlot, "SIDEBAR"));
        int score = lines.size();
        final Set<String> unique = new HashSet<String>();
        for (String line : lines) {
            String value = line.length() > 128 ? line.substring(0, 128) : line;
            while (!unique.add(value)) { value = value + " "; }
            final Object scoreValue = PaperReflection.invoke(objective, "getScore",
                    new Class<?>[] {String.class}, value);
            PaperReflection.invoke(scoreValue, "setScore", new Class<?>[] {int.class}, Integer.valueOf(score--));
        }
        PaperReflection.invoke(viewer, "setScoreboard", new Class<?>[] {type("org.bukkit.scoreboard.Scoreboard")}, board);
        ownedScoreboards.put(viewerId, board);
    }

    /** Applies resolved local tab fields and header/footer; no placeholder or remote query occurs. */
    public void tab(final PlayerId viewerId, final LobbyProjectionPolicy.TabSnapshot snapshot) {
        requireOwner();
        final Object viewer = requirePlayer(viewerId);
        PaperReflection.invoke(viewer, "setPlayerListHeaderFooter",
                new Class<?>[] {String.class, String.class}, snapshot.header(), snapshot.footer());
        final Set<PlayerId> next = new HashSet<PlayerId>();
        for (LobbyProjectionPolicy.TabEntry entry : snapshot.entries()) {
            final Object target = find(entry.playerId());
            if (target != null) {
                PaperReflection.invoke(target, "setPlayerListName", new Class<?>[] {String.class},
                        entry.displayName() + entry.suffix());
                next.add(entry.playerId());
            }
        }
        final Set<PlayerId> previous = ownedTabEntries.get(viewerId);
        if (previous != null) {
            for (PlayerId stale : previous) {
                if (!next.contains(stale)) {
                    final Object target = find(stale);
                    if (target != null) {
                        PaperReflection.invoke(target, "setPlayerListName",
                                new Class<?>[] {String.class}, new Object[] {null});
                    }
                }
            }
        }
        ownedTabEntries.put(viewerId, next);
    }

    /** Creates or atomically updates one match boss bar and its authorized viewer set. */
    public void bossBar(final MatchId matchId, final LobbyProjectionPolicy.BossBarSnapshot snapshot) {
        requireOwner();
        Object bar = bars.get(Objects.requireNonNull(matchId, "matchId"));
        final Object color = PaperReflection.constant(PaperReflection.BAR_COLOR, snapshot.color().name());
        final Object style = PaperReflection.constant(PaperReflection.BAR_STYLE, style(snapshot.style()));
        if (bar == null) {
            final Class<?> flagArray = PaperReflection.emptyArray(PaperReflection.BAR_FLAG).getClass();
            bar = PaperReflection.invokeStatic(Bukkit.class, "createBossBar",
                    new Class<?>[] {String.class, PaperReflection.BAR_COLOR, PaperReflection.BAR_STYLE, flagArray},
                    snapshot.messageKey(), color, style, PaperReflection.emptyArray(PaperReflection.BAR_FLAG));
            bars.put(matchId, bar);
        } else {
            PaperReflection.invoke(bar, "setTitle", new Class<?>[] {String.class}, snapshot.messageKey());
            PaperReflection.invoke(bar, "setColor", new Class<?>[] {PaperReflection.BAR_COLOR}, color);
            PaperReflection.invoke(bar, "setStyle", new Class<?>[] {PaperReflection.BAR_STYLE}, style);
        }
        PaperReflection.invoke(bar, "setProgress", new Class<?>[] {double.class}, Double.valueOf(snapshot.progress()));
        PaperReflection.invoke(bar, "removeAll", new Class<?>[0]);
        for (PlayerId viewer : snapshot.viewers()) {
            final Object player = find(viewer);
            if (player != null) { PaperReflection.invoke(bar, "addPlayer", new Class<?>[] {PaperReflection.PLAYER}, player); }
        }
    }

    /** Clears only projections still owned by this projector for one player. */
    public void clear(final PlayerId playerId) {
        requireOwner();
        final Object player = find(Objects.requireNonNull(playerId, "playerId"));
        final Object owned = ownedScoreboards.remove(playerId);
        if (player != null) {
            if (owned != null && PaperReflection.invoke(player, "getScoreboard", new Class<?>[0]) == owned) {
                final Object main = PaperReflection.invoke(manager(), "getMainScoreboard", new Class<?>[0]);
                PaperReflection.invoke(player, "setScoreboard", new Class<?>[] {type("org.bukkit.scoreboard.Scoreboard")}, main);
            }
            PaperReflection.invoke(player, "setPlayerListHeaderFooter",
                    new Class<?>[] {String.class, String.class}, "", "");
            PaperReflection.invoke(player, "setPlayerListName", new Class<?>[] {String.class}, new Object[] {null});
            for (Object bar : bars.values()) {
                PaperReflection.invoke(bar, "removePlayer", new Class<?>[] {PaperReflection.PLAYER}, player);
            }
        }
        ownedTabEntries.remove(playerId);
    }

    /** Hides and forgets one terminal match bar. */
    public void removeBossBar(final MatchId matchId) {
        requireOwner();
        final Object removed = bars.remove(Objects.requireNonNull(matchId, "matchId"));
        if (removed != null) { PaperReflection.invoke(removed, "removeAll", new Class<?>[0]); }
    }

    /** Clears every tracked projection during plugin disable. */
    public void close() {
        requireOwner();
        for (Object player : online()) {
            final UUID uuid = (UUID) PaperReflection.invoke(player, "getUniqueId", new Class<?>[0]);
            clear(PlayerId.of(uuid));
        }
        for (Object bar : bars.values()) { PaperReflection.invoke(bar, "removeAll", new Class<?>[0]); }
        bars.clear();
        ownedScoreboards.clear();
        ownedTabEntries.clear();
    }

    /** @return active bar count for bounded runtime diagnostics */ public int activeBossBars() { return bars.size(); }
    private static String style(final LobbyProjectionPolicy.BossBarStyle style) {
        switch (style) {
            case SOLID: return "SOLID";
            case SEGMENTED_6: return "SEGMENTED_6";
            case SEGMENTED_10: return "SEGMENTED_10";
            case SEGMENTED_12: return "SEGMENTED_12";
            case SEGMENTED_20: return "SEGMENTED_20";
            default: throw new IllegalArgumentException("unknown boss-bar style");
        }
    }
    private static Object manager() {
        return Objects.requireNonNull(PaperReflection.invokeStatic(Bukkit.class,
                "getScoreboardManager", new Class<?>[0]), "scoreboard manager");
    }
    private static Object requirePlayer(final PlayerId playerId) {
        final Object player = find(playerId);
        if (player == null) { throw new IllegalStateException("player is not online"); }
        return player;
    }
    private static Object find(final PlayerId playerId) {
        return PaperReflection.invokeStatic(Bukkit.class, "getPlayer", new Class<?>[] {UUID.class},
                Objects.requireNonNull(playerId, "playerId").asUuid());
    }
    private static Collection<?> online() {
        return (Collection<?>) PaperReflection.invokeStatic(Bukkit.class, "getOnlinePlayers", new Class<?>[0]);
    }
    private static Class<?> type(final String name) {
        try {
            return Class.forName(name, false, PaperLobbyProjection.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Paper type missing: " + name, failure);
        }
    }
    private static void requireOwner() {
        if (!Bukkit.isPrimaryThread()) { throw new IllegalStateException("Paper projection requires primary thread"); }
    }
}
