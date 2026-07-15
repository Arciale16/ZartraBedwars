package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Instant;
import java.util.Objects;

/** Immutable secret-free authorization and mutation audit fact. */
public final class ArenaAuditRecord {
    private final ArenaOperation operation;
    private final ArenaId arenaId;
    private final AuthorizationSubject actor;
    private final boolean allowed;
    private final DefinitionId outcome;
    private final CorrelationId correlationId;
    private final Instant occurredAt;

    /** Creates one complete audit fact. */
    public ArenaAuditRecord(final ArenaOperation operation, final ArenaId arenaId,
                            final AuthorizationSubject actor, final boolean allowed,
                            final DefinitionId outcome, final CorrelationId correlationId,
                            final Instant occurredAt) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
        this.actor = Objects.requireNonNull(actor, "actor");
        this.allowed = allowed;
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }
    /** @return requested operation */ public ArenaOperation operation() { return operation; }
    /** @return protected arena target */ public ArenaId arenaId() { return arenaId; }
    /** @return authenticated actor */ public AuthorizationSubject actor() { return actor; }
    /** @return authorization/outcome classification */ public boolean allowed() { return allowed; }
    /** @return stable policy or outcome code */ public DefinitionId outcome() { return outcome; }
    /** @return causal correlation identity */ public CorrelationId correlationId() { return correlationId; }
    /** @return occurrence time */ public Instant occurredAt() { return occurredAt; }
}
