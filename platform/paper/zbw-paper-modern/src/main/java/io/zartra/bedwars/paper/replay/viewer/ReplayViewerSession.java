package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.replay.api.ReplayId;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

/** Immutable viewer-specific projection over one Paper spectator runtime session. */
public final class ReplayViewerSession {
    private final UUID viewerId;
    private final ReplayId replayId;
    private final ViewerState state;
    private final ViewerControlAction lastAction;
    private final Integer requestedEventIndex;
    private final ReplayViewerSpeed speed;

    private ReplayViewerSession(final UUID viewerId, final ReplayId replayId,
                                final ViewerState state, final ViewerControlAction lastAction,
                                final Integer requestedEventIndex,
                                final ReplayViewerSpeed speed) {
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        this.state = Objects.requireNonNull(state, "state");
        this.lastAction = Objects.requireNonNull(lastAction, "lastAction");
        this.requestedEventIndex = requestedEventIndex;
        this.speed = Objects.requireNonNull(speed, "speed");
    }

    /** Creates a connected viewer after runtime admission succeeds. */
    public static ReplayViewerSession connected(final UUID viewerId, final ReplayId replayId) {
        return new ReplayViewerSession(viewerId, replayId, ViewerState.CONNECTED,
                ViewerControlAction.VIEW, null, ReplayViewerSpeed.NORMAL);
    }

    /** Starts initial compatibility viewing from CONNECTED. */
    public ReplayViewerSession start() {
        requireState(ViewerState.CONNECTED);
        return transition(ViewerState.WATCHING, ViewerControlAction.VIEW,
                requestedEventIndex, speed);
    }

    /** Plays from CONNECTED or resumes from PAUSED. */
    public ReplayViewerSession play() {
        if (state != ViewerState.CONNECTED && state != ViewerState.PAUSED) {
            throw new IllegalStateException("viewer cannot play in " + state);
        }
        return transition(ViewerState.WATCHING, ViewerControlAction.PLAY,
                requestedEventIndex, speed);
    }

    /** Pauses active viewing. */
    public ReplayViewerSession pause() {
        requireState(ViewerState.WATCHING);
        return transition(ViewerState.PAUSED, ViewerControlAction.PAUSE,
                requestedEventIndex, speed);
    }

    /** Resumes paused viewing. */
    public ReplayViewerSession resume() {
        requireState(ViewerState.PAUSED);
        return transition(ViewerState.WATCHING, ViewerControlAction.RESUME,
                requestedEventIndex, speed);
    }

    /** Applies one of the exact UX speed choices without changing lifecycle state. */
    public ReplayViewerSession changeSpeed(final ReplayViewerSpeed nextSpeed) {
        if (state == ViewerState.DISCONNECTED) {
            throw new IllegalStateException("disconnected viewer cannot change speed");
        }
        return transition(state, ViewerControlAction.SPEED, requestedEventIndex,
                Objects.requireNonNull(nextSpeed, "nextSpeed"));
    }

    /** Records an inclusive event-index seek request without changing play/pause state. */
    public ReplayViewerSession seek(final int eventIndex) {
        if (state != ViewerState.WATCHING && state != ViewerState.PAUSED) {
            throw new IllegalStateException("viewer cannot seek in " + state);
        }
        if (eventIndex < -1) {
            throw new IllegalArgumentException("event index must be at least -1");
        }
        return transition(state, ViewerControlAction.SEEK, Integer.valueOf(eventIndex), speed);
    }

    /** Returns an immutable information action without changing viewer state. */
    public ReplayViewerSession inspect() {
        if (state == ViewerState.DISCONNECTED) {
            throw new IllegalStateException("disconnected viewer cannot be inspected");
        }
        return transition(state, ViewerControlAction.INFO, requestedEventIndex, speed);
    }
    /** Disconnects any non-disconnected viewer. */
    public ReplayViewerSession disconnect() {
        if (state == ViewerState.DISCONNECTED) {
            throw new IllegalStateException("viewer already disconnected");
        }
        return transition(ViewerState.DISCONNECTED, ViewerControlAction.STOP,
                requestedEventIndex, speed);
    }

    private ReplayViewerSession transition(final ViewerState nextState,
                                           final ViewerControlAction action,
                                           final Integer eventIndex,
                                           final ReplayViewerSpeed nextSpeed) {
        return new ReplayViewerSession(viewerId, replayId, nextState, action,
                eventIndex, nextSpeed);
    }

    private void requireState(final ViewerState expected) {
        if (state != expected) {
            throw new IllegalStateException("expected " + expected + " but viewer is " + state);
        }
    }

    /** @return viewer identity */ public UUID viewerId() { return viewerId; }
    /** @return replay identity */ public ReplayId replayId() { return replayId; }
    /** @return current viewer lifecycle state */ public ViewerState state() { return state; }
    /** @return last successful viewer action */ public ViewerControlAction lastAction() {
        return lastAction;
    }
    /** @return selected exact UX speed */ public ReplayViewerSpeed speed() { return speed; }
    /** @return last requested inclusive event index when present */
    public OptionalInt requestedEventIndex() {
        return requestedEventIndex == null
                ? OptionalInt.empty() : OptionalInt.of(requestedEventIndex.intValue());
    }
}
