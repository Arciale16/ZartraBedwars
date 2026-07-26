package io.zartra.bedwars.paper.replay;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.replay.api.ReplayAccessPolicy;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.playback.PlaybackSession;
import io.zartra.bedwars.replay.playback.PlaybackState;
import io.zartra.bedwars.replay.playback.PlaybackSpeed;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owner-thread Paper adapter for asynchronous replay load and spectator lifecycle. */
public final class PaperReplayService {
    /** Basic participant replay permission. */ public static final String VIEW_PERMISSION = "zartrabedwars.replay.view";
    /** Protected staff-evidence replay permission. */ public static final String STAFF_PERMISSION = "zartrabedwars.replay.staff";

    private final ReplaySessionRepository repository;
    private final ReplayAccessPolicy accessPolicy;
    private final ReplayPlaybackEngine playbackEngine;
    private final OwnerThread ownerThread;
    private final Map<UUID, ActiveSession> sessions = new ConcurrentHashMap<UUID, ActiveSession>();
    private final AtomicBoolean running = new AtomicBoolean();

    /** Creates a stopped runtime service around stable replay ports. */
    public PaperReplayService(final ReplaySessionRepository repository,
                              final ReplayAccessPolicy accessPolicy,
                              final ReplayPlaybackEngine playbackEngine,
                              final OwnerThread ownerThread) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.playbackEngine = Objects.requireNonNull(playbackEngine, "playbackEngine");
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
    }

    /** Starts admission exactly once. */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("replay runtime already started");
        }
    }

    /** Loads, authorizes and opens one replay without blocking the Paper owner thread. */
    public CompletionStage<ReplayRuntimeResult> open(final ReplayAudience audience,
                                                     final ReplayId replayId) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(replayId, "replayId");
        if (!running.get()) { return completed(ReplayRuntimeResult.Status.INACTIVE); }
        if (!audience.hasPermission(VIEW_PERMISSION)) { return completed(ReplayRuntimeResult.Status.FORBIDDEN); }
        if (sessions.containsKey(audience.playerId())) { return completed(ReplayRuntimeResult.Status.ALREADY_OPEN); }
        final CompletableFuture<ReplayRuntimeResult> result = new CompletableFuture<ReplayRuntimeResult>();
        final CompletionStage<Optional<ReplaySession>> loaded;
        try {
            loaded = repository.findSession(replayId);
        } catch (RuntimeException failure) {
            return completed(ReplayRuntimeResult.Status.FAILED);
        }
        if (loaded == null) { return completed(ReplayRuntimeResult.Status.FAILED); }
        loaded.whenComplete((value, failure) -> ownerThread.execute(
                () -> finishOpen(audience, value, failure, result)));
        return result;
    }

    /** Starts or resumes the current replay. */
    public ReplayRuntimeResult startPlayback(final UUID playerId) {
        return update(playerId, playbackEngine::play, ReplayRuntimeResult.Status.STARTED);
    }

    /** Pauses the current replay. */
    public ReplayRuntimeResult pause(final UUID playerId) {
        return update(playerId, playbackEngine::pause, ReplayRuntimeResult.Status.PAUSED);
    }

    /** Seeks to an inclusive event index. */
    public ReplayRuntimeResult seek(final UUID playerId, final int eventIndex) {
        return update(playerId, playback -> playbackEngine.seekToEvent(playback, eventIndex),
                ReplayRuntimeResult.Status.SEEKED);
    }

    /** Changes playback speed through the existing engine boundary. */
    public ReplayRuntimeResult changeSpeed(final UUID playerId,
                                           final PlaybackSpeed speed) {
        Objects.requireNonNull(speed, "speed");
        return update(playerId, playback -> playbackEngine.changeSpeed(playback, speed),
                ReplayRuntimeResult.Status.SPEED_CHANGED);
    }

    /** Returns the current authorized runtime projection without mutation. */
    public ReplayRuntimeResult inspect(final UUID playerId) {
        requireOwnerThread();
        final ActiveSession active = sessions.get(Objects.requireNonNull(playerId, "playerId"));
        return active == null
                ? ReplayRuntimeResult.of(ReplayRuntimeResult.Status.NO_SESSION)
                : ReplayRuntimeResult.of(ReplayRuntimeResult.Status.INSPECTED,
                        active.projection());
    }

    /** Stops one spectator session and restores its captured Paper state. */
    public ReplayRuntimeResult stop(final UUID playerId) {
        requireOwnerThread();
        final ActiveSession removed = sessions.remove(Objects.requireNonNull(playerId, "playerId"));
        if (removed == null) { return ReplayRuntimeResult.of(ReplayRuntimeResult.Status.NO_SESSION); }
        restore(removed);
        return ReplayRuntimeResult.of(ReplayRuntimeResult.Status.STOPPED, removed.projection());
    }

    /** Stops admission and schedules cleanup of every active spectator session. */
    public CompletionStage<Void> close() {
        if (!running.compareAndSet(true, false)) { return CompletableFuture.completedFuture(null); }
        final CompletableFuture<Void> result = new CompletableFuture<Void>();
        ownerThread.execute(() -> {
            final List<ActiveSession> active = new ArrayList<ActiveSession>(sessions.values());
            sessions.clear();
            for (ActiveSession session : active) { restore(session); }
            result.complete(null);
        });
        return result;
    }

    /** @return current immutable spectator projection */
    public Optional<SpectatorReplaySession> session(final UUID playerId) {
        final ActiveSession active = sessions.get(Objects.requireNonNull(playerId, "playerId"));
        return active == null ? Optional.empty() : Optional.of(active.projection());
    }

    private void finishOpen(final ReplayAudience audience, final Optional<ReplaySession> loaded,
                            final Throwable failure,
                            final CompletableFuture<ReplayRuntimeResult> result) {
        if (!running.get()) {
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.INACTIVE));
            return;
        }
        if (failure != null || loaded == null) {
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.FAILED));
            return;
        }
        if (!loaded.isPresent()) {
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.NOT_FOUND));
            return;
        }
        final ReplaySession replay = loaded.get();
        final boolean staff = audience.hasPermission(STAFF_PERMISSION);
        final ReplayAccessPolicy.Purpose purpose = staff
                ? ReplayAccessPolicy.Purpose.STAFF_EVIDENCE
                : ReplayAccessPolicy.Purpose.PERSONAL_HISTORY;
        if (!accessPolicy.mayView(replay.metadata(), PlayerId.of(audience.playerId()), purpose, staff)) {
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.FORBIDDEN));
            return;
        }
        final PlaybackSession playback = playbackEngine.load(replay);
        if (playback.state() == PlaybackState.FAILED) {
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.FAILED));
            return;
        }
        try {
            final Object restoration = audience.enterSpectatorReplay();
            final ActiveSession active = new ActiveSession(audience, restoration,
                    new ReplayRuntimeContext(replay, playback));
            final ActiveSession previous = sessions.putIfAbsent(audience.playerId(), active);
            if (previous != null) {
                audience.leaveSpectatorReplay(restoration);
                result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.ALREADY_OPEN));
                return;
            }
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.OPENED, active.projection()));
        } catch (RuntimeException platformFailure) {
            result.complete(ReplayRuntimeResult.of(ReplayRuntimeResult.Status.FAILED));
        }
    }

    private ReplayRuntimeResult update(final UUID playerId, final PlaybackUpdate update,
                                       final ReplayRuntimeResult.Status success) {
        requireOwnerThread();
        final ActiveSession active = sessions.get(Objects.requireNonNull(playerId, "playerId"));
        if (active == null) { return ReplayRuntimeResult.of(ReplayRuntimeResult.Status.NO_SESSION); }
        try {
            active.context = active.context.withPlayback(update.apply(active.context.playback()));
            return ReplayRuntimeResult.of(success, active.projection());
        } catch (IllegalArgumentException | IllegalStateException rejected) {
            return ReplayRuntimeResult.of(ReplayRuntimeResult.Status.INVALID_STATE, active.projection());
        }
    }

    private void restore(final ActiveSession active) {
        try {
            active.audience.leaveSpectatorReplay(active.restoration);
        } catch (RuntimeException ignored) {
            // Cleanup remains best-effort and runtime state is already detached.
        }
    }

    private void requireOwnerThread() {
        if (!ownerThread.isOwnerThread()) { throw new IllegalStateException("replay control requires Paper owner thread"); }
    }

    private static CompletionStage<ReplayRuntimeResult> completed(final ReplayRuntimeResult.Status status) {
        return CompletableFuture.completedFuture(ReplayRuntimeResult.of(status));
    }

    /** Owner-thread scheduler supplied by the Paper composition root. */
    public interface OwnerThread {
        /** Schedules one bounded mutation on the Paper owner thread. */ void execute(Runnable action);
        /** @return whether caller is the Paper owner thread */ boolean isOwnerThread();
    }

    private interface PlaybackUpdate { PlaybackSession apply(PlaybackSession playback); }

    private static final class ActiveSession {
        private final ReplayAudience audience;
        private final Object restoration;
        private ReplayRuntimeContext context;
        private ActiveSession(final ReplayAudience audience, final Object restoration,
                              final ReplayRuntimeContext context) {
            this.audience = audience;
            this.restoration = restoration;
            this.context = context;
        }
        private SpectatorReplaySession projection() {
            return new SpectatorReplaySession(audience.playerId(), context);
        }
    }
}
