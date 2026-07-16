package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefaultProfile;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.arena.validation.ArenaValidationProfile;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class M081ArenaHardeningTest {
    @Test void standardProfileUsesExactTypedDiamondAndEmeraldRequirements() {
        final ArenaBundle source = ArenaTestFixture.complete();
        assertTrue(new ArenaValidation.DefaultValidator().validate(source).mayEnable());
        final java.util.List<ArenaGenerator> changed =
                new java.util.ArrayList<ArenaGenerator>(source.arena().generators());
        for (int index = 0; index < changed.size(); index++) {
            final ArenaGenerator generator = changed.get(index);
            if (generator.type().equals(GeneratorTypeId.of("zartra", "diamond"))) {
                changed.set(index, ArenaGenerator.of(generator.id(),
                        GeneratorTypeId.of("example", "diamond-compatible"),
                        generator.resource(), null, generator.location(), generator.interval()));
            }
        }
        final ArenaBundle heuristicOnly = new ArenaBundle(
                source.arena().toBuilder().generators(changed).build(), source.map());
        final ArenaValidation.Report report =
                new ArenaValidation.DefaultValidator().validate(heuristicOnly);
        assertFalse(report.mayEnable());
        assertTrue(report.issues().stream().anyMatch(issue ->
                issue.field().equals("generators.required.zartra/diamond")));
    }

    @Test void customProfileAllowsArbitraryTypedPrerequisites() {
        final ArenaBundle source = ArenaTestFixture.complete();
        final ArenaGenerator ruby = ArenaGenerator.of(ArenaTestFixture.id("generator/ruby"),
                GeneratorTypeId.of("example", "ruby"), ResourceId.of("example", "ruby"),
                null, ArenaTestFixture.location(40, 10, 50), Duration.ofSeconds(3));
        final ArenaBundle custom = new ArenaBundle(source.arena().toBuilder()
                .generators(Collections.singletonList(ruby))
                .npcs(Collections.<ArenaNpc>emptyList()).build(), source.map());
        final ArenaValidationProfile profile = ArenaValidationProfile.of(
                ArenaTestFixture.id("arena-validation/ruby"),
                Collections.singleton(GeneratorTypeId.of("example", "ruby")),
                false, false, false);
        final ArenaValidation.DefaultValidator validator =
                new ArenaValidation.DefaultValidator(profile);
        assertEquals(profile, validator.profile());
        assertEquals(profile.hashCode(), validator.profile().hashCode());
        assertTrue(validator.validate(custom).mayEnable());
        assertThrows(UnsupportedOperationException.class,
                () -> profile.requiredSharedGeneratorTypes().clear());
    }

    @Test void arenaDefaultsAreTypedReplaceableAndCopiedIntoDrafts() {
        final DefinitionId customMode = ArenaTestFixture.id("mode/custom");
        final ArenaDefaultProfile profile = ArenaDefaultProfile.of(
                ArenaTestFixture.id("arena-profile/custom"),
                ArenaTestFixture.id("world/custom"), ArenaTestFixture.id("group/custom"),
                Collections.singleton(customMode), 3, 30, 6, 7, 90,
                -32.0D, -8.0D, 320.0D);
        final ArenaDefinition draft = ArenaDefinition.builder(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.MAP_ID, "Custom", ArenaTestFixture.NOW, profile).build();
        assertEquals(profile.worldAdapter(), draft.worldAdapter());
        assertEquals(profile.group(), draft.group());
        assertEquals(profile.modes(), draft.modes());
        assertEquals(3, draft.minimumPlayers());
        assertEquals(30, draft.maximumPlayers());
        assertEquals(6, draft.teamSize());
        assertEquals(7, draft.priority());
        assertEquals(90, draft.rotationWeight());
        assertEquals(-32.0D, draft.voidY());
        assertEquals(-8.0D, draft.buildMinimumY());
        assertEquals(320.0D, draft.buildMaximumY());
        assertEquals(ArenaDefaultProfile.standard(), ArenaDefaultProfile.standard());
        assertFalse(profile.equals(ArenaDefaultProfile.standard()));
        assertFalse(profile.equals("other"));
        assertEquals(profile, ArenaDefaultProfile.of(profile.id(), profile.worldAdapter(),
                profile.group(), new HashSet<DefinitionId>(profile.modes()), 3, 30, 6, 7,
                90, -32.0D, -8.0D, 320.0D));
    }

    @Test void defaultAndValidationProfilesRejectMalformedBounds() {
        assertThrows(NullPointerException.class, () -> ArenaDefinition.builder(
                ArenaTestFixture.ARENA_ID, ArenaTestFixture.MAP_ID, "Invalid",
                ArenaTestFixture.NOW, null));
        assertThrows(IllegalArgumentException.class, () -> ArenaDefaultProfile.of(
                ArenaTestFixture.id("profile/bad"), ArenaTestFixture.id("world/x"),
                ArenaTestFixture.id("group/x"), Collections.singleton(
                ArenaTestFixture.id("mode/x")), 1, 257, 1, 0, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> ArenaDefaultProfile.of(
                ArenaTestFixture.id("profile/bad"), ArenaTestFixture.id("world/x"),
                ArenaTestFixture.id("group/x"), Collections.<DefinitionId>emptySet(),
                1, 1, 1, 0, 1, -1, 0, 1));
        assertThrows(NullPointerException.class, () -> defaultProfile(null,
                1, 1, 1, 0, 1, -1, 0, 1));
        final java.util.Set<DefinitionId> nullMode = new HashSet<DefinitionId>();
        nullMode.add(null);
        assertThrows(NullPointerException.class, () -> defaultProfile(nullMode,
                1, 1, 1, 0, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(65),
                1, 1, 1, 0, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                0, 1, 1, 0, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                2, 1, 1, 0, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 0, 0, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, -100001, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 100001, 1, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 0, 0, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 0, 100001, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 0, 1, Double.NaN, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 0, 1, -1, Double.POSITIVE_INFINITY, 1));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 0, 1, -1, 0, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> defaultProfile(modes(1),
                1, 1, 1, 0, 1, -1, 2, 1));
        assertThrows(NullPointerException.class, () -> ArenaValidationProfile.of(
                ArenaTestFixture.id("validation/bad"), null, false, false, false));
        final java.util.Set<GeneratorTypeId> nullType =
                new java.util.HashSet<GeneratorTypeId>();
        nullType.add(null);
        assertThrows(NullPointerException.class, () -> ArenaValidationProfile.of(
                ArenaTestFixture.id("validation/bad"), nullType, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> ArenaValidationProfile.of(
                ArenaTestFixture.id("validation/bad"), generatorTypes(65),
                false, false, false));
    }

    @Test void validationProfilesAndCrossDefinitionChecksRemainDeterministic() {
        final ArenaValidationProfile standard = ArenaValidationProfile.standard();
        assertEquals(standard, standard);
        assertFalse(standard.equals("other"));
        assertTrue(standard.teamGeneratorRequired());
        assertTrue(standard.shopNpcRequired());
        assertTrue(standard.upgradeNpcRequired());
        final ArenaValidationProfile different = ArenaValidationProfile.of(
                ArenaTestFixture.id("validation/different"),
                standard.requiredSharedGeneratorTypes(), false, true, false);
        assertFalse(standard.equals(different));
        assertFalse(different.teamGeneratorRequired());
        assertFalse(different.upgradeNpcRequired());
        assertFalse(standard.equals(validationProfile(standard.id(), false, true, true,
                standard.requiredSharedGeneratorTypes())));
        assertFalse(standard.equals(validationProfile(standard.id(), true, false, true,
                standard.requiredSharedGeneratorTypes())));
        assertFalse(standard.equals(validationProfile(standard.id(), true, true, false,
                standard.requiredSharedGeneratorTypes())));
        assertFalse(standard.equals(validationProfile(ArenaTestFixture.id("validation/id"),
                true, true, true, standard.requiredSharedGeneratorTypes())));
        assertFalse(standard.equals(validationProfile(standard.id(), true, true, true,
                Collections.singleton(GeneratorTypeId.of("example", "other")))));
        assertEquals(standard, validationProfile(standard.id(), true, true, true,
                standard.requiredSharedGeneratorTypes()));

        final ArenaBundle source = ArenaTestFixture.complete();
        assertFalse(new ArenaValidation.DefaultValidator().validate(new ArenaBundle(
                source.arena().toBuilder().group(ArenaTestFixture.id("group/other")).build(),
                source.map())).mayEnable());
        assertFalse(new ArenaValidation.DefaultValidator().validate(new ArenaBundle(
                source.arena().toBuilder().modes(Collections.singleton(
                        ArenaTestFixture.id("mode/other"))).build(), source.map())).mayEnable());
        final MapDefinition restrictive = new MapDefinition(source.mapId(), "Restrictive",
                ArenaTestFixture.NOW, ArenaTestFixture.NOW, 1,
                ArenaTestFixture.id("template/standard"), ArenaTestFixture.id("group/default"),
                ArenaTestFixture.id("author/test"), "", Collections.singleton(
                ArenaTestFixture.id("mode/standard")), 3, 4,
                Collections.<DefinitionId>emptySet(),
                Collections.<DefinitionId, String>emptyMap(),
                ArenaTestFixture.id("validation/valid"));
        assertFalse(new ArenaValidation.DefaultValidator().validate(
                new ArenaBundle(source.arena(), restrictive)).mayEnable());
        final ArenaValidationProfile ruby = ArenaValidationProfile.of(
                ArenaTestFixture.id("validation/ruby"), Collections.singleton(
                GeneratorTypeId.of("example", "ruby")), true, true, true);
        assertFalse(new ArenaValidation.DefaultValidator(ruby).validate(source).mayEnable());
    }

    private static java.util.Set<GeneratorTypeId> generatorTypes(final int count) {
        final java.util.Set<GeneratorTypeId> types = new java.util.HashSet<GeneratorTypeId>();
        for (int index = 0; index < count; index++) {
            types.add(GeneratorTypeId.of("example", "type_" + index));
        }
        return types;
    }

    private static java.util.Set<DefinitionId> modes(final int count) {
        final java.util.Set<DefinitionId> values = new java.util.HashSet<DefinitionId>();
        for (int index = 0; index < count; index++) {
            values.add(ArenaTestFixture.id("mode/" + index));
        }
        return values;
    }

    private static ArenaDefaultProfile defaultProfile(
            final java.util.Set<DefinitionId> modes, final int minimum, final int maximum,
            final int teamSize, final int priority, final int weight, final double voidY,
            final double buildMinimum, final double buildMaximum) {
        return ArenaDefaultProfile.of(ArenaTestFixture.id("profile/test"),
                ArenaTestFixture.id("world/test"), ArenaTestFixture.id("group/test"), modes,
                minimum, maximum, teamSize, priority, weight, voidY, buildMinimum,
                buildMaximum);
    }

    private static ArenaValidationProfile validationProfile(
            final DefinitionId id, final boolean team, final boolean shop,
            final boolean upgrade, final java.util.Set<GeneratorTypeId> types) {
        return ArenaValidationProfile.of(id, types, team, shop, upgrade);
    }
}
