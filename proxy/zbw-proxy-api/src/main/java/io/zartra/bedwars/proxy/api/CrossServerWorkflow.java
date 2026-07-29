package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owner-decision-to-transfer workflow shared by both proxy adapters. */
public final class CrossServerWorkflow {
    private final ProxyAdapterRuntime runtime;

    /** Creates a workflow over the running neutral proxy runtime. */
    public CrossServerWorkflow(final ProxyAdapterRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /** Routes, reserves and issues one bounded transfer token. */
    public CrossServerTransferResult prepare(final CrossServerIntent intent,
            final OwnerHandoff handoff, final UUID tokenId, final Instant now) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(handoff, "handoff");
        Objects.requireNonNull(tokenId, "tokenId");
        Objects.requireNonNull(now, "now");
        if (!handoff.appliesTo(intent) || !handoff.authorized() || intent.expiredAt(now)) {
            return CrossServerTransferResult.failed(intent.operationId(), intent.type(),
                    CrossServerTransferResult.Status.OWNER_REJECTED, "owner-rejected");
        }
        if (!runtime.reservationsAllowed()) {
            return CrossServerTransferResult.failed(intent.operationId(), intent.type(),
                    CrossServerTransferResult.Status.RESERVATION_FAILED,
                    "coordination-unavailable");
        }
        RoutingRequest routingRequest = RoutingRequest.of(intent.operationId(),
                intent.subjectReference(), intent.audience(), handoff.requiredCapabilities(),
                intent.requestedAt(), intent.deadline());
        RoutingResult routing = runtime.route(routingRequest, now);
        if (routing.status() != RoutingResult.Status.ROUTED) {
            return CrossServerTransferResult.failed(intent.operationId(), intent.type(),
                    CrossServerTransferResult.Status.ROUTE_FAILED, routing.code());
        }
        BackendId backend = routing.backendId().get();
        InstanceEpoch epoch = routing.epoch().get();
        Instant expiry = intent.deadline().isBefore(now.plusSeconds(15))
                ? intent.deadline() : now.plusSeconds(15);
        ProxyReservationId reservationId = ProxyReservationId.parse(intent.operationId().toString());
        ReservationRequest request = ReservationRequest.of(reservationId, backend, epoch,
                intent.subjectReference(), backend.value(), now, expiry);
        ReservationResult reservation = runtime.reservations().reserve(request, epoch, now);
        if (reservation.status() != ReservationResult.Status.RESERVED) {
            return CrossServerTransferResult.failed(intent.operationId(), intent.type(),
                    CrossServerTransferResult.Status.RESERVATION_FAILED,
                    "reservation-" + reservation.status().name().toLowerCase().replace('_', '-'));
        }
        TransferToken token = runtime.reservations().token(reservationId, tokenId, now);
        return CrossServerTransferResult.ready(intent.operationId(), intent.type(), routing,
                reservation, token);
    }

    /** Atomically admits one prepared transfer or returns a safe failure. */
    public TokenConsumptionResult complete(final TransferToken token, final String audience,
            final InstanceEpoch epoch, final Instant now) {
        return runtime.reservations().consume(token, audience, epoch, now);
    }
}
