package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable player identity; display names and external account identifiers are not identity. */
public final class PlayerId extends UuidIdentifier {
    private PlayerId(final UUID value) { super(value); }
    /** @param value UUID value @return typed player ID */
    public static PlayerId of(final UUID value) { return new PlayerId(value); }
    /** @param value canonical UUID @return typed player ID @throws IdentifierFormatException if malformed */
    public static PlayerId parse(final String value) { return of(parseUuid(value)); }
}
