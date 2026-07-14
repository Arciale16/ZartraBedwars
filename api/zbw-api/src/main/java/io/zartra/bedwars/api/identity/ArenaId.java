package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable collision-resistant arena identity, independent of display name or map. */
public final class ArenaId extends UuidIdentifier {
    private ArenaId(final UUID value) { super(value); }
    /** @param value UUID value @return typed arena ID */
    public static ArenaId of(final UUID value) { return new ArenaId(value); }
    /** @param value canonical UUID @return typed arena ID @throws IdentifierFormatException if malformed */
    public static ArenaId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant arena ID */
    public static ArenaId random() { return of(UUID.randomUUID()); }
}
