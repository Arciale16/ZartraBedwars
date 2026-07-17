package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.game.mode.ModeFramework;
import io.zartra.bedwars.game.mode.ModeFramework.ConfigField;
import io.zartra.bedwars.game.mode.ModeFramework.DeferredBinding;
import io.zartra.bedwars.game.mode.ModeFramework.Definition;
import io.zartra.bedwars.game.mode.ModeFramework.FieldType;
import io.zartra.bedwars.game.mode.ModeFramework.Layout;
import io.zartra.bedwars.game.mode.ModeFramework.ModeId;
import io.zartra.bedwars.game.mode.ModeFramework.Version;
import io.zartra.bedwars.game.selector.SelectorFramework;
import io.zartra.bedwars.game.selector.SelectorFramework.Candidate;
import io.zartra.bedwars.game.selector.SelectorFramework.Exclusion;
import io.zartra.bedwars.game.selector.SelectorFramework.Lifecycle;
import io.zartra.bedwars.game.selector.SelectorFramework.Order;
import io.zartra.bedwars.game.selector.SelectorFramework.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModeSelectorM10Test {
    private static final ModeId STANDARD = ModeId.of("zartra", "standard");

    @Test void registersAndOrdersModesDeterministically() {
        final List<ModeFramework.Event> events = new ArrayList<ModeFramework.Event>();
        final ModeFramework.Registry registry = new ModeFramework.Registry(4, events::add);
        registry.register(mode(ModeId.of("zartra", "zeta"), true));
        registry.register(mode(ModeId.of("zartra", "alpha"), true));
        assertEquals("zartra:mode/alpha", registry.snapshot().get(0).id().toString());
        assertEquals(2, events.size());
        assertThrows(IllegalArgumentException.class, () -> registry.register(mode(ModeId.of("zartra", "alpha"), true)));
    }

    @Test void enablementIsVersionFenced() {
        final ModeFramework.Registry registry = new ModeFramework.Registry(2, event -> { });
        registry.register(mode(STANDARD, true));
        assertThrows(IllegalStateException.class, () -> registry.setEnabled(STANDARD, new Version(2, 0), false, new Version(2, 1)));
        final Definition disabled = registry.setEnabled(STANDARD, new Version(1, 0), false, new Version(1, 1));
        assertFalse(disabled.enabled());
        assertThrows(IllegalArgumentException.class, () -> disabled.withEnabled(true, new Version(1, 1)));
    }

    @Test void deferredBindingsPreserveLaterOwnership() {
        final Definition definition = mode(ModeId.of("zartra", "swappage"), true);
        assertEquals("M11", definition.deferredBindings().get(0).ownerMilestone());
        assertThrows(IllegalArgumentException.class, () -> new DeferredBinding("ZBW-ADDON-236", "M10"));
    }

    @Test void standardAndCustomLayoutsShareTheSameValidation() {
        assertTrue(mode(STANDARD, true).supports(layout("solo", 8, 1)));
        assertTrue(mode(STANDARD, true).supports(layout("doubles", 8, 2)));
        assertTrue(mode(STANDARD, true).supports(layout("three", 4, 3)));
        assertTrue(mode(STANDARD, true).supports(layout("four", 4, 4)));
        assertTrue(mode(STANDARD, true).supports(layout("duel", 2, 4)));
        assertTrue(mode(STANDARD, true).supports(layout("custom", 12, 3)));
        assertTrue(mode(STANDARD, true).supports(layout("high", 64, 4)));
        assertThrows(IllegalArgumentException.class, () -> layout("too-many", 64, 5));
    }

    @Test void selectorFiltersUnavailableAndIncompatibleCandidates() {
        final SelectorFramework.Service service = new SelectorFramework.Service();
        final Layout layout = layout("solo", 8, 1);
        final Candidate ready = candidate(1, STANDARD, layout, true, true, true, Lifecycle.WAITING, 2, 0, 2L);
        final Candidate disabled = candidate(2, STANDARD, layout, false, true, true, Lifecycle.WAITING, 0, 0, 2L);
        final Candidate unhealthy = candidate(3, STANDARD, layout, true, false, true, Lifecycle.WAITING, 0, 0, 2L);
        final Candidate stale = candidate(4, STANDARD, layout, true, true, true, Lifecycle.WAITING, 0, 0, 1L);
        final SelectorFramework.Page page = service.page(Arrays.asList(disabled, unhealthy, stale, ready),
                query(STANDARD, layout.id(), "", 2, 2L, 0, 10, Order.CONFIGURED), 7L);
        assertEquals(1, page.candidates().size());
        assertEquals(3, page.exclusions().size());
        assertTrue(page.exclusions().stream().anyMatch(value -> value.reason() == Exclusion.DISABLED));
        assertTrue(page.exclusions().stream().anyMatch(value -> value.reason() == Exclusion.UNHEALTHY));
        assertTrue(page.exclusions().stream().anyMatch(value -> value.reason() == Exclusion.STALE));
    }

    @Test void selectorSearchPagingAndOrderingAreDeterministic() {
        final SelectorFramework.Service service = new SelectorFramework.Service();
        final Layout layout = layout("custom", 12, 3);
        final List<Candidate> values = Arrays.asList(
                candidate(3, STANDARD, layout, true, true, true, Lifecycle.WAITING, 10, 0, 4L),
                candidate(1, STANDARD, layout, true, true, true, Lifecycle.WAITING, 20, 0, 4L),
                candidate(2, STANDARD, layout, true, true, true, Lifecycle.WAITING, 1, 0, 4L));
        final SelectorFramework.Page page = service.page(values,
                query(STANDARD, layout.id(), "", 1, 0L, 0, 2, Order.CAPACITY), 9L);
        assertEquals(2, page.pageCount());
        assertEquals(values.get(2).arenaId(), page.candidates().get(0).arenaId());
        final String search = "map-3";
        assertEquals(1, service.page(values, query(STANDARD, layout.id(), search, 1, 0L, 0, 10,
                Order.IDENTITY), 10L).candidates().size());
    }

    @Test void quickJoinAndStaleViewProtectionUseExactRevisions() {
        final SelectorFramework.Service service = new SelectorFramework.Service();
        final Layout layout = layout("solo", 8, 1);
        final Candidate candidate = candidate(1, STANDARD, layout, true, true, true, Lifecycle.WAITING, 0, 0, 4L);
        final Query query = query(STANDARD, layout.id(), "", 1, 0L, 0, 10, Order.IDENTITY);
        final SelectorFramework.Selection selection = service.quickJoin(Collections.singleton(candidate), query, 8L).get();
        assertTrue(service.current(selection, 8L, candidate));
        assertFalse(service.current(selection, 9L, candidate));
        assertFalse(service.current(selection, 8L, candidate(1, STANDARD, layout, true, true, true,
                Lifecycle.WAITING, 0, 0, 5L)));
    }

    @Test void malformedModeAndSelectorInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new Version(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ConfigField(DefinitionId.of("zartra", "field/x"), FieldType.INTEGER, "bad\nvalue", true));
        assertThrows(IllegalArgumentException.class, () -> query(STANDARD, null, "", 0, 0L, 0, 10, Order.IDENTITY));
        assertThrows(IllegalArgumentException.class, () -> new ModeFramework.Registry(0, event -> { }));
    }

    private static Definition mode(final ModeId id, final boolean enabled) {
        return new Definition(id, MessageKey.of("mode.name"), MessageKey.of("mode.description"),
                new Version(1, 0), enabled, 2, 64, 2, 256,
                Collections.singleton(DefinitionId.of("zartra", "capability/selection")),
                Collections.singleton(new ConfigField(DefinitionId.of("zartra", "field/enabled"),
                        FieldType.BOOLEAN, "true", true)),
                Collections.singleton(new DeferredBinding("ZBW-ADDON-236", "M11")));
    }

    private static Layout layout(final String name, final int teams, final int capacity) {
        final List<DefinitionId> identities = new ArrayList<DefinitionId>();
        for (int index = 0; index < teams; index++) {
            identities.add(DefinitionId.of("custom", "team/semantic-" + index));
        }
        return new Layout(DefinitionId.of("zartra", "layout/" + name), identities, capacity);
    }

    private static Candidate candidate(final int seed, final ModeId mode, final Layout layout,
                                       final boolean enabled, final boolean healthy,
                                       final boolean worldReady, final Lifecycle lifecycle,
                                       final int players, final int reserved, final long revision) {
        return new Candidate(ArenaId.of(new UUID(0L, seed)), revision,
                DefinitionId.of("zartra", "map/map-" + seed), mode, layout,
                MessageKey.of("arena.map." + seed), enabled, healthy, worldReady, lifecycle,
                players, reserved, seed, Collections.singleton(DefinitionId.of("zartra", "tag/all")));
    }

    private static Query query(final ModeId mode, final DefinitionId layout, final String search,
                               final int capacity, final long revision, final int page,
                               final int pageSize, final Order order) {
        return new Query(mode, layout, null, null, search, capacity, revision, page, pageSize, order);
    }
}
