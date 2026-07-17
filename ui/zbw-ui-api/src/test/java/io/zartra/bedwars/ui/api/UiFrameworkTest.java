package io.zartra.bedwars.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.PresentationActions;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class UiFrameworkTest {
    private static final GuiPageId PAGE = GuiPageId.of("zartra", "test/page");
    private static final GuiPageId SECOND = GuiPageId.of("zartra", "test/second");
    private static final PlayerId VIEWER = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test void modelValuesValidateBoundsIdentityAndAccessibility() {
        UiModel.ComponentId id = UiModel.ComponentId.of("test/button");
        assertEquals(id, UiModel.ComponentId.parse(id.toString()));
        UiModel.SessionId session = UiModel.SessionId.random();
        assertEquals(session, UiModel.SessionId.parse(session.toString()));
        UiModel.Accessibility accessibility = accessibility();
        assertTrue(accessibility.alternatives().contains(UiModel.Accessibility.InputAlternative.COMMAND));
        assertThrows(IllegalArgumentException.class, () -> new UiModel.Accessibility(MessageKey.of("ui.label"), MessageKey.of("ui.cue"), Collections.<UiModel.Accessibility.InputAlternative>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new UiModel.Component(id, 54, PresentationActions.ActionId.of("test"), MessageKey.of("ui.label"), Collections.<MessageKey>emptyList(), true, accessibility));
        UiModel.Component component = component(0, true);
        UiModel.PageState state = state(2, component);
        assertEquals(component, state.component(component.id()).orElseThrow(AssertionError::new));
        assertFalse(state.component(UiModel.ComponentId.of("missing")).isPresent());
        assertThrows(IllegalArgumentException.class, () -> new UiModel.PageState(0, UiModel.PageState.Status.READY, 0, 1, Arrays.asList(component, component), MessageKey.of("ui.ready")));
        assertThrows(IllegalArgumentException.class, () -> new UiModel.Query(PAGE, -1, "", null, UiModel.Query.Sort.NATURAL));
        assertThrows(IllegalArgumentException.class, () -> new UiModel.Click(session, component.id(), -1, UUID.randomUUID(), Instant.now()));
    }

    @Test void asyncLoadsNavigationAndStaleResultsAreDeterministic() {
        MutableTime time = new MutableTime();
        CompletableFuture<UiModel.PageState> first = new CompletableFuture<UiModel.PageState>();
        UiModel.PageDefinition page = page(PAGE, first);
        UiModel.PageDefinition second = page(SECOND, CompletableFuture.completedFuture(state(1, component(1, true))));
        UiFramework.Registry registry = new UiFramework.Registry(4, Arrays.asList(page, second));
        UiFramework framework = new UiFramework(registry, time, Duration.ofMinutes(5), Duration.ofSeconds(1), 2, 2);
        UiFramework.LoadHandle opened = framework.open(VIEWER, UiModel.Query.first(PAGE));
        assertEquals(UiModel.PageState.Status.LOADING, framework.snapshot(opened.sessionId())
                .orElseThrow(AssertionError::new).state().status());
        UiFramework.LoadHandle refreshed = framework.refresh(opened.sessionId());
        assertEquals(UiFramework.LoadVerdict.STALE, framework.accept(opened.sessionId(), opened.sequence(), state(1, component(0, true))));
        assertEquals(UiFramework.LoadVerdict.ACCEPTED, framework.accept(opened.sessionId(), refreshed.sequence(), state(2, component(0, true))));
        UiFramework.LoadHandle navigation = framework.navigate(opened.sessionId(), UiModel.Query.first(SECOND));
        assertEquals(1, framework.snapshot(opened.sessionId())
                .orElseThrow(AssertionError::new).history().size());
        assertTrue(framework.back(opened.sessionId()).isPresent());
        assertFalse(framework.back(opened.sessionId()).isPresent());
        assertEquals(PAGE, framework.pageDefinition(PAGE).id());
        assertTrue(framework.close(opened.sessionId()));
        assertFalse(framework.close(opened.sessionId()));
        assertEquals(UiFramework.LoadVerdict.EXPIRED, framework.accept(opened.sessionId(), navigation.sequence(), state(1, component(1, true))));
    }

    @Test void clicksRejectWrongViewerStaleDuplicateUnknownAndDisabled() {
        MutableTime time = new MutableTime();
        UiFramework framework = framework(time, state(4, component(0, true), component(1, false)));
        UiFramework.LoadHandle load = framework.open(VIEWER, UiModel.Query.first(PAGE));
        framework.accept(load.sessionId(), load.sequence(), load.stage().toCompletableFuture().join());
        UiModel.Click valid = click(load.sessionId(), component(0, true).id(), 4, time.now, UUID.randomUUID());
        assertEquals(UiFramework.ClickVerdict.WRONG_VIEWER, framework.click(PlayerId.of(UUID.randomUUID()), valid).verdict());
        assertEquals(UiFramework.ClickVerdict.STALE, framework.click(VIEWER, click(load.sessionId(), component(0, true).id(), 3, time.now, UUID.randomUUID())).verdict());
        assertEquals(UiFramework.ClickVerdict.ACCEPTED, framework.click(VIEWER, valid).verdict());
        assertEquals(UiFramework.ClickVerdict.DUPLICATE, framework.click(VIEWER, valid).verdict());
        assertEquals(UiFramework.ClickVerdict.UNKNOWN_COMPONENT, framework.click(VIEWER, click(load.sessionId(), UiModel.ComponentId.of("unknown"), 4, time.now, UUID.randomUUID())).verdict());
        assertEquals(UiFramework.ClickVerdict.DISABLED, framework.click(VIEWER, click(load.sessionId(), component(1, false).id(), 4, time.now, UUID.randomUUID())).verdict());
        framework.close(load.sessionId());
        assertEquals(UiFramework.ClickVerdict.EXPIRED, framework.click(VIEWER, valid).verdict());
    }

    @Test void capacityRegistryAndExpiryFailClosed() {
        MutableTime time = new MutableTime();
        UiFramework framework = framework(time, state(1, component(0, true)));
        UiFramework.LoadHandle load = framework.open(VIEWER, UiModel.Query.first(PAGE));
        framework.open(PlayerId.of(UUID.randomUUID()), UiModel.Query.first(PAGE));
        assertThrows(IllegalStateException.class, () -> framework.open(PlayerId.of(UUID.randomUUID()), UiModel.Query.first(PAGE)));
        time.now = time.now.plusSeconds(360L);
        assertEquals(2, framework.cleanup());
        assertFalse(framework.snapshot(load.sessionId()).isPresent());
        UiFramework.Registry registry = new UiFramework.Registry(1, Collections.singletonList(page(PAGE, CompletableFuture.completedFuture(state(1, component(0, true))))));
        assertThrows(IllegalStateException.class, () -> registry.register(page(SECOND, CompletableFuture.completedFuture(state(1, component(0, true))))));
        assertThrows(IllegalArgumentException.class, () -> registry.require(SECOND));
    }

    private static UiFramework framework(MutableTime time, UiModel.PageState state) { return new UiFramework(new UiFramework.Registry(2, Collections.singletonList(page(PAGE, CompletableFuture.completedFuture(state)))), time, Duration.ofMinutes(5), Duration.ofSeconds(1), 2, 2); }
    private static UiModel.PageDefinition page(GuiPageId id, CompletableFuture<UiModel.PageState> state) { return new UiModel.PageDefinition(id, MessageKey.of("ui.test.title"), (viewer, query) -> state, Arrays.asList(UiModel.Interaction.PRIMARY, UiModel.Interaction.KEYBOARD)); }
    private static UiModel.Component component(int slot, boolean enabled) { return new UiModel.Component(UiModel.ComponentId.of("test/button-" + slot), slot, PresentationActions.ActionId.of("test/action-" + slot), MessageKey.of("ui.test.label"), Collections.singletonList(MessageKey.of("ui.test.help")), enabled, accessibility()); }
    private static UiModel.Accessibility accessibility() { return new UiModel.Accessibility(MessageKey.of("ui.test.semantic"), MessageKey.of("ui.test.cue"), Arrays.asList(UiModel.Accessibility.InputAlternative.COMMAND, UiModel.Accessibility.InputAlternative.KEYBOARD)); }
    private static UiModel.PageState state(long revision, UiModel.Component... components) { return new UiModel.PageState(revision, UiModel.PageState.Status.READY, 0, 1, Arrays.asList(components), MessageKey.of("ui.ready")); }
    private static UiModel.Click click(UiModel.SessionId id, UiModel.ComponentId component, long revision, Instant at, UUID nonce) { return new UiModel.Click(id, component, revision, nonce, at); }
    private static final class MutableTime implements TimeSource { private Instant now = Instant.parse("2026-01-01T00:00:00Z");
     @Override public Instant now() { return now;
    } }
}
