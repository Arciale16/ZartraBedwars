package io.zartra.bedwars.game.addon;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds privacy-safe lobby, tab-list, boss-bar and game-mode projection snapshots. */
public final class LobbyProjectionPolicy {
    private LobbyProjectionPolicy() { }

    /** Maps lifecycle states to non-privileged game modes and exact restoration. */
    public static PlayerStateSnapshot.Mode gameMode(
            final PlayerView state, final PlayerStateSnapshot.Mode captured) {
        Objects.requireNonNull(state, "state");
        switch (state) {
            case WAITING: return PlayerStateSnapshot.Mode.ADVENTURE;
            case PLAYING: return PlayerStateSnapshot.Mode.SURVIVAL;
            case SPECTATING: return PlayerStateSnapshot.Mode.SPECTATOR;
            case RESTORE: return Objects.requireNonNull(captured, "captured");
            default: throw new IllegalArgumentException("unknown player view");
        }
    }

    /** Evaluates protected lobby interactions without platform event classes. */
    public static InteractionDecision interaction(final Interaction interaction,
                                                  final LobbyRules rules) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(rules, "rules");
        if (!interaction.inManagedLobby) { return InteractionDecision.ALLOW; }
        if (interaction.bypass) { return InteractionDecision.ALLOW_AUDITED; }
        if (interaction.type == InteractionType.VOID_DAMAGE && rules.voidProtection) {
            return InteractionDecision.RETURN_TO_SPAWN;
        }
        if (interaction.type == InteractionType.DOUBLE_JUMP && rules.doubleJump) {
            return interaction.cooldownElapsed ? InteractionDecision.APPLY_DOUBLE_JUMP
                    : InteractionDecision.DENY;
        }
        return rules.protectedTypes.contains(interaction.type)
                ? InteractionDecision.DENY : InteractionDecision.ALLOW;
    }

    /** Builds a deterministic privacy-filtered tab snapshot. */
    public static TabSnapshot tab(final List<TabEntry> candidates,
                                  final Set<PlayerId> visiblePrivatePlayers,
                                  final int maximumEntries, final String header,
                                  final String footer, final Instant generatedAt) {
        if (maximumEntries < 1 || maximumEntries > 1000 || header == null || header.length() > 512
                || footer == null || footer.length() > 512) {
            throw new IllegalArgumentException("tab limits or templates are invalid");
        }
        final Set<PlayerId> visibility = new HashSet<PlayerId>(
                Objects.requireNonNull(visiblePrivatePlayers, "visiblePrivatePlayers"));
        List<TabEntry> entries = new ArrayList<TabEntry>();
        for (TabEntry entry : Objects.requireNonNull(candidates, "candidates")) {
            if (entry == null) { throw new IllegalArgumentException("tab entry cannot be null"); }
            if (!entry.privateEntry || visibility.contains(entry.playerId)) { entries.add(entry); }
        }
        Collections.sort(entries, TAB_ORDER);
        if (entries.size() > maximumEntries) {
            entries = new ArrayList<TabEntry>(entries.subList(0, maximumEntries));
        }
        return new TabSnapshot(entries, header, footer,
                Objects.requireNonNull(generatedAt, "generatedAt"));
    }

    /** Computes a stable bounded tab diff without packet or scoreboard ownership assumptions. */
    public static TabDiff diff(final TabSnapshot before, final TabSnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        final Map<PlayerId, TabEntry> oldValues = index(before.entries);
        final Map<PlayerId, TabEntry> newValues = index(after.entries);
        final List<TabEntry> upserts = new ArrayList<TabEntry>();
        final Set<PlayerId> removals = new HashSet<PlayerId>();
        for (TabEntry entry : after.entries) {
            if (!entry.equals(oldValues.get(entry.playerId))) { upserts.add(entry); }
        }
        for (PlayerId old : oldValues.keySet()) {
            if (!newValues.containsKey(old)) { removals.add(old); }
        }
        return new TabDiff(upserts, removals,
                !before.header.equals(after.header) || !before.footer.equals(after.footer));
    }

    private static Map<PlayerId, TabEntry> index(final List<TabEntry> entries) {
        final Map<PlayerId, TabEntry> values = new LinkedHashMap<PlayerId, TabEntry>();
        for (TabEntry entry : entries) { values.put(entry.playerId, entry); }
        return values;
    }

    private static final Comparator<TabEntry> TAB_ORDER = new Comparator<TabEntry>() {
        @Override public int compare(final TabEntry left, final TabEntry right) {
            int compared = Integer.compare(left.section.order, right.section.order);
            if (compared == 0) { compared = Integer.compare(left.teamOrder, right.teamOrder); }
            if (compared == 0) { compared = Integer.compare(right.rankWeight, left.rankWeight); }
            if (compared == 0) { compared = left.displayName.compareToIgnoreCase(right.displayName); }
            return compared == 0 ? left.playerId.toString().compareTo(right.playerId.toString()) : compared;
        }
    };

    /** Creates a validated semantic boss-bar snapshot. */
    public static BossBarSnapshot bossBar(final BossBarState state, final String messageKey,
                                          final double progress, final BossBarColor color,
                                          final BossBarStyle style, final Duration cadence,
                                          final Set<PlayerId> viewers) {
        return new BossBarSnapshot(state, messageKey, progress, color, style, cadence, viewers);
    }

    /** Player lifecycle views with an explicit restoration transition. */
    public enum PlayerView { /** Protected waiting. */ WAITING, /** Active participant. */ PLAYING, /** Eliminated viewer. */ SPECTATING, /** Exact pre-session restore. */ RESTORE }
    /** Lobby interaction categories. */
    public enum InteractionType { /** Block break. */ BREAK, /** Block placement. */ PLACE, /** Item drop. */ DROP, /** Damage. */ DAMAGE, /** Hunger. */ HUNGER, /** Void damage. */ VOID_DAMAGE, /** Flight gesture. */ DOUBLE_JUMP }
    /** Neutral interaction outcomes applied by a platform adapter. */
    public enum InteractionDecision { /** Preserve event. */ ALLOW, /** Preserve and audit bypass. */ ALLOW_AUDITED, /** Cancel event. */ DENY, /** Teleport to configured spawn. */ RETURN_TO_SPAWN, /** Apply configured jump vector/cooldown. */ APPLY_DOUBLE_JUMP }

    /** Immutable interaction facts. */
    public static final class Interaction {
        private final InteractionType type;
        private final boolean inManagedLobby;
        private final boolean bypass;
        private final boolean cooldownElapsed;
        /** Creates an interaction observation. */
        public Interaction(final InteractionType type, final boolean inManagedLobby,
                           final boolean bypass, final boolean cooldownElapsed) {
            this.type = Objects.requireNonNull(type, "type");
            this.inManagedLobby = inManagedLobby;
            this.bypass = bypass;
            this.cooldownElapsed = cooldownElapsed;
        }
    }

    /** Immutable lobby protection and mobility rules. */
    public static final class LobbyRules {
        private final Set<InteractionType> protectedTypes;
        private final boolean voidProtection;
        private final boolean doubleJump;
        /** Creates rules with an explicit protected interaction set. */
        public LobbyRules(final Set<InteractionType> protectedTypes,
                          final boolean voidProtection, final boolean doubleJump) {
            if (protectedTypes == null || protectedTypes.contains(null)) {
                throw new IllegalArgumentException("protected types cannot contain null");
            }
            this.protectedTypes = Collections.unmodifiableSet(
                    new HashSet<InteractionType>(protectedTypes));
            this.voidProtection = voidProtection;
            this.doubleJump = doubleJump;
        }
    }

    /** Tab sections with deterministic global ordering. */
    public enum TabSection {
        /** Staff. */ STAFF(0),
        /** Active teams. */ TEAM(1),
        /** Spectators. */ SPECTATOR(2),
        /** Lobby players. */ LOBBY(3);
        private final int order;
        TabSection(final int order) {
            this.order = order;
        }
    }

    /** Privacy-safe resolved tab entry; no remote provider call is allowed during rendering. */
    public static final class TabEntry {
        private final PlayerId playerId;
        private final TabSection section;
        private final int teamOrder;
        private final int rankWeight;
        private final String displayName;
        private final String suffix;
        private final boolean privateEntry;
        /** Creates one cached, already-authorized entry. */
        public TabEntry(final PlayerId playerId, final TabSection section, final int teamOrder,
                        final int rankWeight, final String displayName, final String suffix,
                        final boolean privateEntry) {
            if (teamOrder < 0 || rankWeight < 0 || displayName == null || displayName.isEmpty()
                    || displayName.length() > 64 || suffix == null || suffix.length() > 128) {
                throw new IllegalArgumentException("tab entry fields are invalid");
            }
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            this.section = Objects.requireNonNull(section, "section");
            this.teamOrder = teamOrder;
            this.rankWeight = rankWeight;
            this.displayName = displayName;
            this.suffix = suffix;
            this.privateEntry = privateEntry;
        }
        /** @return player identity */ public PlayerId playerId() { return playerId; }
        /** @return section */ public TabSection section() { return section; }
        /** @return allowed display name */ public String displayName() { return displayName; }
        /** @return cached suffix */ public String suffix() { return suffix; }
        @Override public int hashCode() { return Objects.hash(playerId, section, teamOrder, rankWeight, displayName, suffix, privateEntry); }
        @Override public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (!(other instanceof TabEntry)) { return false; }
            final TabEntry that = (TabEntry) other;
            return teamOrder == that.teamOrder && rankWeight == that.rankWeight
                    && privateEntry == that.privateEntry && playerId.equals(that.playerId)
                    && section == that.section && displayName.equals(that.displayName)
                    && suffix.equals(that.suffix);
        }
    }

    /** Immutable complete tab render model. */
    public static final class TabSnapshot {
        private final List<TabEntry> entries;
        private final String header;
        private final String footer;
        private final Instant generatedAt;
        private TabSnapshot(final List<TabEntry> entries, final String header,
                            final String footer, final Instant generatedAt) {
            this.entries = Collections.unmodifiableList(new ArrayList<TabEntry>(entries));
            this.header = header;
            this.footer = footer;
            this.generatedAt = generatedAt;
        }
        /** @return sorted entries */ public List<TabEntry> entries() { return entries; }
        /** @return localized header */ public String header() { return header; }
        /** @return localized footer */ public String footer() { return footer; }
        /** @return generation instant used for cadence control */ public Instant generatedAt() { return generatedAt; }
    }

    /** Immutable minimal tab update and stale-entry cleanup plan. */
    public static final class TabDiff {
        private final List<TabEntry> upserts;
        private final Set<PlayerId> removals;
        private final boolean headerFooterChanged;
        private TabDiff(final List<TabEntry> upserts, final Set<PlayerId> removals,
                        final boolean headerFooterChanged) {
            this.upserts = Collections.unmodifiableList(new ArrayList<TabEntry>(upserts));
            this.removals = Collections.unmodifiableSet(new HashSet<PlayerId>(removals));
            this.headerFooterChanged = headerFooterChanged;
        }
        /** @return added or changed entries */ public List<TabEntry> upserts() { return upserts; }
        /** @return stale identities to clear */ public Set<PlayerId> removals() { return removals; }
        /** @return whether header/footer must be updated */ public boolean headerFooterChanged() { return headerFooterChanged; }
    }

    /** Boss-bar lifecycle sources. */
    public enum BossBarState { /** Waiting/countdown. */ WAITING, /** Active match event/team state. */ PLAYING, /** Winner/return countdown. */ POST_GAME }
    /** Version-neutral boss-bar colours. */
    public enum BossBarColor { /** Blue. */ BLUE, /** Green. */ GREEN, /** Red. */ RED, /** Yellow. */ YELLOW, /** White. */ WHITE, /** Purple. */ PURPLE }
    /** Version-neutral boss-bar segmentation styles. */
    public enum BossBarStyle { /** Continuous. */ SOLID, /** Six segments. */ SEGMENTED_6, /** Ten segments. */ SEGMENTED_10, /** Twelve segments. */ SEGMENTED_12, /** Twenty segments. */ SEGMENTED_20 }

    /** Immutable boss-bar render model with explicit cadence and viewers. */
    public static final class BossBarSnapshot {
        private final BossBarState state;
        private final String messageKey;
        private final double progress;
        private final BossBarColor color;
        private final BossBarStyle style;
        private final Duration cadence;
        private final Set<PlayerId> viewers;
        private BossBarSnapshot(final BossBarState state, final String messageKey,
                                final double progress, final BossBarColor color,
                                final BossBarStyle style, final Duration cadence,
                                final Set<PlayerId> viewers) {
            if (messageKey == null || !messageKey.matches("[a-z0-9_.-]{1,128}")
                    || !Double.isFinite(progress) || progress < 0.0D || progress > 1.0D
                    || cadence == null || cadence.isNegative() || cadence.isZero()
                    || cadence.compareTo(Duration.ofSeconds(10)) > 0
                    || viewers == null || viewers.contains(null)) {
                throw new IllegalArgumentException("boss-bar snapshot is invalid");
            }
            this.state = Objects.requireNonNull(state, "state");
            this.messageKey = messageKey;
            this.progress = progress;
            this.color = Objects.requireNonNull(color, "color");
            this.style = Objects.requireNonNull(style, "style");
            this.cadence = cadence;
            this.viewers = Collections.unmodifiableSet(new HashSet<PlayerId>(viewers));
        }
        /** @return lifecycle source */ public BossBarState state() { return state; }
        /** @return localization key */ public String messageKey() { return messageKey; }
        /** @return normalized progress */ public double progress() { return progress; }
        /** @return semantic colour */ public BossBarColor color() { return color; }
        /** @return semantic style */ public BossBarStyle style() { return style; }
        /** @return bounded refresh cadence */ public Duration cadence() { return cadence; }
        /** @return authorized visible viewers */ public Set<PlayerId> viewers() { return viewers; }
    }
}
