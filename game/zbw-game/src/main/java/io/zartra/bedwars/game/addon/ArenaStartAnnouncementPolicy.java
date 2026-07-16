package io.zartra.bedwars.game.addon;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Per-arena threshold, cooldown and regression-safe start-announcement policy. */
public final class ArenaStartAnnouncementPolicy {
    private final Map<ArenaId, Memory> memory = new HashMap<ArenaId, Memory>();

    /** Evaluates one player-count observation and emits at most one publish intent. */
    public synchronized Optional<Announcement> evaluate(
            final Observation observation, final Rules rules) {
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(rules, "rules");
        Memory state = memory.get(observation.arenaId);
        if (state == null) {
            state = new Memory();
            memory.put(observation.arenaId, state);
        }
        if (observation.players < rules.threshold) {
            state.armed = true;
            state.lastPlayers = observation.players;
            return Optional.empty();
        }
        final boolean crossed = state.lastPlayers < rules.threshold
                && observation.players >= rules.threshold;
        final boolean cooled = state.lastPublish == null
                || !observation.observedAt.isBefore(state.lastPublish.plus(rules.cooldown));
        state.lastPlayers = observation.players;
        if (!state.armed || !crossed || !cooled || !observation.joinEligible
                || observation.reservableSlots < 1) {
            return Optional.empty();
        }
        state.armed = false;
        state.lastPublish = observation.observedAt;
        return Optional.of(new Announcement(observation.arenaId, observation.mode,
                observation.group, observation.players, observation.capacity,
                observation.countdownSeconds, rules.audiences, rules.channels,
                rules.permission));
    }

    /** Clears only the selected arena's dedupe/cooldown memory. */
    public synchronized boolean reset(final ArenaId arenaId) {
        return memory.remove(Objects.requireNonNull(arenaId, "arenaId")) != null;
    }

    /** Immutable arena observation supplied by the waiting-state service. */
    public static final class Observation {
        private final ArenaId arenaId;
        private final DefinitionId mode;
        private final DefinitionId group;
        private final int players;
        private final int capacity;
        private final int countdownSeconds;
        private final int reservableSlots;
        private final boolean joinEligible;
        private final Instant observedAt;
        /** Creates a bounded count snapshot. */
        public Observation(final ArenaId arenaId, final DefinitionId mode,
                           final DefinitionId group, final int players, final int capacity,
                           final int countdownSeconds, final int reservableSlots,
                           final boolean joinEligible, final Instant observedAt) {
            if (capacity < 1 || players < 0 || players > capacity || countdownSeconds < 0
                    || reservableSlots < 0 || reservableSlots > capacity - players) {
                throw new IllegalArgumentException("announcement counts are inconsistent");
            }
            this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
            this.mode = Objects.requireNonNull(mode, "mode");
            this.group = Objects.requireNonNull(group, "group");
            this.players = players;
            this.capacity = capacity;
            this.countdownSeconds = countdownSeconds;
            this.reservableSlots = reservableSlots;
            this.joinEligible = joinEligible;
            this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
        }
    }

    /** Immutable localized semantic publish payload. */
    public static final class Announcement {
        private final ArenaId arenaId;
        private final DefinitionId mode;
        private final DefinitionId group;
        private final int players;
        private final int capacity;
        private final int countdown;
        private final Set<Audience> audiences;
        private final Set<Channel> channels;
        private final Optional<String> permission;
        private Announcement(final ArenaId arenaId, final DefinitionId mode,
                             final DefinitionId group, final int players, final int capacity,
                             final int countdown, final Set<Audience> audiences,
                             final Set<Channel> channels, final String permission) {
            this.arenaId = arenaId;
            this.mode = mode;
            this.group = group;
            this.players = players;
            this.capacity = capacity;
            this.countdown = countdown;
            this.audiences = audiences;
            this.channels = channels;
            this.permission = Optional.ofNullable(permission);
        }
        /** @return eligible arena */ public ArenaId arenaId() { return arenaId; }
        /** @return mode semantic value */ public DefinitionId mode() { return mode; }
        /** @return server group semantic value */ public DefinitionId group() { return group; }
        /** @return current admitted players */ public int players() { return players; }
        /** @return arena capacity */ public int capacity() { return capacity; }
        /** @return active countdown seconds */ public int countdown() { return countdown; }
        /** @return immutable delivery audiences */ public Set<Audience> audiences() { return audiences; }
        /** @return immutable semantic channels */ public Set<Channel> channels() { return channels; }
        /** @return optional audience permission */ public Optional<String> permission() { return permission; }
    }

    /** Validated threshold and delivery configuration. */
    public static final class Rules {
        private final int threshold;
        private final Duration cooldown;
        private final Set<Audience> audiences;
        private final Set<Channel> channels;
        private final String permission;
        /** Creates deterministic announcement rules. */
        public Rules(final int threshold, final Duration cooldown,
                     final Set<Audience> audiences, final Set<Channel> channels,
                     final String permission) {
            if (threshold < 1 || threshold > 256 || cooldown == null || cooldown.isNegative()
                    || cooldown.compareTo(Duration.ofHours(1)) > 0) {
                throw new IllegalArgumentException("threshold or cooldown is invalid");
            }
            if (audiences == null || audiences.isEmpty() || audiences.contains(null)
                    || channels == null || channels.isEmpty() || channels.contains(null)) {
                throw new IllegalArgumentException("audiences and channels are required");
            }
            if (audiences.contains(Audience.PERMISSION)
                    && (permission == null || !permission.matches("[a-z0-9.*_-]{1,128}"))) {
                throw new IllegalArgumentException("permission audience requires a valid node");
            }
            this.threshold = threshold;
            this.cooldown = cooldown;
            this.audiences = Collections.unmodifiableSet(EnumSet.copyOf(audiences));
            this.channels = Collections.unmodifiableSet(EnumSet.copyOf(channels));
            this.permission = permission;
        }
    }

    /** Delivery audience semantics; proxy delivery remains owned by M20. */
    public enum Audience { /** Local server. */ LOCAL, /** Server group intent. */ SERVER_GROUP, /** Permission-filtered. */ PERMISSION }
    /** Primary feedback channel semantics. */
    public enum Channel { /** Text chat. */ CHAT, /** Screen title. */ TITLE, /** Action bar. */ ACTION_BAR, /** Sound cue. */ SOUND }
    private static final class Memory {
        private int lastPlayers;
        private boolean armed = true;
        private Instant lastPublish;
    }
}
