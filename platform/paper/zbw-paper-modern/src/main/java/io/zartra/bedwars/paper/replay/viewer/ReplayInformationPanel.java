package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.replay.api.ReplayId;
import java.util.Objects;

/** Immutable replay information panel displayed to an authorized viewer. */
public final class ReplayInformationPanel {
    private final ReplayId replayId;
    private final long currentMillis;
    private final long durationMillis;
    private final ReplayViewerSpeed speed;
    private final ViewerState viewerState;
    private final boolean protectedEvidence;

    /** Creates a validated timeline information snapshot. */
    public ReplayInformationPanel(final ReplayId replayId, final long currentMillis,
                                  final long durationMillis, final ReplayViewerSpeed speed,
                                  final ViewerState viewerState,
                                  final boolean protectedEvidence) {
        if (currentMillis < 0L || durationMillis < 0L || currentMillis > durationMillis) {
            throw new IllegalArgumentException("invalid replay timeline position");
        }
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        this.currentMillis = currentMillis;
        this.durationMillis = durationMillis;
        this.speed = Objects.requireNonNull(speed, "speed");
        this.viewerState = Objects.requireNonNull(viewerState, "viewerState");
        this.protectedEvidence = protectedEvidence;
    }

    /** @return replay identity */ public ReplayId replayId() { return replayId; }
    /** @return current replay-relative timestamp */ public long currentMillis() { return currentMillis; }
    /** @return total replay duration */ public long durationMillis() { return durationMillis; }
    /** @return selected UX speed */ public ReplayViewerSpeed speed() { return speed; }
    /** @return viewer lifecycle state */ public ViewerState viewerState() { return viewerState; }
    /** @return whether staff-evidence controls apply */ public boolean protectedEvidence() {
        return protectedEvidence;
    }
}
