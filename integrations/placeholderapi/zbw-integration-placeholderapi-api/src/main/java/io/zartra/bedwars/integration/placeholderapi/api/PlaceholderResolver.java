package io.zartra.bedwars.integration.placeholderapi.api;

/**
 * Typed placeholder resolver contract.
 *
 * @param context information for the active placeholder expansion.
 */
@FunctionalInterface
public interface PlaceholderResolver {

    PlaceholderResult resolve(PlaceholderContext context);
}
