package io.zartra.bedwars.ui.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.ui.api.UiFramework;
import io.zartra.bedwars.ui.api.UiModel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaperGuiAdapterTest {
    @Test void opensLoadingRendersNewestStateRefreshesAndCloses() {
        PlayerId viewer = PlayerId.of(UUID.randomUUID());
        GuiPageId pageId = GuiPageId.of("zartra", "test/page");
        UiModel.Component component = new UiModel.Component(UiModel.ComponentId.of("test/button"), 3, PresentationActions.ActionId.of("test/action"), MessageKey.of("ui.test.label"), Collections.singletonList(MessageKey.of("ui.test.help")), true, new UiModel.Accessibility(MessageKey.of("ui.test.semantic"), MessageKey.of("ui.test.cue"), Arrays.asList(UiModel.Accessibility.InputAlternative.COMMAND)));
        UiModel.PageState state = new UiModel.PageState(2, UiModel.PageState.Status.READY, 0, 1, Collections.singletonList(component), MessageKey.of("ui.ready"));
        UiModel.PageDefinition page = new UiModel.PageDefinition(pageId, MessageKey.of("ui.test.title"), (id, query) -> CompletableFuture.completedFuture(state), Collections.singletonList(UiModel.Interaction.PRIMARY));
        UiFramework framework = new UiFramework(new UiFramework.Registry(2, Collections.singletonList(page)), TimeSource.FixedTimeSource.at(Instant.parse("2026-01-01T00:00:00Z")), Duration.ofMinutes(5), Duration.ofSeconds(1), 4, 4);
        FakeInventoryPort inventories = new FakeInventoryPort();
        AtomicInteger actions = new AtomicInteger();
        PaperGuiAdapter adapter = new PaperGuiAdapter(framework, localization(), inventories,
                Runnable::run, (id, selected, revision) -> {
                    actions.incrementAndGet();
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                });
        Object paperViewer = new Object();
        UiModel.SessionId session = adapter.open(paperViewer, viewer, UiModel.Query.first(pageId));
        assertEquals(paperViewer, inventories.openedViewer);
         assertEquals(1, inventories.items.size());
        assertEquals("PAPER", inventories.items.get(0).material());
        adapter.refresh(session);
        assertEquals(1, inventories.items.size());
        inventories.rawSlot = 3;
        adapter.onClick(null);
        adapter.onClick(null);
        assertEquals(1, actions.get());
        assertEquals(2, inventories.cancelled);
        adapter.onDrag(null);
        assertEquals(3, inventories.cancelled);
        inventories.rawSlot = 50;
        adapter.onClick(null);
        assertEquals(1, actions.get());
        inventories.eventInventory = new Object();
        adapter.onClick(null);
        adapter.onDrag(null);
        adapter.onClose(null);
        assertEquals(4, inventories.cancelled);
        inventories.eventInventory = inventories.inventory;
        adapter.onClose(null);
        assertFalse(framework.snapshot(session).isPresent());
        adapter.refresh(session);
        UiModel.SessionId shutdownSession = adapter.open(paperViewer, viewer,
                UiModel.Query.first(pageId));
        adapter.closeAll();
        assertEquals(paperViewer, inventories.closedViewer);
        assertFalse(framework.snapshot(shutdownSession).isPresent());
        assertThrows(IllegalArgumentException.class, () -> new PaperGuiAdapter.RenderedItem("bad", "name", Collections.emptyList()));
    }

    private static LocalizationService localization() { return new LocalizationService() {
        @Override public Result<LocalizedMessage> render(MessageKey key, Optional<PlayerId> player, Parameters parameters) { return Result.success(LocalizedMessage.of(LocaleId.parse("en"), key, key.value())); }
        @Override public Result<LocaleId> switchServerLocale(LocaleId locale) { return Result.success(locale); }
        @Override public Result<LocaleId> switchPlayerLocale(PlayerId player, LocaleId locale) { return Result.success(locale); }
    }; }
    private static final class FakeInventoryPort implements PaperGuiAdapter.InventoryPort {
        private final Object inventory = new Object();
         private final List<PaperGuiAdapter.RenderedItem> items = new ArrayList<>();
         private Object openedViewer;
        private Object closedViewer;
        private Object eventInventory = inventory;
         private int rawSlot;
        private int cancelled;
        @Override public Object create(int size, String title) { assertEquals(54, size);
         return inventory;
        }
        @Override public void clear(Object value) { items.clear(); }
        @Override public void set(Object value, int slot, PaperGuiAdapter.RenderedItem item) { items.add(item); }
        @Override public void open(Object viewer, Object value) { openedViewer = viewer; }
        @Override public void close(Object viewer) { closedViewer = viewer; }
        @Override public Object inventory(Object event) { return eventInventory; }
        @Override public int rawSlot(Object event) { return rawSlot; }
        @Override public void cancel(Object event) { cancelled++; }
    }
}
