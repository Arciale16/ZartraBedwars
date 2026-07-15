package io.zartra.bedwars.api.authorization;

import java.util.Objects;

/** Immutable canonical permission node used as an authorization action identifier. */
public final class PermissionNode implements Comparable<PermissionNode> {
    private final String value;

    private PermissionNode(final String value) {
        if (value == null || !value.matches("[a-z0-9][a-z0-9_.-]{2,127}")) {
            throw new IllegalArgumentException("Invalid permission node");
        }
        this.value = value;
    }

    /** @return validated canonical node */
    public static PermissionNode of(final String value) { return new PermissionNode(value); }
    /** @return canonical lower-case node */
    public String value() { return value; }
    @Override public int compareTo(final PermissionNode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value; }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof PermissionNode
                && value.equals(((PermissionNode) other).value);
    }
}
