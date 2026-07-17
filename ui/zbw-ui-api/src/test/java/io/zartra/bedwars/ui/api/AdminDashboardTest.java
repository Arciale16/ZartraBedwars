package io.zartra.bedwars.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AdminDashboardTest {
    @Test void dashboardGeneratesEveryActionPageAndPaginatesSearchesAndSorts() {
        List<PresentationActions.Definition> catalog = PresentationActions.Catalog.standard();
        List<UiModel.PageDefinition> pages = AdminDashboard.pages(catalog, definition -> page(definition.pageId()));
        assertEquals(catalog.size() + 1, pages.size());
        UiModel.PageDefinition dashboard = pages.get(0);
        PlayerId viewer = PlayerId.of(UUID.randomUUID());
        UiModel.PageState first = dashboard.loader().load(viewer, UiModel.Query.first(AdminDashboard.PAGE_ID)).toCompletableFuture().join();
        assertEquals(45, first.components().size());
        assertTrue(first.pageCount() >= 2);
        UiModel.PageState search = dashboard.loader().load(viewer, new UiModel.Query(AdminDashboard.PAGE_ID, 0, "deposit", null, UiModel.Query.Sort.LABEL_DESCENDING)).toCompletableFuture().join();
        assertEquals(5, search.components().size());
        assertTrue(search.components().stream().allMatch(component -> component.accessibility().alternatives().contains(UiModel.Accessibility.InputAlternative.BEDROCK)));
        UiModel.PageState empty = dashboard.loader().load(viewer, new UiModel.Query(AdminDashboard.PAGE_ID, 0, "nothing-matches", null, UiModel.Query.Sort.NATURAL)).toCompletableFuture().join();
        assertEquals(UiModel.PageState.Status.EMPTY, empty.status());
        assertFalse(PresentationParity.validate(catalog, Collections.emptyList(), pages).valid());
    }
    private static UiModel.PageDefinition page(GuiPageId id) { return new UiModel.PageDefinition(id, MessageKey.of("ui.action.title"), (viewer, query) -> CompletableFuture.completedFuture(new UiModel.PageState(1, UiModel.PageState.Status.EMPTY, 0, 1, Collections.<UiModel.Component>emptyList(), MessageKey.of("ui.action.empty"))), Arrays.asList(UiModel.Interaction.PRIMARY)); }
}
