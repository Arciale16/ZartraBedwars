package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable collision-resistant match identity used for idempotency and correlation. */
public final class MatchId extends UuidIdentifier {
    private MatchId(final UUID value) { super(value); }
    /** @param value UUID value @return typed match ID */
    public static MatchId of(final UUID value) { return new MatchId(value); }
    /** @param value canonical UUID @return typed match ID @throws IdentifierFormatException if malformed */
    public static MatchId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant match ID */
    public static MatchId random() { return of(UUID.randomUUID()); }
}
