package io.zartra.bedwars.proxy.bungeecord;

import io.zartra.bedwars.proxy.api.BackendId;
import io.zartra.bedwars.proxy.api.CrossServerTransferResult;
import io.zartra.bedwars.proxy.api.ProxyAdapterRuntime;
import io.zartra.bedwars.proxy.api.ProxyTransport;
import io.zartra.bedwars.proxy.api.SignedProxyMessage;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** BungeeCord-facing lifecycle and plugin-message boundary with no domain ownership. */
public final class BungeeProxyAdapter {
    private final ProxyAdapterRuntime runtime;
    private final ProxyTransport transport;
    /** Creates an adapter over a platform-supplied non-blocking transport. */
    public BungeeProxyAdapter(final ProxyAdapterRuntime runtime, final ProxyTransport transport) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.transport = Objects.requireNonNull(transport, "transport");
    }
    /** Registers runtime lifecycle. */ public boolean start() { return runtime.start(); }
    /** Unregisters runtime lifecycle. */ public boolean stop() { return runtime.stop(); }
    /** Authenticates plugin bytes before a platform decoder sees them. */ public byte[] receive(final SignedProxyMessage message, final Instant now) { return runtime.authenticate(message, now); }
    /** Sends without blocking the proxy event loop. */ public CompletionStage<Void> send(final BackendId backend, final SignedProxyMessage message) { return transport.send(backend, message); }
    /** Dispatches one prepared cross-server transfer through the platform transport. */
    public CompletionStage<Void> dispatch(final CrossServerTransferResult transfer,
            final SignedProxyMessage message) {
        Objects.requireNonNull(transfer, "transfer");
        if (transfer.status() != CrossServerTransferResult.Status.READY) {
            throw new IllegalArgumentException("transfer is not ready");
        }
        BackendId backend = transfer.routing().get().backendId().get();
        return send(backend, message);
    }
    /** Returns shared semantic runtime. */ public ProxyAdapterRuntime runtime() { return runtime; }
}
