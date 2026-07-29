package io.zartra.bedwars.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.SemanticKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Five-path feature matrix tests for ZBW-COMPAT-004..008 and ZBW-INT-010. */
final class ClientAdapterMatrixTest {
    @Test void allFiveClientPathsPreserveEveryFeature() {
        final ClientProviderInventory inventory =
                ClientTestSupport.allProviders();
        final List<ClientPathAdapter> adapters = Arrays.asList(
                new NativeClientAdapter(),
                new ViaVersionClientAdapter(),
                new ViaBackwardsClientAdapter(),
                new ViaRewindClientAdapter(),
                new GeyserFloodgateClientAdapter());
        for (ClientPathAdapter adapter : adapters) {
            assertTrue(adapter.available(inventory));
            final ClientCompatibilityReport report = adapter.evaluate(
                    ClientTestSupport.server(),
                    ClientTestSupport.session(adapter.path()));
            assertTrue(report.activationSafe(), adapter.path().name());
            assertEquals(10, report.outcomes().size());
            assertEquals(adapter.path(), report.path());
            assertEquals("Paper/1.21.1@build133", report.serverRuntime());
            for (ClientFeature feature : ClientFeature.values()) {
                assertTrue(report.outcome(feature).informationPreserved());
            }
        }
    }

    @Test void legacyAndBedrockDecorativeLossHasVisibleEquivalent() {
        final ClientCompatibilityReport rewind =
                new ViaRewindClientAdapter().evaluate(
                        ClientTestSupport.server(),
                        ClientTestSupport.session(ClientPath.VIAREWIND));
        final ClientCompatibilityReport bedrock =
                new GeyserFloodgateClientAdapter().evaluate(
                        ClientTestSupport.server(),
                        ClientTestSupport.session(
                                ClientPath.GEYSER_FLOODGATE));
        for (ClientFeature feature : Arrays.asList(
                ClientFeature.SOUND, ClientFeature.PARTICLE,
                ClientFeature.ENTITY_DISPLAY)) {
            assertEquals(ClientFeatureOutcome.State.DEGRADED,
                    rewind.outcome(feature).state());
            assertTrue(rewind.outcome(feature).decorativeSuppression());
            assertTrue(rewind.outcome(feature).informationPreserved());
            assertEquals(ClientFeatureOutcome.State.DEGRADED,
                    bedrock.outcome(feature).state());
            assertTrue(bedrock.outcome(feature).decorativeSuppression());
        }
        assertEquals(ClientFeatureOutcome.State.FALLBACK,
                bedrock.outcome(ClientFeature.INPUT).state());
        assertTrue(bedrock.outcome(ClientFeature.INPUT).reason()
                .path().contains("controller"));
    }

    @Test void exactProviderChainsAreRequired() {
        final ClientProviderInventory empty = ClientProviderInventory.empty();
        assertTrue(new NativeClientAdapter().available(empty));
        assertFalse(new ViaVersionClientAdapter().available(empty));
        assertFalse(new ViaBackwardsClientAdapter().available(empty));
        assertFalse(new ViaRewindClientAdapter().available(empty));
        assertFalse(new GeyserFloodgateClientAdapter().available(empty));

        final ClientProviderInventory wrongVersion =
                ClientProviderInventory.of(Arrays.asList(
                        ClientProviderProbe.present(
                                ClientProvider.VIAVERSION, "5.4.1"),
                        ClientProviderProbe.present(
                                ClientProvider.VIABACKWARDS, "5.4.2")));
        assertFalse(new ViaBackwardsClientAdapter().available(wrongVersion));
    }

    @Test void missingServerSemanticBlocksOnlyAffectedFeatures() {
        final List<CompatibilityMapping> mappings = new ArrayList<
                CompatibilityMapping>(CompatibilityMappings.complete(
                "test", "STONE", "ITEM", "NBT", "SOUND", "PARTICLE",
                "TEXT", "ENTITY", "PACKET", "GUI", "SCHEDULER"));
        mappings.removeIf(mapping ->
                mapping.semanticKey().kind() == SemanticKey.Kind.PARTICLE);
        final CompatibilityAdapter incomplete =
                ClientTestSupport.server(mappings);
        final ClientCompatibilityReport report =
                new NativeClientAdapter().evaluate(
                        incomplete,
                        ClientTestSupport.session(ClientPath.NATIVE));
        assertFalse(report.activationSafe());
        assertEquals(ClientFeatureOutcome.State.BLOCKED,
                report.outcome(ClientFeature.PARTICLE).state());
        assertEquals(ClientFeatureOutcome.State.NATIVE,
                report.outcome(ClientFeature.TEXT).state());
    }

    @Test void adapterRejectsWrongSessionPathAndVendorLinkageIsAbsent() {
        assertThrows(IllegalArgumentException.class,
                () -> new ViaVersionClientAdapter().evaluate(
                        ClientTestSupport.server(),
                        ClientTestSupport.session(ClientPath.NATIVE)));
        assertMissing("com.viaversion.viaversion.api.Via");
        assertMissing("com.viaversion.viabackwards.api.ViaBackwards");
        assertMissing("com.viaversion.viarewind.api.ViaRewind");
        assertMissing("org.geysermc.geyser.api.GeyserApi");
        assertMissing("org.geysermc.floodgate.api.FloodgateApi");
    }

    private static void assertMissing(final String type) {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                type, false,
                ClientAdapterMatrixTest.class.getClassLoader()));
    }
}
