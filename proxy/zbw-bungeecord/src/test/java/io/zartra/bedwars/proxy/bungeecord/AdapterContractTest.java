package io.zartra.bedwars.proxy.bungeecord;

import io.zartra.bedwars.proxy.bungeecord.BungeeProxyAdapter;
import io.zartra.bedwars.proxy.api.BackendId;
import io.zartra.bedwars.proxy.api.BackendRegistry;
import io.zartra.bedwars.proxy.api.ProtocolVersion;
import io.zartra.bedwars.proxy.api.ProxyAdapterRuntime;
import io.zartra.bedwars.proxy.api.ProxyMessageSecurity;
import io.zartra.bedwars.proxy.api.ProxyReservationCoordinator;
import io.zartra.bedwars.proxy.api.ProxyRoutingEngine;
import io.zartra.bedwars.proxy.api.SignedProxyMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Common semantic adapter contract for ZBW-DEPLOY-004. */
class AdapterContractTest {
    @Test void lifecycleMessagingAndAuthenticationAreEquivalent() {
        Instant now = Instant.ofEpochSecond(100);
        Map<String, byte[]> keys = new LinkedHashMap<String, byte[]>();
        keys.put("key-a", new byte[32]);
        BackendRegistry registry = new BackendRegistry();
        ProxyAdapterRuntime runtime = new ProxyAdapterRuntime(registry,
                new ProxyRoutingEngine(registry, null), new ProxyReservationCoordinator(),
                new ProxyMessageSecurity(ProtocolVersion.of(1, 0), "test", "proxy", keys));
        BungeeProxyAdapter adapter = new BungeeProxyAdapter(runtime, (backend, message) -> CompletableFuture.completedFuture(null));
        Assertions.assertTrue(adapter.start());
        Assertions.assertFalse(adapter.start());
        SignedProxyMessage message = new ProxyMessageSecurity(ProtocolVersion.of(1, 0), "test", "proxy", keys)
                .sign("key-a", "nonce-000000001", now, now.plusSeconds(5), "ok".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), adapter.receive(message, now));
        Assertions.assertDoesNotThrow(() -> adapter.send(BackendId.of("backend-a"), message).toCompletableFuture().join());
        Assertions.assertTrue(adapter.stop());
        Assertions.assertThrows(IllegalStateException.class, () -> runtime.registry());
    }
}
