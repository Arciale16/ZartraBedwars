package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.application.ArenaFailures;
import io.zartra.bedwars.arena.application.ArenaPolicy;
import io.zartra.bedwars.arena.application.SetupApplicationService;
import io.zartra.bedwars.arena.setup.MarkerProposal;
import io.zartra.bedwars.arena.setup.SetupMutation;
import io.zartra.bedwars.arena.setup.SetupPreview;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.spi.SetupCommitPort;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetupApplicationServiceTest {
    private MemoryPorts ports;
    private SetupApplicationService service;
    private CorrelationId correlation;

    @BeforeEach void setUp() {
        ports = new MemoryPorts();
        ports.save(new ArenaRepository.SaveRequest(ArenaTestFixture.complete(), 0, true));
        service = new SetupApplicationService(ports, ports, ports, ports,
                new ArenaValidation.DefaultValidator(), ArenaPolicy.of(10, 2,
                Duration.ofSeconds(30), Duration.ofSeconds(5)), ports, ports, ports,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
        correlation = CorrelationId.random();
    }

    @Test void typedSessionSupportsMutationUndoRedoValidationAndAbandon() {
        SetupSession session = service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        session = service.mutate(session.id(), 0, SetupMutation.metadata(
                ArenaTestFixture.id("metadata/session"), "changed"), ArenaTestFixture.actor(),
                correlation).requireValue();
        assertTrue(session.canUndo());
        session = service.undo(session.id(), 1, ArenaTestFixture.actor(), correlation).requireValue();
        assertTrue(session.canRedo());
        session = service.redo(session.id(), 2, ArenaTestFixture.actor(), correlation).requireValue();
        assertTrue(service.validate(session.id(), 3, ArenaTestFixture.actor(), correlation)
                .requireValue().mayEnable());
        final SetupSession abandoned = service.abandon(session.id(), 3,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(SetupSession.State.ABANDONED, abandoned.state());
        assertEquals(ArenaFailures.CONFLICT, service.mutate(abandoned.id(), 3,
                SetupMutation.metadata(ArenaTestFixture.id("metadata/x"), "x"),
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void previewIsTwoPhaseRevisionBoundAndCanBeExplicitlyApplied() {
        final SetupSession session = service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        final SetupPreview preview = service.preview(session.id(), 0,
                Collections.singletonList(SetupMutation.group(
                        ArenaTestFixture.id("group/preview"))), ArenaTestFixture.actor(),
                correlation).requireValue();
        assertEquals(ArenaTestFixture.id("group/default"), session.draft().arena().group());
        final SetupSession applied = service.applyPreview(preview, ArenaTestFixture.actor(),
                correlation).requireValue();
        assertEquals(ArenaTestFixture.id("group/preview"), applied.draft().arena().group());
        assertEquals(ArenaFailures.CONFLICT, service.applyPreview(preview,
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void markerDiscoveryRequiresExplicitCurrentRevisionApply() {
        final SetupSession session = service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        ports.markerMutation = SetupMutation.metadata(ArenaTestFixture.id("metadata/marker"),
                "discovered");
        final MarkerProposal proposal = service.discoverMarkers(session.id(), 0,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertFalse(session.draft().arena().metadata().containsKey(
                ArenaTestFixture.id("metadata/marker")));
        final SetupSession applied = service.applyMarkers(proposal, ArenaTestFixture.actor(),
                correlation).requireValue();
        assertEquals("discovered", applied.draft().arena().metadata().get(
                ArenaTestFixture.id("metadata/marker")));
        assertEquals(ArenaFailures.CONFLICT, service.applyMarkers(proposal,
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void commitAtomicallyPublishesArenaAndTerminalSession() {
        final SetupSession session = service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        final SetupPreview preview = service.preview(session.id(), 0,
                Collections.singletonList(SetupMutation.metadata(
                        ArenaTestFixture.id("metadata/committed"), "yes")),
                ArenaTestFixture.actor(), correlation).requireValue();
        final SetupCommitPort.CommitResult committed = service.commit(preview, true,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(SetupSession.State.COMMITTED, committed.session().state());
        assertEquals(2, committed.arena().revision());
        assertTrue(committed.arena().lastKnownGood().isPresent());
        assertEquals("yes", committed.arena().bundle().arena().metadata().get(
                ArenaTestFixture.id("metadata/committed")));
    }

    @Test void invalidEnableStaleAuthorizationCancellationAndCapacityFailClosed() {
        final SetupSession first = service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(ArenaFailures.CONFLICT, service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.CONFLICT, service.mutate(first.id(), 3,
                SetupMutation.group(ArenaTestFixture.id("group/x")), ArenaTestFixture.actor(),
                correlation).error().get());
        ports.allow = false;
        assertEquals(ArenaFailures.FORBIDDEN, service.mutate(first.id(), 0,
                SetupMutation.group(ArenaTestFixture.id("group/x")), ArenaTestFixture.actor(),
                correlation).error().get());
        ports.allow = true;
        ports.cancel = true;
        assertEquals(ArenaFailures.FORBIDDEN, service.mutate(first.id(), 0,
                SetupMutation.group(ArenaTestFixture.id("group/x")), ArenaTestFixture.actor(),
                correlation).error().get());
    }

    @Test void missingHistoryMalformedPreviewAndPortFailuresFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> service.begin(
                ArenaTestFixture.ARENA_ID, 0, ArenaTestFixture.actor(), correlation));
        assertEquals(ArenaFailures.NOT_FOUND, service.begin(
                io.zartra.bedwars.api.identity.ArenaId.random(), 1,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.CONFLICT, service.begin(ArenaTestFixture.ARENA_ID, 2,
                ArenaTestFixture.actor(), correlation).error().get());
        final SetupSession session = service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(ArenaFailures.CONFLICT, service.undo(session.id(), 0,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.CONFLICT, service.redo(session.id(), 0,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.INVALID, service.discoverMarkers(session.id(), 0,
                ArenaTestFixture.actor(), correlation).error().get());
        assertThrows(IllegalArgumentException.class, () -> service.preview(session.id(), 0,
                Collections.<SetupMutation>emptyList(), ArenaTestFixture.actor(), correlation));
        assertThrows(IllegalArgumentException.class, () -> service.preview(session.id(), 0,
                java.util.Arrays.asList(SetupMutation.group(ArenaTestFixture.id("group/x")), null),
                ArenaTestFixture.actor(), correlation));
        assertThrows(IllegalArgumentException.class, () -> service.mutate(session.id(), -1,
                SetupMutation.group(ArenaTestFixture.id("group/x")), ArenaTestFixture.actor(),
                correlation));
        assertEquals(ArenaFailures.NOT_FOUND, service.mutate(SetupSessionId.random(), 0,
                SetupMutation.group(ArenaTestFixture.id("group/x")), ArenaTestFixture.actor(),
                correlation).error().get());

        ports.failSessionReads = true;
        assertEquals(ArenaFailures.INVALID, service.mutate(session.id(), 0,
                SetupMutation.group(ArenaTestFixture.id("group/x")), ArenaTestFixture.actor(),
                correlation).error().get());
        ports.failSessionReads = false;
        ports.failCommits = true;
        final SetupPreview preview = service.preview(session.id(), 0,
                Collections.singletonList(SetupMutation.metadata(
                        ArenaTestFixture.id("metadata/commit_failure"), "yes")),
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(ArenaFailures.INVALID, service.commit(preview, false,
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void sessionInventoryAndPersistenceFailuresArePropagated() {
        ports.failSessionLists = true;
        assertEquals(ArenaFailures.INVALID, service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        ports.failSessionLists = false;
        ports.failSessionWrites = true;
        assertEquals(ArenaFailures.INVALID, service.begin(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
    }
}
