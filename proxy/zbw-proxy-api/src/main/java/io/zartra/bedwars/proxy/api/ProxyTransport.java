package io.zartra.bedwars.proxy.api;

import java.util.concurrent.CompletionStage;

/** Non-blocking platform transport port for authenticated proxy/backend messages. */
public interface ProxyTransport {
    /** Sends already-authenticated bytes without blocking a proxy event thread. */
    CompletionStage<Void> send(BackendId destination, SignedProxyMessage message);
}
