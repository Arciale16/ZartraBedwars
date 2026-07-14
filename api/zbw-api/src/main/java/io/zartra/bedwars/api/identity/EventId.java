package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable event identity used for duplicate detection across event consumers. */
public final class EventId extends UuidIdentifier {
    private EventId(final UUID value) { super(value); }
    /** @param value UUID value @return typed event ID */
    public static EventId of(final UUID value) { return new EventId(value); }
    /** @param value canonical UUID @return typed event ID @throws IdentifierFormatException if malformed */
    public static EventId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant event ID */
    public static EventId random() { return of(UUID.randomUUID()); }
}
