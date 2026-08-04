package io.zartra.bedwars.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffAction;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffAuditRecord;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffResult;
import io.zartra.bedwars.replay.api.ReplayId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Regression evidence for ZBW-READY-002, ZBW-UX-003, ZBW-REPLAY-001 and ZBW-ATLAS-003. */
final class ZartraBedWarsPluginCompositionTest {
    private static final UUID ACTOR = new UUID(7L, 9L);
    private static final ReplayId REPLAY = ReplayId.of(new UUID(11L, 13L));

    @Test void m07M08ActionsUseTheirInstalledApplicationRuntimes() {
        assertEquals("bootstrap", ZartraBedWarsPlugin.applicationRuntime(
                PresentationActions.ActionId.of("arena/list")));
        assertEquals("editor", ZartraBedWarsPlugin.applicationRuntime(
                PresentationActions.ActionId.of("setup/status")));
        assertEquals("game", ZartraBedWarsPlugin.applicationRuntime(
                PresentationActions.ActionId.of("game/health")));
        assertEquals("deposit", ZartraBedWarsPlugin.applicationRuntime(
                PresentationActions.ActionId.of("deposit/inspect")));
        assertEquals("uncomposed", ZartraBedWarsPlugin.applicationRuntime(
                PresentationActions.ActionId.of("hotbar-manager/inspect")));
    }
    @Test void cleanServerReplayPortsFailClosedWithoutCrashing() {
        assertFalse(ZartraBedWarsPlugin.degradedReplayRepository().findSession(REPLAY)
                .toCompletableFuture().join().isPresent());
        assertFalse(ZartraBedWarsPlugin.degradedReplayStaffStore().find(REPLAY)
                .toCompletableFuture().join().isPresent());
        assertFalse(ZartraBedWarsPlugin.degradedReplayStaffStore().mark(REPLAY, true)
                .toCompletableFuture().join().booleanValue());
        final ReplayStaffAuditRecord audit = new ReplayStaffAuditRecord(
                0L, Instant.EPOCH, ACTOR, ReplayStaffAction.INSPECT, REPLAY,
                ReplayStaffResult.Status.NOT_FOUND);
        ZartraBedWarsPlugin.degradedReplayAudit().append(audit).toCompletableFuture().join();
    }

    @Test void cleanServerAtlasPortKeepsReadCommandsAvailableAndRejectsMutation() {
        assertTrue(ZartraBedWarsPlugin.degradedAtlasPort().list(ACTOR)
                .toCompletableFuture().join().isEmpty());
        assertEquals("Atlas storage unavailable", ZartraBedWarsPlugin.degradedAtlasPort()
                .diagnostics().toCompletableFuture().join());
        assertFalse(ZartraBedWarsPlugin.degradedAtlasPort()
                .beginReview(ACTOR, AtlasCaseId.of(new UUID(17L, 19L)))
                .toCompletableFuture().join().booleanValue());
    }
}
