package io.zartra.bedwars.api.configuration;

import java.util.Objects;

/** Immutable dot-separated configuration option identity. */
public final class ConfigurationKey implements Comparable<ConfigurationKey> {
    private final String value;

    private ConfigurationKey(final String value) {
        if (value == null || !value.matches("[a-z0-9][a-z0-9_.-]{2,191}")) {
            throw new IllegalArgumentException("Invalid configuration key");
        }
        this.value = value;
    }

    /** @return validated canonical configuration key */
    public static ConfigurationKey of(final String value) { return new ConfigurationKey(value); }
    /** @return canonical key */ public String value() { return value; }
    @Override public int compareTo(final ConfigurationKey other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value; }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof ConfigurationKey
                && value.equals(((ConfigurationKey) other).value);
    }
}
