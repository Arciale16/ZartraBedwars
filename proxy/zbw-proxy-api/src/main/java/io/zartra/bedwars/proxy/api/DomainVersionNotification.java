package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owner-issued version notification; it carries no content or authoritative state. */
public final class DomainVersionNotification {
    /** Coordination families owned outside the proxy. */ public enum Family {
        RESOURCE_SCARCITY,
        ITEM_ROTATION
    }
    private final UUID operationId;
    private final Family family;
    private final String ownerReference;
    private final long version;
    private final Instant issuedAt;

    private DomainVersionNotification(final UUID operationId, final Family family,
            final String ownerReference, final long version, final Instant issuedAt) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.family = Objects.requireNonNull(family, "family");
        this.ownerReference = ProxyContractValidation.token(ownerReference, "ownerReference");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
    }
    /** Creates a version-only notification. */
    public static DomainVersionNotification of(final UUID operationId, final Family family,
            final String ownerReference, final long version, final Instant issuedAt) {
        return new DomainVersionNotification(operationId, family, ownerReference, version, issuedAt);
    }
    /** Returns operation ID. */ public UUID operationId() { return operationId; }
    /** Returns family. */ public Family family() { return family; }
    /** Returns owner reference. */ public String ownerReference() { return ownerReference; }
    /** Returns monotonic owner version. */ public long version() { return version; }
    /** Returns issue time. */ public Instant issuedAt() { return issuedAt; }
    /** Returns a stable tracking key. */ public String trackingKey() { return family.name() + ":" + ownerReference; }
}
