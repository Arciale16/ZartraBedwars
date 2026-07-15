package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.provider.Provider;

/**
 * Version-family adapter SPI.
 *
 * <p>Resolution is bounded, thread-safe and free of I/O. The returned value never exposes a
 * Bukkit, Paper, NMS or packet implementation class. Lifecycle methods inherit the asynchronous
 * provider error and deadline contract.</p>
 */
public interface CompatibilityAdapter extends Provider {
    /** @return exact runtime claim; family names are not certification claims */
    RuntimeClaim runtimeClaim();
    /** @return a typed resolution or explicit unsupported outcome; never null */
    CompatibilityOutcome resolve(SemanticKey key);
    /** @return immutable current registry snapshot */
    SemanticMappingRegistry.Snapshot mappings();

    /** Immutable exact runtime-certification claim. */
    final class RuntimeClaim {
        private final String platform;
        private final String minecraftVersion;
        private final String build;
        private final String sha256;
        /** Creates an exact claim whose values must match locked evidence. */
        public RuntimeClaim(final String platform, final String minecraftVersion,
                            final String build, final String sha256) {
            this.platform = bounded(platform, "platform");
            this.minecraftVersion = bounded(minecraftVersion, "minecraftVersion");
            this.build = bounded(build, "build");
            if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("sha256 must be lowercase hexadecimal");
            }
            this.sha256 = sha256;
        }
        private static String bounded(final String value, final String label) {
            if (value == null || !value.matches("[A-Za-z0-9_.-]{1,64}")) {
                throw new IllegalArgumentException(label + " must be a safe bounded value");
            }
            return value;
        }
        /** @return distribution name */ public String platform() { return platform; }
        /** @return exact Minecraft server version */ public String minecraftVersion() { return minecraftVersion; }
        /** @return exact distribution build */ public String build() { return build; }
        /** @return locked server SHA-256 */ public String sha256() { return sha256; }
    }
}
