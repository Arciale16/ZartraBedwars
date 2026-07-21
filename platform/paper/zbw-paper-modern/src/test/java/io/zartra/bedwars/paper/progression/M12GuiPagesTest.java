package io.zartra.bedwars.paper.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.ui.api.UiModel;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class M12GuiPagesTest {
    @Test
    void createsEveryM12PageWithAsyncLoadingAndAccessibleInteractions() throws Exception {
        final List<UiModel.PageDefinition> pages = M12GuiPages.create((action, viewer, query) ->
                CompletableFuture.completedFuture(new UiModel.PageState(9L,
                        UiModel.PageState.Status.EMPTY, query.page(), query.page() + 1,
                        Collections.emptyList(), MessageKey.of("m12.empty"))));
        assertEquals(17, pages.size());
        final UiModel.PageDefinition first = pages.get(0);
        final UiModel.PageState state = first.loader().load(
                PlayerId.of(new UUID(0L, 12L)), UiModel.Query.first(first.id()))
                .toCompletableFuture().get();
        assertEquals(UiModel.PageState.Status.EMPTY, state.status());
        assertTrue(first.interactions().contains(UiModel.Interaction.PRIMARY));
        assertTrue(first.interactions().contains(UiModel.Interaction.KEYBOARD));
        assertThrows(UnsupportedOperationException.class, () -> pages.clear());
    }

    @Test
    void rejectsMissingProvider() {
        assertThrows(NullPointerException.class, () -> M12GuiPages.create(null));
    }
}
