package io.zartra.bedwars.arena.setup;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaHologram;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** One typed, deterministic mutation of an immutable arena setup draft. */
public final class SetupMutation {
    private final Kind kind;
    private final Object primary;
    private final Object secondary;
    private final Object tertiary;

    private SetupMutation(final Kind kind, final Object primary, final Object secondary,
                          final Object tertiary) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
    }

    /** @return mutation selecting active/template worlds and their adapter */
    public static SetupMutation worlds(final WorldKey active, final WorldKey template,
                                       final DefinitionId adapter) {
        return new SetupMutation(Kind.SELECT_WORLD, Objects.requireNonNull(active, "active"),
                Objects.requireNonNull(template, "template"), Objects.requireNonNull(adapter, "adapter"));
    }
    /** @return mutation configuring the waiting spawn */
    public static SetupMutation waitingSpawn(final ArenaLocation value) { return value(Kind.SET_WAITING_SPAWN, value); }
    /** @return mutation configuring the spectator spawn */
    public static SetupMutation spectatorSpawn(final ArenaLocation value) { return value(Kind.SET_SPECTATOR_SPAWN, value); }
    /** @return mutation configuring playable bounds */
    public static SetupMutation bounds(final ArenaRegion value) { return value(Kind.SET_BOUNDS, value); }
    /** @return mutation configuring void and build limits */
    public static SetupMutation limits(final double voidY, final double minimumY,
                                       final double maximumY) {
        return value(Kind.SET_LIMITS, new Limits(voidY, minimumY, maximumY));
    }
    /** @return mutation adding or replacing a team */
    public static SetupMutation team(final ArenaTeam value) { return value(Kind.UPSERT_TEAM, value); }
    /** @return mutation removing a team and its owned generator/NPC definitions */
    public static SetupMutation removeTeam(final DefinitionId teamId) { return value(Kind.REMOVE_TEAM, teamId); }
    /** @return mutation configuring a team spawn */
    public static SetupMutation teamSpawn(final DefinitionId teamId, final ArenaLocation spawn) {
        return pair(Kind.SET_TEAM_SPAWN, teamId, spawn);
    }
    /** @return mutation configuring a team bed and facing */
    public static SetupMutation teamBed(final DefinitionId teamId, final ArenaLocation bed,
                                        final DefinitionId facing) {
        return new SetupMutation(Kind.SET_TEAM_BED, Objects.requireNonNull(teamId, "teamId"),
                Objects.requireNonNull(bed, "bed"), Objects.requireNonNull(facing, "facing"));
    }
    /** @return mutation adding or replacing a team-resource generator */
    public static SetupMutation teamGenerator(final ArenaGenerator value) { return value(Kind.UPSERT_TEAM_GENERATOR, value); }
    /** @return mutation adding or replacing a diamond generator */
    public static SetupMutation diamondGenerator(final ArenaGenerator value) { return value(Kind.UPSERT_DIAMOND_GENERATOR, value); }
    /** @return mutation adding or replacing an emerald generator */
    public static SetupMutation emeraldGenerator(final ArenaGenerator value) { return value(Kind.UPSERT_EMERALD_GENERATOR, value); }
    /** @return mutation adding or replacing an extension-defined custom generator */
    public static SetupMutation customGenerator(final ArenaGenerator value) { return value(Kind.UPSERT_CUSTOM_GENERATOR, value); }
    /** @return mutation removing a generator */
    public static SetupMutation removeGenerator(final DefinitionId id) { return value(Kind.REMOVE_GENERATOR, id); }
    /** @return mutation adding or replacing a shop NPC */
    public static SetupMutation shopNpc(final ArenaNpc value) { return value(Kind.UPSERT_SHOP_NPC, value); }
    /** @return mutation adding or replacing an upgrade NPC */
    public static SetupMutation upgradeNpc(final ArenaNpc value) { return value(Kind.UPSERT_UPGRADE_NPC, value); }
    /** @return mutation removing an NPC */
    public static SetupMutation removeNpc(final DefinitionId id) { return value(Kind.REMOVE_NPC, id); }
    /** @return mutation adding or replacing a protected region */
    public static SetupMutation protectedRegion(final ArenaRegion value) { return value(Kind.UPSERT_PROTECTED_REGION, value); }
    /** @return mutation removing a protected region */
    public static SetupMutation removeProtectedRegion(final DefinitionId id) { return value(Kind.REMOVE_PROTECTED_REGION, id); }
    /** @return mutation replacing group identity */
    public static SetupMutation group(final DefinitionId value) { return value(Kind.SET_GROUP, value); }
    /** @return mutation replacing supported modes */
    public static SetupMutation modes(final Set<DefinitionId> value) { return value(Kind.SET_MODES, new TreeSet<DefinitionId>(value)); }
    /** @return mutation replacing player/team limits */
    public static SetupMutation playerLimits(final int minimum, final int maximum, final int teamSize) {
        return value(Kind.SET_PLAYER_LIMITS, new PlayerLimits(minimum, maximum, teamSize));
    }
    /** @return mutation adding or replacing a named arena speed */
    public static SetupMutation speed(final DefinitionId id, final Duration value) { return pair(Kind.SET_SPEED, id, value); }
    /** @return mutation adding or replacing a semantic hologram */
    public static SetupMutation hologram(final ArenaHologram value) { return value(Kind.UPSERT_HOLOGRAM, value); }
    /** @return mutation adding or replacing bounded extension metadata */
    public static SetupMutation metadata(final DefinitionId id, final String value) { return pair(Kind.SET_METADATA, id, value); }
    /** @return mutation enabling a semantic event/rule identity */
    public static SetupMutation enableRule(final DefinitionId id) { return value(Kind.ENABLE_RULE, id); }
    /** @return mutation disabling a semantic event/rule identity */
    public static SetupMutation disableRule(final DefinitionId id) { return value(Kind.DISABLE_RULE, id); }

    private static SetupMutation value(final Kind kind, final Object value) {
        return new SetupMutation(kind, Objects.requireNonNull(value, "value"), null, null);
    }
    private static SetupMutation pair(final Kind kind, final Object first, final Object second) {
        return new SetupMutation(kind, Objects.requireNonNull(first, "first"),
                Objects.requireNonNull(second, "second"), null);
    }

    /** @return mutation category */
    public Kind kind() {
        return kind;
    }

    /** @return a new immutable bundle with this mutation applied at the supplied time/version */
    public ArenaBundle apply(final ArenaBundle source, final Instant changedAt) {
        Objects.requireNonNull(source, "source");
        final ArenaDefinition.Builder builder = source.arena().toBuilder();
        switch (kind) {
            case SELECT_WORLD:
                builder.worlds((WorldKey) primary, (WorldKey) secondary)
                        .worldAdapter((DefinitionId) tertiary);
                break;
            case SET_WAITING_SPAWN:
                builder.waitingSpawn((ArenaLocation) primary);
                break;
            case SET_SPECTATOR_SPAWN:
                builder.spectatorSpawn((ArenaLocation) primary);
                break;
            case SET_BOUNDS:
                builder.bounds((ArenaRegion) primary);
                break;
            case SET_LIMITS:
                final Limits limits = (Limits) primary;
                builder.limits(limits.voidY, limits.minimumY, limits.maximumY);
                break;
            case UPSERT_TEAM:
                builder.teams(upsert(source.arena().teams(), (ArenaTeam) primary));
                break;
            case REMOVE_TEAM:
                removeTeam(source, builder, (DefinitionId) primary);
                break;
            case SET_TEAM_SPAWN:
                builder.teams(replaceTeam(source, (DefinitionId) primary,
                        team(source, (DefinitionId) primary)
                                .withSpawn((ArenaLocation) secondary)));
                break;
            case SET_TEAM_BED:
                builder.teams(replaceTeam(source, (DefinitionId) primary,
                    team(source, (DefinitionId) primary).withBed((ArenaLocation) secondary,
                            (DefinitionId) tertiary)));
                break;
            case UPSERT_TEAM_GENERATOR:
            case UPSERT_DIAMOND_GENERATOR:
            case UPSERT_EMERALD_GENERATOR:
            case UPSERT_CUSTOM_GENERATOR:
                builder.generators(upsert(source.arena().generators(), (ArenaGenerator) primary));
                break;
            case REMOVE_GENERATOR:
                builder.generators(remove(source.arena().generators(), (DefinitionId) primary));
                break;
            case UPSERT_SHOP_NPC:
            case UPSERT_UPGRADE_NPC:
                builder.npcs(upsert(source.arena().npcs(), (ArenaNpc) primary));
                break;
            case REMOVE_NPC:
                builder.npcs(removeNpcs(source.arena().npcs(), (DefinitionId) primary));
                break;
            case UPSERT_PROTECTED_REGION:
                builder.protectedRegions(upsertRegion(source.arena().protectedRegions(), (ArenaRegion) primary));
                break;
            case REMOVE_PROTECTED_REGION:
                builder.protectedRegions(removeRegions(source.arena().protectedRegions(), (DefinitionId) primary));
                break;
            case SET_GROUP:
                builder.group((DefinitionId) primary);
                break;
            case SET_MODES:
                builder.modes(castSet(primary));
                break;
            case SET_PLAYER_LIMITS:
                final PlayerLimits players = (PlayerLimits) primary;
                builder.playerLimits(players.minimum, players.maximum, players.teamSize);
                break;
            case SET_SPEED:
                final Map<DefinitionId, Duration> speeds = new TreeMap<DefinitionId, Duration>(source.arena().speeds());
                speeds.put((DefinitionId) primary, (Duration) secondary);
                builder.speeds(speeds);
                break;
            case UPSERT_HOLOGRAM:
                builder.holograms(upsert(source.arena().holograms(), (ArenaHologram) primary));
                break;
            case SET_METADATA:
                final Map<DefinitionId, String> metadata = new TreeMap<DefinitionId, String>(source.arena().metadata());
                metadata.put((DefinitionId) primary, (String) secondary);
                builder.metadata(metadata);
                break;
            case ENABLE_RULE:
            case DISABLE_RULE:
                final Set<DefinitionId> rules = new TreeSet<DefinitionId>(source.arena().rules());
                if (kind == Kind.ENABLE_RULE) {
                    rules.add((DefinitionId) primary);
                } else {
                    rules.remove(primary);
                }
                builder.rules(rules);
                break;
            default:
                throw new IllegalStateException("unhandled setup mutation: " + kind);
        }
        builder.revision(source.arena().version() + 1L, Objects.requireNonNull(changedAt, "changedAt"));
        return new ArenaBundle(builder.build(), source.map());
    }

    private static ArenaTeam team(final ArenaBundle source, final DefinitionId id) {
        for (ArenaTeam value : source.arena().teams()) {
            if (value.id().equals(id)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown team identity");
    }
    private static List<ArenaTeam> replaceTeam(final ArenaBundle source, final DefinitionId id,
        final ArenaTeam replacement) {
        final List<ArenaTeam> values = new ArrayList<ArenaTeam>();
        for (ArenaTeam value : source.arena().teams()) {
            values.add(value.id().equals(id) ? replacement : value);
        }
        return values;
    }
    private static void removeTeam(final ArenaBundle source, final ArenaDefinition.Builder builder,
                                   final DefinitionId id) {
        final List<ArenaTeam> teams = new ArrayList<ArenaTeam>();
        for (ArenaTeam value : source.arena().teams()) {
            if (!value.id().equals(id)) {
                teams.add(value);
            }
        }
        final List<ArenaGenerator> generators = new ArrayList<ArenaGenerator>();
        for (ArenaGenerator value : source.arena().generators()) {
            if (!value.teamId().isPresent() || !value.teamId().get().equals(id)) {
                generators.add(value);
            }
        }
        final List<ArenaNpc> npcs = new ArrayList<ArenaNpc>();
        for (ArenaNpc value : source.arena().npcs()) {
            if (!value.teamId().isPresent() || !value.teamId().get().equals(id)) {
                npcs.add(value);
            }
        }
        builder.teams(teams).generators(generators).npcs(npcs);
    }
    private static <T extends Comparable<? super T>> List<T> upsert(final List<T> source,
                                                                    final T replacement) {
        final List<T> values = new ArrayList<T>();
        boolean replaced = false;
        for (T value : source) {
            if (value.compareTo(replacement) == 0) {
                values.add(replacement);
                replaced = true;
            } else {
                values.add(value);
            }
        }
        if (!replaced) {
            values.add(replacement);
        }
        return values;
    }
    private static List<ArenaGenerator> remove(final List<ArenaGenerator> source, final DefinitionId id) {
        final List<ArenaGenerator> values = new ArrayList<ArenaGenerator>();
        for (ArenaGenerator value : source) {
            if (!value.id().equals(id)) {
                values.add(value);
            }
        }
        return values;
    }
    private static List<ArenaNpc> removeNpcs(final List<ArenaNpc> source, final DefinitionId id) {
        final List<ArenaNpc> values = new ArrayList<ArenaNpc>();
        for (ArenaNpc value : source) {
            if (!value.id().equals(id)) {
                values.add(value);
            }
        }
        return values;
    }
    private static List<ArenaRegion> upsertRegion(final List<ArenaRegion> source,
                                                  final ArenaRegion replacement) {
        final List<ArenaRegion> values = new ArrayList<ArenaRegion>();
        boolean replaced = false;
        for (ArenaRegion value : source) {
            if (value.id().equals(replacement.id())) {
                values.add(replacement);
                replaced = true;
            } else {
                values.add(value);
            }
        }
        if (!replaced) {
            values.add(replacement);
        }
        return values;
    }
    private static List<ArenaRegion> removeRegions(final List<ArenaRegion> source,
        final DefinitionId id) {
        final List<ArenaRegion> values = new ArrayList<ArenaRegion>();
        for (ArenaRegion value : source) {
            if (!value.id().equals(id)) {
                values.add(value);
            }
        }
        return values;
    }
    @SuppressWarnings("unchecked")
    private static Set<DefinitionId> castSet(final Object value) {
        return (Set<DefinitionId>) value;
    }

    /** Atomic setup step identity; presentation bindings remain M09. */
    public enum Kind {
        SELECT_WORLD, SET_WAITING_SPAWN, SET_SPECTATOR_SPAWN, SET_BOUNDS, SET_LIMITS,
        UPSERT_TEAM, REMOVE_TEAM, SET_TEAM_SPAWN, SET_TEAM_BED, UPSERT_TEAM_GENERATOR,
        UPSERT_DIAMOND_GENERATOR, UPSERT_EMERALD_GENERATOR, UPSERT_CUSTOM_GENERATOR,
        REMOVE_GENERATOR, UPSERT_SHOP_NPC, UPSERT_UPGRADE_NPC, REMOVE_NPC,
        UPSERT_PROTECTED_REGION, REMOVE_PROTECTED_REGION, SET_GROUP, SET_MODES,
        SET_PLAYER_LIMITS, SET_SPEED, UPSERT_HOLOGRAM, SET_METADATA, ENABLE_RULE, DISABLE_RULE
    }

    private static final class Limits {
        private final double voidY;
        private final double minimumY;
        private final double maximumY;
        private Limits(final double voidY, final double minimumY, final double maximumY) {
            this.voidY = voidY;
            this.minimumY = minimumY;
            this.maximumY = maximumY;
        }
    }
    private static final class PlayerLimits {
        private final int minimum;
        private final int maximum;
        private final int teamSize;
        private PlayerLimits(final int minimum, final int maximum, final int teamSize) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.teamSize = teamSize;
        }
    }
}
