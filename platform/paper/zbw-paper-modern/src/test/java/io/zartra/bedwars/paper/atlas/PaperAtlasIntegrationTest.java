package io.zartra.bedwars.paper.atlas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-004/005/011/012 Paper permission, routing, privacy and lifecycle evidence. */
class PaperAtlasIntegrationTest {
    @Test void routesReviewerAndStaffOperationsThroughAsyncPort() {
        FakePort port = new FakePort();
        PaperAtlasService service = new PaperAtlasService(port, Runnable::run);
        AtlasCommandRouter router = new AtlasRuntimeBootstrap(service).start();
        Audience reviewer = new Audience(PaperAtlasService.VIEW, PaperAtlasService.STAFF,
                PaperAtlasService.ADMIN);
        String id = port.caseId.toString();
        assertEquals(0, ((java.util.List<?>) router.route(reviewer, "list")
                .toCompletableFuture().join()).size());
        assertTrue((Boolean) router.route(reviewer, "review", id).toCompletableFuture().join());
        assertTrue((Boolean) router.route(reviewer, "verdict", id, "guilty", "evidence")
                .toCompletableFuture().join());
        assertTrue((Boolean) router.route(reviewer, "final", id, "resolved")
                .toCompletableFuture().join());
        assertEquals("healthy", router.route(reviewer, "diagnostics").toCompletableFuture().join());
    }

    @Test void deniesMissingPermissionBeforePortInvocation() {
        FakePort port = new FakePort();
        PaperAtlasService service = new PaperAtlasService(port, Runnable::run);
        service.start();
        assertThrows(SecurityException.class, () -> service.list(new Audience()));
        assertEquals(0, port.calls);
    }

    @Test void presentsOnOwnerExecutorAndCleansUp() {
        FakePort port = new FakePort();
        AtomicBoolean ownerUsed = new AtomicBoolean();
        PaperAtlasService service = new PaperAtlasService(port, task -> {
            ownerUsed.set(true);
            task.run();
        });
        AtlasRuntimeBootstrap bootstrap = new AtlasRuntimeBootstrap(service);
        bootstrap.start();
        Audience audience = new Audience(PaperAtlasService.VIEW);
        service.present(service.list(audience), audience, value -> "cases:" + value.size())
                .toCompletableFuture().join();
        assertTrue(ownerUsed.get());
        assertEquals("cases:0", audience.message);
        bootstrap.stop().toCompletableFuture().join();
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> service.list(audience).toCompletableFuture().join());
    }

    @Test void guiProjectionCopiesCollectionsAndContainsNoIdentityVaultField() {
        java.util.List<String> evidence = new java.util.ArrayList<>();
        evidence.add("replay:one");
        AtlasView view = new AtlasView(AtlasCaseId.random(), evidence, "eligible",
                Collections.emptyList(), Collections.emptyList());
        evidence.clear();
        assertEquals(Collections.singletonList("replay:one"), view.evidenceReferences());
        assertTrue(java.util.Arrays.stream(AtlasView.class.getRecordComponents())
                .noneMatch(component -> component.getName().toLowerCase().contains("identity")));
    }

    private static final class Audience implements AtlasAudience {
        private final UUID id = UUID.randomUUID();
        private final Set<String> permissions;
        private String message;
        private Audience(final String... values) {
            permissions = new HashSet<>(java.util.Arrays.asList(values));
        }
        @Override public UUID playerId() { return id; }
        @Override public boolean hasPermission(final String permission) {
            return permissions.contains(permission);
        }
        @Override public void present(final String value) { message = value; }
    }

    private static final class FakePort implements AtlasPaperPort {
        private final AtlasCaseId caseId = AtlasCaseId.random();
        private int calls;
        @Override public CompletionStage<java.util.List<AtlasCaseSummary>> list(final UUID actorId) {
            calls++;
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        @Override public CompletionStage<AtlasView> open(final UUID actorId,
                                                         final AtlasCaseId id) {
            calls++;
            return CompletableFuture.completedFuture(new AtlasView(id, Collections.emptyList(),
                    "eligible", Collections.emptyList(), Collections.emptyList()));
        }
        @Override public CompletionStage<Boolean> beginReview(final UUID actorId,
                                                               final AtlasCaseId id) {
            calls++;
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletionStage<Boolean> submitVerdict(final UUID actorId,
                final AtlasCaseId id, final String verdict, final String reason) {
            calls++;
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletionStage<Boolean> finalReview(final UUID actorId,
                final AtlasCaseId id, final String disposition) {
            calls++;
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletionStage<String> diagnostics() {
            calls++;
            return CompletableFuture.completedFuture("healthy");
        }
    }
}
