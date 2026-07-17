package io.zartra.bedwars.ui.api;

import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.command.api.PresentationActions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Generates the complete paginated admin dashboard and action pages from the shared catalogue. */
public final class AdminDashboard {
    /** Stable dashboard page ID. */ public static final GuiPageId PAGE_ID = GuiPageId.of("zartra", "m09/admin/dashboard");
    private static final int PAGE_SIZE = 45;
    private AdminDashboard() { throw new AssertionError("No instances"); }

    /**
     * @return dashboard plus one feature page for every action; duplicate page IDs fail closed
     */
    public static List<UiModel.PageDefinition> pages(
            final Collection<PresentationActions.Definition> catalogue,
            final ActionPageFactory actionPages) {
        final List<PresentationActions.Definition> definitions = Collections.unmodifiableList(
                new ArrayList<PresentationActions.Definition>(Objects.requireNonNull(catalogue, "catalogue")));
        final List<UiModel.PageDefinition> pages = new ArrayList<UiModel.PageDefinition>();
        pages.add(new UiModel.PageDefinition(PAGE_ID, MessageKey.of("ui.admin.dashboard.title"),
                (viewer, query) -> CompletableFuture.completedFuture(dashboard(definitions, query)),
                Arrays.asList(UiModel.Interaction.PRIMARY, UiModel.Interaction.KEYBOARD)));
        final Map<GuiPageId, PresentationActions.Definition> unique = new LinkedHashMap<GuiPageId, PresentationActions.Definition>();
        for (PresentationActions.Definition definition : definitions) {
            if (unique.put(definition.pageId(), definition) != null) { throw new IllegalArgumentException("duplicate action page"); }
            pages.add(Objects.requireNonNull(actionPages, "actionPages").create(definition));
        }
        return Collections.unmodifiableList(pages);
    }

    private static UiModel.PageState dashboard(final List<PresentationActions.Definition> definitions,
                                               final UiModel.Query query) {
        final List<PresentationActions.Definition> filtered = new ArrayList<PresentationActions.Definition>();
        final String search = query.search().toLowerCase(Locale.ROOT);
        for (PresentationActions.Definition definition : definitions) {
            final boolean searchMatch = search.isEmpty() || definition.commandPath().contains(search)
                    || definition.id().toString().contains(search);
            final boolean filterMatch = !query.filter().isPresent()
                    || definition.id().value().path().startsWith("action/" + query.filter().get().path());
            if (searchMatch && filterMatch) { filtered.add(definition); }
        }
        if (query.sort() != UiModel.Query.Sort.NATURAL) {
            filtered.sort(Comparator.comparing(PresentationActions.Definition::commandPath));
            if (query.sort() == UiModel.Query.Sort.LABEL_DESCENDING) { Collections.reverse(filtered); }
        }
        final int pageCount = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        final int page = Math.min(query.page(), pageCount - 1);
        final int first = page * PAGE_SIZE;
        final int last = Math.min(filtered.size(), first + PAGE_SIZE);
        final List<UiModel.Component> components = new ArrayList<UiModel.Component>();
        for (int index = first; index < last; index++) {
            final PresentationActions.Definition definition = filtered.get(index);
            final String key = definition.id().value().path().substring("action/".length()).replace('/', '.').replace('-', '.');
            final UiModel.Accessibility accessibility = new UiModel.Accessibility(
                    MessageKey.of("ui.action." + key + ".label"),
                    MessageKey.of("ui.action." + key + ".cue"),
                    Arrays.asList(UiModel.Accessibility.InputAlternative.COMMAND,
                            UiModel.Accessibility.InputAlternative.KEYBOARD,
                            UiModel.Accessibility.InputAlternative.BEDROCK));
            components.add(new UiModel.Component(UiModel.ComponentId.of("dashboard/" + index),
                    index - first, definition.id(), MessageKey.of("ui.action." + key + ".label"),
                    Collections.singletonList(MessageKey.of("ui.action." + key + ".help")),
                    true, accessibility));
        }
        return new UiModel.PageState(1L, components.isEmpty() ? UiModel.PageState.Status.EMPTY
                : UiModel.PageState.Status.READY, page, pageCount, components,
                MessageKey.of(components.isEmpty() ? "ui.dashboard.empty" : "ui.dashboard.ready"));
    }

    /** Creates a feature-specific page that delegates its data to the owning application adapter. */
    public interface ActionPageFactory {
        /** @return complete immutable page definition for one action */
        UiModel.PageDefinition create(PresentationActions.Definition definition);
    }
}
