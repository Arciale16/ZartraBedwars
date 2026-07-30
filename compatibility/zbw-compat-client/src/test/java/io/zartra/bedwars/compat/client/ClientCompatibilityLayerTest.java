package io.zartra.bedwars.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** Async lifecycle, cleanup and bounded-session tests for ZBW-INT-010. */
final class ClientCompatibilityLayerTest {
    @Test void absentProvidersKeepNativeClientSafeAndCleanupIsIdempotent() {
        final Gateway gateway = new Gateway();
        final ClientCompatibilityLayer layer =
                new ClientCompatibilityLayer(gateway, 2);
        assertTrue(layer.start().toCompletableFuture().join());
        assertEquals(ClientCompatibilityLayer.State.RUNNING, layer.state());
        final ClientCompatibilityReport report = layer.open(
                "session_1234", ClientTestSupport.server())
                .toCompletableFuture().join();
        assertTrue(report.activationSafe());
        assertEquals(ClientPath.NATIVE, report.path());
        assertEquals(report, layer.open(
                "session_1234", ClientTestSupport.server())
                .toCompletableFuture().join());
        assertEquals(1, gateway.inspections);
        assertEquals(1, layer.activeSessions());
        assertTrue(layer.close("session_1234").toCompletableFuture().join());
        assertFalse(layer.close("session_1234").toCompletableFuture().join());
        assertEquals(Collections.singletonList("session_1234"),
                gateway.released);
    }

    @Test void unavailableTranslatedPathFailsClosedWithoutReplacingServer() {
        final Gateway gateway = new Gateway();
        gateway.session = ClientTestSupport.session(ClientPath.VIAVERSION);
        final ClientCompatibilityLayer layer =
                new ClientCompatibilityLayer(gateway);
        layer.start().toCompletableFuture().join();
        final ClientCompatibilityReport report = layer.open(
                "session_1234", ClientTestSupport.server())
                .toCompletableFuture().join();
        assertFalse(report.activationSafe());
        assertEquals(ClientPath.VIAVERSION, report.path());
        assertEquals("Paper/1.21.1@build133", report.serverRuntime());
        assertEquals(ClientFeatureOutcome.State.BLOCKED,
                report.outcome(ClientFeature.SHOP).state());
    }

    @Test void exactProvidersEnableTranslatedAndBedrockPaths() {
        final Gateway gateway = new Gateway();
        gateway.inventory = ClientTestSupport.allProviders();
        gateway.session = ClientTestSupport.session(
                ClientPath.GEYSER_FLOODGATE);
        final ClientCompatibilityLayer layer =
                new ClientCompatibilityLayer(gateway);
        assertTrue(layer.start().toCompletableFuture().join());
        assertEquals(gateway.inventory, layer.inventory());
        final ClientCompatibilityReport report = layer.open(
                "session_1234", ClientTestSupport.server())
                .toCompletableFuture().join();
        assertTrue(report.activationSafe());
        assertEquals(ClientPath.GEYSER_FLOODGATE, report.path());
        assertEquals(ClientFeatureOutcome.State.FALLBACK,
                report.outcome(ClientFeature.GUI).state());
    }

    @Test void discoveryAndInspectionFailuresFailClosed() {
        final Gateway discoveryFailure = new Gateway();
        discoveryFailure.failDiscovery = true;
        final ClientCompatibilityLayer failed =
                new ClientCompatibilityLayer(discoveryFailure);
        assertFalse(failed.start().toCompletableFuture().join());
        assertEquals(ClientCompatibilityLayer.State.FAILED, failed.state());
        assertFalse(failed.open("session_1234", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());

        final Gateway inspectionFailure = new Gateway();
        inspectionFailure.failInspection = true;
        final ClientCompatibilityLayer inspected =
                new ClientCompatibilityLayer(inspectionFailure);
        inspected.start().toCompletableFuture().join();
        assertFalse(inspected.open("session_1234", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());

        final Gateway mismatched = new Gateway();
        mismatched.session = new ClientSession(
                "different_1234", ClientPath.NATIVE,
                ClientSession.Edition.JAVA,
                ClientSession.InputMode.JAVA_NATIVE, 47);
        final ClientCompatibilityLayer mismatch =
                new ClientCompatibilityLayer(mismatched);
        mismatch.start().toCompletableFuture().join();
        assertFalse(mismatch.open("session_1234", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());
    }

    @Test void boundedSessionsAndStopReleaseEveryResource() {
        final Gateway gateway = new Gateway();
        final ClientCompatibilityLayer layer =
                new ClientCompatibilityLayer(gateway, 1);
        layer.start().toCompletableFuture().join();
        assertTrue(layer.open("session_1234", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());
        assertFalse(layer.open("session_5678", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());
        assertTrue(layer.stop().toCompletableFuture().join());
        assertEquals(ClientCompatibilityLayer.State.STOPPED, layer.state());
        assertEquals(0, layer.activeSessions());
        assertTrue(gateway.released.contains("session_1234"));
        assertFalse(layer.open("session_9999", ClientTestSupport.server())
                .toCompletableFuture().join().activationSafe());
    }

    @Test void invalidLifecycleInputsAndReleaseFailureAreSafe() {
        final Gateway gateway = new Gateway();
        final ClientCompatibilityLayer layer =
                new ClientCompatibilityLayer(gateway);
        assertThrows(IllegalArgumentException.class,
                () -> new ClientCompatibilityLayer(gateway, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientCompatibilityLayer(gateway, 4097));
        assertThrows(IllegalArgumentException.class,
                () -> layer.open("player", ClientTestSupport.server()));
        layer.start().toCompletableFuture().join();
        layer.open("session_1234", ClientTestSupport.server())
                .toCompletableFuture().join();
        gateway.failRelease = true;
        assertFalse(layer.close("session_1234")
                .toCompletableFuture().join());
        assertTrue(layer.start().toCompletableFuture().join());
    }

    private static final class Gateway implements ClientTranslationGateway {
        private ClientProviderInventory inventory =
                ClientProviderInventory.empty();
        private ClientSession session =
                ClientTestSupport.session(ClientPath.NATIVE);
        private final List<String> released = new ArrayList<String>();
        private boolean failDiscovery;
        private boolean failInspection;
        private boolean failRelease;
        private int inspections;

        @Override public CompletionStage<ClientProviderInventory> discover() {
            if (failDiscovery) {
                final CompletableFuture<ClientProviderInventory> failed =
                        new CompletableFuture<ClientProviderInventory>();
                failed.completeExceptionally(
                        new IllegalStateException("discovery failed"));
                return failed;
            }
            return CompletableFuture.completedFuture(inventory);
        }

        @Override public CompletionStage<ClientSession> inspect(
                final String opaqueSessionKey) {
            inspections++;
            if (failInspection) {
                final CompletableFuture<ClientSession> failed =
                        new CompletableFuture<ClientSession>();
                failed.completeExceptionally(
                        new IllegalStateException("inspection failed"));
                return failed;
            }
            if (!session.sessionKey().equals(opaqueSessionKey)
                    && session.sessionKey().equals("session_1234")) {
                return CompletableFuture.completedFuture(new ClientSession(
                        opaqueSessionKey, session.path(), session.edition(),
                        session.inputMode(), session.protocolVersion()));
            }
            return CompletableFuture.completedFuture(session);
        }

        @Override public CompletionStage<Void> release(
                final String opaqueSessionKey) {
            released.add(opaqueSessionKey);
            if (failRelease) {
                final CompletableFuture<Void> failed =
                        new CompletableFuture<Void>();
                failed.completeExceptionally(
                        new IllegalStateException("release failed"));
                return failed;
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
