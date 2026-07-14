package io.zartra.bedwars.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable base for extension-safe namespaced identifiers.
 *
 * <p>The canonical form is {@code namespace:path}. Each component is lower-case ASCII and may
 * contain letters, digits, underscore, hyphen and period; paths may also contain slashes. The
 * complete form is bounded to 128 characters. Equality is type-sensitive.</p>
 */
public abstract class NamespacedIdentifier implements Comparable<NamespacedIdentifier> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,31}");
    private static final Pattern PATH = Pattern.compile("[a-z0-9][a-z0-9_./-]{0,94}");
    private final String namespace;
    private final String path;

    /**
     * Creates a namespaced identifier.
     *
     * @param namespace owner namespace
     * @param path owner-local path
     * @throws IdentifierFormatException when a component is invalid
     */
    protected NamespacedIdentifier(final String namespace, final String path) {
        this.namespace = validate(namespace, NAMESPACE, "namespace");
        this.path = validate(path, PATH, "path");
        if (namespace.length() + path.length() + 1 > 128) {
            throw new IdentifierFormatException("Namespaced identifier exceeds 128 characters");
        }
    }

    /**
     * Parses a canonical namespaced representation.
     *
     * @param serialized external representation
     * @return namespace and path pair
     * @throws IdentifierFormatException when malformed
     */
    protected static String[] split(final String serialized) {
        if (serialized == null) {
            throw new IdentifierFormatException("Namespaced identifier must not be null");
        }
        final int separator = serialized.indexOf(':');
        if (separator <= 0 || separator != serialized.lastIndexOf(':') || separator == serialized.length() - 1) {
            throw new IdentifierFormatException("Namespaced identifier must contain one ':' separator");
        }
        return new String[] {serialized.substring(0, separator), serialized.substring(separator + 1)};
    }

    private static String validate(final String value, final Pattern pattern, final String label) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IdentifierFormatException("Invalid namespaced identifier " + label);
        }
        return value;
    }

    /** @return owner namespace */
    public final String namespace() {
        return namespace;
    }

    /** @return owner-local path */
    public final String path() {
        return path;
    }

    @Override
    public final String toString() {
        return namespace + ':' + path;
    }

    @Override
    public final int hashCode() {
        return 31 * getClass().hashCode() + Objects.hash(namespace, path);
    }

    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final NamespacedIdentifier that = (NamespacedIdentifier) other;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public final int compareTo(final NamespacedIdentifier other) {
        Objects.requireNonNull(other, "other");
        final int typeOrder = getClass().getName().compareTo(other.getClass().getName());
        if (typeOrder != 0) {
            return typeOrder;
        }
        return toString().compareTo(other.toString());
    }
}
