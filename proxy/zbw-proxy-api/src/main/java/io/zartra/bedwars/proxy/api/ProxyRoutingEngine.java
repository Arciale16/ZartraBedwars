package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Deterministic capacity-aware proxy routing orchestration; it never performs matchmaking. */
public final class ProxyRoutingEngine {
    /** Maximum destination attempts. */
    public static final int MAX_RETRIES = 3;
    private final BackendRegistry registry;
    private final BackendId fallbackLobby;

    /** Creates a router with an optional capability-compatible fallback lobby. */
    public ProxyRoutingEngine(final BackendRegistry registry, final BackendId fallbackLobby) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.fallbackLobby = fallbackLobby;
    }

    /** Routes against one deterministic registry snapshot. */
    public RoutingResult route(final RoutingRequest request, final Instant now) {
        Objects.requireNonNull(request, "request");
        if (!Objects.requireNonNull(now, "now").isBefore(request.deadline())) {
            return RoutingResult.failed(request.requestId(), RoutingResult.Status.REJECTED, "deadline-expired");
        }
        List<BackendRegistration> values = registry.registrations();
        int attempts = 0;
        for (BackendRegistration backend : values) {
            if (attempts >= MAX_RETRIES) {
                break;
            }
            if (request.accepts(backend)) {
                attempts++;
                if (registry.available(backend.backendId()) > 0) {
                    return RoutingResult.routed(request.requestId(), backend.backendId(), backend.epoch());
                }
            }
        }
        if (fallbackLobby != null) {
            for (BackendRegistration backend : values) {
                if (backend.backendId().equals(fallbackLobby) && backend.acceptsRouting()
                        && registry.available(fallbackLobby) > 0) {
                    return RoutingResult.routed(request.requestId(), fallbackLobby, backend.epoch());
                }
            }
        }
        return RoutingResult.failed(request.requestId(), RoutingResult.Status.NO_CAPACITY, "no-capacity");
    }
}
