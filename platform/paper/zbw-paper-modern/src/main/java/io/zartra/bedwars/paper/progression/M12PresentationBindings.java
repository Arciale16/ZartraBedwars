package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.command.api.PresentationActions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Connects the M09 presentation vocabulary to authorized M12 application use cases. */
public final class M12PresentationBindings {
    private M12PresentationBindings() { }

    /** Creates a complete immutable binding map for every M12 action. */
    public static Map<PresentationActions.ActionId, PresentationActions.UseCase> create(
            final Operations operations) {
        Objects.requireNonNull(operations, "operations");
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> result =
                new LinkedHashMap<PresentationActions.ActionId, PresentationActions.UseCase>();
        for (PresentationActions.Definition definition : PresentationActions.Catalog.m12()) {
            final PresentationActions.ActionId action = definition.id();
            if (result.put(action, request -> operations.execute(action, request)) != null) {
                throw new IllegalStateException("duplicate M12 action binding");
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Application facade responsible for authorization, rate limits, audit and mutations. */
    public interface Operations {
        /** Executes a request after revalidating authorization at execution time. */
        CompletionStage<PresentationActions.Response> execute(PresentationActions.ActionId action,
                                                               PresentationActions.Request request);
    }
}
