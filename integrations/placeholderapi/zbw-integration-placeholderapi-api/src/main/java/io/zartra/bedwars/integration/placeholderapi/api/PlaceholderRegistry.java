package io.zartra.bedwars.integration.placeholderapi.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic registry of placeholder resolvers keyed by placeholder id.
 */
public final class PlaceholderRegistry {

    private final Map<String, PlaceholderResolver> resolvers;

    public PlaceholderRegistry() {
        this.resolvers = new LinkedHashMap<>();
    }

    public synchronized void register(final PlaceholderId id, final PlaceholderResolver resolver) {
        final String key = Objects.requireNonNull(id, "id").value();
        Objects.requireNonNull(resolver, "resolver");
        if (resolvers.containsKey(key)) {
            throw new IllegalStateException("Duplicate placeholder resolver: " + key);
        }
        resolvers.put(key, resolver);
    }

    public synchronized void clear() {
        resolvers.clear();
    }

    public synchronized boolean isRegistered(final PlaceholderId id) {
        return resolvers.containsKey(id.value());
    }

    public synchronized int size() {
        return resolvers.size();
    }

    public synchronized Map<String, PlaceholderResolver> resolversSnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(resolvers));
    }

    public PlaceholderResult resolve(final PlaceholderContext context) {
        final PlaceholderResolver resolver = resolvers.get(context.requestedId());
        if (resolver == null) {
            return PlaceholderResult.unavailable(context.fallback());
        }
        try {
            final PlaceholderResult result = resolver.resolve(context);
            return result == null ? PlaceholderResult.fallback(context.fallback()) : result;
        } catch (final Exception e) {
            return PlaceholderResult.unavailable(context.fallback());
        }
    }
}
