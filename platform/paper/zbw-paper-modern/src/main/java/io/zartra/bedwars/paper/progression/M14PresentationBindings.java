package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** M14 command bindings delegated to the established M09 command framework. */
public final class M14PresentationBindings {
    private M14PresentationBindings() { }
    /** Creates execution-time bindings; authorization, validation and confirmation remain operations concerns. */
    public static Map<PresentationActions.ActionId, PresentationActions.Handler> create(final Operations operations) {
        Objects.requireNonNull(operations, "operations");
        final Map<PresentationActions.ActionId, PresentationActions.Handler> result = new LinkedHashMap<>();
        for (PresentationActions.Definition action : PresentationActions.Catalog.m14()) {
            result.put(action.id(), request -> operations.execute(action, request));
        }
        return Collections.unmodifiableMap(result);
    }
    /** Authorized command operation boundary with audit context supplied by command-paper. */
    public interface Operations {
        /** Performs the action only after central M03 authorization and mutation confirmation. */
        Object execute(PresentationActions.Definition action, PresentationActions.Request request);
    }
}
