package io.zartra.bedwars.paper.replay.visual;

import io.zartra.bedwars.replay.playback.PlaybackSession;
import io.zartra.bedwars.replay.playback.PlaybackState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded owner-thread renderer synchronized exclusively from playback projections. */
public final class ReplayVisualAdapter {
    private static final int DEFAULT_MAXIMUM_VIEWERS = 256;
    private final ReplayVisualEngine engine;
    private final ReplayVisualRenderer renderer;
    private final long updateIntervalTicks;
    private final int maximumViewers;
    private final Map<UUID, ViewerProjection> projections =
            new ConcurrentHashMap<UUID, ViewerProjection>();

    /** Creates an adapter with a controlled cadence and the default viewer cap. */
    public ReplayVisualAdapter(final ReplayVisualEngine engine,
                               final ReplayVisualRenderer renderer,
                               final long updateIntervalTicks) {
        this(engine, renderer, updateIntervalTicks, DEFAULT_MAXIMUM_VIEWERS);
    }

    /** Creates an adapter with explicit update and viewer bounds. */
    public ReplayVisualAdapter(final ReplayVisualEngine engine,
                               final ReplayVisualRenderer renderer,
                               final long updateIntervalTicks,
                               final int maximumViewers) {
        if (updateIntervalTicks < 1L || maximumViewers < 1) {
            throw new IllegalArgumentException("visual bounds must be positive");
        }
        this.engine = Objects.requireNonNull(engine, "engine");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.updateIntervalTicks = updateIntervalTicks;
        this.maximumViewers = maximumViewers;
    }

    /**
     * Synchronizes one viewer. Backward seek and first render bypass cadence throttling.
     */
    public ReplayVisualResult synchronize(final UUID viewerId,
                                          final PlaybackSession playback,
                                          final long currentTick) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(playback, "playback");
        if (currentTick < 0L) { throw new IllegalArgumentException("tick must be non-negative"); }
        final ViewerProjection current = projections.get(viewerId);
        if (current == null && projections.size() >= maximumViewers) {
            return ReplayVisualResult.rejected(ReplayVisualResult.Status.OVER_CAPACITY);
        }
        final int eventIndex = playback.cursor().position().eventIndex();
        if (current != null && eventIndex == current.state.eventIndex()) {
            return ReplayVisualResult.success(ReplayVisualResult.Status.UNCHANGED, current.state);
        }
        final boolean seek = current != null && eventIndex < current.state.eventIndex();
        if (!seek && current != null && currentTick - current.lastUpdateTick < updateIntervalTicks) {
            return ReplayVisualResult.success(ReplayVisualResult.Status.UNCHANGED, current.state);
        }
        if (playback.state() == PlaybackState.FAILED) {
            cleanup(viewerId);
            return ReplayVisualResult.rejected(ReplayVisualResult.Status.CORRUPT);
        }
        final ReplayVisualResult rebuilt = engine.reconstruct(playback);
        if (!rebuilt.state().isPresent()) {
            cleanup(viewerId);
            return rebuilt;
        }
        final ReplayVisualState state = rebuilt.state().get();
        try {
            final Map<String, Object> handles = reconcile(viewerId,
                    current == null ? Collections.<String, Object>emptyMap() : current.handles,
                    state);
            projections.put(viewerId, new ViewerProjection(state, handles, currentTick));
            return rebuilt;
        } catch (RuntimeException platformFailure) {
            cleanup(viewerId);
            return ReplayVisualResult.rejected(ReplayVisualResult.Status.CORRUPT);
        }
    }

    /** Removes every representation for one viewer; cleanup is idempotent. */
    public void cleanup(final UUID viewerId) {
        final ViewerProjection removed =
                projections.remove(Objects.requireNonNull(viewerId, "viewerId"));
        if (removed == null) { return; }
        for (Object handle : new ArrayList<Object>(removed.handles.values())) {
            safeRemove(viewerId, handle);
        }
    }

    /** Removes all viewer projections owned by this adapter. */
    public void close() {
        for (UUID viewerId : new ArrayList<UUID>(projections.keySet())) {
            cleanup(viewerId);
        }
    }

    /** @return current immutable visual state */
    public Optional<ReplayVisualState> state(final UUID viewerId) {
        final ViewerProjection projection =
                projections.get(Objects.requireNonNull(viewerId, "viewerId"));
        return projection == null ? Optional.empty() : Optional.of(projection.state);
    }

    private Map<String, Object> reconcile(final UUID viewerId,
                                          final Map<String, Object> previous,
                                          final ReplayVisualState state) {
        final Map<String, Object> next = new LinkedHashMap<String, Object>();
        final Set<Object> previousHandles = Collections.newSetFromMap(
                new IdentityHashMap<Object, Boolean>());
        previousHandles.addAll(previous.values());
        try {
            for (Map.Entry<String, VisualEntityState> entry : state.entities().entrySet()) {
                final VisualEntityState entity = entry.getValue();
                final Object existing = previous.get(entry.getKey());
                if (!entity.alive()) {
                    if (existing != null) { safeRemove(viewerId, existing); }
                    continue;
                }
                if (existing == null) {
                    next.put(entry.getKey(), Objects.requireNonNull(
                            renderer.spawn(viewerId, entity), "renderer handle"));
                } else {
                    renderer.update(viewerId, existing, entity);
                    next.put(entry.getKey(), existing);
                }
            }
            for (Map.Entry<String, Object> entry : previous.entrySet()) {
                if (!next.containsKey(entry.getKey())
                        && !state.entities().containsKey(entry.getKey())) {
                    safeRemove(viewerId, entry.getValue());
                }
            }
            return Collections.unmodifiableMap(next);
        } catch (RuntimeException failure) {
            for (Object handle : next.values()) {
                if (!previousHandles.contains(handle)) { safeRemove(viewerId, handle); }
            }
            throw failure;
        }
    }

    private void safeRemove(final UUID viewerId, final Object handle) {
        try {
            renderer.remove(viewerId, handle);
        } catch (RuntimeException ignored) {
            // The projection is detached even when the platform entity already disappeared.
        }
    }

    private static final class ViewerProjection {
        private final ReplayVisualState state;
        private final Map<String, Object> handles;
        private final long lastUpdateTick;

        private ViewerProjection(final ReplayVisualState state,
                                 final Map<String, Object> handles,
                                 final long lastUpdateTick) {
            this.state = state;
            this.handles = handles;
            this.lastUpdateTick = lastUpdateTick;
        }
    }
}
