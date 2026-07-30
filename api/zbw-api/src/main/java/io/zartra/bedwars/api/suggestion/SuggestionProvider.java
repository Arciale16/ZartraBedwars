package io.zartra.bedwars.api.suggestion;

import io.zartra.bedwars.api.result.Result;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Disabled-by-default advisory suggestion SPI.
 *
 * <p>ZBW-ECO-005: suggestions are untrusted data. They cannot mutate state, authorize an action,
 * apply enforcement or bypass the owning service's validation and human approval.</p>
 */
public interface SuggestionProvider {
    /** @return asynchronous bounded suggestion result */
    CompletionStage<Result<List<Suggestion>>> suggest(SuggestionRequest request);

    /** Read-only purpose-limited request. */
    interface SuggestionRequest {
        /** @return canonical suggestion purpose */ String purpose();
        /** @return hard processing deadline */ Instant deadline();
        /** @return whether an authorized human explicitly requested suggestions */
        boolean humanRequested();
    }

    /** Immutable provider projection; consumers must validate it as untrusted input. */
    interface Suggestion {
        /** @return stable suggestion identity */ String id();
        /** @return operator-safe summary without secrets */ String summary();
        /** @return whether a human must explicitly approve before owner-side validation */
        boolean requiresHumanApproval();
    }
}
