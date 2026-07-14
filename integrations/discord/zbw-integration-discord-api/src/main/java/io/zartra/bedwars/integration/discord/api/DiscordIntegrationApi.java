package io.zartra.bedwars.integration.discord.api;

import io.zartra.bedwars.api.VersionedApi;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.result.Result;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/** Secure read-only Discord query API and outbound event-stream contract. */
public interface DiscordIntegrationApi extends VersionedApi {
    /**
     * Executes a bounded read-only query. Implementations must enforce caller scopes, privacy,
     * field/page limits and deadlines. No query may mutate gameplay, rewards or statistics.
     *
     * @return asynchronously completed typed result
     */
    <R extends QueryResult> CompletionStage<Result<R>> query(Query<R> query);

    /** @return outbound committed-event stream; subscription itself performs no provider I/O */
    OutboundEventStream events();

    /** Typed read-only query. */
    interface Query<R extends QueryResult> {
        /** @return stable query type */ DefinitionId type();
        /** @return immutable authenticated/scoped context */ QueryContext context();
        /** @return expected immutable response type */ Class<R> responseType();
    }

    /** Marker for immutable canonical query responses. */
    interface QueryResult {
        /** @return stable response schema */ DefinitionId schema();
        /** @return positive response schema version */ int schemaVersion();
        /** @return instant at which the returned data was current */ Instant observedAt();
    }

    /** Immutable authenticated query context with least-privilege scopes. */
    final class QueryContext {
        private final ProviderId caller;
        private final CorrelationId correlationId;
        private final Set<Scope> scopes;
        private final Instant deadline;
        private QueryContext(final ProviderId caller, final CorrelationId correlationId,
                             final Set<Scope> scopes, final Instant deadline) {
            this.caller = Objects.requireNonNull(caller, "caller");
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
            if (Objects.requireNonNull(scopes, "scopes").isEmpty()) { throw new IllegalArgumentException("At least one scope is required"); }
            this.scopes = Collections.unmodifiableSet(EnumSet.copyOf(scopes));
            this.deadline = Objects.requireNonNull(deadline, "deadline");
        }
        /** @return scoped query context */ public static QueryContext of(final ProviderId caller, final CorrelationId correlationId, final Set<Scope> scopes, final Instant deadline) { return new QueryContext(caller, correlationId, scopes, deadline); }
        /** @return authenticated provider identity */ public ProviderId caller() { return caller; }
        /** @return correlation identity */ public CorrelationId correlationId() { return correlationId; }
        /** @return immutable authorized scopes */ public Set<Scope> scopes() { return scopes; }
        /** @return absolute timeout deadline */ public Instant deadline() { return deadline; }
    }

    /** Least-privilege query scopes. */
    enum Scope { PUBLIC_STATISTICS, LINKED_PLAYER_STATISTICS, PUBLIC_LEADERBOARDS, ACCOUNT_LINK, PROVIDER_HEALTH }

    /** Asynchronous stream of committed, allowlisted integration events. */
    interface OutboundEventStream {
        /** @return typed subscription result; callback runs on a provider worker */
        Result<Subscription> subscribe(Sink sink);
    }

    /** Non-blocking outbound-event callback. */
    interface Sink {
        /** @param envelope immutable envelope; implementations return immediately */
        void onEvent(DiscordEventEnvelope<? extends DiscordEventEnvelope.Payload> envelope);
    }

    /** Idempotent subscription handle. */
    interface Subscription { /** Stops future callbacks. */ void close(); }
}
