package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable, typed key for one durable aggregate record. */
public final class RecordKey {
    private final DefinitionId aggregateType;
    private final DefinitionId aggregateId;

    private RecordKey(final DefinitionId aggregateType, final DefinitionId aggregateId) {
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
    }

    /** @return a validated record key */
    public static RecordKey of(final DefinitionId aggregateType, final DefinitionId aggregateId) {
        return new RecordKey(aggregateType, aggregateId);
    }

    /** @return aggregate category */ public DefinitionId aggregateType() { return aggregateType; }
    /** @return aggregate identity */ public DefinitionId aggregateId() { return aggregateId; }

    @Override public int hashCode() { return Objects.hash(aggregateType, aggregateId); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof RecordKey)) { return false; }
        final RecordKey that = (RecordKey) other;
        return aggregateType.equals(that.aggregateType) && aggregateId.equals(that.aggregateId);
    }
    @Override public String toString() { return aggregateType + "/" + aggregateId; }
}
