package io.zartra.bedwars.atlas.sql;

/** Fail-closed Atlas persistence error. */
public final class AtlasPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public AtlasPersistenceException(final String message) { super(message); }
    public AtlasPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
