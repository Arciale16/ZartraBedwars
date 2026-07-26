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

    private ReplayViewerSession(final UUID viewerId, final ReplayId replayId,
                                final ViewerState state, final ViewerControlAction lastAction,
                                final Integer requestedEventIndex) {
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        this.state = Objects.requireNonNull(state, "state");
        this.lastAction = Objects.requireNonNull(lastAction, "lastAction");
        this.requestedEventIndex = requestedEventIndex;
    }

    /** Creates a connected viewer after runtime admission succeeds. */
    public static ReplayViewerSession connected(final UUID viewerId, final ReplayId replayId) {
        return new ReplayViewerSession(viewerId, replayId, ViewerState.CONNECTED,
                ViewerControlAction.VIEW, null);
    }

    /** Starts initial viewing from CONNECTED. */
    public ReplayViewerSession start() {
        requireState(ViewerState.CONNECTED);
        return transition(ViewerState.WATCHING, ViewerControlAction.VIEW, null);
    }

    /** Pauses active viewing. */
    public ReplayViewerSession pause() {
        requireState(ViewerState.WATCHING);
        return transition(ViewerState.PAUSED, ViewerControlAction.PAUSE, requestedEventIndex);
    }

    /** Resumes paused viewing. */
    public ReplayViewerSession resume() {
        requireState(ViewerState.PAUSED);
        return transition(ViewerState.WATCHING, ViewerControlAction.RESUME, requestedEventIndex);
    }

    /** Records an inclusive event-index seek request without changing play/pause state. */
    public ReplayViewerSession seek(final int eventIndex) {
        if (state != ViewerState.WATCHING && state != ViewerState.PAUSED) {
            throw new IllegalStateException("viewer cannot seek in " + state);
        }
        if (eventIndex < -1) {
            throw new IllegalArgumentException("event index must be at least -1");
        }
        return transition(state, ViewerControlAction.SEEK, Integer.valueOf(eventIndex));
    }

    /** Disconnects any non-disconnected viewer. */
    public ReplayViewerSession disconnect() {
        if (state == ViewerState.DISCONNECTED) {
            throw new IllegalStateException("viewer already disconnected");
        }
        return transition(ViewerState.DISCONNECTED, ViewerControlAction.STOP,
                requestedEventIndex);
    }

    private ReplayViewerSession transition(final ViewerState nextState,
                                           final ViewerControlAction action,
                                           final Integer eventIndex) {
        return new ReplayViewerSession(viewerId, replayId, nextState, action, eventIndex);
    }

    private void requireState(final ViewerState expected) {
        if (state != expected) {
            throw new IllegalStateException("expected " + expected + " but viewer is " + state);
        }
    }

    /** @return viewer identity */
    public UUID viewerId() { return viewerId; }
    /** @return replay identity */
    public ReplayId replayId() { return replayId; }
    /** @return current viewer lifecycle state */
    public ViewerState state() { return state; }
    /** @return last successful viewer action */
    public ViewerControlAction lastAction() { return lastAction; }
    /** @return last requested inclusive event index when present */
    public OptionalInt requestedEventIndex() {
        return requestedEventIndex == null
                ? OptionalInt.empty() : OptionalInt.of(requestedEventIndex.intValue());
    }
}
