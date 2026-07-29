package io.zartra.bedwars.proxy.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** ZBW-DEPLOY-002..004 and ZBW-READY-013/014 M20 closure certification. */
class ProxyFailureSecurityPerformanceTest {
    private static final Instant NOW = Instant.ofEpochSecond(10_000);
    private static final byte[] PAYLOAD = "proxy-operation".getBytes(StandardCharsets.UTF_8);

    @Test void crashAckLossAndRestartRemainSingleAdmission() {
        ProxyReservationCoordinator authority = new ProxyReservationCoordinator();
        ProxyAdapterRuntime firstProxy = runtime(authority, 1);
        ProxyAdapterRuntime secondProxy = runtime(authority, 1);
        CrossServerIntent firstIntent = intent("subject-shared");
        CrossServerTransferResult prepared = prepare(firstProxy, firstIntent, NOW);
        Assertions.assertEquals(CrossServerTransferResult.Status.READY, prepared.status());

        firstProxy.stop();
        Assertions.assertEquals(DegradationState.OFFLINE, firstProxy.diagnostic(NOW).state());
        CrossServerIntent duplicateIntent = intent("subject-shared");
        Assertions.assertEquals(CrossServerTransferResult.Status.RESERVATION_FAILED,
                prepare(secondProxy, duplicateIntent, NOW).status());

        CrossServerWorkflow survivingBackend = new CrossServerWorkflow(secondProxy);
        TransferToken token = prepared.token().get();
        Assertions.assertEquals(TokenConsumptionResult.Status.CONSUMED,
                survivingBackend.complete(token, "backend-a", InstanceEpoch.of(1), NOW).status());
        Assertions.assertEquals(TokenConsumptionResult.Status.DUPLICATE,
                survivingBackend.complete(token, "backend-a", InstanceEpoch.of(1), NOW).status());
        Assertions.assertEquals(CrossServerTransferResult.Status.READY,
                prepare(secondProxy, duplicateIntent, NOW).status());
    }

    @Test void backendCrashStaleRegistryAndNewEpochFailClosed() {
        ProxyReservationCoordinator authority = new ProxyReservationCoordinator();
        ProxyAdapterRuntime runtime = runtime(authority, 1);
        CrossServerTransferResult prepared = prepare(runtime, intent("subject-old-epoch"), NOW);
        TransferToken oldToken = prepared.token().get();
        RoutingRequest request = RoutingRequest.of(UUID.randomUUID(), "subject-route", "proxy",
                Collections.singleton("flow.queue"), NOW, NOW.plusSeconds(20));
        Assertions.assertEquals(RoutingResult.Status.NO_CAPACITY,
                runtime.route(request, NOW.plusSeconds(11)).status());

        runtime.registry().register(registration(2));
        runtime.registry().heartbeat(heartbeat(2), NOW.plusSeconds(11));
        Assertions.assertEquals(TokenConsumptionResult.Status.STALE_EPOCH,
                new CrossServerWorkflow(runtime).complete(oldToken, "backend-a",
                        InstanceEpoch.of(2), NOW.plusSeconds(11)).status());
        Assertions.assertFalse(runtime.registry().heartbeat(heartbeat(1), NOW.plusSeconds(11)));
    }

    @Test void redisOutagePartitionBackendLossAndDrainPauseReservations() {
        ProxyAdapterRuntime runtime = runtime(new ProxyReservationCoordinator(), 1);
        CrossServerIntent intent = intent("subject-degraded");
        runtime.updateAvailability(false, true, false, false);
        assertPaused(runtime, intent, DegradationState.RESERVATIONS_PAUSED,
                "coordination-unavailable");
        runtime.updateAvailability(true, true, true, false);
        assertPaused(runtime, intent, DegradationState.RESERVATIONS_PAUSED, "network-partition");
        runtime.updateAvailability(true, false, false, false);
        assertPaused(runtime, intent, DegradationState.LOCAL_ONLY, "backend-unreachable");
        runtime.updateAvailability(true, true, false, true);
        assertPaused(runtime, intent, DegradationState.DRAINING, "proxy-draining");
        runtime.updateAvailability(true, true, false, false);
        Assertions.assertEquals(CrossServerTransferResult.Status.READY, prepare(runtime, intent, NOW).status());
    }

    @Test void signaturesRejectForgerySubstitutionExpiryAudienceAndClockSkew() {
        Map<String, byte[]> keys = keys();
        ProxyMessageSecurity verifier = security(ProtocolVersion.of(1, 0), "proxy-a", keys);
        SignedProxyMessage valid = verifier.sign("key-a", "nonce-security-01", NOW,
                NOW.plusSeconds(5), PAYLOAD);
        Assertions.assertArrayEquals(PAYLOAD, verifier.authenticate(valid, NOW));
        Assertions.assertThrows(SecurityException.class, () -> verifier.authenticate(valid, NOW));

        ProxyMessageSecurity signer = security(ProtocolVersion.of(1, 0), "proxy-a", keys);
        SignedProxyMessage original = signer.sign("key-a", "nonce-security-02", NOW,
                NOW.plusSeconds(5), PAYLOAD);
        byte[] forgedSignature = original.signature();
        forgedSignature[0] ^= 1;
        SignedProxyMessage forged = SignedProxyMessage.of(original.protocol(), original.environment(),
                original.audience(), original.keyId(), "nonce-security-03", original.issuedAt(),
                original.deadline(), original.payload(), forgedSignature);
        Assertions.assertThrows(SecurityException.class, () -> signer.authenticate(forged, NOW));

        ProxyMessageSecurity destination = security(ProtocolVersion.of(1, 0), "proxy-b", keys);
        SignedProxyMessage substituted = SignedProxyMessage.of(original.protocol(), original.environment(),
                "proxy-b", original.keyId(), "nonce-security-04", original.issuedAt(),
                original.deadline(), original.payload(), original.signature());
        Assertions.assertThrows(SecurityException.class, () -> destination.authenticate(substituted, NOW));
        Assertions.assertThrows(SecurityException.class, () -> destination.authenticate(original, NOW));
        Assertions.assertThrows(SecurityException.class,
                () -> signer.authenticate(signer.sign("key-a", "nonce-security-05", NOW,
                        NOW.plusSeconds(5), PAYLOAD), NOW.plusSeconds(5)));
        Assertions.assertThrows(SecurityException.class,
                () -> signer.authenticate(signer.sign("key-a", "nonce-security-06",
                        NOW.minusSeconds(31), NOW.plusSeconds(5), PAYLOAD), NOW));
        Assertions.assertThrows(SecurityException.class,
                () -> signer.authenticate(signer.sign("key-a", "nonce-security-07",
                        NOW.plusSeconds(31), NOW.plusSeconds(35), PAYLOAD), NOW));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> signer.sign("key-a", "nonce-security-08", NOW, NOW.plusSeconds(5),
                        new byte[SignedProxyMessage.MAX_PAYLOAD_BYTES + 1]));
    }

    @Test void rollingMinorSchemaWorksWhileMajorMismatchFailsClosed() {
        Map<String, byte[]> keys = keys();
        ProxyMessageSecurity newerMinor = security(ProtocolVersion.of(1, 1), "proxy", keys);
        SignedProxyMessage rolling = newerMinor.sign("key-a", "nonce-rolling-001", NOW,
                NOW.plusSeconds(5), PAYLOAD);
        Assertions.assertArrayEquals(PAYLOAD,
                security(ProtocolVersion.of(1, 0), "proxy", keys).authenticate(rolling, NOW));
        SignedProxyMessage incompatible = security(ProtocolVersion.of(2, 0), "proxy", keys)
                .sign("key-a", "nonce-rolling-002", NOW, NOW.plusSeconds(5), PAYLOAD);
        Assertions.assertThrows(SecurityException.class,
                () -> security(ProtocolVersion.of(1, 0), "proxy", keys)
                        .authenticate(incompatible, NOW));
    }

    @Test void proxyFourEquivalentRoutingMeetsLatencyAndMemoryBounds() {
        BackendRegistry registry = new BackendRegistry();
        for (int index = 0; index < 4; index++) {
            String id = "backend-" + index;
            registry.register(BackendRegistration.of(BackendId.of(id), InstanceEpoch.of(1),
                    BackendCapabilities.of(Collections.singleton("flow.queue")),
                    BackendStatus.ONLINE, NOW));
            registry.heartbeat(Heartbeat.of(BackendId.of(id), InstanceEpoch.of(1),
                    CapacitySnapshot.of(1000, 0, 0), HealthSnapshot.of(
                            HealthSnapshot.State.HEALTHY, "ok", NOW), NOW,
                    NOW.plusSeconds(30)), NOW);
        }
        ProxyRoutingEngine router = new ProxyRoutingEngine(registry, null);
        int operations = 8000;
        long[] nanos = new long[operations];
        for (int index = 0; index < operations; index++) {
            RoutingRequest request = RoutingRequest.of(UUID.randomUUID(), "subject-" + index,
                    "proxy", Collections.singleton("flow.queue"), NOW, NOW.plusSeconds(5));
            long started = System.nanoTime();
            Assertions.assertEquals(RoutingResult.Status.ROUTED, router.route(request, NOW).status());
            nanos[index] = System.nanoTime() - started;
        }
        Arrays.sort(nanos);
        Assertions.assertTrue(percentile(nanos, 0.95D) <= 5_000_000L, "PROXY_4 p95 exceeded 5 ms");
        Assertions.assertTrue(percentile(nanos, 0.99D) <= 15_000_000L, "PROXY_4 p99 exceeded 15 ms");
        Assertions.assertTrue(registry.registrations().size() <= BackendRegistry.MAX_BACKENDS);
        Assertions.assertEquals(4, registry.registrations().size());
    }

    private static void assertPaused(final ProxyAdapterRuntime runtime,
            final CrossServerIntent intent, final DegradationState state, final String code) {
        Assertions.assertFalse(runtime.reservationsAllowed());
        ProxyDiagnostic diagnostic = runtime.diagnostic(NOW);
        Assertions.assertEquals(state, diagnostic.state());
        Assertions.assertEquals(code, diagnostic.code());
        CrossServerTransferResult result = prepare(runtime, intent, NOW);
        Assertions.assertEquals(CrossServerTransferResult.Status.RESERVATION_FAILED, result.status());
        Assertions.assertEquals("coordination-unavailable", result.code());
    }

    private static long percentile(final long[] sorted, final double percentile) {
        return sorted[(int) Math.ceil(sorted.length * percentile) - 1];
    }

    private static CrossServerTransferResult prepare(final ProxyAdapterRuntime runtime,
            final CrossServerIntent intent, final Instant now) {
        OwnerHandoff handoff = OwnerHandoff.of(intent.operationId(), "owner-decision", 1, true,
                Collections.singleton("flow.queue"));
        return new CrossServerWorkflow(runtime).prepare(intent, handoff, UUID.randomUUID(), now);
    }

    private static CrossServerIntent intent(final String subject) {
        return CrossServerIntent.of(UUID.randomUUID(), CrossServerFlowType.REMOTE_QUEUE, subject,
                "proxy", "queue-owner", Collections.singletonMap("queue_ref", "queue-a"),
                Collections.<String>emptyList(), NOW, NOW.plusSeconds(30));
    }

    private static ProxyAdapterRuntime runtime(final ProxyReservationCoordinator authority,
            final long epoch) {
        BackendRegistry registry = new BackendRegistry();
        registry.register(registration(epoch));
        registry.heartbeat(heartbeat(epoch), NOW);
        ProxyAdapterRuntime runtime = new ProxyAdapterRuntime(registry,
                new ProxyRoutingEngine(registry, null), authority,
                security(ProtocolVersion.of(1, 0), "proxy", keys()));
        runtime.start();
        return runtime;
    }

    private static BackendRegistration registration(final long epoch) {
        return BackendRegistration.of(BackendId.of("backend-a"), InstanceEpoch.of(epoch),
                BackendCapabilities.of(Collections.singleton("flow.queue")), BackendStatus.ONLINE, NOW);
    }

    private static Heartbeat heartbeat(final long epoch) {
        Instant issued = epoch == 1 ? NOW : NOW.plusSeconds(11);
        return Heartbeat.of(BackendId.of("backend-a"), InstanceEpoch.of(epoch),
                CapacitySnapshot.of(100, 0, 0), HealthSnapshot.of(HealthSnapshot.State.HEALTHY,
                        "ok", issued), issued, issued.plusSeconds(10));
    }

    private static ProxyMessageSecurity security(final ProtocolVersion version,
            final String audience, final Map<String, byte[]> keys) {
        return new ProxyMessageSecurity(version, "test", audience, keys);
    }

    private static Map<String, byte[]> keys() {
        Map<String, byte[]> keys = new LinkedHashMap<String, byte[]>();
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 11);
        keys.put("key-a", key);
        return keys;
    }
}
