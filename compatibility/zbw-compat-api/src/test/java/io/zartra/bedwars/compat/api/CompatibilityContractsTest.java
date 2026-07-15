package io.zartra.bedwars.compat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class CompatibilityContractsTest {
    private static final SemanticKey MATERIAL = SemanticKey.of(SemanticKey.Kind.MATERIAL,
            DefinitionId.of("zartra", "material/team_block"));
    private static final SemanticKey SOUND = SemanticKey.of(SemanticKey.Kind.SOUND,
            DefinitionId.of("zartra", "sound/confirm"));
    private static final CompatibilityMapping MAPPING = new CompatibilityMapping(MATERIAL,
            DefinitionId.of("zartra", "renderer/material"), "WHITE_WOOL");

    @Test void semanticKeysAreTypedOrderedAndValidated() {
        assertEquals(MATERIAL, SemanticKey.of(SemanticKey.Kind.MATERIAL,
                DefinitionId.of("zartra", "material/team_block")));
        assertNotEquals(MATERIAL, SemanticKey.of(SemanticKey.Kind.ITEM, MATERIAL.id()));
        assertTrue(MATERIAL.compareTo(SOUND) < 0);
        assertTrue(MATERIAL.toString().startsWith("material:"));
        assertThrows(NullPointerException.class, () -> SemanticKey.of(null, MATERIAL.id()));
    }

    @Test void mappingHasValueSemanticsAndSafeBoundary() {
        final CompatibilityMapping equal = new CompatibilityMapping(MATERIAL, MAPPING.rendererId(),
                MAPPING.platformValue());
        assertEquals(MAPPING, equal);
        assertEquals(MAPPING.hashCode(), equal.hashCode());
        assertEquals(MAPPING, MAPPING);
        assertNotEquals(MAPPING, "mapping");
        assertNotEquals(MAPPING, new CompatibilityMapping(SOUND, MAPPING.rendererId(),
                MAPPING.platformValue()));
        assertNotEquals(MAPPING, new CompatibilityMapping(MATERIAL, reason("other_renderer"),
                MAPPING.platformValue()));
        assertNotEquals(MAPPING, new CompatibilityMapping(MATERIAL, MAPPING.rendererId(),
                "BLACK_WOOL"));
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityMapping(
                MATERIAL, MAPPING.rendererId(), "unsafe value"));
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityMapping(
                MATERIAL, MAPPING.rendererId(), null));
    }

    @Test void outcomesPreserveGameplayOrExplicitlyReject() {
        assertEquals(CompatibilityOutcome.State.SUPPORTED,
                CompatibilityOutcome.supported(MAPPING).state());
        assertEquals(CompatibilityOutcome.State.FALLBACK,
                CompatibilityOutcome.fallback(MAPPING, reason("fallback")).state());
        final CompatibilityOutcome degraded = CompatibilityOutcome.degraded(
                MAPPING, reason("degraded"), true);
        assertTrue(degraded.gameplayPreserved());
        assertTrue(degraded.decorativeSuppression());
        final CompatibilityOutcome unsupported = CompatibilityOutcome.unsupported(reason("missing"));
        assertFalse(unsupported.gameplayPreserved());
        assertFalse(unsupported.mapping().isPresent());
        assertFalse(CompatibilityOutcome.degraded(
                MAPPING, reason("degraded_visible"), false).decorativeSuppression());
    }

    @Test void registryRejectsDuplicatesAndRetainsLastKnownGood() {
        final SemanticMappingRegistry registry = new SemanticMappingRegistry(
                Collections.singletonList(MAPPING), Collections.singleton(MATERIAL));
        final long version = registry.snapshot().version();
        final SemanticMappingRegistry.Activation duplicate = registry.activate(
                Arrays.asList(MAPPING, MAPPING), Collections.singleton(MATERIAL));
        assertFalse(duplicate.activated());
        assertFalse(duplicate.validation().isValid());
        assertEquals(version, registry.snapshot().version());
        final SemanticMappingRegistry.Activation missing = registry.activate(
                Collections.<CompatibilityMapping>emptyList(),
                new HashSet<SemanticKey>(Arrays.asList(MATERIAL, SOUND)));
        assertFalse(missing.activated());
        assertEquals(MAPPING, registry.snapshot().find(MATERIAL).get());
    }

    @Test void registryAtomicallyActivatesCompleteCandidate() {
        final SemanticMappingRegistry registry = new SemanticMappingRegistry(
                Collections.singletonList(MAPPING), Collections.singleton(MATERIAL));
        final CompatibilityMapping sound = new CompatibilityMapping(SOUND,
                reason("sound_renderer"), "minecraft:block.note_block.hat");
        final SemanticMappingRegistry.Activation activation = registry.activate(
                Arrays.asList(MAPPING, sound),
                new HashSet<SemanticKey>(Arrays.asList(MATERIAL, SOUND)));
        assertTrue(activation.activated());
        assertTrue(activation.validation().isValid());
        assertEquals(2L, registry.snapshot().version());
        assertEquals(2, registry.snapshot().mappings().size());
    }

    @Test void runtimeClaimRequiresExactDigestAndSafeValues() {
        final CompatibilityAdapter.RuntimeClaim claim = new CompatibilityAdapter.RuntimeClaim(
                "Paper", "1.21.1", "133", repeat("a", 64));
        assertEquals("133", claim.build());
        assertEquals(repeat("a", 64), claim.sha256());
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityAdapter.RuntimeClaim(
                "Paper", "1.21.1", "133", "short"));
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityAdapter.RuntimeClaim(
                null, "1.21.1", "133", repeat("a", 64)));
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityAdapter.RuntimeClaim(
                "Paper runtime", "1.21.1", "133", repeat("a", 64)));
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityAdapter.RuntimeClaim(
                "Paper", "1.21.1", "133", null));
    }

    @Test void validationIssuesAreSortedAndNullSafe() {
        final CompatibilityValidation.Issue second = new CompatibilityValidation.Issue(
                reason("z_code"), SOUND);
        final CompatibilityValidation.Issue first = new CompatibilityValidation.Issue(
                reason("a_code"), MATERIAL);
        final CompatibilityValidation validation = new CompatibilityValidation(
                Arrays.asList(second, first));
        assertEquals(first, validation.issues().get(0));
        assertEquals(MATERIAL, first.key());
        assertEquals(reason("a_code"), first.code());
        assertTrue(first.compareTo(second) < 0);
        assertTrue(new CompatibilityValidation.Issue(reason("b_code"), MATERIAL)
                .compareTo(first) > 0);
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityValidation(
                Arrays.asList(first, null)));
        assertThrows(NullPointerException.class, () -> first.compareTo(null));
        assertThrows(NullPointerException.class,
                () -> new CompatibilityValidation.Issue(null, MATERIAL));
    }

    @Test void invalidInitialRegistryFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new SemanticMappingRegistry(
                Collections.<CompatibilityMapping>emptyList(), Collections.singleton(MATERIAL)));
        assertThrows(NullPointerException.class, () -> new SemanticMappingRegistry(
                Collections.singletonList(MAPPING), Collections.singleton(null)));
        assertThrows(NullPointerException.class, () -> new SemanticMappingRegistry(
                Arrays.asList(MAPPING, null), Collections.singleton(MATERIAL)));
        assertThrows(NullPointerException.class, () -> new SemanticMappingRegistry(
                Collections.singletonList(MAPPING), null));
    }

    private static DefinitionId reason(final String value) {
        return DefinitionId.of("zartra", "compat/" + value);
    }

    private static String repeat(final String value, final int count) {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) { builder.append(value); }
        return builder.toString();
    }
}
