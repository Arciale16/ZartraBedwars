package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.paper.replay.PaperReplayCommands;
import io.zartra.bedwars.paper.replay.ReplayAudience;
import io.zartra.bedwars.paper.replay.ReplayRuntimeResult;
import io.zartra.bedwars.paper.replay.visual.ReplayVisualAdapter;
import io.zartra.bedwars.replay.api.ReplayId;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

/** Viewer lifecycle, controls and bounded menu projection over the Paper replay runtime. */
public final class ReplayViewerAdapter {
    private static final int DEFAULT_MAXIMUM_VIEWERS = 256;
    private final PaperReplayCommands runtime;
    private final ReplayViewerPresentation presentation;
    private final ReplayVisualAdapter visuals;
    private final LongSupplier currentTick;
    private final ReplayMenuFactory menuFactory;
    private final int maximumViewers;
    private final Object admissionLock = new Object();
    private final Map<UUID, ReplayViewerSession> viewers = new ConcurrentHashMap<UUID, ReplayViewerSession>();
    private final Map<UUID, ReplayMenuState> menus = new ConcurrentHashMap<UUID, ReplayMenuState>();
    private final Set<UUID> opening = ConcurrentHashMap.newKeySet();

    /** Creates a viewer adapter without owning replay engine or persistence policy. */
    public ReplayViewerAdapter(final PaperReplayCommands runtime,
                               final ReplayViewerPresentation presentation) {
        this(runtime, presentation, null, () -> 0L);
    }

    /** Creates a viewer adapter synchronized with a bounded visual projection. */
    public ReplayViewerAdapter(final PaperReplayCommands runtime,
                               final ReplayViewerPresentation presentation,
                               final ReplayVisualAdapter visuals,
                               final LongSupplier currentTick) {
        this(runtime, presentation, visuals, currentTick, DEFAULT_MAXIMUM_VIEWERS);
    }

    /** Creates a viewer adapter with explicit concurrent-viewer admission bounds. */
    public ReplayViewerAdapter(final PaperReplayCommands runtime,
                               final ReplayViewerPresentation presentation,
                               final ReplayVisualAdapter visuals,
                               final LongSupplier currentTick,
                               final int maximumViewers) {
        if (maximumViewers < 1) {
            throw new IllegalArgumentException("maximum viewers must be positive");
        }
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.visuals = visuals;
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
        this.menuFactory = new ReplayMenuFactory();
        this.maximumViewers = maximumViewers;
    }

    /** Opens and starts one authorized replay viewer session. */
    public CompletionStage<ReplayViewerResult> view(final ReplayAudience audience, final String replayId) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(replayId, "replayId");
        final UUID viewerId = audience.playerId();
        if (!admit(viewerId)) {
            presentation.reject(viewerId, ReplayViewerResult.Status.INVALID_STATE);
            return CompletableFuture.completedFuture(ReplayViewerResult.of(ReplayViewerResult.Status.INVALID_STATE));
        }
        final CompletableFuture<ReplayViewerResult> result = new CompletableFuture<ReplayViewerResult>();
        runtime.open(audience, replayId).whenComplete((opened, failure) -> {
            try {
                if (failure != null || opened == null) {
                    completeRejected(viewerId, ReplayViewerResult.Status.FAILED, result);
                    return;
                }
                if (opened.status() != ReplayRuntimeResult.Status.OPENED) {
                    completeRejected(viewerId, map(opened.status()), result);
                    return;
                }
                final ReplayViewerSession connected = ReplayViewerSession.connected(viewerId, ReplayId.parse(replayId));
                final ReplayRuntimeResult started = runtime.start(viewerId);
                if (started.status() != ReplayRuntimeResult.Status.STARTED) {
                    runtime.stop(viewerId);
                    completeRejected(viewerId, map(started.status()), result);
                    return;
                }
                final ReplayViewerSession watching = connected.start();
                viewers.put(viewerId, watching);
                try {
                    synchronize(viewerId, watching, started);
                    result.complete(ReplayViewerResult.success(watching));
                } catch (RuntimeException projectionFailure) {
                    failClosed(viewerId);
                    result.complete(ReplayViewerResult.of(ReplayViewerResult.Status.FAILED));
                }
            } finally {
                opening.remove(viewerId);
            }
        });
        return result;
    }

    /** Starts or resumes the active viewer. */
    public ReplayViewerResult play(final UUID viewerId) {
        final ReplayViewerSession current = viewers.get(Objects.requireNonNull(viewerId, "viewerId"));
        if (current != null && current.state() == ViewerState.WATCHING) { return info(viewerId); }
        return update(viewerId, runtime::start, ReplayViewerSession::play);
    }

    /** Pauses one active viewer. */
    public ReplayViewerResult pause(final UUID viewerId) {
        return update(viewerId, runtime::pause, ReplayViewerSession::pause);
    }

    /** Resumes one paused viewer. */
    public ReplayViewerResult resume(final UUID viewerId) { return play(viewerId); }

    /** Changes to one of the exact supported playback speeds. */
    public ReplayViewerResult speed(final UUID viewerId, final ReplayViewerSpeed speed) {
        Objects.requireNonNull(speed, "speed");
        return update(viewerId, id -> runtime.speed(id, speed.playbackSpeed()),
                session -> session.changeSpeed(speed));
    }

    /** Applies an inclusive event-index seek request. */
    public ReplayViewerResult seek(final UUID viewerId, final int eventIndex) {
        return update(viewerId, id -> runtime.seek(id, eventIndex), session -> session.seek(eventIndex));
    }

    /** Inspects and presents the current authorized replay without exposing another session. */
    public ReplayViewerResult info(final UUID viewerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        final ReplayViewerSession current = viewers.get(viewerId);
        if (current == null) {
            presentation.reject(viewerId, ReplayViewerResult.Status.NO_SESSION);
            return ReplayViewerResult.of(ReplayViewerResult.Status.NO_SESSION);
        }
        final ReplayRuntimeResult inspected = runtime.info(viewerId);
        if (inspected.status() != ReplayRuntimeResult.Status.INSPECTED) {
            final ReplayViewerResult.Status status = map(inspected.status());
            presentation.reject(viewerId, status);
            return ReplayViewerResult.of(status);
        }
        final ReplayViewerSession projected = current.inspect();
        try {
            synchronize(viewerId, projected, inspected);
            return ReplayViewerResult.success(projected);
        } catch (RuntimeException projectionFailure) {
            failClosed(viewerId);
            return ReplayViewerResult.of(ReplayViewerResult.Status.FAILED);
        }
    }

    /** Stops runtime viewing and clears all owned presentation. */
    public ReplayViewerResult stop(final UUID viewerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        final ReplayViewerSession current = viewers.remove(viewerId);
        menus.remove(viewerId);
        if (current == null) {
            presentation.clear(viewerId);
            return ReplayViewerResult.of(ReplayViewerResult.Status.NO_SESSION);
        }
        final ReplayRuntimeResult stopped = runtime.stop(viewerId);
        final ReplayViewerSession disconnected = current.disconnect();
        cleanupVisuals(viewerId);
        presentation.clear(viewerId);
        if (stopped.status() != ReplayRuntimeResult.Status.STOPPED) {
            presentation.reject(viewerId, map(stopped.status()));
            return ReplayViewerResult.of(map(stopped.status()));
        }
        return ReplayViewerResult.success(disconnected);
    }

    /** Clears a viewer after disconnect; idempotent when no viewer exists. */
    public void disconnect(final UUID viewerId) { stop(Objects.requireNonNull(viewerId, "viewerId")); }

    /** Stops and clears every viewer session owned by this adapter. */
    public void close() {
        for (UUID viewerId : new ArrayList<UUID>(viewers.keySet())) { stop(viewerId); }
    }

    /** @return current immutable viewer projection */
    public Optional<ReplayViewerSession> session(final UUID viewerId) {
        return Optional.ofNullable(viewers.get(Objects.requireNonNull(viewerId, "viewerId")));
    }

    /** @return current bounded immutable replay menu projection */
    public Optional<ReplayMenuState> menu(final UUID viewerId) {
        return Optional.ofNullable(menus.get(Objects.requireNonNull(viewerId, "viewerId")));
    }

    private ReplayViewerResult update(final UUID viewerId,
                                      final Function<UUID, ReplayRuntimeResult> runtimeAction,
                                      final Function<ReplayViewerSession, ReplayViewerSession> viewerAction) {
        Objects.requireNonNull(viewerId, "viewerId");
        final ReplayViewerSession current = viewers.get(viewerId);
        if (current == null) {
            presentation.reject(viewerId, ReplayViewerResult.Status.NO_SESSION);
            return ReplayViewerResult.of(ReplayViewerResult.Status.NO_SESSION);
        }
        final ReplayViewerSession next;
        try {
            next = viewerAction.apply(current);
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            presentation.reject(viewerId, ReplayViewerResult.Status.INVALID_STATE);
            return ReplayViewerResult.of(ReplayViewerResult.Status.INVALID_STATE);
        }
        final ReplayRuntimeResult runtimeResult = runtimeAction.apply(viewerId);
        if (runtimeResult.status() != expected(next.lastAction())) {
            final ReplayViewerResult.Status status = map(runtimeResult.status());
            presentation.reject(viewerId, status);
            return ReplayViewerResult.of(status);
        }
        viewers.put(viewerId, next);
        try {
            synchronize(viewerId, next, runtimeResult);
            return ReplayViewerResult.success(next);
        } catch (RuntimeException projectionFailure) {
            failClosed(viewerId);
            return ReplayViewerResult.of(ReplayViewerResult.Status.FAILED);
        }
    }

    private void synchronize(final UUID viewerId, final ReplayViewerSession session,
                             final ReplayRuntimeResult runtimeResult) {
        synchronizeVisuals(viewerId, runtimeResult);
        if (!runtimeResult.session().isPresent()) { throw new IllegalStateException("runtime projection missing"); }
        final ReplayMenuState menu = menuFactory.create(session, runtimeResult.session().get());
        menus.put(viewerId, menu);
        presentation.showMenu(menu);
    }

    private void completeRejected(final UUID viewerId, final ReplayViewerResult.Status status,
                                  final CompletableFuture<ReplayViewerResult> result) {
        presentation.reject(viewerId, status);
        presentation.clear(viewerId);
        result.complete(ReplayViewerResult.of(status));
    }

    private void synchronizeVisuals(final UUID viewerId, final ReplayRuntimeResult runtimeResult) {
        if (visuals != null && runtimeResult.session().isPresent()) {
            visuals.synchronize(viewerId, runtimeResult.session().get().context().playback(), currentTick.getAsLong());
        }
    }

    private void cleanupVisuals(final UUID viewerId) { if (visuals != null) { visuals.cleanup(viewerId); } }

    private boolean admit(final UUID viewerId) {
        synchronized (admissionLock) {
            if (viewers.containsKey(viewerId) || opening.contains(viewerId)
                    || viewers.size() + opening.size() >= maximumViewers) {
                return false;
            }
            return opening.add(viewerId);
        }
    }

    private void failClosed(final UUID viewerId) {
        viewers.remove(viewerId);
        menus.remove(viewerId);
        runtime.stop(viewerId);
        cleanupVisuals(viewerId);
        try {
            presentation.clear(viewerId);
        } catch (RuntimeException ignored) {
            // Runtime and visual resources are already detached.
        }
        try {
            presentation.reject(viewerId, ReplayViewerResult.Status.FAILED);
        } catch (RuntimeException ignored) {
            // A presentation failure cannot retain the replay session.
        }
    }

    private static ReplayRuntimeResult.Status expected(final ViewerControlAction action) {
        switch (action) {
            case PLAY:
            case RESUME: return ReplayRuntimeResult.Status.STARTED;
            case PAUSE: return ReplayRuntimeResult.Status.PAUSED;
            case SPEED: return ReplayRuntimeResult.Status.SPEED_CHANGED;
            case SEEK: return ReplayRuntimeResult.Status.SEEKED;
            default: throw new IllegalArgumentException("unsupported update action " + action);
        }
    }

    private static ReplayViewerResult.Status map(final ReplayRuntimeResult.Status status) {
        switch (status) {
            case FORBIDDEN: return ReplayViewerResult.Status.FORBIDDEN;
            case NOT_FOUND: return ReplayViewerResult.Status.NOT_FOUND;
            case NO_SESSION: return ReplayViewerResult.Status.NO_SESSION;
            case INVALID_STATE:
            case ALREADY_OPEN: return ReplayViewerResult.Status.INVALID_STATE;
            default: return ReplayViewerResult.Status.FAILED;
        }
    }
}
