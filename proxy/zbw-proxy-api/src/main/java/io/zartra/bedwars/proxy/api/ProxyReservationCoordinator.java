package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Atomic bounded reservation and transfer-token coordinator for one proxy process. */
public final class ProxyReservationCoordinator {
    /** Maximum in-flight reservations. */
    public static final int MAX_RESERVATIONS = 5000;
    private final Map<ProxyReservationId, ReservationRequest> reservations = new HashMap<ProxyReservationId, ReservationRequest>();
    private final Map<String, ProxyReservationId> subjects = new HashMap<String, ProxyReservationId>();
    private final Set<UUID> consumed = new HashSet<UUID>();

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
        boolean duplicate = consumed.contains(token.tokenId());
        TokenConsumptionResult result = token.evaluateConsumption(audience, epoch, now, duplicate);
        if (result.consumed()) {
            ReservationRequest request = reservations.remove(token.reservationId());
            if (request == null) {
                return TokenConsumptionResult.of(token.tokenId(), TokenConsumptionResult.Status.INVALID);
            }
            subjects.remove(request.subjectReference());
            consumed.add(token.tokenId());
        }
        return result;
    }

    private void cleanup(final Instant now) {
        for (ReservationRequest request : new HashMap<ProxyReservationId, ReservationRequest>(reservations).values()) {
            if (!now.isBefore(request.expiresAt())) {
                reservations.remove(request.id());
                subjects.remove(request.subjectReference());
            }
        }
    }
}
