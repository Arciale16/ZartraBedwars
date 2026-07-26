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

/** Viewer lifecycle/presentation adapter over the existing Paper replay runtime. */
public final class ReplayViewerAdapter {
    private final PaperReplayCommands runtime;
    private final ReplayViewerPresentation presentation;
    private final ReplayVisualAdapter visuals;
    private final LongSupplier currentTick;
    private final Map<UUID, ReplayViewerSession> viewers =
            new ConcurrentHashMap<UUID, ReplayViewerSession>();
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
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.visuals = visuals;
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
    }

    /** Opens and starts one authorized replay viewer session. */
    public CompletionStage<ReplayViewerResult> view(final ReplayAudience audience,
                                                    final String replayId) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(replayId, "replayId");
        final UUID viewerId = audience.playerId();
        if (viewers.containsKey(viewerId) || !opening.add(viewerId)) {
            presentation.reject(viewerId, ReplayViewerResult.Status.INVALID_STATE);
            return CompletableFuture.completedFuture(
                    ReplayViewerResult.of(ReplayViewerResult.Status.INVALID_STATE));
        }
        final CompletableFuture<ReplayViewerResult> result =
                new CompletableFuture<ReplayViewerResult>();
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
                final ReplayViewerSession connected = ReplayViewerSession.connected(
                        viewerId, ReplayId.parse(replayId));
                final ReplayRuntimeResult started = runtime.start(viewerId);
                if (started.status() != ReplayRuntimeResult.Status.STARTED) {
                    runtime.stop(viewerId);
                    completeRejected(viewerId, map(started.status()), result);
                    return;
                }
                final ReplayViewerSession watching = connected.start();
                viewers.put(viewerId, watching);
                synchronizeVisuals(viewerId, started);
                presentation.show(watching);
                result.complete(ReplayViewerResult.success(watching));
            } finally {
                opening.remove(viewerId);
            }
        });
        return result;
    }

    /** Pauses one active viewer. */
    public ReplayViewerResult pause(final UUID viewerId) {
        return update(viewerId, runtime::pause, ReplayViewerSession::pause);
    }

    /** Resumes one paused viewer. */
    public ReplayViewerResult resume(final UUID viewerId) {
        return update(viewerId, runtime::start, ReplayViewerSession::resume);
    }

    /** Applies an inclusive event-index seek request. */
    public ReplayViewerResult seek(final UUID viewerId, final int eventIndex) {
        return update(viewerId, id -> runtime.seek(id, eventIndex),
                session -> session.seek(eventIndex));
    }

    /** Stops runtime viewing and clears all owned presentation. */
    public ReplayViewerResult stop(final UUID viewerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        final ReplayViewerSession current = viewers.remove(viewerId);
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
    public void disconnect(final UUID viewerId) {
        stop(Objects.requireNonNull(viewerId, "viewerId"));
    }

    /** Stops and clears every viewer session owned by this adapter. */
    public void close() {
        for (UUID viewerId : new ArrayList<UUID>(viewers.keySet())) {
            stop(viewerId);
        }
    }
    /** @return current immutable viewer projection */
    public Optional<ReplayViewerSession> session(final UUID viewerId) {
        return Optional.ofNullable(viewers.get(Objects.requireNonNull(viewerId, "viewerId")));
    }

    private ReplayViewerResult update(final UUID viewerId,
                                      final Function<UUID, ReplayRuntimeResult> runtimeAction,
                                      final Function<ReplayViewerSession,
                                              ReplayViewerSession> viewerAction) {
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
        synchronizeVisuals(viewerId, runtimeResult);
        presentation.show(next);
        return ReplayViewerResult.success(next);
    }

    private void completeRejected(final UUID viewerId,
                                  final ReplayViewerResult.Status status,
                                  final CompletableFuture<ReplayViewerResult> result) {
        presentation.reject(viewerId, status);
        presentation.clear(viewerId);
        result.complete(ReplayViewerResult.of(status));
    }

    private void synchronizeVisuals(final UUID viewerId,
                                    final ReplayRuntimeResult runtimeResult) {
        if (visuals != null && runtimeResult.session().isPresent()) {
            visuals.synchronize(viewerId,
                    runtimeResult.session().get().context().playback(),
                    currentTick.getAsLong());
        }
    }

    private void cleanupVisuals(final UUID viewerId) {
        if (visuals != null) { visuals.cleanup(viewerId); }
    }

    private static ReplayRuntimeResult.Status expected(final ViewerControlAction action) {
        switch (action) {
            case PAUSE:
                return ReplayRuntimeResult.Status.PAUSED;
            case RESUME:
                return ReplayRuntimeResult.Status.STARTED;
            case SEEK:
                return ReplayRuntimeResult.Status.SEEKED;
            default:
                throw new IllegalArgumentException("unsupported update action " + action);
        }
    }

    private static ReplayViewerResult.Status map(final ReplayRuntimeResult.Status status) {
        switch (status) {
            case FORBIDDEN:
                return ReplayViewerResult.Status.FORBIDDEN;
            case NOT_FOUND:
                return ReplayViewerResult.Status.NOT_FOUND;
            case NO_SESSION:
                return ReplayViewerResult.Status.NO_SESSION;
            case INVALID_STATE:
            case ALREADY_OPEN:
                return ReplayViewerResult.Status.INVALID_STATE;
            default:
                return ReplayViewerResult.Status.FAILED;
        }
    }
}
