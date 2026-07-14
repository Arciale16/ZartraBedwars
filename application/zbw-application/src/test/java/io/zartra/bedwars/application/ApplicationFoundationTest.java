package io.zartra.bedwars.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.content.ContentRegistry;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.application.capability.ProviderCapabilityPolicy;
import io.zartra.bedwars.application.content.ImmutableContentRegistry;
import io.zartra.bedwars.domain.generator.GeneratorDefinition;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ApplicationFoundationTest {
    @Test
    void capabilityPolicyAcceptsOnlyCompleteProviderDeclarations() {
        final CapabilityId requiredId = CapabilityId.of("zartra", "required");
        final CapabilitySet required = CapabilitySet.of(Collections.singleton(requiredId));
        final ProviderCapabilityPolicy policy = new ProviderCapabilityPolicy();
        assertTrue(policy.evaluate(required, required).isSuccess());
        assertFalse(policy.evaluate(required, CapabilitySet.empty()).isSuccess());
        assertEquals("zartra:provider/missing_capability",
                policy.evaluate(required, CapabilitySet.empty()).error().get().code().toString());
    }

    @Test
    void contentRegistryRejectsDuplicatesAndSortsDefinitions() {
        final GeneratorDefinition alpha = GeneratorDefinition.of(GeneratorTypeId.of("test", "alpha"),
                ResourceId.of("test", "alpha"), 1);
        final GeneratorDefinition beta = GeneratorDefinition.of(GeneratorTypeId.of("test", "beta"),
                ResourceId.of("test", "beta"), 1);
        final ContentRegistry.RegistryVersion version = ContentRegistry.RegistryVersion.of(
                SemanticVersion.parse("1.0.0"), 1);
        final ImmutableContentRegistry<GeneratorTypeId, GeneratorDefinition> registry =
                ImmutableContentRegistry.assemble(Arrays.asList(beta, alpha), version);
        assertEquals(alpha, registry.definitions().get(0));
        assertEquals(beta, registry.find(beta.id()).get());
        assertFalse(registry.find(GeneratorTypeId.of("test", "missing")).isPresent());
        assertThrows(ImmutableContentRegistry.DuplicateContentIdException.class,
                () -> ImmutableContentRegistry.assemble(Arrays.asList(alpha, alpha), version));
        assertThrows(UnsupportedOperationException.class, () -> registry.definitions().add(alpha));
    }
}
