package io.zartra.bedwars.paper.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M12PaperProjectionTest {
    @Test
    void everyMutationRequiresOwnerThreadBeforeTouchingPlatform() {
        final AtomicInteger calls = new AtomicInteger();
        final M12PaperProjection projection = new M12PaperProjection(() -> false,
                platform(calls));
        assertThrows(IllegalStateException.class, () -> projection.feedback(null));
        assertThrows(IllegalStateException.class, () -> projection.open(null, null));
        assertThrows(IllegalStateException.class, () -> projection.clear(null));
        assertEquals(0, calls.get());
    }

    @Test
    void forwardsOnlyValidatedIntentsOnOwnerThread() {
        final AtomicInteger calls = new AtomicInteger();
        final M12PaperProjection projection = new M12PaperProjection(() -> true,
                platform(calls));
        final PlayerId player = player();
        final M12PaperProjection.Feedback feedback = new M12PaperProjection.Feedback(player,
                M12PaperProjection.Kind.XP_BAR, "progression.xp.awarded", null, 0.75F);
        projection.feedback(feedback);
        projection.open(player, new Object());
        projection.clear(player);
        assertEquals(3, calls.get());
        assertEquals(player, feedback.player());
        assertEquals(M12PaperProjection.Kind.XP_BAR, feedback.kind());
        assertEquals("progression.xp.awarded", feedback.messageKey());
        assertNull(feedback.soundKey());
        assertEquals(0.75F, feedback.progress());
    }

    @Test
    void validatesCollaboratorsArgumentsAndFeedbackBounds() {
        assertThrows(NullPointerException.class, () -> new M12PaperProjection(null, null));
        assertThrows(NullPointerException.class, () -> new M12PaperProjection(() -> true, null));
        final M12PaperProjection projection = new M12PaperProjection(() -> true,
                platform(new AtomicInteger()));
        assertThrows(NullPointerException.class, () -> projection.feedback(null));
        assertThrows(NullPointerException.class, () -> projection.open(null, new Object()));
        assertThrows(NullPointerException.class, () -> projection.open(player(), null));
        assertThrows(NullPointerException.class, () -> projection.clear(null));
        assertThrows(IllegalArgumentException.class, () -> feedback("BAD KEY", null, 0F));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", "bad", 0F));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", null, -1F));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", null, 2F));
    }

    @Test
    void exposesAllFeedbackKinds() {
        assertEquals(7, M12PaperProjection.Kind.values().length);
    }

    private static M12PaperProjection.Feedback feedback(final String message, final String sound,
                                                         final float progress) {
        return new M12PaperProjection.Feedback(player(), M12PaperProjection.Kind.MESSAGE,
                message, sound, progress);
    }

    private static M12PaperProjection.Platform platform(final AtomicInteger calls) {
        return new M12PaperProjection.Platform() {
            @Override public void feedback(final M12PaperProjection.Feedback feedback) {
                calls.incrementAndGet();
            }
            @Override public void open(final PlayerId player, final Object inventoryView) {
                calls.incrementAndGet();
            }
            @Override public void clear(final PlayerId player) { calls.incrementAndGet(); }
        };
    }

    private static PlayerId player() { return PlayerId.of(new UUID(0L, 120L)); }
}
