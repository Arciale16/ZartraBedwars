package io.zartra.bedwars.paper.replay.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.paper.replay.PaperReplayCommands;
import io.zartra.bedwars.paper.replay.PaperReplayService;
import io.zartra.bedwars.paper.replay.ReplayAudience;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-004/007/010 viewer lifecycle, cleanup and routing tests. */
final class ReplayViewerFoundationTest {
    private static final UUID PLAYER = new UUID(0L, 617L);
    private static final ReplayId REPLAY =
            ReplayId.parse("00000000-0000-0000-0000-000000000617");

    @Test
    void immutableViewerModelEnforcesLifecycleAndSeekBounds() {
        final ReplayViewerSession connected = ReplayViewerSession.connected(PLAYER, REPLAY);
        final ReplayViewerSession watching = connected.start();
        final ReplayViewerSession paused = watching.pause();
        final ReplayViewerSession sought = paused.seek(0);
        final ReplayViewerSession resumed = sought.resume();
        final ReplayViewerSession disconnected = resumed.disconnect();

        assertEquals(ViewerState.CONNECTED, connected.state());
        assertEquals(ViewerState.WATCHING, watching.state());
        assertEquals(ViewerState.PAUSED, sought.state());
        assertEquals(ViewerControlAction.SEEK, sought.lastAction());
        assertEquals(0, sought.requestedEventIndex().orElseThrow());
        assertEquals(ViewerState.DISCONNECTED, disconnected.state());
        assertThrows(IllegalStateException.class, connected::pause);
        assertThrows(IllegalStateException.class, watching::resume);
        assertThrows(IllegalArgumentException.class, () -> paused.seek(-2));
        assertThrows(IllegalStateException.class, disconnected::disconnect);
    }

    @Test
    void viewerAdapterControlsRuntimeAndAlwaysClearsPresentation() {
        final RecordingPresentation presentation = new RecordingPresentation();
        final RuntimeHarness runtime = runtime(completed(Optional.of(completedReplay())));
        final ReplayViewerAdapter viewer =
                new ReplayViewerAdapter(runtime.commands, presentation);
        final FakeAudience audience = new FakeAudience(true);

        final ReplayViewerResult opened =
                viewer.view(audience, REPLAY.toString()).toCompletableFuture().join();
        assertEquals(ReplayViewerResult.Status.SUCCESS, opened.status());
        assertEquals(ViewerState.WATCHING, opened.session().orElseThrow().state());
        assertEquals(ViewerState.WATCHING, presentation.lastState.get());
        assertEquals(ViewerState.PAUSED, viewer.pause(PLAYER)
                .session().orElseThrow().state());
        assertEquals(ViewerState.WATCHING, viewer.resume(PLAYER)
                .session().orElseThrow().state());
        assertEquals(0, viewer.seek(PLAYER, 0).session().orElseThrow()
                .requestedEventIndex().orElseThrow());
        final ReplayViewerResult stopped = viewer.stop(PLAYER);
        assertEquals(ViewerState.DISCONNECTED, stopped.session().orElseThrow().state());
        assertEquals(1, presentation.clears.get());
        assertEquals(1, audience.left.get());
        assertFalse(viewer.session(PLAYER).isPresent());
    }

    @Test
    void duplicateViewCannotClearOrReplaceActiveViewer() {
        final RecordingPresentation presentation = new RecordingPresentation();
        final RuntimeHarness runtime = runtime(completed(Optional.of(completedReplay())));
        final ReplayViewerAdapter viewer =
                new ReplayViewerAdapter(runtime.commands, presentation);
        final FakeAudience audience = new FakeAudience(true);

        viewer.view(audience, REPLAY.toString()).toCompletableFuture().join();
        assertEquals(ReplayViewerResult.Status.INVALID_STATE,
                viewer.view(audience, REPLAY.toString()).toCompletableFuture().join().status());
        assertEquals(ViewerState.WATCHING, viewer.session(PLAYER).orElseThrow().state());
        assertEquals(0, presentation.clears.get());
    }
    @Test
    void permissionAndFailedLoadNeverCreateViewerSession() {
        final RecordingPresentation presentation = new RecordingPresentation();
        RuntimeHarness runtime = runtime(completed(Optional.of(completedReplay())));
        ReplayViewerAdapter viewer = new ReplayViewerAdapter(runtime.commands, presentation);

        assertEquals(ReplayViewerResult.Status.FORBIDDEN,
                viewer.view(new FakeAudience(false), REPLAY.toString())
                        .toCompletableFuture().join().status());
        assertFalse(viewer.session(PLAYER).isPresent());

        final CompletableFuture<Optional<ReplaySession>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("private storage detail"));
        runtime = runtime(failed);
        viewer = new ReplayViewerAdapter(runtime.commands, presentation);
        assertEquals(ReplayViewerResult.Status.FAILED,
                viewer.view(new FakeAudience(true), REPLAY.toString())
                        .toCompletableFuture().join().status());
        assertFalse(viewer.session(PLAYER).isPresent());
        assertTrue(presentation.clears.get() >= 2);
    }

    @Test
    void invalidViewerSessionDoesNotReachRuntimeMutation() {
        final RecordingPresentation presentation = new RecordingPresentation();
        final RuntimeHarness runtime = runtime(completed(Optional.of(completedReplay())));
        final ReplayViewerAdapter viewer =
                new ReplayViewerAdapter(runtime.commands, presentation);

        assertEquals(ReplayViewerResult.Status.NO_SESSION, viewer.pause(PLAYER).status());
        assertEquals(ReplayViewerResult.Status.NO_SESSION, viewer.stop(PLAYER).status());
        assertEquals(ReplayViewerResult.Status.NO_SESSION, viewer.seek(PLAYER, 0).status());
        assertEquals(3, presentation.rejections.get() + presentation.clears.get());
    }

    @Test
    void commandRouterAcceptsOnlyFiveExactFoundationRoutes() {
        final RuntimeHarness runtime = runtime(completed(Optional.of(completedReplay())));
        final ReplayViewerAdapter viewer =
                new ReplayViewerAdapter(runtime.commands, new RecordingPresentation());
        final ReplayViewerCommandRouter router = new ReplayViewerCommandRouter(viewer);
        final FakeAudience audience = new FakeAudience(true);

        assertEquals(ReplayViewerResult.Status.SUCCESS,
                route(router, audience, "view", REPLAY.toString()).status());
        assertEquals(ViewerState.PAUSED,
                route(router, audience, "pause").session().orElseThrow().state());
        assertEquals(ViewerState.WATCHING,
                route(router, audience, "resume").session().orElseThrow().state());
        assertEquals(0, route(router, audience, "seek", "0").session()
                .orElseThrow().requestedEventIndex().orElseThrow());
        assertEquals(ViewerState.DISCONNECTED,
                route(router, audience, "stop").session().orElseThrow().state());
        assertEquals(ReplayViewerResult.Status.INVALID_COMMAND,
                route(router, audience, "seek", "bad").status());
        assertEquals(ReplayViewerResult.Status.INVALID_COMMAND,
                route(router, audience, "view").status());
        assertEquals(ReplayViewerResult.Status.INVALID_COMMAND,
                route(router, audience, "unknown").status());
    }

    @Test
    void viewerBootstrapCleansDisconnectAndShutdownIdempotently() {
        final RuntimeHarness runtime = runtime(completed(Optional.of(completedReplay())));
        final RecordingPresentation presentation = new RecordingPresentation();
        final ReplayViewerAdapter viewer =
                new ReplayViewerAdapter(runtime.commands, presentation);
        final AtomicReference<Consumer<UUID>> disconnect = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        final ReplayViewerBootstrap bootstrap = new ReplayViewerBootstrap(viewer, callback -> {
            disconnect.set(callback);
            return () -> closed.set(true);
        });
        bootstrap.start();
        assertThrows(IllegalStateException.class, bootstrap::start);
        final FakeAudience audience = new FakeAudience(true);
        viewer.view(audience, REPLAY.toString()).toCompletableFuture().join();
        disconnect.get().accept(PLAYER);
        assertFalse(viewer.session(PLAYER).isPresent());

        viewer.view(audience, REPLAY.toString()).toCompletableFuture().join();
        bootstrap.stop();
        bootstrap.stop();
        assertTrue(closed.get());
        assertFalse(viewer.session(PLAYER).isPresent());
        assertEquals(2, audience.left.get());
    }

    private static ReplayViewerResult route(final ReplayViewerCommandRouter router,
                                            final ReplayAudience audience,
                                            final String... tokens) {
        return router.route(audience, Arrays.asList(tokens)).toCompletableFuture().join();
    }

    private static RuntimeHarness runtime(
            final CompletionStage<Optional<ReplaySession>> loaded) {
        final PaperReplayService service = new PaperReplayService(repository(loaded),
                new io.zartra.bedwars.replay.api.ReplayAccessPolicy(),
                new ReplayPlaybackEngine(), new ImmediateOwnerThread());
        service.start();
        return new RuntimeHarness(service, new PaperReplayCommands(service));
    }

    private static CompletionStage<Optional<ReplaySession>> completed(
            final Optional<ReplaySession> replay) {
        return CompletableFuture.completedFuture(replay);
    }

    private static ReplaySession completedReplay() {
        final ReplayMetadata metadata = new ReplayMetadata(REPLAY,
                MatchId.parse("00000000-0000-0000-0000-000000000608"),
                Instant.parse("2026-07-26T00:00:00Z"), 1,
                Set.of(PlayerId.of(PLAYER)), false);
        return ReplaySession.create(metadata).start().record(new ReplayEvent(
                "event-0", 0L, 10L, Instant.parse("2026-07-26T00:00:10Z"),
                ReplayEvent.Source.GAME, "match.transition",
                Collections.singletonMap("state", "RUNNING"))).complete();
    }

    private static ReplaySessionRepository repository(
            final CompletionStage<Optional<ReplaySession>> loaded) {
        return new ReplaySessionRepository() {
            @Override public CompletionStage<Boolean> create(final ReplaySession session) {
                throw new UnsupportedOperationException();
            }
            @Override public CompletionStage<SaveResult> save(final ReplaySession session,
                                                               final ReplayState expectedState) {
                throw new UnsupportedOperationException();
            }
            @Override public CompletionStage<Optional<ReplaySession>> findSession(
                    final ReplayId replayId) {
                return loaded;
            }
        };
    }

    private static final class RuntimeHarness {
        private final PaperReplayService service;
        private final PaperReplayCommands commands;
        private RuntimeHarness(final PaperReplayService service,
                               final PaperReplayCommands commands) {
            this.service = service;
            this.commands = commands;
        }
    }

    private static final class ImmediateOwnerThread implements PaperReplayService.OwnerThread {
        @Override public void execute(final Runnable action) { action.run(); }
        @Override public boolean isOwnerThread() { return true; }
    }

    private static final class FakeAudience implements ReplayAudience {
        private final boolean permission;
        private final AtomicInteger left = new AtomicInteger();
        private FakeAudience(final boolean permission) { this.permission = permission; }
        @Override public UUID playerId() { return PLAYER; }
        @Override public boolean hasPermission(final String node) { return permission; }
        @Override public Object enterSpectatorReplay() { return "restore"; }
        @Override public void leaveSpectatorReplay(final Object restoration) {
            left.incrementAndGet();
        }
    }

    private static final class RecordingPresentation implements ReplayViewerPresentation {
        private final AtomicReference<ViewerState> lastState = new AtomicReference<>();
        private final AtomicInteger rejections = new AtomicInteger();
        private final AtomicInteger clears = new AtomicInteger();
        @Override public void show(final ReplayViewerSession session) {
            lastState.set(session.state());
        }
        @Override public void reject(final UUID viewerId,
                                     final ReplayViewerResult.Status status) {
            rejections.incrementAndGet();
        }
        @Override public void clear(final UUID viewerId) { clears.incrementAndGet(); }
    }
}
