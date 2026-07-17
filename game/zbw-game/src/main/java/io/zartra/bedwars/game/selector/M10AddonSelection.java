package io.zartra.bedwars.game.selector;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.Party;
import io.zartra.bedwars.game.model.TeamDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Team-selector, Compass tracker and quick-communication policies allocated to M10. */
public final class M10AddonSelection {
    private M10AddonSelection() { throw new AssertionError("No instances"); }

    /** Team selection outcome. */ public enum TeamVerdict { /** Preference recorded. */ ACCEPTED, /** Selection removed. */ CLEARED, /** Team full. */ FULL, /** Permission denied. */ FORBIDDEN, /** Selection phase locked. */ LOCKED, /** Arena or party revision stale. */ STALE, /** Party cannot fit atomically. */ PARTY_TOO_LARGE }

    /** Immutable occupancy facts. */
    public static final class TeamOption {
        private final TeamDefinition team;
        private final int occupied;
        private final boolean enabled;
        private final boolean permitted;
        /** Creates one team option. */
        public TeamOption(final TeamDefinition team, final int occupied, final boolean enabled,
                          final boolean permitted) {
            this.team = Objects.requireNonNull(team, "team");
            if (occupied < 0 || occupied > team.capacity()) { throw new IllegalArgumentException("invalid occupancy"); }
            this.occupied = occupied; this.enabled = enabled; this.permitted = permitted;
        }
        /** @return team */ public TeamDefinition team() { return team; }
        /** @return occupied places */ public int occupied() { return occupied; }
        /** @return free places */ public int available() { return team.capacity() - occupied; }
        /** @return enabled */ public boolean enabled() { return enabled; }
        /** @return authorized */ public boolean permitted() { return permitted; }
    }

    /** Revision-bound team preference. */
    public static final class TeamSelection {
        private final ArenaId arenaId;
        private final long arenaRevision;
        private final long partyRevision;
        private final List<PlayerId> actors;
        private final DefinitionId teamId;
        private TeamSelection(final ArenaId arenaId, final long arenaRevision,
                              final long partyRevision, final Collection<PlayerId> actors,
                              final DefinitionId teamId) {
            this.arenaId = arenaId; this.arenaRevision = arenaRevision; this.partyRevision = partyRevision;
            this.actors = Collections.unmodifiableList(new ArrayList<PlayerId>(actors)); this.teamId = teamId;
        }
        /** @return arena */ public ArenaId arenaId() { return arenaId; }
        /** @return arena revision */ public long arenaRevision() { return arenaRevision; }
        /** @return party revision, or zero for solo */ public long partyRevision() { return partyRevision; }
        /** @return atomic actors */ public List<PlayerId> actors() { return actors; }
        /** @return selected team */ public DefinitionId teamId() { return teamId; }
    }

    /** Bounded team preference service with no silent party splitting. */
    public static final class TeamSelector {
        private final int capacity;
        private final Map<PlayerId, TeamSelection> selections = new LinkedHashMap<PlayerId, TeamSelection>();
        /** Creates a bounded selector. */
        public TeamSelector(final int capacity) {
            if (capacity < 1 || capacity > 100000) { throw new IllegalArgumentException("invalid selector capacity"); }
            this.capacity = capacity;
        }
        /** Records one solo or party preference atomically. */
        public synchronized TeamVerdict select(final ArenaId arenaId, final long arenaRevision,
                                               final PlayerId actor, final Party party,
                                               final TeamOption option, final boolean locked) {
            Objects.requireNonNull(arenaId, "arenaId"); Objects.requireNonNull(actor, "actor");
            final TeamOption checked = Objects.requireNonNull(option, "option");
            if (locked) { return TeamVerdict.LOCKED; }
            if (!checked.enabled() || !checked.permitted()) { return TeamVerdict.FORBIDDEN; }
            final List<PlayerId> actors = party == null ? Collections.singletonList(actor) : party.members();
            if (party != null && !party.leader().equals(actor)) { return TeamVerdict.FORBIDDEN; }
            if (actors.size() > checked.team().capacity()) { return TeamVerdict.PARTY_TOO_LARGE; }
            if (actors.size() > checked.available()) { return TeamVerdict.FULL; }
            int additions = 0;
            for (PlayerId player : actors) { if (!selections.containsKey(player)) { additions++; } }
            if (selections.size() + additions > capacity) { throw new IllegalStateException("team selector capacity reached"); }
            final TeamSelection selection = new TeamSelection(arenaId, arenaRevision,
                    party == null ? 0L : party.revision(), actors, checked.team().id());
            for (PlayerId player : actors) { selections.put(player, selection); }
            return TeamVerdict.ACCEPTED;
        }
        /** Clears the complete party selection for the actor. */
        public synchronized TeamVerdict clear(final PlayerId actor) {
            final TeamSelection selection = selections.get(Objects.requireNonNull(actor, "actor"));
            if (selection == null) { return TeamVerdict.CLEARED; }
            for (PlayerId player : selection.actors()) { selections.remove(player); }
            return TeamVerdict.CLEARED;
        }
        /** Restores only a still-current selection after reconnect. */
        public synchronized Optional<TeamSelection> reconnect(final PlayerId actor,
                                                             final long arenaRevision,
                                                             final long partyRevision) {
            final TeamSelection selection = selections.get(Objects.requireNonNull(actor, "actor"));
            if (selection == null) { return Optional.empty(); }
            if (selection.arenaRevision() != arenaRevision || selection.partyRevision() != partyRevision) {
                clear(actor); return Optional.empty();
            }
            return Optional.of(selection);
        }
        /** Deterministically chooses the least-occupied eligible team. */
        public TeamOption autoAssign(final Collection<TeamOption> options, final int actors) {
            final List<TeamOption> eligible = new ArrayList<TeamOption>();
            for (TeamOption option : Objects.requireNonNull(options, "options")) {
                final TeamOption checked = Objects.requireNonNull(option, "option");
                if (checked.enabled() && checked.permitted() && checked.available() >= actors) { eligible.add(checked); }
            }
            eligible.sort(Comparator.comparingInt(TeamOption::occupied)
                    .thenComparing(value -> value.team().id().toString()));
            if (eligible.isEmpty()) { throw new IllegalStateException("no team can fit actor group"); }
            return eligible.get(0);
        }
        /** Removes every preference for a reset arena. @return player rows removed */
        public synchronized int reset(final ArenaId arenaId) {
            final List<PlayerId> removals = new ArrayList<PlayerId>();
            for (Map.Entry<PlayerId, TeamSelection> entry : selections.entrySet()) {
                if (entry.getValue().arenaId().equals(Objects.requireNonNull(arenaId, "arenaId"))) { removals.add(entry.getKey()); }
            }
            for (PlayerId player : removals) { selections.remove(player); }
            return removals.size();
        }
        /** @return number of indexed players */ public synchronized int size() { return selections.size(); }
    }

    /** Privacy-safe tracker target facts. */
    public static final class TrackerTarget {
        private final PlayerId playerId;
        private final DefinitionId teamId;
        private final double distance;
        private final boolean living;
        private final boolean visible;
        private final boolean vanished;
        private final boolean spectator;
        /** Creates target facts. */
        public TrackerTarget(final PlayerId playerId, final DefinitionId teamId,
                             final double distance, final boolean living, final boolean visible,
                             final boolean vanished, final boolean spectator) {
            if (!Double.isFinite(distance) || distance < 0D) { throw new IllegalArgumentException("invalid distance"); }
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            this.teamId = Objects.requireNonNull(teamId, "teamId"); this.distance = distance;
            this.living = living; this.visible = visible; this.vanished = vanished; this.spectator = spectator;
        }
        /** @return player */ public PlayerId playerId() { return playerId; }
        /** @return team */ public DefinitionId teamId() { return teamId; }
        /** @return distance */ public double distance() { return distance; }
        /** @return privacy-safe eligibility */ public boolean eligible() { return living && visible && !vanished && !spectator; }
    }

    /** Localized quick communication categories. */
    public enum Callout { /** Attack. */ ATTACK, /** Defend. */ DEFEND, /** Resources. */ RESOURCES, /** Danger. */ DANGER, /** Regroup. */ REGROUP }

    /** Immutable team-only callout. */
    public static final class Message {
        private final PlayerId sender;
        private final DefinitionId teamId;
        private final Callout callout;
        private final PlayerId safeTarget;
        private final Instant createdAt;
        private Message(final PlayerId sender, final DefinitionId teamId, final Callout callout,
                        final PlayerId safeTarget, final Instant createdAt) {
            this.sender = sender; this.teamId = teamId; this.callout = callout;
            this.safeTarget = safeTarget; this.createdAt = createdAt;
        }
        /** @return sender */ public PlayerId sender() { return sender; }
        /** @return audience team */ public DefinitionId teamId() { return teamId; }
        /** @return localized category */ public Callout callout() { return callout; }
        /** @return privacy-safe target */ public Optional<PlayerId> safeTarget() { return Optional.ofNullable(safeTarget); }
        /** @return creation instant */ public Instant createdAt() { return createdAt; }
    }

    /** Tracker and anti-spam quick-communication service. */
    public static final class Compass {
        private final TimeSource time;
        private final Duration cooldown;
        private final double maximumRange;
        private final int maximumSenders;
        private final Map<PlayerId, Instant> lastMessages = new LinkedHashMap<PlayerId, Instant>();
        /** Creates bounded Compass policy. */
        public Compass(final TimeSource time, final Duration cooldown, final double maximumRange,
                       final int maximumSenders) {
            this.time = Objects.requireNonNull(time, "time");
            if (cooldown == null || cooldown.isNegative() || cooldown.isZero()
                    || !Double.isFinite(maximumRange) || maximumRange <= 0D
                    || maximumSenders < 1 || maximumSenders > 100000) {
                throw new IllegalArgumentException("invalid Compass policy");
            }
            this.cooldown = cooldown; this.maximumRange = maximumRange; this.maximumSenders = maximumSenders;
        }
        /** @return nearest eligible enemy using distance then stable identity */
        public Optional<TrackerTarget> nearest(final DefinitionId ownTeam,
                                              final Collection<TrackerTarget> targets) {
            final List<TrackerTarget> eligible = new ArrayList<TrackerTarget>();
            for (TrackerTarget target : Objects.requireNonNull(targets, "targets")) {
                final TrackerTarget checked = Objects.requireNonNull(target, "target");
                if (checked.eligible() && !checked.teamId().equals(ownTeam)
                        && checked.distance() <= maximumRange) { eligible.add(checked); }
            }
            eligible.sort(Comparator.comparingDouble(TrackerTarget::distance)
                    .thenComparing(value -> value.playerId().toString()));
            return eligible.isEmpty() ? Optional.<TrackerTarget>empty() : Optional.of(eligible.get(0));
        }
        /** Creates one rate-limited team-only callout and strips an ineligible target. */
        public synchronized Message callout(final PlayerId sender, final DefinitionId team,
                                            final Callout callout, final TrackerTarget target) {
            Objects.requireNonNull(sender, "sender"); Objects.requireNonNull(team, "team");
            final Instant previous = lastMessages.get(sender);
            if (previous != null && previous.plus(cooldown).isAfter(time.now())) {
                throw new IllegalStateException("Compass callout cooldown active");
            }
            if (!lastMessages.containsKey(sender) && lastMessages.size() >= maximumSenders) {
                throw new IllegalStateException("Compass sender capacity reached");
            }
            lastMessages.put(sender, time.now());
            final PlayerId safe = target != null && target.eligible() ? target.playerId() : null;
            return new Message(sender, team, Objects.requireNonNull(callout, "callout"), safe, time.now());
        }
        /** Removes expired cooldown rows. @return removed count */
        public synchronized int cleanup() {
            final Set<PlayerId> removals = new LinkedHashSet<PlayerId>();
            for (Map.Entry<PlayerId, Instant> entry : lastMessages.entrySet()) {
                if (!entry.getValue().plus(cooldown).isAfter(time.now())) { removals.add(entry.getKey()); }
            }
            for (PlayerId player : removals) { lastMessages.remove(player); }
            return removals.size();
        }
    }
}
