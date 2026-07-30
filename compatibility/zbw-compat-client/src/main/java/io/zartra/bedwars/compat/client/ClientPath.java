package io.zartra.bedwars.compat.client;

/** Independent client translation path; never a server adapter selection. */
public enum ClientPath {
    /** Native Java protocol for the selected server runtime. */
    NATIVE,
    /** ViaVersion translated Java client. */
    VIAVERSION,
    /** ViaBackwards translated Java client. */
    VIABACKWARDS,
    /** ViaRewind translated legacy Java client. */
    VIAREWIND,
    /** Geyser translation plus Floodgate identity and input. */
    GEYSER_FLOODGATE
}
