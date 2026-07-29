package io.zartra.bedwars.proxy.bungeecord;

import io.zartra.bedwars.proxy.bungeecord.BungeeProxyAdapter;
import io.zartra.bedwars.proxy.api.BackendCapabilities;
import io.zartra.bedwars.proxy.api.BackendId;
import io.zartra.bedwars.proxy.api.BackendRegistration;
import io.zartra.bedwars.proxy.api.BackendRegistry;
import io.zartra.bedwars.proxy.api.BackendStatus;
import io.zartra.bedwars.proxy.api.CapacitySnapshot;
import io.zartra.bedwars.proxy.api.CrossServerFlowType;
import io.zartra.bedwars.proxy.api.CrossServerIntent;
import io.zartra.bedwars.proxy.api.CrossServerTransferResult;
import io.zartra.bedwars.proxy.api.CrossServerWorkflow;
import io.zartra.bedwars.proxy.api.HealthSnapshot;
import io.zartra.bedwars.proxy.api.Heartbeat;
import io.zartra.bedwars.proxy.api.InstanceEpoch;
import io.zartra.bedwars.proxy.api.OwnerHandoff;
import io.zartra.bedwars.proxy.api.ProtocolVersion;
import io.zartra.bedwars.proxy.api.ProxyAdapterRuntime;
import io.zartra.bedwars.proxy.api.ProxyMessageSecurity;
import io.zartra.bedwars.proxy.api.ProxyReservationCoordinator;
import io.zartra.bedwars.proxy.api.ProxyRoutingEngine;
import io.zartra.bedwars.proxy.api.SignedProxyMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Common semantic adapter contract for ZBW-DEPLOY-004 and cross-server flows. */
class AdapterContractTest {
    @Test void lifecycleMessagingAuthenticationAndTransferAreEquivalent() {
        Instant now = Instant.ofEpochSecond(100);
        Map<String, byte[]> keys = new LinkedHashMap<String, byte[]>();
        keys.put("key-a", new byte[32]);
        BackendRegistry registry = new BackendRegistry();
        ProxyAdapterRuntime runtime = new ProxyAdapterRuntime(registry,
                new ProxyRoutingEngine(registry, null), new ProxyReservationCoordinator(),
                new ProxyMessageSecurity(ProtocolVersion.of(1, 0), "test", "proxy", keys));
        AtomicReference<BackendId> sent = new AtomicReference<BackendId>();
        BungeeProxyAdapter adapter = new BungeeProxyAdapter(runtime, (backend, message) -> {
            sent.set(backend);
            return CompletableFuture.completedFuture(null);
        });
        Assertions.assertTrue(adapter.start());
        Assertions.assertFalse(adapter.start());
        SignedProxyMessage message = new ProxyMessageSecurity(ProtocolVersion.of(1, 0),
                "test", "proxy", keys).sign("key-a", "nonce-000000001", now,
                        now.plusSeconds(5), "ok".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8),
                adapter.receive(message, now));
        registry.register(BackendRegistration.of(BackendId.of("backend-a"), InstanceEpoch.of(1),
                BackendCapabilities.of(Collections.singleton("flow.queue")), BackendStatus.ONLINE, now));
        registry.heartbeat(Heartbeat.of(BackendId.of("backend-a"), InstanceEpoch.of(1),
                CapacitySnapshot.of(10, 0, 0), HealthSnapshot.of(HealthSnapshot.State.HEALTHY,
                        "ok", now), now, now.plusSeconds(10)), now);
        CrossServerIntent intent = CrossServerIntent.of(UUID.randomUUID(),
                CrossServerFlowType.REMOTE_QUEUE, "subject-a", "proxy", "queue-owner",
                Collections.singletonMap("queue_ref", "queue-a"), Collections.<String>emptyList(),
                now, now.plusSeconds(5));
        OwnerHandoff handoff = OwnerHandoff.of(intent.operationId(), "decision-a", 1, true,
                Collections.singleton("flow.queue"));
        CrossServerTransferResult transfer = new CrossServerWorkflow(runtime).prepare(intent,
                handoff, UUID.randomUUID(), now);
        adapter.dispatch(transfer, message).toCompletableFuture().join();
        Assertions.assertEquals(BackendId.of("backend-a"), sent.get());
        Assertions.assertThrows(IllegalArgumentException.class, () -> adapter.dispatch(
                CrossServerTransferResult.failed(UUID.randomUUID(), CrossServerFlowType.REMOTE_QUEUE,
                        CrossServerTransferResult.Status.ROUTE_FAILED, "failed"), message));
        Assertions.assertTrue(adapter.stop());
        Assertions.assertThrows(IllegalStateException.class, () -> runtime.registry());
    }
}
