package io.zartra.bedwars.paper.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
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
        };
        final M11PaperProjection denied = new M11PaperProjection(() -> false, platform);
        assertThrows(IllegalStateException.class, () -> denied.clear(player()));
        assertEquals(0, calls.get());
        final M11PaperProjection allowed = new M11PaperProjection(() -> true, platform);
        allowed.clear(player());
        assertEquals(1, calls.get());
        assertTrue(allowed != denied);
    }
    private static PlayerId player() { return PlayerId.of(new UUID(0L, 11L)); }
}
