package io.zartra.bedwars.integration.placeholderapi.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Context passed to PlaceholderAPI resolvers during placeholder expansion.
 *
 * @param namespace the placeholder namespace.
 * @param playerId the player identifier, if present.
 * @param requestedId the raw placeholder token identifier.
 * @param fallback a configured fallback string.
 * @param offlineAllowed whether missing players should be resolved from persistence.
 */
public final class PlaceholderContext {

    private final String namespace;
    private final UUID playerId;
    private final String requestedId;
    private final String fallback;
    private final boolean offlineAllowed;

    private PlaceholderContext(
            final String namespace,
            final UUID playerId,
            final String requestedId,
            final String fallback,
            final boolean offlineAllowed
    ) {
        this.namespace = namespace;
        this.playerId = playerId;
        this.requestedId = requestedId;
        this.fallback = fallback;
        this.offlineAllowed = offlineAllowed;
    }

    public static PlaceholderContext of(
            final String namespace,
            final UUID playerId,
            final String requestedId,
            final String fallback,
            final boolean offlineAllowed
    ) {
        return new PlaceholderContext(
                Objects.requireNonNull(namespace, "namespace"),
                playerId,
                Objects.requireNonNull(requestedId, "requestedId"),
                fallback == null ? "-" : fallback,
                offlineAllowed
        );
    }

    public String namespace() {
        return namespace;
    }

    public Optional<UUID> playerId() {
        return Optional.ofNullable(playerId);
    }

    public String requestedId() {
        return requestedId;
    }

    public String fallback() {
        return fallback;
    }

    public boolean offlineAllowed() {
        return offlineAllowed;
    }
}
