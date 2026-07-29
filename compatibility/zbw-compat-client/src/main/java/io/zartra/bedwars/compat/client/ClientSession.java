package io.zartra.bedwars.compat.client;

import java.util.Objects;

/** Immutable, opaque client protocol observation without player identity. */
public final class ClientSession {
    private final String sessionKey;
    private final ClientPath path;
    private final Edition edition;
    private final InputMode inputMode;
    private final int protocolVersion;

    /**
     * Creates a bounded client observation.
     *
     * @param sessionKey opaque runtime correlation key, never a player name
     * @param path detected client translation path
     * @param edition client edition
     * @param inputMode detected input family
     * @param protocolVersion positive wire protocol number
     */
    public ClientSession(final String sessionKey, final ClientPath path,
                         final Edition edition, final InputMode inputMode,
                         final int protocolVersion) {
        if (sessionKey == null || !sessionKey.matches("[A-Za-z0-9_-]{8,64}")) {
            throw new IllegalArgumentException("sessionKey must be opaque and bounded");
        }
        if (protocolVersion < 1 || protocolVersion > 10000) {
            throw new IllegalArgumentException("protocolVersion is out of bounds");
        }
        this.sessionKey = sessionKey;
        this.path = Objects.requireNonNull(path, "path");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.inputMode = Objects.requireNonNull(inputMode, "inputMode");
        if ((path == ClientPath.GEYSER_FLOODGATE) != (edition == Edition.BEDROCK)) {
            throw new IllegalArgumentException(
                    "Geyser/Floodgate is the only Bedrock path");
        }
        if (edition == Edition.JAVA && inputMode != InputMode.JAVA_NATIVE) {
            throw new IllegalArgumentException("Java clients use native Java input");
        }
        this.protocolVersion = protocolVersion;
    }

    /** @return opaque session key */ public String sessionKey() { return sessionKey; }
    /** @return detected translation path */ public ClientPath path() { return path; }
    /** @return client edition */ public Edition edition() { return edition; }
    /** @return input family */ public InputMode inputMode() { return inputMode; }
    /** @return positive protocol version */ public int protocolVersion() {
        return protocolVersion;
    }

    @Override public int hashCode() {
        return Objects.hash(sessionKey, path, edition, inputMode, protocolVersion);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ClientSession)) { return false; }
        final ClientSession that = (ClientSession) other;
        return protocolVersion == that.protocolVersion
                && sessionKey.equals(that.sessionKey) && path == that.path
                && edition == that.edition && inputMode == that.inputMode;
    }

    /** Supported client editions. */
    public enum Edition { JAVA, BEDROCK }

    /** Privacy-safe input family required for parity decisions. */
    public enum InputMode { JAVA_NATIVE, KEYBOARD_MOUSE, CONTROLLER, TOUCH }
}
