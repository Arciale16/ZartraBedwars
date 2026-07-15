package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/** Immutable versioned message persisted by the transactional outbox or inbox. */
public final class MessageEnvelope {
    private final IdempotencyKey operationId;
    private final EventMetadata metadata;
    private final byte[] payload;
    private final Instant availableAt;

    private MessageEnvelope(final IdempotencyKey operationId, final EventMetadata metadata,
                            final byte[] payload, final Instant availableAt) {
        if (payload == null || payload.length == 0) { throw new IllegalArgumentException("payload must not be empty"); }
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.payload = Arrays.copyOf(payload, payload.length);
        this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
    }

    /** @return validated immutable envelope */
    public static MessageEnvelope of(final IdempotencyKey operationId, final EventMetadata metadata,
                                     final byte[] payload, final Instant availableAt) {
        return new MessageEnvelope(operationId, metadata, payload, availableAt);
    }

    /** @return unique operation identity */ public IdempotencyKey operationId() { return operationId; }
    /** @return causal, version and ordering metadata */ public EventMetadata metadata() { return metadata; }
    /** @return defensive payload copy */ public byte[] payload() { return Arrays.copyOf(payload, payload.length); }
    /** @return earliest dispatch instant */ public Instant availableAt() { return availableAt; }

    @Override public int hashCode() {
        return 31 * Objects.hash(operationId, metadata, availableAt) + Arrays.hashCode(payload);
    }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof MessageEnvelope)) { return false; }
        final MessageEnvelope that = (MessageEnvelope) other;
        return operationId.equals(that.operationId) && metadata.equals(that.metadata)
                && availableAt.equals(that.availableAt) && Arrays.equals(payload, that.payload);
    }
}
