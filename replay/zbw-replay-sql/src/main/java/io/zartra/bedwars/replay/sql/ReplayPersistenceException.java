package io.zartra.bedwars.replay.sql;

/** Sanitized unchecked failure used to complete asynchronous persistence stages exceptionally. */
public final class ReplayPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    /** Creates a persistence failure without exposing SQL text or player data. */
    public ReplayPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
    /** Creates a persistence failure for malformed authoritative rows. */
    public ReplayPersistenceException(final String message) { super(message); }
}
