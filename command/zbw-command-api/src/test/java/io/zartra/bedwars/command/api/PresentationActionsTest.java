package io.zartra.bedwars.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PresentationActionsTest {
    @Test void standardCatalogIsAtomicUniqueAndCoversEveryM09Family() {
        List<PresentationActions.Definition> catalog = PresentationActions.Catalog.standard();
        assertEquals(87, catalog.size());
        assertEquals(catalog.size(), catalog.stream().map(PresentationActions.Definition::id).distinct().count());
        assertTrue(catalog.stream().anyMatch(value -> value.commandPath().equals("/deposit hand")));
        assertTrue(catalog.stream().anyMatch(value -> value.id().toString().contains("setup/markers")));
        assertTrue(catalog.stream().anyMatch(value -> value.id().toString().contains("bossbar/preview")));
        assertTrue(catalog.stream().allMatch(value -> !value.requirementIds().isEmpty()));
        assertTrue(catalog.stream().filter(PresentationActions.Definition::destructive).count() >= 5);
    }

    @Test void registryRequiresExactBindingsAndMapsFailures() {
        List<PresentationActions.Definition> definitions = PresentationActions.Catalog.standard().subList(0, 2);
        Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings = new LinkedHashMap<PresentationActions.ActionId, PresentationActions.UseCase>();
        bindings.put(definitions.get(0).id(), request -> java.util.concurrent.CompletableFuture.completedFuture(PresentationActions.Response.simple(PresentationActions.Response.Status.SUCCESS, "presentation.success", 1)));
        assertThrows(IllegalArgumentException.class, () -> new PresentationActions.Registry(definitions, bindings));
        bindings.put(definitions.get(1).id(), request -> { throw new IllegalStateException(); });
        PresentationActions.Registry registry = new PresentationActions.Registry(definitions, bindings);
        PresentationActions.Request request = request(definitions.get(0).id());
        assertEquals(PresentationActions.Response.Status.SUCCESS, registry.execute(request).toCompletableFuture().join().status());
        assertEquals(PresentationActions.Response.Status.ERROR, registry.execute(request(definitions.get(1).id())).toCompletableFuture().join().status());
        assertEquals(PresentationActions.Response.Status.NOT_FOUND, registry.execute(request(PresentationActions.ActionId.of("unknown/action"))).toCompletableFuture().join().status());
        assertTrue(registry.handler(definitions.get(0).id()).isPresent());
        assertEquals(2, registry.size());
        bindings.put(PresentationActions.ActionId.of("unknown/binding"), value -> null);
        assertThrows(IllegalArgumentException.class, () -> new PresentationActions.Registry(definitions, bindings));
    }

    @Test void requestResponseAndIdsValidateBoundaries() {
        PresentationActions.ActionId id = PresentationActions.ActionId.of("test/action");
        assertEquals(id, PresentationActions.ActionId.parse(id.toString()));
        assertThrows(IllegalArgumentException.class, () -> new PresentationActions.Request(subject(), id, DefinitionId.of("zartra", "target"), -1, CorrelationId.random(), CommandModel.Arguments.empty(), PresentationActions.Surface.GUI));
        assertThrows(IllegalArgumentException.class, () -> PresentationActions.Response.simple(PresentationActions.Response.Status.SUCCESS, "presentation.success", -1));
    }

    private static PresentationActions.Request request(PresentationActions.ActionId id) { return new PresentationActions.Request(subject(), id, DefinitionId.of("zartra", "target"), 0, CorrelationId.random(), CommandModel.Arguments.empty(), PresentationActions.Surface.COMMAND); }
    private static AuthorizationSubject subject() { return AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER, DefinitionId.of("zartra", "player/test")); }
}
