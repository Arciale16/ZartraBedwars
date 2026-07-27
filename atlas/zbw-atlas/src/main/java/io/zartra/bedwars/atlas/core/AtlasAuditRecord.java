package io.zartra.bedwars.atlas.core;

import java.time.Instant;
import java.util.Objects;

/** Immutable content-free Atlas policy audit record. */
public final class AtlasAuditRecord {
    private final String actor;
    private final String action;
    private final String target;
    private final Instant occurredAt;
    private final String result;
    private final String beforeReference;
    private final String afterReference;

    public AtlasAuditRecord(final String actor, final String action, final String target,
                            final Instant occurredAt, final String result,
                            final String beforeReference, final String afterReference) {
        this.actor = token(actor);
        this.action = token(action);
        this.target = token(target);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.result = token(result);
        this.beforeReference = token(beforeReference);
        this.afterReference = token(afterReference);
    }

    private static String token(final String value) {
        if (value == null || value.isEmpty() || value.length() > 160
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("audit values must be opaque tokens");
        }
        return value;
    }

    public String actor() { return actor; }
    public String action() { return action; }
    public String target() { return target; }
    public Instant occurredAt() { return occurredAt; }
    public String result() { return result; }
    public String beforeReference() { return beforeReference; }
    public String afterReference() { return afterReference; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasAuditRecord)) { return false; }
        AtlasAuditRecord that = (AtlasAuditRecord) other;
        return actor.equals(that.actor) && action.equals(that.action) && target.equals(that.target)
                && occurredAt.equals(that.occurredAt) && result.equals(that.result)
                && beforeReference.equals(that.beforeReference)
                && afterReference.equals(that.afterReference);
    }
    @Override public int hashCode() {
        return Objects.hash(actor, action, target, occurredAt, result,
                beforeReference, afterReference);
    }
}
