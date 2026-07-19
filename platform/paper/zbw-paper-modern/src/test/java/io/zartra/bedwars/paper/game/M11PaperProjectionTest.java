package io.zartra.bedwars.paper.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.shop.generator.GeneratorBatch;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import io.zartra.bedwars.shop.upgrade.TeamEffectIntent;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M11PaperProjectionTest {
    @Test
    void constructorsRejectMissingCollaborators() {
        assertThrows(NullPointerException.class, () -> new M11PaperProjection(null, null));
        assertThrows(NullPointerException.class, () -> new M11PaperProjection(() -> true, null));
    }
    @Test
    void allM11PlatformMutationsAreOwnerThreadGuarded() {
        final AtomicInteger calls = new AtomicInteger();
        final M11PaperProjection.Platform platform = new M11PaperProjection.Platform() {
            @Override public void deliver(final GeneratorBatch batch) { calls.incrementAndGet(); }
            @Override public void apply(final TeamEffectIntent intent) { calls.incrementAndGet(); }
            @Override public boolean apply(final DefinitionId effect,
                                           final UtilityItemDefinition definition,
                                           final ItemActionRequest request) {
                calls.incrementAndGet();
                return true;
            }
            @Override public void clear(final PlayerId playerId) { calls.incrementAndGet(); }
            @Override public void clearMatch(final MatchId matchId) { calls.incrementAndGet(); }
        };
        final M11PaperProjection denied = new M11PaperProjection(() -> false, platform);
        assertThrows(IllegalStateException.class, () -> denied.clear(player()));
        assertEquals(0, calls.get());
        final M11PaperProjection allowed = new M11PaperProjection(() -> true, platform);
        allowed.clear(player());
        allowed.clearMatch(MatchId.of(new UUID(0L, 12L)));
        assertEquals(2, calls.get());
        assertTrue(allowed != denied);
    }

    @Test
    void everyMutationRejectsOffOwnerThreadBeforeItsArgument() {
        final AtomicInteger calls = new AtomicInteger();
        final M11PaperProjection denied = new M11PaperProjection(() -> false,
                new CountingPlatform(calls));
        assertThrows(IllegalStateException.class, () -> denied.deliver(null));
        assertThrows(IllegalStateException.class, () -> denied.apply((TeamEffectIntent) null));
        assertThrows(IllegalStateException.class, () -> denied.apply(null, null, null));
        assertThrows(IllegalStateException.class, () -> denied.clear(null));
        assertThrows(IllegalStateException.class, () -> denied.clearMatch(null));
        assertEquals(0, calls.get());
    }

    @Test
    void everyMutationValidatesArgumentsOnOwnerThread() {
        final M11PaperProjection projection = new M11PaperProjection(() -> true,
                new CountingPlatform(new AtomicInteger()));
        assertThrows(NullPointerException.class, () -> projection.deliver(null));
        assertThrows(NullPointerException.class, () -> projection.apply((TeamEffectIntent) null));
        assertThrows(NullPointerException.class, () -> projection.apply(null, null, null));
        assertThrows(NullPointerException.class, () -> projection.clear(null));
        assertThrows(NullPointerException.class, () -> projection.clearMatch(null));
    }

    @Test
    void forwardsEveryValidIntentOnOwnerThread() {
        final AtomicInteger calls = new AtomicInteger();
        final M11PaperProjection projection = new M11PaperProjection(() -> true,
                new CountingPlatform(calls));
        projection.deliver(BukkitM11PlatformTest.batch("projection", 1));
        projection.apply(BukkitM11PlatformTest.effect("projection-effect", null));
        assertTrue(!projection.apply(DefinitionId.of("test", "effect/projection"),
                BukkitM11PlatformTest.definition(),
                BukkitM11PlatformTest.request("projection-utility")));
        assertEquals(3, calls.get());
    }

    private static final class CountingPlatform implements M11PaperProjection.Platform {
        private final AtomicInteger calls;
        private CountingPlatform(final AtomicInteger calls) { this.calls = calls; }
        @Override public void deliver(final GeneratorBatch batch) { calls.incrementAndGet(); }
        @Override public void apply(final TeamEffectIntent intent) { calls.incrementAndGet(); }
        @Override public boolean apply(final io.zartra.bedwars.api.identity.DefinitionId effect,
                                       final UtilityItemDefinition definition,
                                       final ItemActionRequest request) {
            calls.incrementAndGet();
            return false;
        }
        @Override public void clear(final PlayerId playerId) { calls.incrementAndGet(); }
        @Override public void clearMatch(final MatchId matchId) { calls.incrementAndGet(); }
    }
    private static PlayerId player() { return PlayerId.of(new UUID(0L, 11L)); }
}
