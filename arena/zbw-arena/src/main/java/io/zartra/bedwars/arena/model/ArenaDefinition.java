package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable aggregate root for arena setup and lifecycle configuration. */
public final class ArenaDefinition {
    private final ArenaId id;
    private final MapId mapId;
    private final String displayName;
    private final WorldKey world;
    private final WorldKey templateWorld;
    private final DefinitionId worldAdapter;
    private final DefinitionId group;
    private final Set<DefinitionId> modes;
    private final int minimumPlayers;
    private final int maximumPlayers;
    private final int teamSize;
    private final int priority;
    private final int rotationWeight;
    private final ArenaLocation waitingSpawn;
    private final ArenaLocation spectatorSpawn;
    private final ArenaRegion bounds;
    private final double voidY;
    private final double buildMinimumY;
    private final double buildMaximumY;
    private final List<ArenaTeam> teams;
    private final List<ArenaGenerator> generators;
    private final List<ArenaNpc> npcs;
    private final List<ArenaRegion> protectedRegions;
    private final List<ArenaHologram> holograms;
    private final Map<DefinitionId, Duration> speeds;
    private final Set<DefinitionId> rules;
    private final Map<DefinitionId, String> metadata;
    private final Status status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    private ArenaDefinition(final Builder builder) {
        id = Objects.requireNonNull(builder.id, "id");
        mapId = Objects.requireNonNull(builder.mapId, "mapId");
        displayName = text(builder.displayName, "displayName", 64);
        world = builder.world;
        templateWorld = builder.templateWorld;
        worldAdapter = Objects.requireNonNull(builder.worldAdapter, "worldAdapter");
        group = Objects.requireNonNull(builder.group, "group");
        modes = immutableSet(builder.modes, "modes", 64, false);
        if (builder.minimumPlayers < 1 || builder.maximumPlayers < builder.minimumPlayers
                || builder.maximumPlayers > 256 || builder.teamSize < 1
                || builder.teamSize > builder.maximumPlayers) {
            throw new IllegalArgumentException("invalid player or team-size limits");
        }
        minimumPlayers = builder.minimumPlayers;
        maximumPlayers = builder.maximumPlayers;
        teamSize = builder.teamSize;
        if (builder.priority < -100000 || builder.priority > 100000
                || builder.rotationWeight < 1 || builder.rotationWeight > 100000) {
            throw new IllegalArgumentException("invalid priority or rotation weight");
        }
        priority = builder.priority;
        rotationWeight = builder.rotationWeight;
        waitingSpawn = builder.waitingSpawn;
        spectatorSpawn = builder.spectatorSpawn;
        bounds = builder.bounds;
        finite(builder.voidY, "voidY");
        finite(builder.buildMinimumY, "buildMinimumY");
        finite(builder.buildMaximumY, "buildMaximumY");
        if (builder.buildMaximumY < builder.buildMinimumY) {
            throw new IllegalArgumentException("build maximum precedes minimum");
        }
        voidY = builder.voidY;
        buildMinimumY = builder.buildMinimumY;
        buildMaximumY = builder.buildMaximumY;
        teams = immutableSorted(builder.teams, "teams", 64);
        generators = immutableSorted(builder.generators, "generators", 256);
        npcs = immutableSorted(builder.npcs, "npcs", 256);
        protectedRegions = immutableRegions(builder.protectedRegions);
        holograms = immutableSorted(builder.holograms, "holograms", 128);
        speeds = immutableSpeeds(builder.speeds);
        rules = immutableSet(builder.rules, "rules", 128, true);
        metadata = immutableMetadata(builder.metadata);
        status = Objects.requireNonNull(builder.status, "status");
        createdAt = Objects.requireNonNull(builder.createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt) || builder.version < 0L) {
            throw new IllegalArgumentException("invalid arena timestamps or version");
        }
        version = builder.version;
    }

    /** @return a builder containing required identity and safe defaults */
    public static Builder builder(final ArenaId id, final MapId mapId, final String displayName,
                                  final Instant createdAt) {
        return new Builder(id, mapId, displayName, createdAt);
    }

    /** @return a mutable construction helper initialized from this immutable definition */
    public Builder toBuilder() { return new Builder(this); }

    private static String text(final String value, final String label, final int maximum) {
        if (value == null || value.trim().isEmpty() || value.length() > maximum
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is blank, unsafe or too long");
        }
        return value;
    }

    private static void finite(final double value, final String label) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }

    private static <T extends Comparable<? super T>> List<T> immutableSorted(
            final List<T> source, final String label, final int maximum) {
        final List<T> copy = new ArrayList<T>(Objects.requireNonNull(source, label));
        if (copy.size() > maximum || copy.contains(null)) {
            throw new IllegalArgumentException(label + " is too large or contains null");
        }
        Collections.sort(copy);
        for (int index = 1; index < copy.size(); index++) {
            if (copy.get(index - 1).compareTo(copy.get(index)) == 0) {
                throw new IllegalArgumentException(label + " contains duplicate identities");
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<ArenaRegion> immutableRegions(final List<ArenaRegion> source) {
        final Map<DefinitionId, ArenaRegion> sorted = new TreeMap<DefinitionId, ArenaRegion>();
        for (ArenaRegion region : Objects.requireNonNull(source, "protectedRegions")) {
            if (sorted.put(Objects.requireNonNull(region, "region").id(), region) != null) {
                throw new IllegalArgumentException("protectedRegions contains duplicate identities");
            }
        }
        if (sorted.size() > 128) { throw new IllegalArgumentException("too many protected regions"); }
        return Collections.unmodifiableList(new ArrayList<ArenaRegion>(sorted.values()));
    }

    private static Set<DefinitionId> immutableSet(final Set<DefinitionId> source,
                                                   final String label, final int maximum,
                                                   final boolean emptyAllowed) {
        final Set<DefinitionId> copy = new TreeSet<DefinitionId>();
        for (DefinitionId value : Objects.requireNonNull(source, label)) {
            copy.add(Objects.requireNonNull(value, label + " entry"));
        }
        if ((!emptyAllowed && copy.isEmpty()) || copy.size() > maximum) {
            throw new IllegalArgumentException(label + " has an invalid size or null entry");
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Map<DefinitionId, Duration> immutableSpeeds(
            final Map<DefinitionId, Duration> source) {
        final Map<DefinitionId, Duration> copy = new TreeMap<DefinitionId, Duration>();
        for (Map.Entry<DefinitionId, Duration> entry
                : Objects.requireNonNull(source, "speeds").entrySet()) {
            final Duration value = Objects.requireNonNull(entry.getValue(), "speed");
            if (value.isZero() || value.isNegative() || value.compareTo(Duration.ofDays(1)) > 0) {
                throw new IllegalArgumentException("speed durations must be positive and bounded");
            }
            copy.put(Objects.requireNonNull(entry.getKey(), "speed key"), value);
        }
        if (copy.size() > 64) { throw new IllegalArgumentException("too many speed settings"); }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<DefinitionId, String> immutableMetadata(final Map<DefinitionId, String> source) {
        final Map<DefinitionId, String> copy = new TreeMap<DefinitionId, String>();
        for (Map.Entry<DefinitionId, String> entry
                : Objects.requireNonNull(source, "metadata").entrySet()) {
            final String value = Objects.requireNonNull(entry.getValue(), "metadata value");
            if (value.length() > 1024 || value.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("metadata value is unsafe or too long");
            }
            copy.put(Objects.requireNonNull(entry.getKey(), "metadata key"), value);
        }
        if (copy.size() > 128) { throw new IllegalArgumentException("too many metadata entries"); }
        return Collections.unmodifiableMap(copy);
    }

    /** @return stable arena identity */ public ArenaId id() { return id; }
    /** @return stable map identity */ public MapId mapId() { return mapId; }
    /** @return mutable-through-copy display name */ public String displayName() { return displayName; }
    /** @return selected active world */ public Optional<WorldKey> world() { return Optional.ofNullable(world); }
    /** @return selected immutable template world */ public Optional<WorldKey> templateWorld() { return Optional.ofNullable(templateWorld); }
    /** @return selected world-provider adapter identity */ public DefinitionId worldAdapter() { return worldAdapter; }
    /** @return arena group */ public DefinitionId group() { return group; }
    /** @return supported modes */ public Set<DefinitionId> modes() { return modes; }
    /** @return minimum players */ public int minimumPlayers() { return minimumPlayers; }
    /** @return maximum players */ public int maximumPlayers() { return maximumPlayers; }
    /** @return players per team */ public int teamSize() { return teamSize; }
    /** @return selector priority */ public int priority() { return priority; }
    /** @return rotation weight */ public int rotationWeight() { return rotationWeight; }
    /** @return waiting-lobby spawn */ public Optional<ArenaLocation> waitingSpawn() { return Optional.ofNullable(waitingSpawn); }
    /** @return spectator spawn */ public Optional<ArenaLocation> spectatorSpawn() { return Optional.ofNullable(spectatorSpawn); }
    /** @return playable bounds */ public Optional<ArenaRegion> bounds() { return Optional.ofNullable(bounds); }
    /** @return void boundary */ public double voidY() { return voidY; }
    /** @return build minimum */ public double buildMinimumY() { return buildMinimumY; }
    /** @return build maximum */ public double buildMaximumY() { return buildMaximumY; }
    /** @return sorted team definitions */ public List<ArenaTeam> teams() { return teams; }
    /** @return sorted generator definitions */ public List<ArenaGenerator> generators() { return generators; }
    /** @return sorted NPC definitions */ public List<ArenaNpc> npcs() { return npcs; }
    /** @return protected regions */ public List<ArenaRegion> protectedRegions() { return protectedRegions; }
    /** @return semantic holograms */ public List<ArenaHologram> holograms() { return holograms; }
    /** @return named duration/speed settings */ public Map<DefinitionId, Duration> speeds() { return speeds; }
    /** @return enabled arena event/rule identities */ public Set<DefinitionId> rules() { return rules; }
    /** @return bounded extension metadata */ public Map<DefinitionId, String> metadata() { return metadata; }
    /** @return lifecycle state */ public Status status() { return status; }
    /** @return creation time */ public Instant createdAt() { return createdAt; }
    /** @return last change time */ public Instant updatedAt() { return updatedAt; }
    /** @return optimistic version */ public long version() { return version; }

    @Override public int hashCode() {
        return Objects.hash(id, mapId, displayName, world, templateWorld, worldAdapter, group, modes,
                minimumPlayers, maximumPlayers, teamSize, priority, rotationWeight, waitingSpawn,
                spectatorSpawn, bounds, voidY, buildMinimumY, buildMaximumY, teams, generators,
                npcs, protectedRegions, holograms, speeds, rules, metadata, status, createdAt,
                updatedAt, version);
    }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaDefinition)) { return false; }
        final ArenaDefinition that = (ArenaDefinition) other;
        return Objects.deepEquals(equalityState(), that.equalityState());
    }

    private Object[] equalityState() {
        return new Object[] {id, mapId, displayName, world, templateWorld, worldAdapter, group,
            modes, minimumPlayers, maximumPlayers, teamSize, priority, rotationWeight,
            waitingSpawn, spectatorSpawn, bounds, voidY, buildMinimumY, buildMaximumY, teams,
            generators, npcs, protectedRegions, holograms, speeds, rules, metadata, status,
            createdAt, updatedAt, version};
    }

    /** Arena configuration lifecycle independent of match state. */
    public enum Status { DRAFT, DISABLED, ENABLED }

    /** Mutable construction helper; built values are immutable and validated. */
    public static final class Builder {
        private ArenaId id;
        private MapId mapId;
        private String displayName;
        private WorldKey world;
        private WorldKey templateWorld;
        private DefinitionId worldAdapter = DefinitionId.of("zartra", "world/native");
        private DefinitionId group = DefinitionId.of("zartra", "group/default");
        private Set<DefinitionId> modes = new TreeSet<DefinitionId>(Collections.singleton(
                DefinitionId.of("zartra", "mode/standard")));
        private int minimumPlayers = 2;
        private int maximumPlayers = 16;
        private int teamSize = 4;
        private int priority;
        private int rotationWeight = 100;
        private ArenaLocation waitingSpawn;
        private ArenaLocation spectatorSpawn;
        private ArenaRegion bounds;
        private double voidY = -16.0D;
        private double buildMinimumY = 0.0D;
        private double buildMaximumY = 256.0D;
        private List<ArenaTeam> teams = new ArrayList<ArenaTeam>();
        private List<ArenaGenerator> generators = new ArrayList<ArenaGenerator>();
        private List<ArenaNpc> npcs = new ArrayList<ArenaNpc>();
        private List<ArenaRegion> protectedRegions = new ArrayList<ArenaRegion>();
        private List<ArenaHologram> holograms = new ArrayList<ArenaHologram>();
        private Map<DefinitionId, Duration> speeds = new TreeMap<DefinitionId, Duration>();
        private Set<DefinitionId> rules = new TreeSet<DefinitionId>();
        private Map<DefinitionId, String> metadata = new TreeMap<DefinitionId, String>();
        private Status status = Status.DRAFT;
        private Instant createdAt;
        private Instant updatedAt;
        private long version;

        private Builder(final ArenaId id, final MapId mapId, final String displayName,
                        final Instant createdAt) {
            this.id = Objects.requireNonNull(id, "id");
            this.mapId = Objects.requireNonNull(mapId, "mapId");
            this.displayName = displayName;
            this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
            this.updatedAt = createdAt;
        }
        private Builder(final ArenaDefinition source) {
            id = source.id;
            mapId = source.mapId;
            displayName = source.displayName;
            world = source.world;
            templateWorld = source.templateWorld;
            worldAdapter = source.worldAdapter;
            group = source.group;
            modes = new TreeSet<DefinitionId>(source.modes);
            minimumPlayers = source.minimumPlayers;
            maximumPlayers = source.maximumPlayers;
            teamSize = source.teamSize;
            priority = source.priority;
            rotationWeight = source.rotationWeight;
            waitingSpawn = source.waitingSpawn;
            spectatorSpawn = source.spectatorSpawn;
            bounds = source.bounds;
            voidY = source.voidY;
            buildMinimumY = source.buildMinimumY;
            buildMaximumY = source.buildMaximumY;
            teams = new ArrayList<ArenaTeam>(source.teams);
            generators = new ArrayList<ArenaGenerator>(source.generators);
            npcs = new ArrayList<ArenaNpc>(source.npcs);
            protectedRegions = new ArrayList<ArenaRegion>(source.protectedRegions);
            holograms = new ArrayList<ArenaHologram>(source.holograms);
            speeds = new TreeMap<DefinitionId, Duration>(source.speeds);
            rules = new TreeSet<DefinitionId>(source.rules);
            metadata = new TreeMap<DefinitionId, String>(source.metadata);
            status = source.status;
            createdAt = source.createdAt;
            updatedAt = source.updatedAt;
            version = source.version;
        }

        /** @return this builder */
        public Builder identity(final ArenaId value, final MapId map) {
            id = value;
            mapId = map;
            return this;
        }
        /** @return this builder */
        public Builder displayName(final String value) {
            displayName = value;
            return this;
        }
        /** @return this builder */
        public Builder worlds(final WorldKey active, final WorldKey template) {
            world = active;
            templateWorld = template;
            return this;
        }
        /** @return this builder */
        public Builder worldAdapter(final DefinitionId value) {
            worldAdapter = value;
            return this;
        }
        /** @return this builder */
        public Builder group(final DefinitionId value) {
            group = value;
            return this;
        }
        /** @return this builder */
        public Builder modes(final Set<DefinitionId> value) {
            modes = new TreeSet<DefinitionId>(value);
            return this;
        }
        /** @return this builder */
        public Builder playerLimits(final int minimum, final int maximum, final int perTeam) {
            minimumPlayers = minimum;
            maximumPlayers = maximum;
            teamSize = perTeam;
            return this;
        }
        /** @return this builder */
        public Builder selection(final int value, final int weight) {
            priority = value;
            rotationWeight = weight;
            return this;
        }
        /** @return this builder */
        public Builder waitingSpawn(final ArenaLocation value) {
            waitingSpawn = value;
            return this;
        }
        /** @return this builder */
        public Builder spectatorSpawn(final ArenaLocation value) {
            spectatorSpawn = value;
            return this;
        }
        /** @return this builder */
        public Builder bounds(final ArenaRegion value) {
            bounds = value;
            return this;
        }
        /** @return this builder */
        public Builder limits(final double voidLimit, final double buildMinimum,
                              final double buildMaximum) {
            voidY = voidLimit;
            buildMinimumY = buildMinimum;
            buildMaximumY = buildMaximum;
            return this;
        }
        /** @return this builder */
        public Builder teams(final List<ArenaTeam> value) {
            teams = new ArrayList<ArenaTeam>(value);
            return this;
        }
        /** @return this builder */
        public Builder generators(final List<ArenaGenerator> value) {
            generators = new ArrayList<ArenaGenerator>(value);
            return this;
        }
        /** @return this builder */
        public Builder npcs(final List<ArenaNpc> value) {
            npcs = new ArrayList<ArenaNpc>(value);
            return this;
        }
        /** @return this builder */
        public Builder protectedRegions(final List<ArenaRegion> value) {
            protectedRegions = new ArrayList<ArenaRegion>(value);
            return this;
        }
        /** @return this builder */
        public Builder holograms(final List<ArenaHologram> value) {
            holograms = new ArrayList<ArenaHologram>(value);
            return this;
        }
        /** @return this builder */
        public Builder speeds(final Map<DefinitionId, Duration> value) {
            speeds = new TreeMap<DefinitionId, Duration>(value);
            return this;
        }
        /** @return this builder */
        public Builder rules(final Set<DefinitionId> value) {
            rules = new TreeSet<DefinitionId>(value);
            return this;
        }
        /** @return this builder */
        public Builder metadata(final Map<DefinitionId, String> value) {
            metadata = new TreeMap<DefinitionId, String>(value);
            return this;
        }
        /** @return this builder */
        public Builder status(final Status value) {
            status = value;
            return this;
        }
        /** @return this builder */
        public Builder revision(final long value, final Instant changedAt) {
            version = value;
            updatedAt = changedAt;
            return this;
        }
        /** @return validated immutable arena definition */
        public ArenaDefinition build() {
            return new ArenaDefinition(this);
        }
    }
}
