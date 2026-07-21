package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.util.Objects;

/** Immutable record of a persistent entitlement grant. */
public final class EntitlementGrant {
    private final PlayerProgressionId owner;
    private final EntitlementId entitlementId;
    private final IdempotencyKey idempotencyKey;
    private final AuditMetadata audit;

    /** Creates an auditable idempotent entitlement grant. */
    public EntitlementGrant(final PlayerProgressionId owner, final EntitlementId entitlementId,
                            final IdempotencyKey idempotencyKey, final AuditMetadata audit) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.entitlementId = Objects.requireNonNull(entitlementId, "entitlementId");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return entitlement owner */ public PlayerProgressionId owner() { return owner; }
    /** @return entitlement identity */ public EntitlementId entitlementId() { return entitlementId; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
