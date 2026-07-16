package io.zartra.bedwars.game.spi;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;

/** Owner-thread boundary for applying and restoring player-visible state. */
public interface PlayerProjection {
    /** @return true only while executing on the platform owner thread */
    boolean isOwnerThread();

    /** Applies one complete immutable projection atomically from the player's perspective. */
    void apply(PlayerId playerId, PlayerView view);

    /** Restores the exact state captured before admission. */
    void restore(PlayerStateSnapshot capturedState);

    /** Clears all plugin-owned presentation for an offline or removed player. */
    void clear(PlayerId playerId);

    /** Closed set of M08 player presentation intents. */
    enum PlayerView {
        /** Protected waiting player. */ WAITING,
        /** Active team player. */ PLAYING,
        /** Eliminated viewer. */ SPECTATOR,
        /** End-state participant. */ POST_GAME
    }
}
