package io.zartra.bedwars.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Immutable model and provider-boundary tests for ZBW-INT-010. */
final class ClientModelTest {
    @Test void probesAndInventoryAreImmutableDeterministicAndExact() {
        final ClientProviderProbe first = ClientProviderProbe.present(
                ClientProvider.VIAVERSION, "5.4.2");
        final ClientProviderProbe same = ClientProviderProbe.present(
                ClientProvider.VIAVERSION, "5.4.2");
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertTrue(first.exactlyCompatible());
        assertEquals("5.4.2", first.version().orElseThrow(AssertionError::new));
        assertEquals(1, first.bindingCount());
        assertEquals("ViaVersion", first.provider().displayName());

        final ClientProviderInventory inventory =
                ClientProviderInventory.of(Collections.singletonList(first));
        assertEquals(inventory, ClientProviderInventory.of(
                Collections.singletonList(same)));
        assertEquals(inventory.hashCode(), ClientProviderInventory.of(
                Collections.singletonList(same)).hashCode());
        assertTrue(inventory.supports(ClientPath.NATIVE));
        assertTrue(inventory.supports(ClientPath.VIAVERSION));
        assertFalse(inventory.supports(ClientPath.VIABACKWARDS));
        assertEquals(ClientProviderStatus.ABSENT,
                inventory.probe(ClientProvider.FLOODGATE).status());
        assertThrows(UnsupportedOperationException.class,
                () -> inventory.probes().clear());
    }

    @Test void duplicateAndIncompatibleProvidersFailClosed() {
        final ClientProviderInventory duplicate = ClientProviderInventory.of(
                Arrays.asList(
                        ClientProviderProbe.present(
                                ClientProvider.VIAVERSION, "5.4.2"),
                        ClientProviderProbe.present(
                                ClientProvider.VIAVERSION, "5.4.2")));
        assertEquals(ClientProviderStatus.DUPLICATE,
                duplicate.probe(ClientProvider.VIAVERSION).status());
        assertEquals(2,
                duplicate.probe(ClientProvider.VIAVERSION).bindingCount());
        assertFalse(duplicate.supports(ClientPath.VIAVERSION));

        final ClientProviderInventory incompatible = ClientProviderInventory.of(
                Collections.singletonList(ClientProviderProbe.incompatible(
                        ClientProvider.VIAVERSION, "5.4.1")));
        assertFalse(incompatible.supports(ClientPath.VIAVERSION));
        assertFalse(ClientProviderProbe.absent(
                ClientProvider.VIAVERSION).version().isPresent());
        assertNotEquals(ClientProviderProbe.absent(ClientProvider.GEYSER),
                ClientProviderProbe.absent(ClientProvider.FLOODGATE));
    }

    @Test void invalidProbeAndInventoryStatesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientProviderProbe.present(
                        ClientProvider.VIAVERSION, "bad version"));
        assertThrows(IllegalArgumentException.class,
                () -> ClientProviderProbe.duplicate(
                        ClientProvider.VIAVERSION, "5.4.2", 1));
        assertThrows(IllegalArgumentException.class,
                () -> ClientProviderProbe.duplicate(
                        ClientProvider.VIAVERSION, "5.4.2", 17));
        assertThrows(NullPointerException.class,
                () -> ClientProviderInventory.of(
                        Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class,
                () -> ClientProviderInventory.of(Arrays.asList(
                        ClientProviderProbe.absent(ClientProvider.GEYSER),
                        ClientProviderProbe.absent(ClientProvider.FLOODGATE),
                        ClientProviderProbe.absent(ClientProvider.VIAVERSION),
                        ClientProviderProbe.absent(ClientProvider.VIABACKWARDS),
                        ClientProviderProbe.absent(ClientProvider.VIAREWIND),
                        ClientProviderProbe.absent(ClientProvider.GEYSER),
                        ClientProviderProbe.absent(ClientProvider.FLOODGATE),
                        ClientProviderProbe.absent(ClientProvider.VIAVERSION),
                        ClientProviderProbe.absent(ClientProvider.VIABACKWARDS),
                        ClientProviderProbe.absent(ClientProvider.VIAREWIND),
                        ClientProviderProbe.absent(ClientProvider.GEYSER),
                        ClientProviderProbe.absent(ClientProvider.FLOODGATE),
                        ClientProviderProbe.absent(ClientProvider.VIAVERSION),
                        ClientProviderProbe.absent(ClientProvider.VIABACKWARDS),
                        ClientProviderProbe.absent(ClientProvider.VIAREWIND),
                        ClientProviderProbe.absent(ClientProvider.GEYSER),
                        ClientProviderProbe.absent(ClientProvider.FLOODGATE))));
    }

    @Test void sessionProtectsPrivacyEditionAndInputBoundaries() {
        final ClientSession first = ClientTestSupport.session(ClientPath.NATIVE);
        final ClientSession same = ClientTestSupport.session(ClientPath.NATIVE);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals("session_1234", first.sessionKey());
        assertEquals(47, first.protocolVersion());
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession("player", ClientPath.NATIVE,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.JAVA_NATIVE, 47));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession("session_1234", ClientPath.NATIVE,
                        ClientSession.Edition.BEDROCK,
                        ClientSession.InputMode.TOUCH, 47));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession("session_1234", ClientPath.NATIVE,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.CONTROLLER, 47));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession("session_1234", ClientPath.NATIVE,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.JAVA_NATIVE, 0));
    }

    @Test void featureOutcomesAndReportsEnforceCompleteParity() {
        final ClientFeatureOutcome nativeText = new ClientFeatureOutcome(
                ClientFeature.TEXT, ClientFeatureOutcome.State.NATIVE,
                DefinitionId.of("zartra", "compat/client/native"), true, false);
        assertEquals(nativeText, new ClientFeatureOutcome(
                ClientFeature.TEXT, ClientFeatureOutcome.State.NATIVE,
                DefinitionId.of("zartra", "compat/client/native"), true, false));
        assertEquals(nativeText.hashCode(), new ClientFeatureOutcome(
                ClientFeature.TEXT, ClientFeatureOutcome.State.NATIVE,
                DefinitionId.of("zartra", "compat/client/native"),
                true, false).hashCode());
        assertThrows(IllegalArgumentException.class,
                () -> new ClientFeatureOutcome(
                        ClientFeature.TEXT,
                        ClientFeatureOutcome.State.BLOCKED,
                        DefinitionId.of("zartra", "compat/client/blocked"),
                        true, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientFeatureOutcome(
                        ClientFeature.TEXT,
                        ClientFeatureOutcome.State.FALLBACK,
                        DefinitionId.of("zartra", "compat/client/fallback"),
                        true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientCompatibilityReport(
                        ClientPath.NATIVE, "Paper/1.21.1@build133",
                        Collections.singletonList(nativeText)));
    }
}
