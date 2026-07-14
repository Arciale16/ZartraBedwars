package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable collision-resistant moderation or Atlas case identity. */
public final class CaseId extends UuidIdentifier {
    private CaseId(final UUID value) { super(value); }
    /** @param value UUID value @return typed case ID */
    public static CaseId of(final UUID value) { return new CaseId(value); }
    /** @param value canonical UUID @return typed case ID @throws IdentifierFormatException if malformed */
    public static CaseId parse(final String value) { return of(parseUuid(value)); }
    /** @return a newly generated collision-resistant case ID */
    public static CaseId random() { return of(UUID.randomUUID()); }
}
