package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared platform-neutral lifecycle facade used identically by BungeeCord and Velocity. */
public final class ProxyAdapterRuntime {
    private final BackendRegistry registry;
    private final ProxyRoutingEngine routing;
    private final ProxyReservationCoordinator reservations;
    private final ProxyMessageSecurity security;
    private final AtomicBoolean running = new AtomicBoolean();

    /** Creates the shared proxy runtime. */
    public ProxyAdapterRuntime(final BackendRegistry registry, final ProxyRoutingEngine routing,
            final ProxyReservationCoordinator reservations, final ProxyMessageSecurity security) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.routing = Objects.requireNonNull(routing, "routing");
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.security = Objects.requireNonNull(security, "security");
    }
    /** Starts once. */ public boolean start() { return running.compareAndSet(false, true); }
    /** Stops once. */ public boolean stop() { return running.compareAndSet(true, false); }
    /** Reports lifecycle state. */ public boolean running() { return running.get(); }
    /** Returns registry. */ public BackendRegistry registry() {
        requireRunning();
        return registry;
    }
    /** Routes without domain calculations. */ public RoutingResult route(final RoutingRequest request, final Instant now) {
        requireRunning();
        return routing.route(request, now);
    }
    /** Returns reservation coordinator. */ public ProxyReservationCoordinator reservations() {
        requireRunning();
        return reservations;
    }
    /** Authenticates before exposing bytes to an adapter decoder. */ public byte[] authenticate(final SignedProxyMessage message, final Instant now) {
        requireRunning();
        return security.authenticate(message, now);
    }
    private void requireRunning() { if (!running()) { throw new IllegalStateException("proxy runtime is stopped"); } }
}
