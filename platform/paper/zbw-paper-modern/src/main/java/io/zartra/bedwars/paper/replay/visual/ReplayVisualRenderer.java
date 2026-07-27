package io.zartra.bedwars.paper.replay.visual;

import java.util.UUID;

/** Owner-thread boundary for Paper replay representations. */
public interface ReplayVisualRenderer {
    /** Spawns one viewer-isolated representation and returns an opaque handle. */
    Object spawn(UUID viewerId, VisualEntityState state);
    /** Updates an existing representation. */
    void update(UUID viewerId, Object handle, VisualEntityState state);
    /** Removes an existing representation safely. */
    void remove(UUID viewerId, Object handle);
}
