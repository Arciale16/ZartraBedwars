package io.zartra.bedwars.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.domain.generator.GenerationMultiplier;
import io.zartra.bedwars.domain.generator.GeneratorDefinition;
import io.zartra.bedwars.domain.generator.ResourceGenerationProfile;
import io.zartra.bedwars.domain.privategame.ResourceScarcity;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class GeneratorAndPrivateGameTest {
    @Test
    void multiplierHasCanonicalSerializationAndBounds() {
        assertEquals("1", GenerationMultiplier.parse("1.000").toString());
        assertEquals(GenerationMultiplier.NORMAL, GenerationMultiplier.of(BigDecimal.ONE));
        assertTrue(GenerationMultiplier.parse("2.5").compareTo(GenerationMultiplier.NORMAL) > 0);
        assertThrows(IllegalArgumentException.class, () -> GenerationMultiplier.parse("-0.1"));
        assertThrows(IllegalArgumentException.class, () -> GenerationMultiplier.parse("not-decimal"));
        assertThrows(IllegalArgumentException.class, () -> GenerationMultiplier.parse(null));
        assertThrows(NullPointerException.class, () -> GenerationMultiplier.of(null));
        assertThrows(IllegalArgumentException.class,
                () -> GenerationMultiplier.parse("12345678901234567890123456789012345"));
        assertThrows(IllegalArgumentException.class, () -> GenerationMultiplier.parse("0.0000000000001"));
        assertEquals(GenerationMultiplier.parse("2.50"), GenerationMultiplier.parse("2.5"));
        assertNotEquals(GenerationMultiplier.parse("2.5"), GenerationMultiplier.parse("2.6"));
        assertNotEquals(GenerationMultiplier.parse("2.5"), "2.5");
    }

    @Test
    void resourceProfileSupportsIndependentNativeAndCustomResources() {
        final ResourceId iron = ResourceId.of("zartra", "iron");
        final ResourceId custom = ResourceId.of("example", "crystal");
        final ResourceGenerationProfile profile = ResourceGenerationProfile.of(Arrays.asList(
                ResourceGenerationProfile.Entry.of(custom, GenerationMultiplier.parse("3")),
                ResourceGenerationProfile.Entry.of(iron, GenerationMultiplier.parse("0.5"))));
        assertEquals(custom, profile.entries().get(0).resourceId());
        assertEquals("3", profile.multiplier(custom).get().toString());
        assertFalse(profile.multiplier(ResourceId.of("zartra", "gold")).isPresent());
        assertThrows(ResourceGenerationProfile.DuplicateResourceException.class,
                () -> ResourceGenerationProfile.of(Arrays.asList(
                        ResourceGenerationProfile.Entry.of(iron, GenerationMultiplier.NORMAL),
                        ResourceGenerationProfile.Entry.of(iron, GenerationMultiplier.NORMAL))));
        assertThrows(UnsupportedOperationException.class,
                () -> profile.entries().add(ResourceGenerationProfile.Entry.of(iron, GenerationMultiplier.NORMAL)));
    }

    @Test
    void generatorAndResourceScarcityContractsRetainStableIds() {
        final GeneratorDefinition generator = GeneratorDefinition.of(GeneratorTypeId.of("example", "crystal"),
                ResourceId.of("example", "crystal"), 1);
        assertEquals("example:crystal", generator.id().toString());
        assertEquals(generator.outputResource().toString(), generator.id().toString());
        assertThrows(IllegalArgumentException.class, () -> GeneratorDefinition.of(generator.id(), generator.outputResource(), 0));
        assertEquals("zartra:resource_scarcity", ResourceScarcity.definition().id().toString());
        assertTrue(ResourceScarcity.definition().supportsCustomResources());
        assertEquals(5, ResourceScarcity.Preset.values().length);
        assertEquals("zartra:resource_scarcity/scarce", ResourceScarcity.Preset.SCARCE.id().toString());
        final ResourceGenerationProfile empty = ResourceGenerationProfile.of(Collections.emptyList());
        assertEquals(ResourceScarcity.Preset.NORMAL, ResourceScarcity.Settings.preset(
                ResourceScarcity.Preset.NORMAL, empty).preset().get());
        assertFalse(ResourceScarcity.Settings.custom(empty).preset().isPresent());
    }
}
