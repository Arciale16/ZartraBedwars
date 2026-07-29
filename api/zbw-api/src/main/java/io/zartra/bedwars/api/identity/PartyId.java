package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable native or external party identity. */
public final class PartyId extends UuidIdentifier {
    private PartyId(final UUID value) { super(value); }

    /** @param value UUID value @return typed party ID */
    public static PartyId of(final UUID value) { return new PartyId(value); }

    /** @param value canonical UUID @return typed party ID @throws IdentifierFormatException if malformed */
    public static PartyId parse(final String value) { return of(parseUuid(value)); }
}
