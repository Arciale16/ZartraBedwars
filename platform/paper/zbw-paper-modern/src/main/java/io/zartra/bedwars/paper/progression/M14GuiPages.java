package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.ui.api.UiModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** M14 catalogue, profile and campaign pages hosted by the existing M09 GUI framework. */
public final class M14GuiPages {
    private M14GuiPages() { }
    /** Creates asynchronous, query-aware pages; UiModel preserves stale-view and error states. */
    public static List<UiModel.PageDefinition> create(final ViewProvider provider) {
        Objects.requireNonNull(provider, "provider");
        final List<UiModel.PageDefinition> result = new ArrayList<>();
        for (PresentationActions.Definition action : PresentationActions.Catalog.m14()) {
            result.add(new UiModel.PageDefinition(action.pageId(), MessageKey.of("m14."
                    + action.id().toString().replace(':', '.').replace('/', '.') + ".title"),
                    (viewer, query) -> provider.load(action.id(), viewer, query),
                    Arrays.asList(UiModel.Interaction.PRIMARY, UiModel.Interaction.KEYBOARD)));
        }
        return Collections.unmodifiableList(result);
    }
    /** Off-thread query projection boundary; UI state exposes loading, empty, error and ready views. */
    public interface ViewProvider {
        /** Loads one bounded filtered/paginated page. */
        CompletionStage<UiModel.PageState> load(PresentationActions.ActionId action, PlayerId viewer,
                                                UiModel.Query query);
    }
}
