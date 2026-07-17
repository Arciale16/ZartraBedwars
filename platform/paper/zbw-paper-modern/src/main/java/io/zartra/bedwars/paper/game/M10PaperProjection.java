package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.selector.SelectorFramework.Page;
import io.zartra.bedwars.game.spectator.SpectatorFramework.Session;
import java.util.Objects;

/**
 * Java 21 primary-Paper translation boundary for M10. Every platform mutation is owner-thread
 * guarded; all selection and spectator decisions are supplied by Java 8 application services.
 */
public final class M10PaperProjection {
    private final OwnerThread ownerThread;
    private final Platform platform;

    /** Creates a projection around closed Paper-owned mutation ports. */
    public M10PaperProjection(final OwnerThread ownerThread, final Platform platform) {
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    /** Renders an already-decided immutable selector page. */
    public void renderSelector(final PlayerId playerId, final Page page) {
        requireOwner();
        platform.renderSelector(Objects.requireNonNull(playerId, "playerId"),
                Objects.requireNonNull(page, "page"));
    }

    /** Applies an already-decided spectator state and configured inventory/options. */
    public void applySpectator(final Session session) {
        requireOwner();
        platform.applySpectator(Objects.requireNonNull(session, "session"));
    }

    /** Applies exact captured restoration state and clears all owned spectator UI/effects. */
    public void restore(final PlayerStateSnapshot capturedState) {
        requireOwner();
        final PlayerStateSnapshot checked = Objects.requireNonNull(capturedState, "capturedState");
        platform.restore(checked);
        platform.clearOwnedState(checked.playerId());
    }

    /** Clears M10-owned selector/spectator platform state after disconnect or shutdown. */
    public void clear(final PlayerId playerId) {
        requireOwner();
        platform.clearOwnedState(Objects.requireNonNull(playerId, "playerId"));
    }

    private void requireOwner() {
        if (!ownerThread.isOwnerThread()) {
            throw new IllegalStateException("M10 Paper mutation attempted off owner thread");
        }
    }

    /** Owner-thread predicate supplied by the Paper bootstrap. */
    public interface OwnerThread { /** @return whether the current thread owns platform mutation */ boolean isOwnerThread(); }

    /** Closed Paper mutation surface; implementations retain all Bukkit objects. */
    public interface Platform {
        /** Renders one selector page. */ void renderSelector(PlayerId playerId, Page page);
        /** Applies spectator movement, visibility, inventory and camera state. */ void applySpectator(Session session);
        /** Restores a captured pre-session player state. */ void restore(PlayerStateSnapshot capturedState);
        /** Clears every M10-owned inventory/effect/view. */ void clearOwnedState(PlayerId playerId);
    }
}
