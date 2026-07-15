package io.zartra.bedwars.world.api;

import java.util.Objects;

/** Immutable platform-neutral world identity and safe directory name. */
public final class WorldKey implements Comparable<WorldKey> {
    private final String value;
    private WorldKey(final String value) { this.value = value; }
    /** @return validated lower-case world key */
    public static WorldKey of(final String value) {
        if (value == null || !value.matches("[a-z0-9][a-z0-9_-]{0,47}")) {
            throw new IllegalArgumentException("world key must be a safe lower-case name");
        }
        return new WorldKey(value);
    }
    /** @return canonical world name */ public String value() { return value; }
    @Override public int compareTo(final WorldKey other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) { return this == other || other instanceof WorldKey && value.equals(((WorldKey) other).value); }
    @Override public String toString() { return value; }
}
