package io.zartra.bedwars.arena;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaHologram;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

final class ArenaTestFixture {
    static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");
    static final ArenaId ARENA_ID = ArenaId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    static final MapId MAP_ID = MapId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    static final DefinitionId RED = id("team/red");
    static final DefinitionId BLUE = id("team/blue");

    private ArenaTestFixture() { }

    static ArenaBundle complete() {
        final ArenaTeam red = ArenaTeam.create(RED, "Red", id("color/red"))
                .withSpawn(location(10, 10, 10)).withBed(location(12, 10, 10), id("facing/east"));
        final ArenaTeam blue = ArenaTeam.create(BLUE, "Blue", id("color/blue"))
                .withSpawn(location(90, 10, 90)).withBed(location(88, 10, 90), id("facing/west"));
        final ArenaDefinition arena = ArenaDefinition.builder(ARENA_ID, MAP_ID, "Original", NOW)
                .worlds(WorldKey.of("arena_original"), WorldKey.of("template_original"))
                .worldAdapter(id("world/native")).group(id("group/default"))
                .modes(new HashSet<DefinitionId>(Collections.singleton(id("mode/standard"))))
                .playerLimits(2, 4, 2).selection(5, 100)
                .waitingSpawn(location(50, 20, 50)).spectatorSpawn(location(50, 30, 50))
                .bounds(ArenaRegion.between(id("region/bounds"), location(0, 0, 0),
                        location(100, 100, 100))).limits(-1, 0, 100)
                .teams(Arrays.asList(red, blue))
                .generators(Arrays.asList(
                        generator("generator/red", "team_iron", "iron", RED, 15, 10, 10),
                        generator("generator/blue", "team_iron", "iron", BLUE, 85, 10, 90),
                        generator("generator/diamond", "diamond", "diamond", null, 40, 10, 40),
                        generator("generator/emerald", "emerald", "emerald", null, 60, 10, 60)))
                .npcs(Arrays.asList(
                        npc("npc/red/shop", ArenaNpc.Kind.SHOP, RED, 14, 10, 10),
                        npc("npc/red/upgrade", ArenaNpc.Kind.TEAM_UPGRADE, RED, 16, 10, 10),
                        npc("npc/blue/shop", ArenaNpc.Kind.SHOP, BLUE, 84, 10, 90),
                        npc("npc/blue/upgrade", ArenaNpc.Kind.TEAM_UPGRADE, BLUE, 82, 10, 90)))
                .protectedRegions(Collections.singletonList(ArenaRegion.between(
                        id("region/protected"), location(45, 0, 45), location(55, 20, 55))))
                .holograms(Collections.singletonList(new ArenaHologram(id("hologram/info"),
                        location(50, 12, 51), Arrays.asList(id("message/title"), id("message/help")))))
                .speeds(Collections.singletonMap(id("speed/diamond"), Duration.ofSeconds(30)))
                .rules(new HashSet<DefinitionId>(Collections.singleton(id("rule/bed_break"))))
                .metadata(Collections.singletonMap(id("metadata/origin"), "test"))
                .status(ArenaDefinition.Status.DISABLED).revision(1, NOW).build();
        final MapDefinition map = new MapDefinition(MAP_ID, "Original", NOW, NOW, 1,
                id("template/standard"), id("group/default"), id("author/test"),
                "Complete deterministic map", Collections.singleton(id("mode/standard")),
                1, 4, Collections.singleton(id("tag/standard")),
                Collections.singletonMap(id("metadata/source"), "original"),
                id("validation/valid"));
        return new ArenaBundle(arena, map);
    }

    static AuthorizationSubject actor() {
        return AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER, id("actor/admin"));
    }
    static DefinitionId id(final String path) { return DefinitionId.of("zartra", path); }
    static ArenaLocation location(final double x, final double y, final double z) {
        return ArenaLocation.of(x, y, z, 0, 0);
    }
    private static ArenaGenerator generator(final String id, final String type,
                                            final String resource, final DefinitionId team,
                                            final double x, final double y, final double z) {
        return ArenaGenerator.of(ArenaTestFixture.id(id), GeneratorTypeId.of("zartra", type),
                ResourceId.of("zartra", resource), team, location(x, y, z),
                Duration.ofSeconds(1));
    }
    private static ArenaNpc npc(final String id, final ArenaNpc.Kind kind,
                                final DefinitionId team, final double x, final double y,
                                final double z) {
        return ArenaNpc.of(ArenaTestFixture.id(id), kind, team, location(x, y, z));
    }
}
