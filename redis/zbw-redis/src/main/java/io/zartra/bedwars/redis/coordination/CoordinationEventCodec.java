package io.zartra.bedwars.redis.coordination;

import io.zartra.bedwars.redis.api.InvalidationVersion;
import io.zartra.bedwars.redis.api.OperationId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Deterministic schema-one codec for bounded coordination notifications. */
public final class CoordinationEventCodec {
    private static final String SCHEMA = "1";

    /** Encodes an event without domain payloads or sensitive identity. */
    public byte[] encode(final CoordinationEvent event) {
        final CoordinationEvent checked = Objects.requireNonNull(event, "event");
        final String subject = Base64.getUrlEncoder().withoutPadding().encodeToString(
                checked.subject().getBytes(StandardCharsets.UTF_8));
        return String.join("|", SCHEMA, checked.type().name(), subject,
                checked.version().toString(), checked.operationId().toString(),
                checked.occurredAt().toString()).getBytes(StandardCharsets.UTF_8);
    }

    /** Decodes schema one and fails closed for unknown or malformed data. */
    public CoordinationEvent decode(final byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.length == 0 || payload.length > 256 * 1024) {
            throw new IllegalArgumentException("invalid coordination payload size");
        }
        final String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != 6 || !SCHEMA.equals(fields[0])) {
            throw new IllegalArgumentException("unsupported coordination event schema");
        }
        try {
            final String subject = new String(Base64.getUrlDecoder().decode(fields[2]),
                    StandardCharsets.UTF_8);
            return new CoordinationEvent(CoordinationEvent.Type.valueOf(fields[1]), subject,
                    InvalidationVersion.of(Long.parseLong(fields[3])),
                    OperationId.parse(fields[4]), Instant.parse(fields[5]));
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("malformed coordination event", malformed);
        }
    }
}
