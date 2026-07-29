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
    private volatile boolean coordinationAvailable = true;
    private volatile boolean backendReachable = true;
    private volatile boolean partitioned;
    private volatile boolean draining;

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
    /**
     * Updates cached deployment availability without performing network work.
     *
     * <p>Adapters feed this snapshot from M19 health and proxy/backend transport callbacks.
     */
    public synchronized void updateAvailability(final boolean coordinationAvailable,
            final boolean backendReachable, final boolean partitioned, final boolean draining) {
        this.coordinationAvailable = coordinationAvailable;
        this.backendReachable = backendReachable;
        this.partitioned = partitioned;
        this.draining = draining;
    }
    /** Reports whether a new cross-node reservation is currently safe. */
    public boolean reservationsAllowed() {
        return running() && coordinationAvailable && backendReachable && !partitioned && !draining;
    }
    /** Returns a privacy-safe operational health report. */
    public ProxyDiagnostic diagnostic(final Instant now) {
        Objects.requireNonNull(now, "now");
        final DegradationState state;
        final String code;
        if (!running()) {
            state = DegradationState.OFFLINE;
            code = "runtime-stopped";
        } else if (draining) {
            state = DegradationState.DRAINING;
            code = "proxy-draining";
        } else if (!coordinationAvailable || partitioned) {
            state = DegradationState.RESERVATIONS_PAUSED;
            code = partitioned ? "network-partition" : "coordination-unavailable";
        } else if (!backendReachable) {
            state = DegradationState.LOCAL_ONLY;
            code = "backend-unreachable";
        } else {
            state = DegradationState.NORMAL;
            code = "ready";
        }
        return ProxyDiagnostic.of(state, code, registry.registrations().size(),
                reservations.pendingReservations(), now);
    }
    private void requireRunning() { if (!running()) { throw new IllegalStateException("proxy runtime is stopped"); } }
}
