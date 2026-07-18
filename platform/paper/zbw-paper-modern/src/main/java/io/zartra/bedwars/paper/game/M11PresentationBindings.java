package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.command.api.PresentationActions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * M09 presentation bindings for the complete M11 action catalogue.
 *
 * <p>This adapter contains no shop rules. It preserves the action identity, authenticated request,
 * revision and surface selected by M09 and delegates to the injected M11 application facade.</p>
 */
public final class M11PresentationBindings {
    private M11PresentationBindings() { }

    /** Creates one complete, duplicate-free binding map for the M11 catalogue. */
    public static Map<PresentationActions.ActionId, PresentationActions.UseCase> create(
            final Operations operations) {
        Objects.requireNonNull(operations, "operations");
        final List<PresentationActions.Definition> definitions = PresentationActions.Catalog.m11();
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> result =
                new LinkedHashMap<PresentationActions.ActionId, PresentationActions.UseCase>();
        for (PresentationActions.Definition definition : definitions) {
            final PresentationActions.ActionId id = definition.id();
            final PresentationActions.UseCase previous = result.put(id,
                    request -> operations.execute(id, request));
            if (previous != null) { throw new IllegalStateException("duplicate M11 action binding"); }
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    /** Presentation-neutral facade implemented by the M11 composition root. */
    public interface Operations {
        /** Executes one authorized M11 request and returns its typed M09 response. */
        CompletionStage<PresentationActions.Response> execute(PresentationActions.ActionId action,
                                                               PresentationActions.Request request);
    }
}
