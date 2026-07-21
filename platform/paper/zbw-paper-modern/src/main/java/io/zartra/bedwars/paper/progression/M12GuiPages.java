package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.api.identity.GuiPageId;
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

/** Builds M12 pages for the existing bounded M09 GUI framework. */
public final class M12GuiPages {
    private M12GuiPages() { }

    /** Creates all player and administration page definitions. */
    public static List<UiModel.PageDefinition> create(final ViewProvider provider) {
        Objects.requireNonNull(provider, "provider");
        final List<UiModel.PageDefinition> pages = new ArrayList<UiModel.PageDefinition>();
        for (PresentationActions.Definition action : PresentationActions.Catalog.m12()) {
            pages.add(page(action, provider));
        }
        return Collections.unmodifiableList(pages);
    }

    private static UiModel.PageDefinition page(final PresentationActions.Definition action,
                                                final ViewProvider provider) {
        return new UiModel.PageDefinition(action.pageId(),
                MessageKey.of("m12." + path(action.pageId()) + ".title"),
                (viewer, query) -> provider.load(action.id(), viewer, query),
                Arrays.asList(UiModel.Interaction.PRIMARY, UiModel.Interaction.KEYBOARD));
    }

    private static String path(final GuiPageId id) {
        return id.toString().replace(':', '.').replace('/', '.');
    }

    /** Async, bounded view projection implemented by the M12 composition root. */
    public interface ViewProvider {
        /** Loads immutable page state off the Minecraft owner thread. */
        CompletionStage<UiModel.PageState> load(PresentationActions.ActionId action,
                                                PlayerId viewer, UiModel.Query query);
    }
}
