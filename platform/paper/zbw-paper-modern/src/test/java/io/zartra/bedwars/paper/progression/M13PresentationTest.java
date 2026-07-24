package io.zartra.bedwars.paper.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.ui.api.UiModel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M13PresentationTest {
    @Test void bindsEveryM13ActionToExistingFramework() {
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings =
                M13PresentationBindings.create((action, request) ->
                        CompletableFuture.completedFuture(PresentationActions.Response.simple(
                                PresentationActions.Response.Status.SUCCESS, "m13.success", 0)));
        assertEquals(17, bindings.size());
        assertThrows(UnsupportedOperationException.class, bindings::clear);
        assertThrows(NullPointerException.class, () -> M13PresentationBindings.create(null));
    }

    @Test void pagesExposeAsyncEmptyAndAccessibleStates() throws Exception {
        final List<UiModel.PageDefinition> pages = M13GuiPages.create((action, viewer, query) ->
                CompletableFuture.completedFuture(new UiModel.PageState(4L,
                        UiModel.PageState.Status.EMPTY, query.page(), query.page() + 1,
                        Collections.emptyList(), MessageKey.of("m13.empty"))));
        assertEquals(17, pages.size());
        final UiModel.PageDefinition page = pages.get(0);
        assertEquals(UiModel.PageState.Status.EMPTY, page.loader().load(player(),
                UiModel.Query.first(page.id())).toCompletableFuture().get().status());
        assertTrue(page.interactions().contains(UiModel.Interaction.KEYBOARD));
        assertThrows(UnsupportedOperationException.class, pages::clear);
        assertThrows(NullPointerException.class, () -> M13GuiPages.create(null));
    }

    @Test void paperFeedbackIsOwnerThreadOnlyAndPolicyFree() {
        final AtomicInteger calls = new AtomicInteger();
        final M13PaperProjection.Platform platform = new M13PaperProjection.Platform() {
            @Override public void feedback(final M13PaperProjection.Feedback feedback) { calls.incrementAndGet(); }
            @Override public void open(final PlayerId player, final Object inventory) { calls.incrementAndGet(); }
            @Override public void clear(final PlayerId player) { calls.incrementAndGet(); }
        };
        final M13PaperProjection denied = new M13PaperProjection(() -> false, platform);
        assertThrows(IllegalStateException.class, () -> denied.clear(player()));
        assertEquals(0, calls.get());
        final M13PaperProjection projection = new M13PaperProjection(() -> true, platform);
        final M13PaperProjection.Feedback feedback = new M13PaperProjection.Feedback(player(),
                M13PaperProjection.Kind.ACHIEVEMENT, "achievement.unlocked",
                "UI_TOAST_CHALLENGE_COMPLETE", "FIREWORK", 1F);
        projection.feedback(feedback);
        projection.open(player(), new Object());
        projection.clear(player());
        assertEquals(3, calls.get());
        assertEquals("FIREWORK", feedback.particleKey());
        assertEquals(6, M13PaperProjection.Kind.values().length);
        assertThrows(IllegalArgumentException.class, () -> new M13PaperProjection.Feedback(player(),
                M13PaperProjection.Kind.QUEST, "bad key", null, null, 0F));
        assertThrows(IllegalArgumentException.class, () -> new M13PaperProjection.Feedback(player(),
                M13PaperProjection.Kind.QUEST, "quest.done", "bad", null, 0F));
        assertThrows(IllegalArgumentException.class, () -> new M13PaperProjection.Feedback(player(),
                M13PaperProjection.Kind.QUEST, "quest.done", null, "bad", 0F));
        assertThrows(IllegalArgumentException.class, () -> new M13PaperProjection.Feedback(player(),
                M13PaperProjection.Kind.QUEST, "quest.done", null, null, 2F));
        assertNull(new M13PaperProjection.Feedback(player(), M13PaperProjection.Kind.OBJECTIVE,
                "objective.done", null, null, 0F).soundKey());
    }

    private static PlayerId player() { return PlayerId.of(new UUID(0L, 13L)); }
}
