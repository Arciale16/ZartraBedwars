package io.zartra.bedwars.paper.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.command.api.CommandModel;
import io.zartra.bedwars.command.api.PresentationActions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class M11BindingsTest {
    @Test
    void bindsAndRoutesEveryM11ActionWithoutReplacingTheM09Framework() {
        final List<PresentationActions.ActionId> invoked =
                new ArrayList<PresentationActions.ActionId>();
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings =
                M11PresentationBindings.create((action, request) -> {
                    invoked.add(action);
                    assertEquals(action, request.action());
                    return CompletableFuture.completedFuture(PresentationActions.Response.simple(
                            PresentationActions.Response.Status.SUCCESS,
                            "presentation.success", 1L));
                });

        assertEquals(PresentationActions.Catalog.m11().size(), bindings.size());
        for (PresentationActions.Definition definition : PresentationActions.Catalog.m11()) {
            final PresentationActions.ActionId action = definition.id();
            bindings.get(action).execute(request(action)).toCompletableFuture().join();
        }
        assertEquals(new ArrayList<PresentationActions.ActionId>(bindings.keySet()), invoked);
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.clear());
    }

    @Test
    void rejectsMissingOperations() {
        assertThrows(NullPointerException.class, () -> M11PresentationBindings.create(null));
    }

    private static PresentationActions.Request request(final PresentationActions.ActionId action) {
        return new PresentationActions.Request(AuthorizationSubject.of(
                AuthorizationSubject.Kind.CONSOLE, DefinitionId.of("zartra", "console/test")),
                action, DefinitionId.of("zartra", "target/test"), 0L, CorrelationId.random(),
                CommandModel.Arguments.empty(), PresentationActions.Surface.GUI);
    }
}
