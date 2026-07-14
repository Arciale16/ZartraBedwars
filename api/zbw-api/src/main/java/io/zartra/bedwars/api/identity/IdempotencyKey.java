package io.zartra.bedwars.api.identity;

/** Immutable namespaced key used to make an externally visible operation idempotent. */
public final class IdempotencyKey extends NamespacedIdentifier {
    private IdempotencyKey(final String namespace, final String path) { super(namespace, path); }
    /** @return typed idempotency key @throws IdentifierFormatException if either component is invalid */
    public static IdempotencyKey of(final String namespace, final String path) { return new IdempotencyKey(namespace, path); }
    /** @return parsed idempotency key @throws IdentifierFormatException if malformed */
    public static IdempotencyKey parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
