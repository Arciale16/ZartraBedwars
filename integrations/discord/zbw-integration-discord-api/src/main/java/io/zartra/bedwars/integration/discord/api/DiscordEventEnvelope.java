package io.zartra.bedwars.integration.discord.api;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable versioned outbound Discord event envelope.
 *
 * @param <P> canonical immutable payload type
 */
public final class DiscordEventEnvelope<P extends DiscordEventEnvelope.Payload> {
    private final EventMetadata metadata;
    private final IdempotencyKey idempotencyKey;
    private final Sensitivity sensitivity;
    private final CapabilityId requiredCapability;
    private final Instant deliveryDeadline;
    private final P payload;

    private DiscordEventEnvelope(final EventMetadata metadata, final IdempotencyKey idempotencyKey,
                                 final Sensitivity sensitivity, final CapabilityId requiredCapability,
                                 final Instant deliveryDeadline, final P payload) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.sensitivity = Objects.requireNonNull(sensitivity, "sensitivity");
        this.requiredCapability = Objects.requireNonNull(requiredCapability, "requiredCapability");
        this.deliveryDeadline = Objects.requireNonNull(deliveryDeadline, "deliveryDeadline");
        this.payload = Objects.requireNonNull(payload, "payload");
        if (deliveryDeadline.isBefore(metadata.occurredAt())) {
            throw new IllegalArgumentException("deliveryDeadline precedes occurrence time");
        }
    }

    /** @return immutable envelope */
    public static <P extends Payload> DiscordEventEnvelope<P> of(final EventMetadata metadata,
            final IdempotencyKey idempotencyKey, final Sensitivity sensitivity,
            final CapabilityId requiredCapability, final Instant deliveryDeadline, final P payload) {
        return new DiscordEventEnvelope<P>(metadata, idempotencyKey, sensitivity, requiredCapability,
                deliveryDeadline, payload);
    }

    /** @return event identity, schema, order and thread metadata */ public EventMetadata metadata() { return metadata; }
    /** @return stable duplicate-prevention key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return sensitivity policy */ public Sensitivity sensitivity() { return sensitivity; }
    /** @return provider capability required for delivery */ public CapabilityId requiredCapability() { return requiredCapability; }
    /** @return absolute delivery deadline after which retry is forbidden */ public Instant deliveryDeadline() { return deliveryDeadline; }
    /** @return canonical immutable payload */ public P payload() { return payload; }

    /** Marker for canonical immutable payload DTOs. Raw maps and Discord SDK objects are forbidden. */
    public interface Payload {
        /** @return stable namespaced payload schema name */ io.zartra.bedwars.api.identity.DefinitionId schema();
        /** @return positive payload schema version */ int schemaVersion();
    }

    /** Data sensitivity classification enforced before provider delivery. */
    public enum Sensitivity {
        /** Public data allowed by visibility policy. */ PUBLIC,
        /** Data allowed only for a verified, consented linked account. */ LINKED_ACCOUNT,
        /** Data allowed only to authenticated staff-scoped providers. */ STAFF_RESTRICTED,
        /** Data that must never cross the Discord integration gateway. */ PROHIBITED_EXTERNAL
    }
}
