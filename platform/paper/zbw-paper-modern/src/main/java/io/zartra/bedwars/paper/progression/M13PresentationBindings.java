package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Binds every M13 command and GUI action to the existing M09 presentation framework. */
public final class M13PresentationBindings {
    private M13PresentationBindings() { }

    /** Creates exact bindings; the operation rechecks M03 authorization and writes audit context. */
    public static Map<PresentationActions.ActionId, PresentationActions.UseCase> create(
            final Operations operations) {
        Objects.requireNonNull(operations, "operations");
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> result =
                new LinkedHashMap<PresentationActions.ActionId, PresentationActions.UseCase>();
        for (PresentationActions.Definition definition : PresentationActions.Catalog.m13()) {
            final PresentationActions.ActionId action = definition.id();
            result.put(action, request -> operations.execute(action, request));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Authorized, validated, audited application boundary containing no Paper logic. */
    public interface Operations {
        /** Executes one query or confirmed mutation asynchronously. */
        CompletionStage<PresentationActions.Response> execute(PresentationActions.ActionId action,
                                                               PresentationActions.Request request);
    }
}
