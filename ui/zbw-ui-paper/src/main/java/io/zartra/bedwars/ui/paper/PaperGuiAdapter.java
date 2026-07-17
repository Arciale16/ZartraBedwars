package io.zartra.bedwars.ui.paper;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.ui.api.UiFramework;
import io.zartra.bedwars.ui.api.UiModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Thin Paper inventory adapter. It translates inputs and renders neutral page state; validation,
 * navigation, authorization and business rules remain in neutral services.
 */
public final class PaperGuiAdapter implements Listener {
    private final UiFramework framework;
    private final LocalizationService localization;
    private final InventoryPort inventories;
    private final OwnerThread ownerThread;
    private final ActionSink actions;
    private final Map<Object, Binding> bindings = Collections.synchronizedMap(new IdentityHashMap<Object, Binding>());

    /** Creates an adapter around explicit Paper and use-case boundaries. */
    public PaperGuiAdapter(final UiFramework framework, final LocalizationService localization,
                           final InventoryPort inventories, final OwnerThread ownerThread,
                           final ActionSink actions) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.localization = Objects.requireNonNull(localization, "localization");
        this.inventories = Objects.requireNonNull(inventories, "inventories");
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    /** Opens loading state immediately and applies the newest asynchronous result on owner thread. */
    public UiModel.SessionId open(final Object paperViewer, final PlayerId viewer,
                                  final UiModel.Query query) {
        Objects.requireNonNull(paperViewer, "paperViewer");
        final UiFramework.LoadHandle load = framework.open(viewer, query);
        final Object inventory = inventories.create(54, localize(
                framework.pageDefinition(query.pageId()).title(), Optional.of(viewer)));
        bindings.put(inventory, new Binding(load.sessionId(), viewer, paperViewer));
        inventories.open(paperViewer, inventory);
        render(inventory, loadingItem());
        observe(inventory, load);
        return load.sessionId();
    }

    /** Refreshes an open session without closing its inventory. */
    public void refresh(final UiModel.SessionId id) {
        final Optional<Map.Entry<Object, Binding>> found = binding(id);
        if (!found.isPresent()) { return; }
        observe(found.get().getKey(), framework.refresh(id));
    }

    private void observe(final Object inventory, final UiFramework.LoadHandle load) {
        load.stage().whenComplete((state, failure) -> ownerThread.execute(() -> {
            final UiModel.PageState resolved = failure == null && state != null ? state
                    : new UiModel.PageState(0L, UiModel.PageState.Status.ERROR, 0, 1,
                    Collections.<UiModel.Component>emptyList(),
                    io.zartra.bedwars.api.localization.MessageKey.of("ui.load.failed"));
            if (framework.accept(load.sessionId(), load.sequence(), resolved)
                    == UiFramework.LoadVerdict.ACCEPTED && bindings.containsKey(inventory)) {
                render(inventory, resolved);
            }
        }));
    }

    private void render(final Object inventory, final UiModel.PageState state) {
        inventories.clear(inventory);
        for (UiModel.Component component : state.components()) {
            final Binding binding = bindings.get(inventory);
            if (binding == null) { return; }
            final List<String> lore = new ArrayList<String>();
            for (io.zartra.bedwars.api.localization.MessageKey line : component.lore()) {
                lore.add(localize(line, Optional.of(binding.viewer)));
            }
            lore.add(localize(component.accessibility().nonColorCue(), Optional.of(binding.viewer)));
            inventories.set(inventory, component.slot(), new RenderedItem(component.enabled()
                    ? "PAPER" : "BARRIER", localize(component.label(), Optional.of(binding.viewer)), lore));
        }
        if (state.components().isEmpty()) {
            inventories.set(inventory, 22, new RenderedItem(state.status() == UiModel.PageState.Status.ERROR
                    ? "BARRIER" : "PAPER", localize(state.message(), Optional.<PlayerId>empty()),
                    Collections.<String>emptyList()));
        }
    }

    private UiModel.PageState loadingItem() {
        return new UiModel.PageState(0L, UiModel.PageState.Status.LOADING, 0, 1,
                Collections.<UiModel.Component>emptyList(),
                io.zartra.bedwars.api.localization.MessageKey.of("ui.loading"));
    }

    @EventHandler public void onClick(final InventoryClickEvent event) {
        final Object inventory = inventories.inventory(event);
        final Binding binding = bindings.get(inventory);
        if (binding == null) { return; }
        inventories.cancel(event);
        final int slot = inventories.rawSlot(event);
        final Optional<UiFramework.SessionSnapshot> snapshot = framework.snapshot(binding.session);
        if (!snapshot.isPresent()) { return; }
        UiModel.Component selected = null;
        for (UiModel.Component component : snapshot.get().state().components()) {
            if (component.slot() == slot) { selected = component;
             break;
            }
        }
        if (selected == null) { return; }
        final UiModel.Click click = new UiModel.Click(binding.session, selected.id(),
                snapshot.get().state().revision(), UUID.randomUUID(), Instant.now());
        final UiFramework.ClickResult accepted = framework.click(binding.viewer, click);
        if (accepted.verdict() != UiFramework.ClickVerdict.ACCEPTED) { return; }
        final CompletionStage<Boolean> result = actions.execute(binding.viewer,
                accepted.component().orElseThrow(), snapshot.get().state().revision());
        Objects.requireNonNull(result, "action result").whenComplete((refresh, failure) -> {
            if (failure == null && Boolean.TRUE.equals(refresh)) { ownerThread.execute(() -> refresh(binding.session)); }
        });
    }

    @EventHandler public void onDrag(final InventoryDragEvent event) {
        if (bindings.containsKey(inventories.inventory(event))) { inventories.cancel(event); }
    }

    @EventHandler public void onClose(final InventoryCloseEvent event) {
        final Binding binding = bindings.remove(inventories.inventory(event));
        if (binding != null) { framework.close(binding.session); }
    }

    /** Removes all live inventory bindings during plugin shutdown. */
    public void closeAll() {
        final List<Binding> copy;
        synchronized (bindings) { copy = new ArrayList<Binding>(bindings.values());
         bindings.clear();
        }
        for (Binding binding : copy) { framework.close(binding.session);
         inventories.close(binding.paperViewer);
        }
    }

    private Optional<Map.Entry<Object, Binding>> binding(final UiModel.SessionId id) {
        synchronized (bindings) {
            for (Map.Entry<Object, Binding> entry : bindings.entrySet()) {
                if (entry.getValue().session.equals(id)) { return Optional.of(entry); }
            }
        }
        return Optional.empty();
    }

    private String localize(final io.zartra.bedwars.api.localization.MessageKey key,
                            final Optional<PlayerId> player) {
        final io.zartra.bedwars.api.result.Result<LocalizationService.LocalizedMessage> value =
                localization.render(key, player, LocalizationService.Parameters.empty());
        return value.isSuccess() ? value.requireValue().text() : key.value();
    }

    /** Platform inventory operations, supplied by the exact-version adapter. */
    public interface InventoryPort {
        /** @return new closed inventory */ Object create(int size, String title);
        /** Clears all slots. */ void clear(Object inventory);
        /** Sets one rendered item. */ void set(Object inventory, int slot, RenderedItem item);
        /** Opens an inventory for a viewer. */ void open(Object viewer, Object inventory);
        /** Closes the viewer inventory. */ void close(Object viewer);
        /** @return inventory from a Paper event */ Object inventory(Object event);
        /** @return clicked raw slot */ int rawSlot(Object event);
        /** Cancels a Paper event. */ void cancel(Object event);
    }
    /** Owner-thread scheduler boundary. */ public interface OwnerThread { /** Executes a Paper mutation on its owner thread. */ void execute(Runnable mutation); }
    /** Shared action invocation boundary; implementations use the same registry as commands. */
    public interface ActionSink { /** @return whether the page should refresh */ CompletionStage<Boolean> execute(PlayerId viewer, UiModel.Component component, long viewRevision); }

    /** Immutable version-neutral item rendering instruction. */
    public static final class RenderedItem {
        private final String material;
         private final String name;
        private final List<String> lore;
        /** Creates one rendered item. */ public RenderedItem(final String material, final String name, final List<String> lore) { if (material == null || !material.matches("[A-Z][A-Z0-9_]{1,63}")) { throw new IllegalArgumentException("invalid material");
         } this.material = material;
         this.name = Objects.requireNonNull(name, "name");
         this.lore = Collections.unmodifiableList(new ArrayList<String>(Objects.requireNonNull(lore, "lore")));
        }
        /** @return safe material name */ public String material() { return material; }
        /** @return plain name */ public String name() { return name; }
        /** @return plain lore */ public List<String> lore() { return lore; }
    }

    private static final class Binding {
        private final UiModel.SessionId session;
         private final PlayerId viewer;
        private final Object paperViewer;
        private Binding(final UiModel.SessionId session, final PlayerId viewer, final Object paperViewer) { this.session = session;
         this.viewer = viewer;
         this.paperViewer = paperViewer;
        }
    }
}
