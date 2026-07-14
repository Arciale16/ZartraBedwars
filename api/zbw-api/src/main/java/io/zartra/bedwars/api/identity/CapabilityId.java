package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a provider or extension capability. */
public final class CapabilityId extends NamespacedIdentifier {
    private CapabilityId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed capability ID @throws IdentifierFormatException if either component is invalid */
    public static CapabilityId of(final String namespace, final String path) { return new CapabilityId(namespace, path); }
    /** @return parsed capability ID @throws IdentifierFormatException if malformed */
    public static CapabilityId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
