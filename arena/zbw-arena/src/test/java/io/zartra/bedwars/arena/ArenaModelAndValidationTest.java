package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.arena.application.ArenaPolicy;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaHologram;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.arena.setup.MarkerProposal;
import io.zartra.bedwars.arena.setup.SetupMutation;
import io.zartra.bedwars.arena.setup.SetupPreview;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import io.zartra.bedwars.arena.setup.SetupToolDefinition;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArenaModelAndValidationTest {
    @Test void completeFixtureIsImmutableEqualAndMayEnable() {
        final ArenaBundle bundle = ArenaTestFixture.complete();
        final ArenaBundle equal = ArenaTestFixture.complete();
        assertEquals(bundle, equal);
        assertEquals(bundle.hashCode(), equal.hashCode());
        assertEquals(bundle.arenaId(), bundle.arena().id());
        assertEquals(bundle.mapId(), bundle.map().id());
        assertTrue(new ArenaValidation.DefaultValidator().validate(bundle).mayEnable());
        assertThrows(UnsupportedOperationException.class, () -> bundle.arena().teams().clear());
        assertThrows(UnsupportedOperationException.class, () -> bundle.map().metadata().clear());
    }

    @Test void valueTypesNormalizeValidateAndCompare() {
        final ArenaLocation location = ArenaLocation.of(1, 2, 3, 270, -45);
        assertEquals(-90, location.yaw());
        assertEquals(-45, location.pitch());
        assertEquals(location, ArenaLocation.of(1, 2, 3, -90, -45));
        assertThrows(IllegalArgumentException.class, () -> ArenaLocation.of(Double.NaN, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ArenaLocation.of(0, 0, 0, 0, 91));
        final ArenaRegion region = ArenaRegion.between(ArenaTestFixture.id("region/test"),
                ArenaTestFixture.location(10, 10, 10), ArenaTestFixture.location(0, 0, 0));
        assertTrue(region.contains(ArenaTestFixture.location(5, 5, 5)));
        assertFalse(region.contains(ArenaTestFixture.location(11, 5, 5)));
        assertEquals(0, region.minimum().x());
    }

    @Test void modelConstructorsRejectMalformedAndDuplicateData() {
        assertThrows(IllegalArgumentException.class, () -> ArenaTeam.create(
                ArenaTestFixture.RED, "", ArenaTestFixture.id("color/red")));
        assertThrows(IllegalArgumentException.class, () -> ArenaGenerator.of(
                ArenaTestFixture.id("generator/x"), GeneratorTypeId.of("zartra", "custom"),
                ResourceId.of("zartra", "custom"), null, ArenaTestFixture.location(1, 1, 1),
                Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new ArenaHologram(
                ArenaTestFixture.id("hologram/x"), ArenaTestFixture.location(1, 1, 1),
                Collections.<DefinitionId>emptyList()));
        final ArenaBundle source = ArenaTestFixture.complete();
        assertThrows(IllegalArgumentException.class, () -> source.arena().toBuilder()
                .teams(Arrays.asList(source.arena().teams().get(0), source.arena().teams().get(0)))
                .build());
        assertThrows(IllegalArgumentException.class, () -> source.arena().toBuilder()
                .playerLimits(0, 1, 1).build());
        assertThrows(IllegalArgumentException.class, () -> source.arena().toBuilder()
                .limits(0, 10, 5).build());
        assertThrows(IllegalArgumentException.class, () -> new ArenaBundle(source.arena(),
                new io.zartra.bedwars.arena.model.MapDefinition(
                        io.zartra.bedwars.api.identity.MapId.random(), "Other",
                        ArenaTestFixture.NOW, ArenaTestFixture.NOW, 0,
                        ArenaTestFixture.id("template/x"), ArenaTestFixture.id("group/x"),
                        ArenaTestFixture.id("author/x"), "", Collections.singleton(
                        ArenaTestFixture.id("mode/x")), 1, 2, Collections.<DefinitionId>emptySet(),
                        Collections.<DefinitionId, String>emptyMap(),
                        ArenaTestFixture.id("validation/pending"))));
    }

    @Test void validatorReportsEveryBlockingCategory() {
        final ArenaBundle valid = ArenaTestFixture.complete();
        final ArenaDefinition invalid = valid.arena().toBuilder().worlds(null, null)
                .waitingSpawn(null).spectatorSpawn(null).bounds(null)
                .teams(Collections.singletonList(ArenaTeam.create(ArenaTestFixture.RED, "Red",
                        ArenaTestFixture.id("color/red"))))
                .generators(Collections.<ArenaGenerator>emptyList())
                .npcs(Collections.<ArenaNpc>emptyList()).playerLimits(2, 8, 2).build();
        final ArenaValidation.Report report = new ArenaValidation.DefaultValidator().validate(
                new ArenaBundle(invalid, valid.map()));
        assertFalse(report.mayEnable());
        assertTrue(report.issues().size() >= 8);
        assertEquals(ArenaValidation.Severity.ERROR, report.issues().get(0).severity());
        assertThrows(IllegalArgumentException.class, () -> new ArenaValidation.Issue(
                ArenaTestFixture.id("validation/x"), ArenaValidation.Severity.INFO,
                "unsafe path!", "message.key"));
    }

    @Test void everySetupMutationProducesIndependentDraftState() {
        final ArenaBundle original = ArenaTestFixture.complete();
        ArenaBundle changed = original;
        final DefinitionId green = ArenaTestFixture.id("team/green");
        final ArenaTeam team = ArenaTeam.create(green, "Green", ArenaTestFixture.id("color/green"));
        final ArenaGenerator custom = ArenaGenerator.of(ArenaTestFixture.id("generator/custom"),
                GeneratorTypeId.of("example", "custom"), ResourceId.of("example", "ruby"), green,
                ArenaTestFixture.location(25, 10, 25), Duration.ofSeconds(2));
        final ArenaNpc shop = ArenaNpc.of(ArenaTestFixture.id("npc/green/shop"),
                ArenaNpc.Kind.SHOP, green, ArenaTestFixture.location(26, 10, 25));
        final ArenaRegion protectedRegion = ArenaRegion.between(ArenaTestFixture.id("region/new"),
                ArenaTestFixture.location(20, 1, 20), ArenaTestFixture.location(22, 5, 22));
        final SetupMutation[] mutations = {
            SetupMutation.worlds(WorldKey.of("new_world"), WorldKey.of("new_template"),
                    ArenaTestFixture.id("world/custom")),
            SetupMutation.waitingSpawn(ArenaTestFixture.location(51, 20, 50)),
            SetupMutation.spectatorSpawn(ArenaTestFixture.location(51, 30, 50)),
            SetupMutation.bounds(ArenaRegion.between(ArenaTestFixture.id("region/new_bounds"),
                    ArenaTestFixture.location(0, 0, 0), ArenaTestFixture.location(110, 110, 110))),
            SetupMutation.limits(-5, 0, 110), SetupMutation.team(team),
            SetupMutation.teamSpawn(green, ArenaTestFixture.location(20, 10, 20)),
            SetupMutation.teamBed(green, ArenaTestFixture.location(22, 10, 20),
                    ArenaTestFixture.id("facing/north")),
            SetupMutation.teamGenerator(custom), SetupMutation.diamondGenerator(custom),
            SetupMutation.emeraldGenerator(custom), SetupMutation.customGenerator(custom),
            SetupMutation.shopNpc(shop), SetupMutation.upgradeNpc(shop),
            SetupMutation.protectedRegion(protectedRegion),
            SetupMutation.group(ArenaTestFixture.id("group/new")),
            SetupMutation.modes(new HashSet<DefinitionId>(Arrays.asList(
                    ArenaTestFixture.id("mode/standard"), ArenaTestFixture.id("mode/rush")))),
            SetupMutation.playerLimits(2, 6, 2),
            SetupMutation.speed(ArenaTestFixture.id("speed/custom"), Duration.ofSeconds(5)),
            SetupMutation.hologram(new ArenaHologram(ArenaTestFixture.id("hologram/new"),
                    ArenaTestFixture.location(25, 15, 25), Collections.singletonList(
                    ArenaTestFixture.id("message/new")))),
            SetupMutation.metadata(ArenaTestFixture.id("metadata/new"), "value"),
            SetupMutation.enableRule(ArenaTestFixture.id("rule/new")),
            SetupMutation.disableRule(ArenaTestFixture.id("rule/new")),
            SetupMutation.removeNpc(shop.id()), SetupMutation.removeGenerator(custom.id()),
            SetupMutation.removeProtectedRegion(protectedRegion.id()), SetupMutation.removeTeam(green)
        };
        for (SetupMutation mutation : mutations) {
            changed = mutation.apply(changed, ArenaTestFixture.NOW.plusSeconds(1));
        }
        assertNotEquals(original, changed);
        assertEquals("Original", original.arena().displayName());
        assertFalse(changed.arena().rules().contains(ArenaTestFixture.id("rule/new")));
        assertFalse(changed.arena().teams().stream().anyMatch(value -> value.id().equals(green)));
        assertThrows(IllegalArgumentException.class, () -> SetupMutation.teamSpawn(
                ArenaTestFixture.id("team/missing"), ArenaTestFixture.location(0, 1, 0))
                .apply(original, ArenaTestFixture.NOW));
    }

    @Test void setupHistoryPreviewProposalAndToolContractsAreRevisionBound() {
        final SetupSessionId id = SetupSessionId.of(UUID.fromString(
                "50000000-0000-0000-0000-000000000001"));
        SetupSession session = SetupSession.begin(id, ArenaTestFixture.actor(), 1,
                ArenaTestFixture.complete());
        assertFalse(session.canUndo());
        session = session.mutate(SetupMutation.metadata(ArenaTestFixture.id("metadata/a"), "a"),
                ArenaTestFixture.NOW);
        assertTrue(session.canUndo());
        final SetupSession undone = session.undo();
        assertTrue(undone.canRedo());
        final SetupSession redone = undone.redo();
        assertEquals(session.draft(), redone.draft());
        final SetupPreview preview = new SetupPreview(id, redone.draftRevision(), redone.draft(),
                new ArenaValidation.DefaultValidator().validate(redone.draft()));
        assertTrue(preview.matches(redone));
        assertFalse(preview.matches(redone.mutate(SetupMutation.metadata(
                ArenaTestFixture.id("metadata/b"), "b"), ArenaTestFixture.NOW)));
        final MarkerProposal proposal = new MarkerProposal(id, redone.draftRevision(),
                Collections.singletonList(SetupMutation.metadata(
                        ArenaTestFixture.id("metadata/c"), "c")));
        assertTrue(proposal.matches(redone));
        assertThrows(IllegalArgumentException.class, () -> new MarkerProposal(id, 0,
                Collections.<SetupMutation>emptyList()));
        final SetupToolDefinition tool = new SetupToolDefinition(
                ArenaTestFixture.id("setup/tool"), SetupMutation.Kind.SET_BOUNDS, 2,
                ArenaTestFixture.id("message/tool"), ArenaTestFixture.id("message/tool_help"));
        assertEquals(2, tool.slot());
        assertThrows(IllegalArgumentException.class, () -> new SetupToolDefinition(
                ArenaTestFixture.id("setup/bad"), SetupMutation.Kind.SET_BOUNDS, 9,
                ArenaTestFixture.id("message/tool"), ArenaTestFixture.id("message/tool_help")));
    }

    @Test void configurationPolicyRejectsUnboundedValues() {
        final ArenaPolicy policy = ArenaPolicy.of(100, 10, Duration.ofSeconds(30),
                Duration.ofSeconds(5));
        assertEquals(100, policy.maximumArenas());
        assertThrows(IllegalArgumentException.class, () -> ArenaPolicy.of(0, 1,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> ArenaPolicy.of(1, 0,
                Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> ArenaPolicy.of(1, 1,
                Duration.ZERO, Duration.ofSeconds(1)));
    }
}
