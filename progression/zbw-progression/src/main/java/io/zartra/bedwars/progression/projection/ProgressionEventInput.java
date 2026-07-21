package io.zartra.bedwars.progression.projection;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.util.Arrays;
import java.util.Objects;

/** Immutable serialized M08 event input accepted by a progression projector. */
public final class ProgressionEventInput {
    private final EventMetadata metadata;
    private final PlayerProgressionId playerId;
    private final DefinitionId eventKind;
    private final IdempotencyKey idempotencyKey;
    private final byte[] payload;

    /** Creates an immutable event input with a defensive payload copy. */
    public ProgressionEventInput(final EventMetadata metadata,
                                 final PlayerProgressionId playerId,
                                 final DefinitionId eventKind,
                                 final IdempotencyKey idempotencyKey,
                                 final byte[] payload) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.eventKind = Objects.requireNonNull(eventKind, "eventKind");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.payload = Objects.requireNonNull(payload, "payload").clone();
        if (this.payload.length > 1_048_576) { throw new IllegalArgumentException("payload exceeds 1 MiB"); }
    }
    /** @return source event metadata */ public EventMetadata metadata() { return metadata; }
    /** @return affected progression aggregate */ public PlayerProgressionId playerId() { return playerId; }
    /** @return version-neutral input kind */ public DefinitionId eventKind() { return eventKind; }
    /** @return inbox duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return defensive payload copy */ public byte[] payload() { return payload.clone(); }
    @Override public int hashCode() { return 31 * Objects.hash(metadata, playerId, eventKind, idempotencyKey) + Arrays.hashCode(payload); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ProgressionEventInput)) { return false; }
        final ProgressionEventInput that = (ProgressionEventInput) other;
        return metadata.equals(that.metadata) && playerId.equals(that.playerId)
                && eventKind.equals(that.eventKind)
                && idempotencyKey.equals(that.idempotencyKey)
                && Arrays.equals(payload, that.payload);
    }
}
