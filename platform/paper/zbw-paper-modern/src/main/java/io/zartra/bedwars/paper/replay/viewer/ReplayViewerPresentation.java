package io.zartra.bedwars.paper.replay.viewer;

import java.util.UUID;

/** Owner-thread Paper presentation boundary for simple viewer feedback and cleanup. */
public interface ReplayViewerPresentation {
    /** Presents the latest successful viewer state. */
    void show(ReplayViewerSession session);
    /** Presents a synchronized replay menu; legacy presenters receive the viewer state. */
    default void showMenu(final ReplayMenuState menu) {
        show(menu.viewerSession());
    }
    /** Presents a sanitized rejected/failed outcome. */
    void reject(UUID viewerId, ReplayViewerResult.Status status);
    /** Clears every presentation element owned by this viewer foundation. */
    void clear(UUID viewerId);
}
