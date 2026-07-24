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

/** M13 player and administration pages hosted by the bounded M09 GUI session framework. */
public final class M13GuiPages {
    private M13GuiPages() { }

    /** Creates pages with asynchronous loading, paging, filtering and stale-revision protection. */
    public static List<UiModel.PageDefinition> create(final ViewProvider provider) {
        Objects.requireNonNull(provider, "provider");
        final List<UiModel.PageDefinition> result = new ArrayList<UiModel.PageDefinition>();
        for (PresentationActions.Definition action : PresentationActions.Catalog.m13()) {
            result.add(new UiModel.PageDefinition(action.pageId(),
                    MessageKey.of("m13." + action.id().toString().replace(':', '.').replace('/', '.') + ".title"),
                    (viewer, query) -> provider.load(action.id(), viewer, query),
                    Arrays.asList(UiModel.Interaction.PRIMARY, UiModel.Interaction.KEYBOARD)));
        }
        return Collections.unmodifiableList(result);
    }

    /** Off-thread view projection. PageState expresses loading, empty, error and ready states. */
    public interface ViewProvider {
        /** Loads one bounded page using the query page, filter and search fields. */
        CompletionStage<UiModel.PageState> load(PresentationActions.ActionId action,
                                                PlayerId viewer, UiModel.Query query);
    }
}
