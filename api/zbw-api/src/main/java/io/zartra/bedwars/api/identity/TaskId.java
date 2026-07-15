package io.zartra.bedwars.api.identity;

import java.util.UUID;

/** Immutable identity for one accepted unit of scheduled work. */
public final class TaskId extends UuidIdentifier {
    private TaskId(final UUID value) { super(value); }
    /** @param value UUID value @return typed task ID */
    public static TaskId of(final UUID value) { return new TaskId(value); }
    /** @param value canonical UUID @return typed task ID @throws IdentifierFormatException if malformed */
    public static TaskId parse(final String value) { return of(parseUuid(value)); }
    /** @return newly generated collision-resistant task ID */
    public static TaskId random() { return of(UUID.randomUUID()); }
}
