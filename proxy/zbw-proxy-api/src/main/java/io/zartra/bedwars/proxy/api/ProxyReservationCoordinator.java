package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Atomic bounded reservation and transfer-token coordinator for one proxy process. */
public final class ProxyReservationCoordinator {
    /** Maximum in-flight reservations. */
    public static final int MAX_RESERVATIONS = 5000;
    /** Maximum retained single-use token records. */
    public static final int MAX_CONSUMED_TOKENS = 10000;
    private final Map<ProxyReservationId, ReservationRequest> reservations = new HashMap<ProxyReservationId, ReservationRequest>();
    private final Map<String, ProxyReservationId> subjects = new HashMap<String, ProxyReservationId>();
    private final Map<UUID, Instant> consumed = new LinkedHashMap<UUID, Instant>();

    /** Atomically reserves a subject against the current backend epoch. */
    public synchronized ReservationResult reserve(final ReservationRequest request,
            final InstanceEpoch currentEpoch, final Instant now) {
        Objects.requireNonNull(request, "request");
        cleanup(Objects.requireNonNull(now, "now"));
        if (!request.epoch().equals(currentEpoch)) {
            return ReservationResult.failed(request.id(), ReservationResult.Status.STALE_EPOCH);
        }
        if (!now.isBefore(request.expiresAt())) {
            return ReservationResult.failed(request.id(), ReservationResult.Status.EXPIRED);
        }
        ProxyReservationId existing = subjects.get(request.subjectReference());
        if (existing != null || reservations.containsKey(request.id())
                || reservations.size() >= MAX_RESERVATIONS) {
            return ReservationResult.failed(request.id(), ReservationResult.Status.CONFLICT);
        }
        reservations.put(request.id(), request);
        subjects.put(request.subjectReference(), request.id());
        return ReservationResult.reserved(request.id(), request.backendId(), request.epoch(), request.expiresAt());
    }

    /** Creates bounded 15-second transfer claims for an existing reservation. */
    public synchronized TransferToken token(final ProxyReservationId id, final UUID tokenId,
            final Instant now) {
        cleanup(Objects.requireNonNull(now, "now"));
        ReservationRequest request = reservations.get(Objects.requireNonNull(id, "id"));
        if (request == null) {
            throw new IllegalStateException("reservation unavailable");
        }
        Instant expiry = request.expiresAt().isBefore(now.plusSeconds(15))
                ? request.expiresAt() : now.plusSeconds(15);
        return TransferToken.of(tokenId, id, request.backendId(), request.epoch(),
                request.audience(), now, expiry);
    }

    /** Atomically validates and consumes a token once. */
    public synchronized TokenConsumptionResult consume(final TransferToken token,
            final String audience, final InstanceEpoch epoch, final Instant now) {
        Objects.requireNonNull(token, "token");
        cleanup(Objects.requireNonNull(now, "now"));
        boolean duplicate = consumed.containsKey(token.tokenId());
        TokenConsumptionResult result = token.evaluateConsumption(audience, epoch, now, duplicate);
        if (result.consumed()) {
            if (consumed.size() >= MAX_CONSUMED_TOKENS) {
                return TokenConsumptionResult.of(token.tokenId(), TokenConsumptionResult.Status.INVALID);
            }
            ReservationRequest request = reservations.remove(token.reservationId());
            if (request == null) {
                return TokenConsumptionResult.of(token.tokenId(), TokenConsumptionResult.Status.INVALID);
            }
            subjects.remove(request.subjectReference());
            consumed.put(token.tokenId(), token.expiresAt());
        }
        return result;
    }

    /** Returns bounded in-flight reservation metadata count. */
    public synchronized int pendingReservations() {
        return reservations.size();
    }

    /** Returns bounded recently consumed token metadata count. */
    public synchronized int consumedTokens() {
        return consumed.size();
    }

    private void cleanup(final Instant now) {
        for (ReservationRequest request : new HashMap<ProxyReservationId, ReservationRequest>(reservations).values()) {
            if (!now.isBefore(request.expiresAt())) {
                reservations.remove(request.id());
                subjects.remove(request.subjectReference());
            }
        }
        for (Map.Entry<UUID, Instant> token
                : new LinkedHashMap<UUID, Instant>(consumed).entrySet()) {
            if (!now.isBefore(token.getValue())) {
                consumed.remove(token.getKey());
            }
        }
    }
}
