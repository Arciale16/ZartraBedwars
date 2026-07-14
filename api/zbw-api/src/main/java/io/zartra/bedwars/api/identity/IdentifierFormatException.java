package io.zartra.bedwars.api.identity;

/**
 * Indicates that an external identifier representation is malformed.
 *
 * <p>The rejected value is intentionally not retained so diagnostics cannot accidentally keep
 * sensitive input alive. Callers at configuration or transport boundaries should translate this
 * exception into a typed {@code Result}.</p>
 */
public final class IdentifierFormatException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a format failure.
     *
     * @param message safe diagnostic text
     */
    public IdentifierFormatException(final String message) {
        super(message);
    }

    /**
     * Creates a format failure with a parsing cause.
     *
     * @param message safe diagnostic text
     * @param cause parsing cause
     */
    public IdentifierFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
