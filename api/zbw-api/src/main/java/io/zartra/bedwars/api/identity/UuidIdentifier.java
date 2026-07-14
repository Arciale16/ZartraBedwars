package io.zartra.bedwars.api.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable base for type-safe UUID identifiers.
 *
 * <p>Equality is type-sensitive: an arena and a match with the same UUID are not equal. The
 * canonical serialization form is the lower-case RFC 4122 string returned by {@link #toString()}.
 * Subclasses are safe to share between threads.</p>
 */
public abstract class UuidIdentifier {
    private final UUID value;

    /**
     * Creates an identifier from a UUID.
     *
     * @param value UUID value
     */
    protected UuidIdentifier(final UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Parses a canonical UUID.
     *
     * @param serialized external representation
     * @return parsed UUID
     * @throws IdentifierFormatException when the representation is null or malformed
     */
    protected static UUID parseUuid(final String serialized) {
        if (serialized == null) {
            throw new IdentifierFormatException("UUID identifier must not be null");
        }
        try {
            final UUID parsed = UUID.fromString(serialized);
            if (!parsed.toString().equals(serialized)) {
                throw new IdentifierFormatException("UUID identifier is not canonical");
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            if (exception instanceof IdentifierFormatException) {
                throw (IdentifierFormatException) exception;
            }
            throw new IdentifierFormatException("UUID identifier is malformed", exception);
        }
    }

    /**
     * Returns the UUID for adapters that require a standard Java value.
     *
     * @return immutable UUID value
     */
    public final UUID asUuid() {
        return value;
    }

    @Override
    public final String toString() {
        return value.toString();
    }

    @Override
    public final int hashCode() {
        return 31 * getClass().hashCode() + value.hashCode();
    }

    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        return other != null && getClass() == other.getClass()
                && value.equals(((UuidIdentifier) other).value);
    }
}
