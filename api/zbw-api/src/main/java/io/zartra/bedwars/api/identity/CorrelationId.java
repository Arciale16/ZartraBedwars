package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable correlation identity shared by causally related operations and events. */
public final class CorrelationId extends UuidIdentifier {
    private CorrelationId(final UUID value) { super(value); }
    /** @param value UUID value @return typed correlation ID */
    public static CorrelationId of(final UUID value) { return new CorrelationId(value); }
    /** @param value canonical UUID @return typed correlation ID @throws IdentifierFormatException if malformed */
    public static CorrelationId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant correlation ID */
    public static CorrelationId random() { return of(UUID.randomUUID()); }
}
