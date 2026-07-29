package io.zartra.bedwars.party.sql;

/** Typed party persistence or malformed-data failure. */
public final class PartyPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** @param message safe diagnostic message */
    public PartyPersistenceException(final String message) { super(message); }

    /** @param message safe diagnostic message @param cause underlying failure */
    public PartyPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
