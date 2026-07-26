package io.zartra.bedwars.paper.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.replay.api.ReplayAccessPolicy;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import io.zartra.bedwars.replay.playback.PlaybackState;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import java.time.Instant;
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

/** ZBW-REPLAY-004/007/009/010 Paper runtime lifecycle tests. */
final class PaperReplayRuntimeTest {
    private static final UUID PLAYER = new UUID(0L, 17L);
    private static final ReplayId REPLAY = ReplayId.parse("00000000-0000-0000-0000-000000000017");

    @Test
    void opensControlsAndStopsAuthorizedParticipantSession() {
        final FakeAudience audience = new FakeAudience(true, false);
        final PaperReplayService service = service(repository(CompletableFuture.completedFuture(
                Optional.of(completedReplay()))), new ImmediateOwnerThread());
        service.start();

        final ReplayRuntimeResult opened = service.open(audience, REPLAY).toCompletableFuture().join();
        assertEquals(ReplayRuntimeResult.Status.OPENED, opened.status());
        assertEquals(1, audience.entered.get());
        assertEquals(PlaybackState.READY,
                opened.session().orElseThrow().context().playback().state());
        assertEquals(ReplayRuntimeResult.Status.STARTED, service.startPlayback(PLAYER).status());
        assertEquals(ReplayRuntimeResult.Status.PAUSED, service.pause(PLAYER).status());
        assertEquals(ReplayRuntimeResult.Status.SEEKED, service.seek(PLAYER, 0).status());
        assertEquals(ReplayRuntimeResult.Status.STOPPED, service.stop(PLAYER).status());
        assertEquals(1, audience.left.get());
        assertFalse(service.session(PLAYER).isPresent());
    }

    @Test
    void rejectsPermissionBeforeRepositoryAccessAndEnforcesPrivacyPolicy() {
        final AtomicInteger loads = new AtomicInteger();
        final ReplaySessionRepository repository = repository(id -> {
            loads.incrementAndGet();
            return CompletableFuture.completedFuture(Optional.of(completedReplay()));
        });
        final PaperReplayService deniedService = service(repository, new ImmediateOwnerThread());
        deniedService.start();
        assertEquals(ReplayRuntimeResult.Status.FORBIDDEN,
                deniedService.open(new FakeAudience(false, false), REPLAY)
                        .toCompletableFuture().join().status());
        assertEquals(0, loads.get());

        final PaperReplayService privateService = service(
                repository(CompletableFuture.completedFuture(Optional.of(protectedReplay()))),
                new ImmediateOwnerThread());
        privateService.start();
        assertEquals(ReplayRuntimeResult.Status.FORBIDDEN,
                privateService.open(new FakeAudience(true, false), REPLAY)
                        .toCompletableFuture().join().status());
        assertEquals(ReplayRuntimeResult.Status.OPENED,
                privateService.open(new FakeAudience(true, true), REPLAY)
                        .toCompletableFuture().join().status());
    }

    @Test
    void handlesMissingFailedAndNonPlayableLoadsWithoutSpectatorMutation() {
        final FakeAudience audience = new FakeAudience(true, false);
        PaperReplayService service = service(repository(CompletableFuture.completedFuture(
                Optional.empty())), new ImmediateOwnerThread());
        service.start();
        assertEquals(ReplayRuntimeResult.Status.NOT_FOUND,
                service.open(audience, REPLAY).toCompletableFuture().join().status());

        final CompletableFuture<Optional<ReplaySession>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("database secret"));
        service = service(repository(failed), new ImmediateOwnerThread());
        service.start();
        assertEquals(ReplayRuntimeResult.Status.FAILED,
                service.open(audience, REPLAY).toCompletableFuture().join().status());

        service = service(repository(CompletableFuture.completedFuture(
                Optional.of(baseReplay().start()))), new ImmediateOwnerThread());
        service.start();
        assertEquals(ReplayRuntimeResult.Status.FAILED,
                service.open(audience, REPLAY).toCompletableFuture().join().status());
        assertEquals(0, audience.entered.get());
    }

    @Test
    void bootstrapRegistersDisconnectAndCleansEverySessionOnStop() {
        final PaperReplayService service = service(repository(CompletableFuture.completedFuture(
                Optional.of(completedReplay()))), new ImmediateOwnerThread());
        final AtomicReference<Consumer<UUID>> disconnect = new AtomicReference<>();
        final AtomicBoolean registrationClosed = new AtomicBoolean();
        final ReplayRuntimeBootstrap bootstrap = new ReplayRuntimeBootstrap(service, callback -> {
            disconnect.set(callback);
            return () -> registrationClosed.set(true);
        });
        bootstrap.start();
        final FakeAudience first = new FakeAudience(true, false);
        assertEquals(ReplayRuntimeResult.Status.OPENED,
                bootstrap.commands().open(first, REPLAY.toString()).toCompletableFuture().join().status());
        disconnect.get().accept(PLAYER);
        assertEquals(1, first.left.get());
        assertFalse(service.session(PLAYER).isPresent());

        final FakeAudience second = new FakeAudience(true, false);
        assertEquals(ReplayRuntimeResult.Status.OPENED,
                bootstrap.commands().open(second, REPLAY.toString()).toCompletableFuture().join().status());
        bootstrap.stop().toCompletableFuture().join();
        assertTrue(registrationClosed.get());
        assertEquals(1, second.left.get());
        assertEquals(ReplayRuntimeResult.Status.INACTIVE,
                service.open(second, REPLAY).toCompletableFuture().join().status());
        bootstrap.stop().toCompletableFuture().join();
    }

    @Test
    void closeBeforeAsyncLoadCompletionPreventsLateJoin() {
        final CompletableFuture<Optional<ReplaySession>> loading = new CompletableFuture<>();
        final PaperReplayService service = service(repository(loading), new ImmediateOwnerThread());
        final FakeAudience audience = new FakeAudience(true, false);
        service.start();
        final CompletionStage<ReplayRuntimeResult> open = service.open(audience, REPLAY);
        service.close().toCompletableFuture().join();
        loading.complete(Optional.of(completedReplay()));
        assertEquals(ReplayRuntimeResult.Status.INACTIVE, open.toCompletableFuture().join().status());
        assertEquals(0, audience.entered.get());
    }

    @Test
    void rejectsInvalidCommandInputStateAndWrongThreadControls() {
        final PaperReplayService inactive = service(repository(CompletableFuture.completedFuture(
                Optional.of(completedReplay()))), new ImmediateOwnerThread());
        final PaperReplayCommands commands = new PaperReplayCommands(inactive);
        assertEquals(ReplayRuntimeResult.Status.NOT_FOUND,
                commands.open(new FakeAudience(true, false), "bad-id").toCompletableFuture().join().status());
        assertEquals(ReplayRuntimeResult.Status.NO_SESSION, commands.stop(PLAYER).status());

        final PaperReplayService wrongThread = service(repository(CompletableFuture.completedFuture(
                Optional.of(completedReplay()))), new WrongOwnerThread());
        assertThrows(IllegalStateException.class, () -> wrongThread.stop(PLAYER));
        inactive.start();
        assertThrows(IllegalStateException.class, inactive::start);
    }

    private static PaperReplayService service(final ReplaySessionRepository repository,
                                              final PaperReplayService.OwnerThread owner) {
        return new PaperReplayService(repository, new ReplayAccessPolicy(),
                new ReplayPlaybackEngine(), owner);
    }

    private static ReplaySession completedReplay() {
        return baseReplay().start().record(new ReplayEvent("event-0", 0L, 10L,
                Instant.parse("2026-07-26T00:00:10Z"), ReplayEvent.Source.GAME,
                "match.transition", Collections.singletonMap("state", "RUNNING"))).complete();
    }

    private static ReplaySession protectedReplay() {
        final ReplayMetadata metadata = metadata(true);
        return ReplaySession.create(metadata).start().complete();
    }

    private static ReplaySession baseReplay() { return ReplaySession.create(metadata(false)); }

    private static ReplayMetadata metadata(final boolean protectedEvidence) {
        return new ReplayMetadata(REPLAY, MatchId.parse("00000000-0000-0000-0000-000000000008"),
                Instant.parse("2026-07-26T00:00:00Z"), 1,
                Set.of(PlayerId.of(PLAYER)), protectedEvidence);
    }

    private static ReplaySessionRepository repository(
            final CompletionStage<Optional<ReplaySession>> result) {
        return repository(id -> result);
    }

    private static ReplaySessionRepository repository(final Loader loader) {
        return new ReplaySessionRepository() {
            @Override public CompletionStage<Boolean> create(final ReplaySession session) {
                throw new UnsupportedOperationException();
            }
            @Override public CompletionStage<SaveResult> save(final ReplaySession session,
                                                               final ReplayState expected) {
                throw new UnsupportedOperationException();
            }
            @Override public CompletionStage<Optional<ReplaySession>> findSession(final ReplayId id) {
                return loader.load(id);
            }
        };
    }

    private interface Loader {
        CompletionStage<Optional<ReplaySession>> load(ReplayId replayId);
    }

    private static final class ImmediateOwnerThread implements PaperReplayService.OwnerThread {
        @Override public void execute(final Runnable action) { action.run(); }
        @Override public boolean isOwnerThread() { return true; }
    }

    private static final class WrongOwnerThread implements PaperReplayService.OwnerThread {
        @Override public void execute(final Runnable action) { action.run(); }
        @Override public boolean isOwnerThread() { return false; }
    }

    private static final class FakeAudience implements ReplayAudience {
        private final boolean view;
        private final boolean staff;
        private final AtomicInteger entered = new AtomicInteger();
        private final AtomicInteger left = new AtomicInteger();
        private FakeAudience(final boolean view, final boolean staff) {
            this.view = view;
            this.staff = staff;
        }
        @Override public UUID playerId() { return PLAYER; }
        @Override public boolean hasPermission(final String permission) {
            return PaperReplayService.STAFF_PERMISSION.equals(permission) ? staff : view;
        }
        @Override public Object enterSpectatorReplay() {
            entered.incrementAndGet();
            return "restore";
        }
        @Override public void leaveSpectatorReplay(final Object restoration) { left.incrementAndGet(); }
    }
}
