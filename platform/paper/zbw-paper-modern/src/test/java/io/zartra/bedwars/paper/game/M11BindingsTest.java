package io.zartra.bedwars.paper.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.command.api.CommandModel;
import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class M11BindingsTest {
    @Test
    void bindsEveryM11ActionWithoutReplacingTheM09Framework() {
        final AtomicReference<PresentationActions.ActionId> invoked = new AtomicReference<>();
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings =
                M11PresentationBindings.create((action, request) -> {
                    invoked.set(action);
                    return CompletableFuture.completedFuture(PresentationActions.Response.simple(
                            PresentationActions.Response.Status.SUCCESS, "presentation.success", 1L));
                });
        assertEquals(PresentationActions.Catalog.m11().size(), bindings.size());
        final PresentationActions.ActionId first = PresentationActions.Catalog.m11().get(0).id();
        bindings.get(first).execute(request(first)).toCompletableFuture().join();
        assertEquals(first, invoked.get());
    }

    private static PresentationActions.Request request(final PresentationActions.ActionId action) {
        return new PresentationActions.Request(AuthorizationSubject.of(
                AuthorizationSubject.Kind.CONSOLE, DefinitionId.of("zartra", "console/test")),
                action, DefinitionId.of("zartra", "target/test"), 0L, CorrelationId.random(),
                CommandModel.Arguments.empty(), PresentationActions.Surface.GUI);
    }
}
