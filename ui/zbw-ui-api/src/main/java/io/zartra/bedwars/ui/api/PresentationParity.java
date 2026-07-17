package io.zartra.bedwars.ui.api;

import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Deterministic validator proving action, executable command and GUI-page parity. */
public final class PresentationParity {
    private PresentationParity() { throw new AssertionError("No instances"); }
    /** @return immutable validation report */
    public static Report validate(final Collection<PresentationActions.Definition> catalogue,
                                  final Collection<CommandFramework.InventoryEntry> commands,
                                  final Collection<UiModel.PageDefinition> pages) {
        final Set<String> commandIds = new LinkedHashSet<String>();
        for (CommandFramework.InventoryEntry command : Objects.requireNonNull(commands, "commands")) {
            if (command.executable()) { commandIds.add(command.id().toString()); }
        }
        final Set<GuiPageId> pageIds = new LinkedHashSet<GuiPageId>();
        for (UiModel.PageDefinition page : Objects.requireNonNull(pages, "pages")) { pageIds.add(page.id()); }
        final Set<PresentationActions.ActionId> missingCommands = new LinkedHashSet<PresentationActions.ActionId>();
        final Set<PresentationActions.ActionId> missingPages = new LinkedHashSet<PresentationActions.ActionId>();
        for (PresentationActions.Definition definition : Objects.requireNonNull(catalogue, "catalogue")) {
            if (!commandIds.contains(definition.id().toString())) { missingCommands.add(definition.id()); }
            if (!pageIds.contains(definition.pageId())) { missingPages.add(definition.id()); }
        }
        return new Report(missingCommands, missingPages);
    }

    /** Immutable parity report. */
    public static final class Report {
        private final Set<PresentationActions.ActionId> missingCommands;
        private final Set<PresentationActions.ActionId> missingPages;
        private Report(final Set<PresentationActions.ActionId> missingCommands,
                       final Set<PresentationActions.ActionId> missingPages) {
            this.missingCommands = java.util.Collections.unmodifiableSet(missingCommands);
            this.missingPages = java.util.Collections.unmodifiableSet(missingPages);
        }
        /** @return whether every catalogue action is represented */ public boolean valid() { return missingCommands.isEmpty() && missingPages.isEmpty(); }
        /** @return actions without executable commands */ public Set<PresentationActions.ActionId> missingCommands() { return missingCommands; }
        /** @return actions without GUI pages */ public Set<PresentationActions.ActionId> missingPages() { return missingPages; }
    }
}
