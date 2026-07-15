package io.zartra.bedwars.arena.setup;

import java.util.Objects;
import java.util.UUID;

/** Immutable collision-resistant setup-session identity. */
public final class SetupSessionId {
    private final UUID value;
    private SetupSessionId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
    /** @return a new process-random session identity */ public static SetupSessionId random() { return new SetupSessionId(UUID.randomUUID()); }
    /** @return a typed identity from a UUID */ public static SetupSessionId of(final UUID value) { return new SetupSessionId(value); }
    /** @return a parsed canonical identity */
    public static SetupSessionId parse(final String value) {
        if (value == null) { throw new IllegalArgumentException("session ID is null"); }
        final UUID parsed = UUID.fromString(value);
        if (!parsed.toString().equals(value)) { throw new IllegalArgumentException("session ID is not canonical"); }
        return of(parsed);
    }
    /** @return UUID value */ public UUID asUuid() { return value; }
    @Override public String toString() { return value.toString(); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return other instanceof SetupSessionId
                && value.equals(((SetupSessionId) other).value);
    }
}
