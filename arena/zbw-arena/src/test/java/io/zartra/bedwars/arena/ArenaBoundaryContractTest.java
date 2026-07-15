package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
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
import io.zartra.bedwars.arena.setup.MarkerProposal;
import io.zartra.bedwars.arena.setup.SetupMutation;
import io.zartra.bedwars.arena.setup.SetupPreview;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArenaBoundaryContractTest {
    @Test void locationsAndRegionsRejectEveryUnsafeBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> ArenaLocation.of(Double.NaN, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ArenaLocation.of(0, Double.POSITIVE_INFINITY, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ArenaLocation.of(0, 0, Double.NEGATIVE_INFINITY, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ArenaLocation.of(0, 0, 0, Float.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ArenaLocation.of(0, 0, 0, 0, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> ArenaLocation.of(0, 0, 0, 0, -91));
        assertEquals(179, ArenaLocation.of(0, 0, 0, -181, 0).yaw());
        assertEquals(-180, ArenaLocation.of(0, 0, 0, 180, 0).yaw());

        final ArenaRegion region = ArenaRegion.between(ArenaTestFixture.id("region/boundary"),
                ArenaTestFixture.location(0, 0, 0), ArenaTestFixture.location(10, 10, 10));
        assertFalse(region.contains(ArenaTestFixture.location(-1, 5, 5)));
        assertFalse(region.contains(ArenaTestFixture.location(5, -1, 5)));
        assertFalse(region.contains(ArenaTestFixture.location(5, 5, -1)));
        assertFalse(region.contains(ArenaTestFixture.location(5, 11, 5)));
        assertFalse(region.contains(ArenaTestFixture.location(5, 5, 11)));
        assertThrows(NullPointerException.class, () -> region.contains(null));
        assertThrows(NullPointerException.class, () -> ArenaRegion.between(null,
                ArenaTestFixture.location(0, 0, 0), ArenaTestFixture.location(1, 1, 1)));
        assertNotEquals(region, "region");
        assertNotEquals(ArenaTestFixture.location(0, 0, 0), "location");
    }

    @Test void placementsRejectMalformedIdentityTextAndDuration() {
        assertThrows(IllegalArgumentException.class, () -> ArenaTeam.create(
                ArenaTestFixture.RED, null, ArenaTestFixture.id("color/red")));
        assertThrows(IllegalArgumentException.class, () -> ArenaTeam.create(
                ArenaTestFixture.RED, repeat('x', 49), ArenaTestFixture.id("color/red")));
        assertThrows(IllegalArgumentException.class, () -> ArenaTeam.create(
                ArenaTestFixture.RED, "bad\nname", ArenaTestFixture.id("color/red")));
        assertThrows(IllegalArgumentException.class, () -> ArenaTeam.create(
                ArenaTestFixture.RED, "bad\rname", ArenaTestFixture.id("color/red")));
        final ArenaTeam team = ArenaTeam.create(ArenaTestFixture.RED, "Red",
                ArenaTestFixture.id("color/red"));
        assertThrows(NullPointerException.class, () -> team.withSpawn(null));
        assertThrows(NullPointerException.class, () -> team.withBed(null,
                ArenaTestFixture.id("facing/north")));
        assertThrows(NullPointerException.class, () -> team.withBed(
                ArenaTestFixture.location(1, 1, 1), null));
        assertThrows(NullPointerException.class, () -> team.compareTo(null));
        assertNotEquals(team, "team");

        assertThrows(IllegalArgumentException.class, () -> generator(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> generator(Duration.ofHours(2)));
        assertThrows(NullPointerException.class, () -> ArenaGenerator.of(null,
                GeneratorTypeId.of("zartra", "test"), ResourceId.of("zartra", "test"), null,
                ArenaTestFixture.location(1, 1, 1), Duration.ofSeconds(1)));
        final ArenaGenerator generator = generator(Duration.ofSeconds(1));
        assertThrows(NullPointerException.class, () -> generator.compareTo(null));
        assertNotEquals(generator, "generator");

        assertThrows(NullPointerException.class, () -> ArenaNpc.of(
                ArenaTestFixture.id("npc/test"), null, null,
                ArenaTestFixture.location(1, 1, 1)));
        final ArenaNpc npc = ArenaNpc.of(ArenaTestFixture.id("npc/test"), ArenaNpc.Kind.SHOP,
                null, ArenaTestFixture.location(1, 1, 1));
        assertThrows(NullPointerException.class, () -> npc.compareTo(null));
        assertNotEquals(npc, "npc");

        final List<DefinitionId> tooManyMessages = new ArrayList<DefinitionId>();
        for (int index = 0; index < 17; index++) {
            tooManyMessages.add(ArenaTestFixture.id("message/" + index));
        }
        assertThrows(IllegalArgumentException.class, () -> new ArenaHologram(
                ArenaTestFixture.id("hologram/test"), ArenaTestFixture.location(1, 1, 1),
                tooManyMessages));
        assertThrows(IllegalArgumentException.class, () -> new ArenaHologram(
                ArenaTestFixture.id("hologram/test"), ArenaTestFixture.location(1, 1, 1),
                Arrays.asList(ArenaTestFixture.id("message/one"), null)));
        final ArenaHologram hologram = new ArenaHologram(ArenaTestFixture.id("hologram/test"),
                ArenaTestFixture.location(1, 1, 1), Collections.singletonList(
                ArenaTestFixture.id("message/one")));
        assertThrows(NullPointerException.class, () -> hologram.compareTo(null));
        assertNotEquals(hologram, "hologram");
    }

    @Test void mapMetadataEnforcesEveryBoundedCollectionAndRevisionRule() {
        final MapDefinition valid = map("Map", ArenaTestFixture.NOW, ArenaTestFixture.NOW, 1,
                1, 4, definitions(1), Collections.<DefinitionId>emptySet(),
                Collections.<DefinitionId, String>emptyMap());
        assertEquals("Renamed", valid.rename("Renamed", ArenaTestFixture.NOW.plusSeconds(1))
                .displayName());
        assertNotEquals(valid, "map");
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW.minusSeconds(1), 1, 1, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, -1, 1, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map(" ", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map(repeat('x', 65),
                ArenaTestFixture.NOW, ArenaTestFixture.NOW, 1, 1, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("bad\rname", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 0, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 4, 3, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 65, definitions(1),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 4, Collections.<DefinitionId>emptySet(),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 4, definitions(65),
                Collections.<DefinitionId>emptySet(), Collections.<DefinitionId, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 4, definitions(1), definitions(65),
                Collections.<DefinitionId, String>emptyMap()));
        final Map<DefinitionId, String> invalidMetadata = new HashMap<DefinitionId, String>();
        invalidMetadata.put(ArenaTestFixture.id("metadata/test"), repeat('x', 1025));
        assertThrows(IllegalArgumentException.class, () -> map("Map", ArenaTestFixture.NOW,
                ArenaTestFixture.NOW, 1, 1, 4, definitions(1),
                Collections.<DefinitionId>emptySet(), invalidMetadata));
    }

    @Test void arenaAggregateEnforcesLimitsCollectionsAndTemporalOrdering() {
        final ArenaDefinition source = ArenaTestFixture.complete().arena();
        assertNotEquals(source, "arena");
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().displayName(" ").build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().displayName(repeat('x', 65)).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().displayName("bad\nname").build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().playerLimits(2, 257, 2).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().playerLimits(2, 4, 5).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().selection(-100001, 1).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().selection(100001, 1).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().selection(0, 0).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().selection(0, 100001).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().limits(Double.NaN, 0, 1).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().limits(0, Double.POSITIVE_INFINITY, 1).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().limits(0, 0, Double.NEGATIVE_INFINITY).build());
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().modes(Collections.<DefinitionId>emptySet()).build());
        final List<ArenaTeam> tooManyTeams = new ArrayList<ArenaTeam>();
        for (int index = 0; index < 65; index++) {
            tooManyTeams.add(ArenaTeam.create(ArenaTestFixture.id("team/" + index),
                    "Team " + index, ArenaTestFixture.id("color/test")));
        }
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().teams(tooManyTeams).build());
        final List<ArenaTeam> nullTeam = new ArrayList<ArenaTeam>(source.teams());
        nullTeam.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().teams(nullTeam).build());
        final Map<DefinitionId, Duration> speeds = new HashMap<DefinitionId, Duration>();
        speeds.put(ArenaTestFixture.id("speed/invalid"), Duration.ofDays(2));
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().speeds(speeds).build());
        final Map<DefinitionId, String> metadata = new HashMap<DefinitionId, String>();
        metadata.put(ArenaTestFixture.id("metadata/invalid"), "bad\rvalue");
        assertThrows(IllegalArgumentException.class,
                () -> source.toBuilder().metadata(metadata).build());
        assertThrows(IllegalArgumentException.class, () -> source.toBuilder()
                .revision(-1, ArenaTestFixture.NOW).build());
        assertThrows(IllegalArgumentException.class, () -> source.toBuilder()
                .revision(2, ArenaTestFixture.NOW.minusSeconds(1)).build());
        assertThrows(NullPointerException.class,
                () -> source.toBuilder().status(null).build());
    }

    @Test void setupValuesAreCanonicalRevisionBoundAndTerminal() {
        final String canonical = "abcdef00-0000-0000-0000-000000000001";
        final SetupSessionId sessionId = SetupSessionId.parse(canonical);
        assertEquals(sessionId, SetupSessionId.of(UUID.fromString(canonical)));
        assertNotEquals(sessionId, SetupSessionId.random());
        assertNotEquals(sessionId, "session");
        assertThrows(IllegalArgumentException.class, () -> SetupSessionId.parse(null));
        assertThrows(IllegalArgumentException.class,
                () -> SetupSessionId.parse(canonical.toUpperCase(java.util.Locale.ROOT)));

        final SetupSession initial = SetupSession.begin(sessionId, ArenaTestFixture.actor(), 1,
                ArenaTestFixture.complete());
        assertThrows(IllegalStateException.class, initial::undo);
        assertThrows(IllegalStateException.class, initial::redo);
        final SetupSession terminal = initial.committed();
        assertThrows(IllegalStateException.class, () -> terminal.mutate(
                SetupMutation.metadata(ArenaTestFixture.id("metadata/x"), "x"),
                ArenaTestFixture.NOW));
        assertThrows(IllegalStateException.class, terminal::committed);
        final ArenaBundle source = ArenaTestFixture.complete();
        final ArenaBundle anotherArena = new ArenaBundle(source.arena().toBuilder()
                .identity(io.zartra.bedwars.api.identity.ArenaId.random(), source.mapId())
                .build(), source.map());
        assertThrows(IllegalArgumentException.class, () -> initial.applyPreview(anotherArena));

        final MarkerProposal proposal = new MarkerProposal(sessionId, 0,
                Collections.singletonList(SetupMutation.metadata(
                        ArenaTestFixture.id("metadata/x"), "x")));
        assertFalse(proposal.matches(SetupSession.begin(SetupSessionId.random(),
                ArenaTestFixture.actor(), 1, ArenaTestFixture.complete())));
        assertThrows(IllegalArgumentException.class, () -> new MarkerProposal(sessionId, -1,
                Collections.singletonList(SetupMutation.metadata(
                        ArenaTestFixture.id("metadata/x"), "x"))));
        assertThrows(IllegalArgumentException.class, () -> new MarkerProposal(sessionId, 0,
                Arrays.asList(SetupMutation.metadata(ArenaTestFixture.id("metadata/x"), "x"),
                        null)));

        final ArenaValidation.Report report = new ArenaValidation.Report(
                Collections.<ArenaValidation.Issue>emptyList());
        final SetupPreview preview = new SetupPreview(sessionId, 0, ArenaTestFixture.complete(),
                report);
        assertTrue(preview.matches(initial));
        assertFalse(preview.matches(initial.mutate(SetupMutation.metadata(
                ArenaTestFixture.id("metadata/x"), "x"), ArenaTestFixture.NOW)));
        assertThrows(IllegalArgumentException.class, () -> new SetupPreview(sessionId, -1,
                ArenaTestFixture.complete(), report));
    }

    private static ArenaGenerator generator(final Duration interval) {
        return ArenaGenerator.of(ArenaTestFixture.id("generator/test"),
                GeneratorTypeId.of("zartra", "test"), ResourceId.of("zartra", "test"), null,
                ArenaTestFixture.location(1, 1, 1), interval);
    }

    private static MapDefinition map(final String name, final Instant created,
                                     final Instant updated, final long version,
                                     final int minimumTeamSize, final int maximumTeamSize,
                                     final Set<DefinitionId> modes, final Set<DefinitionId> tags,
                                     final Map<DefinitionId, String> metadata) {
        return new MapDefinition(ArenaTestFixture.MAP_ID, name, created, updated, version,
                ArenaTestFixture.id("template/test"), ArenaTestFixture.id("group/test"),
                ArenaTestFixture.id("author/test"), "description", modes, minimumTeamSize,
                maximumTeamSize, tags, metadata, ArenaTestFixture.id("validation/test"));
    }

    private static Set<DefinitionId> definitions(final int count) {
        final Set<DefinitionId> result = new HashSet<DefinitionId>();
        for (int index = 0; index < count; index++) {
            result.add(ArenaTestFixture.id("value/" + index));
        }
        return result;
    }

    private static String repeat(final char value, final int count) {
        final char[] result = new char[count];
        Arrays.fill(result, value);
        return new String(result);
    }
}
