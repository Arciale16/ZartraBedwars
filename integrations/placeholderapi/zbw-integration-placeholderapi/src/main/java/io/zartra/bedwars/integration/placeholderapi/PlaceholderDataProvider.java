package io.zartra.bedwars.integration.placeholderapi;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides read-only placeholder values from stable domain modules.
 *
 * Implementations must not mutate game state, must be thread safe, and
 * must not call storage systems directly.
 */
@FunctionalInterface
public interface PlaceholderDataProvider<T> {
    Optional<T> getValue(UUID playerId);
}
