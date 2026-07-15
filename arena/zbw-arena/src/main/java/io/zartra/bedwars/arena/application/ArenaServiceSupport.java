package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.event.ApiEvent;
import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.spi.ArenaAuditSink;
import io.zartra.bedwars.arena.spi.ArenaEventSink;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Shared authorization, audit and event policy for M07 application services. */
final class ArenaServiceSupport {
    private static final DefinitionId ALLOWED = DefinitionId.of("zartra", "arena/outcome/allowed");
    private static final DefinitionId CANCELLED = DefinitionId.of("zartra", "arena/outcome/cancelled");
    private final AuthorizationService authorization;
    private final ArenaAuditSink audit;
    private final ArenaEventSink events;
    private final TimeSource timeSource;
    private final AtomicLong sequence = new AtomicLong();

    ArenaServiceSupport(final AuthorizationService authorization, final ArenaAuditSink audit,
                        final ArenaEventSink events, final TimeSource timeSource) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.events = Objects.requireNonNull(events, "events");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }

    boolean authorize(final ArenaOperation operation, final ArenaId arenaId,
                      final AuthorizationSubject actor, final CorrelationId correlationId) {
        final AuthorizationDecision decision = authorization.authorize(AuthorizationRequest.of(
                actor, operation.permission(), target(arenaId)));
        if (!decision.isAllowed()) {
            audit.publish(new ArenaAuditRecord(operation, arenaId, actor, false, decision.reason(),
                    correlationId, timeSource.now()));
            return false;
        }
        return true;
    }

    boolean before(final ArenaOperation operation, final ArenaId arenaId,
                   final AuthorizationSubject actor, final CorrelationId correlationId) {
        final ArenaEvents.BeforeChange event = new ArenaEvents.BeforeChange(metadata(operation,
                correlationId, EventMetadata.ThreadContext.APPLICATION_WORKER, "before"),
                operation, arenaId, actor);
        final ApiEvent.Decision decision = events.before(event);
        if (decision.isCancellation()) {
            audit.publish(new ArenaAuditRecord(operation, arenaId, actor, false, CANCELLED,
                    correlationId, timeSource.now()));
            return false;
        }
        return true;
    }

    void after(final ArenaOperation operation, final ArenaId arenaId,
               final AuthorizationSubject actor, final CorrelationId correlationId,
               final long revision) {
        audit.publish(new ArenaAuditRecord(operation, arenaId, actor, true, ALLOWED,
                correlationId, timeSource.now()));
        events.after(new ArenaEvents.Changed(metadata(operation, correlationId,
                EventMetadata.ThreadContext.APPLICATION_WORKER, "changed"), operation,
                arenaId, revision));
    }

    void failure(final ArenaOperation operation, final ArenaId arenaId,
                 final AuthorizationSubject actor, final CorrelationId correlationId,
                 final DefinitionId reason) {
        audit.publish(new ArenaAuditRecord(operation, arenaId, actor, true, reason,
                correlationId, timeSource.now()));
    }

    TimeSource timeSource() { return timeSource; }

    private EventMetadata metadata(final ArenaOperation operation,
                                   final CorrelationId correlationId,
                                   final EventMetadata.ThreadContext context,
                                   final String phase) {
        return EventMetadata.of(EventId.random(), EventTypeId.of("zartra", "arena/"
                        + operation.name().toLowerCase() + "/" + phase), correlationId,
                timeSource.now(), sequence.getAndIncrement(), 1, context);
    }

    private static DefinitionId target(final ArenaId id) {
        return DefinitionId.of("zartra", "arena/" + id.toString());
    }
}
