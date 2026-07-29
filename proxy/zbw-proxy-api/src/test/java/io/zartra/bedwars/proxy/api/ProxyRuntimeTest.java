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

/** ZBW-DEPLOY-002/004 and ZBW-READY-013 routing/security regression tests. */
class ProxyRuntimeTest {
    private static final Instant NOW = Instant.ofEpochSecond(1000);
    private static BackendRegistration backend(final String id, final long epoch, final BackendStatus status) {
        return BackendRegistration.of(BackendId.of(id), InstanceEpoch.of(epoch),
                BackendCapabilities.of(Collections.singleton("mode.bedwars")), status, NOW);
    }
    private static Heartbeat heartbeat(final String id, final long epoch, final int active) {
        return Heartbeat.of(BackendId.of(id), InstanceEpoch.of(epoch), CapacitySnapshot.of(10, active, 0),
                HealthSnapshot.of(HealthSnapshot.State.HEALTHY, "ok", NOW), NOW, NOW.plusSeconds(10));
    }

    @Test void registryRejectsStaleAndOrdersDeterministically() {
        BackendRegistry registry = new BackendRegistry();
        registry.register(backend("backend-b", 2, BackendStatus.ONLINE));
        registry.register(backend("backend-a", 1, BackendStatus.ONLINE));
        Assertions.assertFalse(registry.heartbeat(heartbeat("backend-b", 1, 0), NOW));
        Assertions.assertTrue(registry.heartbeat(heartbeat("backend-b", 2, 0), NOW));
        Assertions.assertEquals("backend-a", registry.registrations().get(0).backendId().value());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> registry.register(backend("backend-b", 1, BackendStatus.ONLINE)));
        registry.expire(NOW.plusSeconds(10));
        Assertions.assertEquals(BackendStatus.OFFLINE, registry.registrations().get(1).status());
    }

    @Test void routerHonorsHealthDrainCapacityAndFallback() {
        BackendRegistry registry = new BackendRegistry();
        registry.register(backend("game-a", 1, BackendStatus.DRAINING));
        registry.register(backend("game-b", 1, BackendStatus.ONLINE));
        registry.register(backend("lobby", 1, BackendStatus.ONLINE));
        registry.heartbeat(heartbeat("game-a", 1, 0), NOW);
        registry.heartbeat(heartbeat("game-b", 1, 2), NOW);
        registry.heartbeat(heartbeat("lobby", 1, 0), NOW);
        RoutingRequest request = RoutingRequest.of(UUID.randomUUID(), "subject-01", "proxy-main",
                Collections.singleton("mode.bedwars"), NOW, NOW.plusSeconds(5));
        ProxyRoutingEngine engine = new ProxyRoutingEngine(registry, BackendId.of("lobby"));
        Assertions.assertEquals(BackendId.of("game-b"), engine.route(request, NOW).backendId().get());
        registry.status(BackendId.of("game-b"), InstanceEpoch.of(1), BackendStatus.UNHEALTHY);
        Assertions.assertEquals(BackendId.of("lobby"), engine.route(request, NOW).backendId().get());
        Assertions.assertEquals(RoutingResult.Status.REJECTED, engine.route(request, NOW.plusSeconds(5)).status());
    }

    @Test void reservationIsAtomicAndTokenIsSingleUse() {
        ProxyReservationCoordinator coordinator = new ProxyReservationCoordinator();
        ProxyReservationId id = ProxyReservationId.random();
        ReservationRequest request = ReservationRequest.of(id, BackendId.of("game-a"), InstanceEpoch.of(4),
                "subject-01", "game-a", NOW, NOW.plusSeconds(15));
        Assertions.assertEquals(ReservationResult.Status.RESERVED,
                coordinator.reserve(request, InstanceEpoch.of(4), NOW).status());
        Assertions.assertEquals(ReservationResult.Status.CONFLICT,
                coordinator.reserve(request, InstanceEpoch.of(4), NOW).status());
        TransferToken token = coordinator.token(id, UUID.randomUUID(), NOW);
        Assertions.assertEquals(TokenConsumptionResult.Status.CONSUMED,
                coordinator.consume(token, "game-a", InstanceEpoch.of(4), NOW).status());
        Assertions.assertEquals(TokenConsumptionResult.Status.DUPLICATE,
                coordinator.consume(token, "game-a", InstanceEpoch.of(4), NOW).status());
    }

    @Test void authenticationPrecedesPayloadExposureAndRejectsReplay() {
        Map<String, byte[]> keys = new LinkedHashMap<String, byte[]>();
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        keys.put("key-a", key);
        ProxyMessageSecurity security = new ProxyMessageSecurity(ProtocolVersion.of(1, 0),
                "production", "proxy-main", keys);
        SignedProxyMessage message = security.sign("key-a", "nonce-0000000001", NOW,
                NOW.plusSeconds(5), "heartbeat".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals("heartbeat".getBytes(StandardCharsets.UTF_8),
                security.authenticate(message, NOW));
        Assertions.assertThrows(SecurityException.class, () -> security.authenticate(message, NOW));
        byte[] bad = message.signature();
        bad[0] ^= 1;
        SignedProxyMessage forged = SignedProxyMessage.of(message.protocol(), message.environment(),
                message.audience(), message.keyId(), "nonce-0000000002", message.issuedAt(),
                message.deadline(), message.payload(), bad);
        Assertions.assertThrows(SecurityException.class, () -> security.authenticate(forged, NOW));
    }
    @Test void negativeRuntimeBranchesFailClosed() {
        BackendRegistry registry = new BackendRegistry();
        Assertions.assertFalse(registry.status(BackendId.of("missing"), InstanceEpoch.of(1), BackendStatus.ONLINE));
        registry.register(backend("game-a", 1, BackendStatus.ONLINE));
        Assertions.assertFalse(registry.status(BackendId.of("game-a"), InstanceEpoch.of(2), BackendStatus.DRAINING));
        RoutingRequest request = RoutingRequest.of(UUID.randomUUID(), "subject-02", "proxy-main",
                Collections.singleton("mode.bedwars"), NOW, NOW.plusSeconds(5));
        Assertions.assertEquals(RoutingResult.Status.NO_CAPACITY,
                new ProxyRoutingEngine(registry, null).route(request, NOW).status());
        ProxyReservationCoordinator coordinator = new ProxyReservationCoordinator();
        ReservationRequest stale = ReservationRequest.of(ProxyReservationId.random(), BackendId.of("game-a"),
                InstanceEpoch.of(1), "subject-02", "game-a", NOW, NOW.plusSeconds(5));
        Assertions.assertEquals(ReservationResult.Status.STALE_EPOCH,
                coordinator.reserve(stale, InstanceEpoch.of(2), NOW).status());
        Assertions.assertThrows(IllegalStateException.class,
                () -> coordinator.token(stale.id(), UUID.randomUUID(), NOW));
    }
    @Test void authenticatedPeerRateIsBounded() {
        Map<String, byte[]> keys = new LinkedHashMap<String, byte[]>();
        keys.put("key-a", new byte[32]);
        ProxyMessageSecurity security = new ProxyMessageSecurity(ProtocolVersion.of(1, 0),
                "test", "proxy", keys);
        for (int index = 0; index < ProxyMessageSecurity.BURST; index++) {
            String nonce = "rate-nonce-" + index;
            SignedProxyMessage message = security.sign("key-a", nonce, NOW, NOW.plusSeconds(5),
                    "ok".getBytes(StandardCharsets.UTF_8));
            security.authenticate(message, NOW);
        }
        SignedProxyMessage excess = security.sign("key-a", "rate-nonce-excess", NOW,
                NOW.plusSeconds(5), "ok".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(SecurityException.class, () -> security.authenticate(excess, NOW));
    }
}
