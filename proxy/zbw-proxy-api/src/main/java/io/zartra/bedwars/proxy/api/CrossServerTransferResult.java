package io.zartra.bedwars.proxy.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Explicit cross-server orchestration outcome without domain or platform objects. */
public final class CrossServerTransferResult {
    /** Workflow outcome. */ public enum Status {
        READY,
        OWNER_REJECTED,
        ROUTE_FAILED,
        RESERVATION_FAILED
    }
    private final UUID operationId;
    private final CrossServerFlowType type;
    private final Status status;
    private final RoutingResult routing;
    private final ReservationResult reservation;
    private final TransferToken token;
    private final String code;

    private CrossServerTransferResult(final UUID operationId, final CrossServerFlowType type,
            final Status status, final RoutingResult routing, final ReservationResult reservation,
            final TransferToken token, final String code) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.type = Objects.requireNonNull(type, "type");
        this.status = Objects.requireNonNull(status, "status");
        this.code = ProxyContractValidation.token(code, "code");
        if ((status == Status.READY) != (routing != null && reservation != null && token != null)) {
            throw new IllegalArgumentException("ready result requires routing, reservation and token");
        }
        this.routing = routing;
        this.reservation = reservation;
        this.token = token;
    }
    /** Creates a ready transfer. */
    public static CrossServerTransferResult ready(final UUID operationId,
            final CrossServerFlowType type, final RoutingResult routing,
            final ReservationResult reservation, final TransferToken token) {
        return new CrossServerTransferResult(operationId, type, Status.READY,
                routing, reservation, token, "ready");
    }
    /** Creates a rejected transfer. */
    public static CrossServerTransferResult failed(final UUID operationId,
            final CrossServerFlowType type, final Status status, final String code) {
        if (status == Status.READY) { throw new IllegalArgumentException("use ready factory"); }
        return new CrossServerTransferResult(operationId, type, status, null, null, null, code);
    }
    /** Returns operation ID. */ public UUID operationId() { return operationId; }
    /** Returns workflow family. */ public CrossServerFlowType type() { return type; }
    /** Returns status. */ public Status status() { return status; }
    /** Returns routing result when ready. */ public Optional<RoutingResult> routing() { return Optional.ofNullable(routing); }
    /** Returns reservation when ready. */ public Optional<ReservationResult> reservation() { return Optional.ofNullable(reservation); }
    /** Returns transfer token when ready. */ public Optional<TransferToken> token() { return Optional.ofNullable(token); }
    /** Returns privacy-safe outcome code. */ public String code() { return code; }
}
