package io.zartra.bedwars.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.content.ContentRegistry;
import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.extension.ExtensionValidation;
import io.zartra.bedwars.api.extension.MinecraftVersion;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ContentPackId;
import io.zartra.bedwars.api.identity.ExtensionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CapabilityMetadataContractTest {
    @Test
    void capabilitySetIsSortedImmutableAndDuplicateSafe() {
        final CapabilityId alpha = CapabilityId.of("test", "alpha");
        final CapabilityId beta = CapabilityId.of("test", "beta");
        final CapabilitySet set = CapabilitySet.of(Arrays.asList(beta, alpha));
        assertEquals(Arrays.asList(alpha, beta), set.values());
        assertTrue(set.containsAll(CapabilitySet.of(Collections.singleton(alpha))));
        assertThrows(CapabilitySet.DuplicateCapabilityException.class,
                () -> CapabilitySet.of(Arrays.asList(alpha, alpha)));
        assertThrows(UnsupportedOperationException.class, () -> set.values().add(alpha));
    }

    @Test
    void providerDescriptorAndHealthContainNoImplementationState() {
        final Provider.Descriptor descriptor = Provider.Descriptor.of(ProviderId.of("test", "provider"),
                SemanticVersion.parse("1.0.0"), CapabilitySet.empty());
        assertEquals("test:provider", descriptor.id().toString());
        final Provider.Health health = Provider.Health.of(Provider.HealthStatus.DEGRADED,
                Instant.parse("2026-07-14T12:00:00Z"), "provider.timeout");
        assertEquals(Provider.HealthStatus.DEGRADED, health.status());
        assertEquals("provider.timeout", health.diagnosticCode());
        assertThrows(IllegalArgumentException.class, () -> Provider.Health.of(
                Provider.HealthStatus.HEALTHY, Instant.now(), "secret endpoint"));
    }

    @Test
    void extensionMetadataPreservesAllMarketplaceFields() {
        final ExtensionMetadata.Dependency dependency = ExtensionMetadata.Dependency.of(
                ExtensionId.of("test", "base"), VersionRange.parse("[1.0.0,2.0.0)"), false);
        final ExtensionMetadata metadata = ExtensionMetadata.builder().schemaVersion(1)
                .id(ExtensionId.of("test", "extension")).displayName("Test Extension")
                .version(SemanticVersion.parse("1.0.0")).entrypoint("org.example.TestExtension")
                .apiVersions(VersionRange.parse("[1.0.0,2.0.0)"))
                .productVersions(VersionRange.parse("[0.1.0,1.0.0)"))
                .minecraftVersions(ExtensionMetadata.MinecraftRange.inclusive(
                        MinecraftVersion.parse("1.8.8"), MinecraftVersion.parse("1.21.11")))
                .dependencies(Collections.singletonList(dependency))
                .requiredApis(CapabilitySet.of(Collections.singleton(CapabilityId.of("zartra", "generator"))))
                .providedCapabilities(CapabilitySet.of(Collections.singleton(CapabilityId.of("test", "mode"))))
                .permissions(Collections.singletonList(ExtensionMetadata.PermissionNode.of("zartrabedwars.test.use")))
                .configurationKeys(Collections.singletonList(ExtensionMetadata.ConfigurationKey.of("extensions.test.enabled")))
                .build();
        assertEquals("test:extension", metadata.id().toString());
        assertFalse(metadata.dependencies().get(0).optional());
        assertTrue(metadata.minecraftVersions().contains(MinecraftVersion.parse("1.8.8")));
        assertEquals("zartrabedwars.test.use", metadata.permissions().get(0).value());
        assertThrows(UnsupportedOperationException.class, () -> metadata.dependencies().add(dependency));
        final ExtensionValidation.Target target = ExtensionValidation.Target.of(
                SemanticVersion.parse("1.0.0"), SemanticVersion.parse("0.1.0"), MinecraftVersion.parse("1.21.11"));
        assertEquals(SemanticVersion.parse("1.0.0"), target.apiVersion());
    }

    @Test
    void contentMetadataIsVersionedAndImmutable() {
        final ContentRegistry.PackMetadata pack = ContentRegistry.PackMetadata.of(
                ContentPackId.of("test", "pack"), SemanticVersion.parse("1.0.0"),
                VersionRange.parse("[1.0.0,2.0.0)"), Collections.singletonList(ContentPackId.of("test", "base")));
        assertEquals("test:pack", pack.id().toString());
        assertThrows(UnsupportedOperationException.class,
                () -> pack.dependencies().add(ContentPackId.of("test", "other")));
        assertEquals(2, ContentRegistry.RegistryVersion.of(SemanticVersion.parse("1.0.0"), 2).schemaVersion());
    }
}
