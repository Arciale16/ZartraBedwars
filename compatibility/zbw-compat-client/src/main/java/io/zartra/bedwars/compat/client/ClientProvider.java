package io.zartra.bedwars.compat.client;

/** Exact optional client translation providers selected by the M22 lock. */
public enum ClientProvider {
    /** ViaVersion protocol bridge. */
    VIAVERSION("ViaVersion", "5.4.2"),
    /** ViaBackwards backwards protocol bridge. */
    VIABACKWARDS("ViaBackwards", "5.4.2"),
    /** ViaRewind legacy protocol bridge. */
    VIAREWIND("ViaRewind", "4.0.6"),
    /** Geyser Java-to-Bedrock protocol bridge. */
    GEYSER("Geyser", "2.7.0"),
    /** Floodgate Bedrock identity/input bridge. */
    FLOODGATE("Floodgate", "2.2.4");

    private final String displayName;
    private final String requiredVersion;

    ClientProvider(final String displayName, final String requiredVersion) {
        this.displayName = displayName;
        this.requiredVersion = requiredVersion;
    }

    /** @return operator-facing provider name */
    public String displayName() {
        return displayName;
    }

    /** @return exact preselected version */
    public String requiredVersion() {
        return requiredVersion;
    }
}
