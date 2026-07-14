package io.zartra.bedwars.api.extension;

import java.util.Objects;

/** Immutable three-component Minecraft server version used by extension compatibility metadata. */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {
    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(final int major, final int minor, final int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** @return parsed {@code major.minor.patch} version @throws IllegalArgumentException when malformed */
    public static MinecraftVersion parse(final String value) {
        if (value == null || !value.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")) {
            throw new IllegalArgumentException("Minecraft version must use major.minor.patch");
        }
        final String[] components = value.split("\\.");
        try {
            return new MinecraftVersion(Integer.parseInt(components[0]), Integer.parseInt(components[1]),
                    Integer.parseInt(components[2]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Minecraft version component exceeds integer bounds", exception);
        }
    }

    /** @return major component */ public int major() { return major; }
    /** @return minor component */ public int minor() { return minor; }
    /** @return patch component */ public int patch() { return patch; }
    @Override public int compareTo(final MinecraftVersion other) {
        Objects.requireNonNull(other, "other");
        int result = Integer.compare(major, other.major);
        if (result == 0) { result = Integer.compare(minor, other.minor); }
        if (result == 0) { result = Integer.compare(patch, other.patch); }
        return result;
    }
    @Override public String toString() { return major + "." + minor + "." + patch; }
    @Override public int hashCode() { return Objects.hash(major, minor, patch); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof MinecraftVersion)) { return false; }
        final MinecraftVersion that = (MinecraftVersion) other;
        return major == that.major && minor == that.minor && patch == that.patch;
    }
}
