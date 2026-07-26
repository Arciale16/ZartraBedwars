package io.zartra.bedwars.integration.placeholderapi.api;

/**
 * Typed placeholder resolver contract.
 *
 */
@FunctionalInterface
public interface PlaceholderResolver {

    PlaceholderResult resolve(PlaceholderContext context);
}
