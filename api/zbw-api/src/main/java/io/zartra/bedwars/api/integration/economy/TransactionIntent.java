package io.zartra.bedwars.api.integration.economy;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Immutable request to an economy adapter; it is not a ledger transaction record. */
public final class TransactionIntent {
    private final IdempotencyKey operationId;
    private final PlayerId playerId;
    private final DefinitionId currencyId;
    private final Direction direction;
    private final BigDecimal amount;
    private final String reasonCode;
    private final Instant deadline;

    /**
     * Creates a bounded economy intent.
     *
     * @param operationId idempotency identity
     * @param playerId affected player
     * @param currencyId stable currency identity
     * @param direction credit or debit
     * @param amount positive bounded amount
     * @param reasonCode sanitized audit reason
     * @param deadline operation deadline
     */
    public TransactionIntent(final IdempotencyKey operationId, final PlayerId playerId,
                             final DefinitionId currencyId, final Direction direction,
                             final BigDecimal amount, final String reasonCode,
                             final Instant deadline) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        if (amount.signum() <= 0 || amount.precision() > 34) {
            throw new IllegalArgumentException("amount must be positive and bounded");
        }
        if (reasonCode == null || !reasonCode.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("reasonCode must be a safe stable key");
        }
        this.reasonCode = reasonCode;
    }

    /** @return idempotency identity */
    public IdempotencyKey operationId() { return operationId; }
    /** @return player identity */
    public PlayerId playerId() { return playerId; }
    /** @return currency identity */
    public DefinitionId currencyId() { return currencyId; }
    /** @return transaction direction */
    public Direction direction() { return direction; }
    /** @return positive amount */
    public BigDecimal amount() { return amount; }
    /** @return sanitized audit reason */
    public String reasonCode() { return reasonCode; }
    /** @return operation deadline */
    public Instant deadline() { return deadline; }

    /** Economy intent direction. */
    public enum Direction { CREDIT, DEBIT }
}
