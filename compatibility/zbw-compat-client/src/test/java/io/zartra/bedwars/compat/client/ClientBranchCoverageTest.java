package io.zartra.bedwars.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** Validation-branch regressions for ZBW-COMPAT-008 fail-closed behavior. */
final class ClientBranchCoverageTest {
    @Test void reportsRejectUnsafeOrIncompletePayloadsDeterministically() {
        final List<ClientFeatureOutcome> nativeOutcomes =
                outcomes(ClientFeatureOutcome.State.NATIVE);
        final ClientCompatibilityReport first = new ClientCompatibilityReport(
                ClientPath.NATIVE, "Paper/1.21.1@build133", nativeOutcomes);
        final ClientCompatibilityReport same = new ClientCompatibilityReport(
                ClientPath.NATIVE, "Paper/1.21.1@build133", nativeOutcomes);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, "report");
        assertFalse(first.equals(null));
        assertEquals(ClientFeatureOutcome.State.NATIVE,
                first.outcome(ClientFeature.GUI).state());
        assertThrows(UnsupportedOperationException.class,
                () -> first.outcomes().clear());

        final List<ClientFeatureOutcome> blocked =
                outcomes(ClientFeatureOutcome.State.NATIVE);
        blocked.set(0, new ClientFeatureOutcome(
                ClientFeature.GUI, ClientFeatureOutcome.State.BLOCKED,
                DefinitionId.of("zartra", "compat/client/blocked"),
                false, false));
        assertFalse(new ClientCompatibilityReport(
                ClientPath.NATIVE, "Paper/1.21.1@build133",
                blocked).activationSafe());
        assertThrows(IllegalArgumentException.class,
                () -> new ClientCompatibilityReport(
                        ClientPath.NATIVE, "bad runtime", nativeOutcomes));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientCompatibilityReport(
                        ClientPath.NATIVE, null, nativeOutcomes));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientCompatibilityReport(
                        ClientPath.NATIVE, "Paper/1.21.1@build133",
                        Arrays.asList(nativeOutcomes.get(0),
                                nativeOutcomes.get(0))));
        assertThrows(NullPointerException.class,
                () -> new ClientCompatibilityReport(
                        ClientPath.NATIVE, "Paper/1.21.1@build133",
                        Collections.singletonList(null)));
    }

    @Test void outcomeAndSessionNullOrBoundaryStatesAreRejected() {
        final ClientFeatureOutcome blocked = new ClientFeatureOutcome(
                ClientFeature.INPUT, ClientFeatureOutcome.State.BLOCKED,
                DefinitionId.of("zartra", "compat/client/blocked"),
                false, false);
        final ClientFeatureOutcome degraded = new ClientFeatureOutcome(
                ClientFeature.PARTICLE, ClientFeatureOutcome.State.DEGRADED,
                DefinitionId.of("zartra", "compat/client/degraded"),
                true, true);
        assertFalse(blocked.informationPreserved());
        assertTrue(degraded.decorativeSuppression());
        assertNotEquals(blocked, degraded);
        assertNotEquals(blocked, "outcome");
        assertFalse(blocked.equals(null));
        assertThrows(NullPointerException.class,
                () -> new ClientFeatureOutcome(
                        null, ClientFeatureOutcome.State.NATIVE,
                        DefinitionId.of("zartra", "compat/client/native"),
                        true, false));
        assertThrows(NullPointerException.class,
                () -> new ClientFeatureOutcome(
                        ClientFeature.TEXT, null,
                        DefinitionId.of("zartra", "compat/client/native"),
                        true, false));
        assertThrows(NullPointerException.class,
                () -> new ClientFeatureOutcome(
                        ClientFeature.TEXT,
                        ClientFeatureOutcome.State.NATIVE,
                        null, true, false));

        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession("session_1234", ClientPath.NATIVE,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.JAVA_NATIVE, 10001));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession(null, ClientPath.NATIVE,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.JAVA_NATIVE, 47));
        assertThrows(NullPointerException.class,
                () -> new ClientSession("session_1234", ClientPath.NATIVE,
                        null, ClientSession.InputMode.JAVA_NATIVE, 47));
        assertThrows(NullPointerException.class,
                () -> new ClientSession("session_1234", ClientPath.NATIVE,
                        ClientSession.Edition.JAVA, null, 47));
        assertThrows(NullPointerException.class,
                () -> new ClientSession("session_1234", null,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.JAVA_NATIVE, 47));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession("session_1234",
                        ClientPath.GEYSER_FLOODGATE,
                        ClientSession.Edition.JAVA,
                        ClientSession.InputMode.JAVA_NATIVE, 47));
        assertNotEquals(ClientTestSupport.session(ClientPath.NATIVE),
                ClientTestSupport.session(ClientPath.VIAVERSION));
        assertFalse(ClientTestSupport.session(ClientPath.NATIVE).equals(null));
    }

    @Test void providerNullDuplicateAndVersionBranchesFailClosed() {
        assertThrows(NullPointerException.class,
                () -> ClientProviderProbe.present(null, "5.4.2"));
        assertThrows(IllegalArgumentException.class,
                () -> ClientProviderProbe.present(
                        ClientProvider.VIAVERSION, null));
        assertThrows(IllegalArgumentException.class,
                () -> ClientProviderProbe.duplicate(
                        ClientProvider.VIAVERSION, "5.4.2", -1));
        final ClientProviderProbe old = ClientProviderProbe.incompatible(
                ClientProvider.VIAVERSION, "5.4.1");
        assertFalse(old.exactlyCompatible());
        assertNotEquals(old, "probe");
        assertFalse(old.equals(null));
        final ClientProviderInventory duplicateAbsent =
                ClientProviderInventory.of(Arrays.asList(
                        ClientProviderProbe.absent(ClientProvider.GEYSER),
                        ClientProviderProbe.absent(ClientProvider.GEYSER)));
        assertEquals("2.7.0", duplicateAbsent.probe(
                ClientProvider.GEYSER).version().orElseThrow(
                        AssertionError::new));
        assertThrows(NullPointerException.class,
                () -> duplicateAbsent.supports(null));
        assertNotEquals(duplicateAbsent, "inventory");
        assertFalse(duplicateAbsent.equals(null));
    }

    @Test void synchronousGatewayFailuresAreContained() {
        final ThrowingGateway gateway = new ThrowingGateway();
        final ClientCompatibilityLayer layer =
                new ClientCompatibilityLayer(gateway);
        assertFalse(layer.start().toCompletableFuture().join());
        assertEquals(ClientCompatibilityLayer.State.FAILED, layer.state());

        final ThrowingGateway inspect = new ThrowingGateway();
        inspect.throwDiscovery = false;
        final ClientCompatibilityLayer running =
                new ClientCompatibilityLayer(inspect);
        assertTrue(running.start().toCompletableFuture().join());
        assertFalse(running.open("session_1234", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());

        inspect.throwInspection = false;
        running.open("session_5678", ClientTestSupport.server())
                .toCompletableFuture().join();
        assertFalse(running.close("session_5678")
                .toCompletableFuture().join());
        running.open("session_9999", ClientTestSupport.server())
                .toCompletableFuture().join();
        assertTrue(running.stop().toCompletableFuture().join());
    }

    private static List<ClientFeatureOutcome> outcomes(
            final ClientFeatureOutcome.State state) {
        final List<ClientFeatureOutcome> result =
                new ArrayList<ClientFeatureOutcome>();
        for (ClientFeature feature : ClientFeature.values()) {
            result.add(new ClientFeatureOutcome(feature, state,
                    DefinitionId.of("zartra", "compat/client/native"),
                    true, false));
        }
        return result;
    }

    private static final class ThrowingGateway
            implements ClientTranslationGateway {
        private boolean throwDiscovery = true;
        private boolean throwInspection = true;

        @Override public CompletionStage<ClientProviderInventory> discover() {
            if (throwDiscovery) {
                throw new IllegalStateException("discovery");
            }
            return java.util.concurrent.CompletableFuture.completedFuture(
                    ClientProviderInventory.empty());
        }

        @Override public CompletionStage<ClientSession> inspect(
                final String opaqueSessionKey) {
            if (throwInspection) {
                throw new IllegalStateException("inspection");
            }
            return java.util.concurrent.CompletableFuture.completedFuture(
                    new ClientSession(opaqueSessionKey, ClientPath.NATIVE,
                            ClientSession.Edition.JAVA,
                            ClientSession.InputMode.JAVA_NATIVE, 47));
        }

        @Override public CompletionStage<Void> release(
                final String opaqueSessionKey) {
            throw new IllegalStateException("release");
        }
    }
}
