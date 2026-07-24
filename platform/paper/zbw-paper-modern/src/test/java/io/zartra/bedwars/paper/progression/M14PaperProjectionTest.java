package io.zartra.bedwars.paper.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M14PaperProjectionTest {
    @Test
    void requiresTheOwnerThreadBeforeCallingThePlatform() {
        final AtomicInteger calls = new AtomicInteger();
        final M14PaperProjection projection = new M14PaperProjection(() -> false, platform(calls), budget());
        assertThrows(IllegalStateException.class, () -> projection.feedback(null));
        assertThrows(IllegalStateException.class, () -> projection.open(null, null));
        assertThrows(IllegalStateException.class, () -> projection.clear(null));
        assertEquals(0, calls.get());
    }

    @Test
    void forwardsBoundedFeedbackAndLifecycleOperations() {
        final AtomicInteger calls = new AtomicInteger();
        final M14PaperProjection projection = new M14PaperProjection(() -> true, platform(calls), budget());
        final M14PaperProjection.Feedback feedback = feedback("cosmetic.equipped", "ENTITY_PLAYER_LEVELUP",
                "HAPPY_VILLAGER", 2, 1);
        projection.feedback(feedback);
        projection.open(player(), new Object());
        projection.clear(player());
        assertEquals(3, calls.get());
        assertEquals(M14PaperProjection.Kind.COSMETIC, feedback.kind());
    }

    @Test
    void validatesCollaboratorsBudgetsAndFeedbackIdentifiers() {
        assertThrows(NullPointerException.class, () -> new M14PaperProjection(null, null, null));
        assertThrows(NullPointerException.class, () -> new M14PaperProjection(() -> true, null, budget()));
        assertThrows(NullPointerException.class, () -> new M14PaperProjection(() -> true, platform(new AtomicInteger()), null));
        assertThrows(IllegalArgumentException.class, () -> new M14PaperProjection.Budget(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new M14PaperProjection.Budget(0, -1));
        final M14PaperProjection projection = new M14PaperProjection(() -> true, platform(new AtomicInteger()), budget());
        assertThrows(NullPointerException.class, () -> projection.feedback(null));
        assertThrows(NullPointerException.class, () -> projection.open(null, new Object()));
        assertThrows(NullPointerException.class, () -> projection.open(player(), null));
        assertThrows(NullPointerException.class, () -> projection.clear(null));
        assertThrows(IllegalArgumentException.class, () -> projection.feedback(feedback("BAD KEY", null, null, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", "bad", null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", null, "bad", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", null, null, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> feedback("valid.key", null, null, 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> projection.feedback(feedback("valid.key", null, null, 3, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> projection.feedback(feedback("valid.key", null, null, 0, 2)));
    }

    @Test
    void exposesAllM14FeedbackKinds() {
        assertEquals(3, M14PaperProjection.Kind.values().length);
    }

    private static M14PaperProjection.Budget budget() {
        return new M14PaperProjection.Budget(2, 1);
    }

    private static M14PaperProjection.Feedback feedback(final String messageKey, final String soundKey,
                                                         final String particleKey, final int particles,
                                                         final int entities) {
        return new M14PaperProjection.Feedback(player(), M14PaperProjection.Kind.COSMETIC, messageKey,
                soundKey, particleKey, particles, entities);
    }

    private static M14PaperProjection.Platform platform(final AtomicInteger calls) {
        return new M14PaperProjection.Platform() {
            @Override public void feedback(final M14PaperProjection.Feedback feedback) { calls.incrementAndGet(); }
            @Override public void open(final PlayerId player, final Object inventory) { calls.incrementAndGet(); }
            @Override public void clear(final PlayerId player) { calls.incrementAndGet(); }
        };
    }

    private static PlayerId player() { return PlayerId.of(new UUID(0L, 140L)); }
}
