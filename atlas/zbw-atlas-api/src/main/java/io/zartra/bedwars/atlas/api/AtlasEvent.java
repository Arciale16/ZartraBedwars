package io.zartra.bedwars.atlas.api;

import io.zartra.bedwars.api.event.ApiEvent;

/**
 * Marker for immutable post-commit Atlas facts.
 *
 * <p>Listeners run in the thread context declared by event metadata and must not block it.
 */
public interface AtlasEvent extends ApiEvent {
}
