package io.zartra.bedwars.api.configuration;

/** Immutable positive configuration document version. */
public final class ConfigurationVersion implements Comparable<ConfigurationVersion> {
    private final int value;

    private ConfigurationVersion(final int value) {
        if (value < 1) { throw new IllegalArgumentException("Configuration version must be positive"); }
        this.value = value;
    }

    /** @return positive configuration version */
    public static ConfigurationVersion of(final int value) { return new ConfigurationVersion(value); }
    /** @return integer version */ public int value() { return value; }
    @Override public int compareTo(final ConfigurationVersion other) {
        if (other == null) { throw new NullPointerException("other"); }
        return Integer.compare(value, other.value);
    }
    @Override public String toString() { return Integer.toString(value); }
    @Override public int hashCode() { return value; }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof ConfigurationVersion
                && value == ((ConfigurationVersion) other).value;
    }
}
