package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable collision-resistant replay identity, never derived from mutable display data. */
public final class ReplayId extends UuidIdentifier {
    private ReplayId(final UUID value) { super(value); }
    /** @param value UUID value @return typed replay ID */
    public static ReplayId of(final UUID value) { return new ReplayId(value); }
    /** @param value canonical UUID @return typed replay ID @throws IdentifierFormatException if malformed */
    public static ReplayId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant replay ID */
    public static ReplayId random() { return of(UUID.randomUUID()); }
}
