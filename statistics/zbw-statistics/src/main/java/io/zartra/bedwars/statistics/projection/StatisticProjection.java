package io.zartra.bedwars.statistics.projection;

import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Objects;

/** Typed caller-transaction-bound contracts for deterministic statistic aggregation. */
public final class StatisticProjection {
    private StatisticProjection() { }
    /** Existing event family;
    M15 creates no replacement lifecycle. */ public enum Source { MATCH, SETTLEMENT, PROGRESSION }
    /** Explicit result state makes duplicate suppression observable. */ public enum Status { APPLIED, DUPLICATE, REJECTED, RETRYABLE_FAILURE }
    /** Immutable fact normalized from an existing M08, M11 or M12 event boundary. */ public static final class Event {
        private final EventId eventId;
        private final IdempotencyKey idempotencyKey;
        private final Source source;
        private final StatisticId statisticId;
        private final StatisticScope scope;
        private final long delta;
        private final StatisticAudit audit;
        /** Creates a validated, non-negative contribution. */ public Event(final EventId eventId, final IdempotencyKey idempotencyKey, final Source source, final StatisticId statisticId, final StatisticScope scope, final long delta, final StatisticAudit audit) {
            this.eventId = Objects.requireNonNull(eventId, "eventId");
            this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            this.source = Objects.requireNonNull(source, "source");
            this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
            this.scope = Objects.requireNonNull(scope, "scope");
            if (delta < 0) { throw new IllegalArgumentException("delta must be non-negative");
            }
            this.delta = delta;
            this.audit = Objects.requireNonNull(audit, "audit");
        }
        /** @return original canonical event identity */ public EventId eventId() { return eventId;
        } /** @return exactly-once key */ public IdempotencyKey idempotencyKey() { return idempotencyKey;
        } /** @return original event family */ public Source source() { return source;
        } /** @return target definition */ public StatisticId statisticId() { return statisticId;
        } /** @return isolated dimension */ public StatisticScope scope() { return scope;
        } /** @return contribution */ public long delta() { return delta;
        } /** @return audit metadata */ public StatisticAudit audit() { return audit;
        }
    }
    /** Immutable projection outcome without exception-driven duplicate handling. */ public static final class ResultState { private final Status status;
    private final Instant processedAt;
        /** Creates a projection result. */ public ResultState(final Status status, final Instant processedAt) { this.status = Objects.requireNonNull(status, "status");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt");
        }
        /** @return explicit outcome */ public Status status() { return status;
        } /** @return processing timestamp */ public Instant processedAt() { return processedAt;
        }
    }
    /** Claims and projects only in a caller-owned M04 transaction. */ public interface Projector { /** @return typed outcome */ Result<ResultState> project(UnitOfWork unitOfWork, Event event);
    }
    /** Atomic idempotency boundary normally backed by the existing M04 inbox/outbox model. */ public interface IdempotencyPort { /** @return true only for the invocation that owns the key */ Result<Boolean> claim(UnitOfWork unitOfWork, IdempotencyKey key, Instant claimedAt);
    }
    /** Bounded rebuild request;
    implicit full rebuilds are forbidden. */ public static final class RebuildRequest { private final StatisticId statisticId;
    private final StatisticScope scope;
    private final int maximumEvents;
        /** Creates a bounded rebuild request. */ public RebuildRequest(final StatisticId statisticId, final StatisticScope scope, final int maximumEvents) { this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
        this.scope = Objects.requireNonNull(scope, "scope");
        if (maximumEvents < 1 || maximumEvents > 100000) { throw new IllegalArgumentException("maximumEvents must be within 1..100000");
        } this.maximumEvents = maximumEvents;
        }
        /** @return statistic to rebuild */ public StatisticId statisticId() { return statisticId;
        } /** @return isolated dimension */ public StatisticScope scope() { return scope;
        } /** @return explicit work bound */ public int maximumEvents() { return maximumEvents;
        }
    }
    /** Runs authorized bounded rebuild work outside an owner thread. */ public interface Rebuilder { /** @return typed outcome */ Result<ResultState> rebuild(UnitOfWork unitOfWork, RebuildRequest request);
    }
}
