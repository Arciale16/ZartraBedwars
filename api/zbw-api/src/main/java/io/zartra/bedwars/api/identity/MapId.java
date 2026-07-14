package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable collision-resistant map identity that survives rename, import and migration. */
public final class MapId extends UuidIdentifier {
    private MapId(final UUID value) { super(value); }
    /** @param value UUID value @return typed map ID */
    public static MapId of(final UUID value) { return new MapId(value); }
    /** @param value canonical UUID @return typed map ID @throws IdentifierFormatException if malformed */
    public static MapId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant map ID */
    public static MapId random() { return of(UUID.randomUUID()); }
}
