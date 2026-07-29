package io.zartra.bedwars.api.integration.party;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable provider mutation or transfer intent. */
public final class PartyIntent {
    private final IdempotencyKey operationId;
    private final Action action;
    private final PartyId partyId;
    private final PlayerId actorId;
    private final PlayerId subjectId;
    private final String destination;
    private final Instant deadline;

    /**
     * Creates a party intent.
     *
     * @param operationId idempotency identity
     * @param action requested action
     * @param partyId party identity, nullable only for create
     * @param actorId acting player
     * @param subjectId optional affected player
     * @param destination optional sanitized transfer destination
     * @param deadline operation deadline
     */
    public PartyIntent(final IdempotencyKey operationId, final Action action,
                       final PartyId partyId, final PlayerId actorId,
                       final PlayerId subjectId, final String destination,
                       final Instant deadline) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.action = Objects.requireNonNull(action, "action");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        if (action != Action.CREATE && partyId == null) {
            throw new IllegalArgumentException("partyId is required");
        }
        if (destination != null
                && !destination.matches("[a-z0-9][a-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException("destination must be sanitized");
        }
        this.partyId = partyId;
        this.subjectId = subjectId;
        this.destination = destination;
    }

    /** @return idempotency identity */
    public IdempotencyKey operationId() { return operationId; }
    /** @return requested action */
    public Action action() { return action; }
    /** @return optional party identity */
    public Optional<PartyId> partyId() { return Optional.ofNullable(partyId); }
    /** @return actor identity */
    public PlayerId actorId() { return actorId; }
    /** @return optional affected player */
    public Optional<PlayerId> subjectId() { return Optional.ofNullable(subjectId); }
    /** @return optional transfer destination */
    public Optional<String> destination() { return Optional.ofNullable(destination); }
    /** @return operation deadline */
    public Instant deadline() { return deadline; }

    /** Supported provider-neutral party actions. */
    public enum Action {
        CREATE, INVITE, ACCEPT, LEAVE, KICK, PROMOTE, DISBAND, TRANSFER
    }
}
