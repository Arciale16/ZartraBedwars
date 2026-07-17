package io.zartra.bedwars.paper.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.spectator.SpectatorFramework;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class M10PaperProjectionTest {
    @Test void everyPlatformMutationRequiresOwnerThread() {
        final AtomicInteger calls = new AtomicInteger();
        final M10PaperProjection.Platform platform = platform(calls);
        final M10PaperProjection denied = new M10PaperProjection(() -> false, platform);
        assertThrows(IllegalStateException.class, () -> denied.applySpectator(session()));
        assertThrows(IllegalStateException.class, () -> denied.restore(state()));
        assertThrows(IllegalStateException.class, () -> denied.clear(player()));
        assertEquals(0, calls.get());
        final M10PaperProjection allowed = new M10PaperProjection(() -> true, platform);
        allowed.applySpectator(session()); allowed.restore(state()); allowed.clear(player());
        assertEquals(4, calls.get());
    }

    @Test void matchingWorkRunsOffCallerAndExecutorDrains() throws Exception {
        final BoundedMatchmakingExecutor executor = new BoundedMatchmakingExecutor(1, 2, "zbw-m10-match");
        final String caller = Thread.currentThread().getName();
        final AtomicReference<String> worker = new AtomicReference<>();
        assertEquals("matched", executor.submit(() -> { worker.set(Thread.currentThread().getName()); return "matched"; },
                Duration.ofSeconds(2)).get(2, TimeUnit.SECONDS));
        assertTrue(worker.get().startsWith("zbw-m10-match-"));
        assertTrue(!caller.equals(worker.get()));
        assertTrue(executor.close(Duration.ofSeconds(2)));
        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> "late", Duration.ofSeconds(1)));
    }

    @Test void boundedExecutorRejectsOverflowAndTimesOut() throws Exception {
        final BoundedMatchmakingExecutor executor = new BoundedMatchmakingExecutor(1, 1, "zbw-m10-bound");
        final CountDownLatch hold = new CountDownLatch(1);
        final CompletableFuture<String> held = executor.submit(() -> { try { hold.await(); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); } return "held"; }, Duration.ofSeconds(5));
        final CompletableFuture<String> queued = executor.submit(() -> "queued", Duration.ofSeconds(5));
        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> "overflow", Duration.ofSeconds(5)));
        hold.countDown();
        assertEquals("held", held.get(2, TimeUnit.SECONDS));
        assertEquals("queued", queued.get(2, TimeUnit.SECONDS));
        executor.submit(() -> { try { Thread.sleep(100); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); } return "slow"; }, Duration.ofMillis(10));
        Thread.sleep(30);
        assertTrue(executor.timedOut() >= 1);
        executor.close(Duration.ofSeconds(2));
    }

    private static M10PaperProjection.Platform platform(final AtomicInteger calls) {
        return new M10PaperProjection.Platform() {
            @Override public void renderSelector(final PlayerId playerId,
                                                  final io.zartra.bedwars.game.selector.SelectorFramework.Page page) { calls.incrementAndGet(); }
            @Override public void applySpectator(final SpectatorFramework.Session session) { calls.incrementAndGet(); }
            @Override public void restore(final PlayerStateSnapshot capturedState) { calls.incrementAndGet(); }
            @Override public void clearOwnedState(final PlayerId playerId) { calls.incrementAndGet(); }
        };
    }
    private static SpectatorFramework.Session session() {
        final SpectatorFramework.Service service = new SpectatorFramework.Service(
                new SpectatorFramework.Policy(2, Duration.ofSeconds(30), true,
                        new SpectatorFramework.Restrictions(true, true, false,
                                Collections.<DefinitionId>emptySet())),
                TimeSource.FixedTimeSource.at(Instant.parse("2026-07-17T12:00:00Z")), event -> { });
        return service.enter(MatchId.of(new UUID(1, 1)), SpectatorFramework.EntryReason.EXTERNAL, state());
    }
    private static PlayerStateSnapshot state() {
        return new PlayerStateSnapshot(player(), PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(DefinitionId.of("zartra", "world/lobby"), 0, 70, 0, 0, 0),
                PlayerStateSnapshot.Mode.ADVENTURE, true);
    }
    private static PlayerId player() { return PlayerId.of(new UUID(0, 1)); }
}
