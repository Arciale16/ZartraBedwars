package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a generator type. */
public final class GeneratorTypeId extends NamespacedIdentifier {
    private GeneratorTypeId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed generator ID @throws IdentifierFormatException if either component is invalid */
    public static GeneratorTypeId of(final String namespace, final String path) { return new GeneratorTypeId(namespace, path); }
    /** @return parsed generator ID @throws IdentifierFormatException if malformed */
    public static GeneratorTypeId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
