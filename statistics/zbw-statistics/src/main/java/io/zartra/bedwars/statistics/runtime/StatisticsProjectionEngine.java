package io.zartra.bedwars.statistics.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.PlayerStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticDefinition;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.projection.StatisticProjection;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Applies one existing-event contribution atomically through caller-owned persistence ports. */
public final class StatisticsProjectionEngine {
    private final Store store;

    /** Creates an engine with an explicit durable store. */
    public StatisticsProjectionEngine(final Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Projects one player event; duplicate idempotency keys never change an aggregate twice. */
    public Result<Outcome> project(final UnitOfWork unitOfWork, final PlayerId playerId,
                                   final StatisticDefinition definition,
                                   final StatisticProjection.Event event, final Instant now) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(now, "now");
        if (!definition.id().equals(event.statisticId())) {
            throw new IllegalArgumentException("event statistic does not match definition");
        }
        final Result<Boolean> claimed = store.claim(unitOfWork, event.idempotencyKey(), now);
        if (claimed.isFailure()) { return Result.failure(claimed.error().get()); }
        if (!claimed.requireValue()) { return Result.success(Outcome.duplicateOutcome()); }
        final Result<Optional<PlayerStatistic>> current = store.find(unitOfWork, playerId,
                event.statisticId(), event.scope());
        if (current.isFailure()) { return Result.failure(current.error().get()); }
        final PlayerStatistic before = current.requireValue().orElse(null);
        final long previous = before == null ? 0L : before.value();
        final long next = aggregate(definition.aggregation(), previous, event.delta());
        final RecordRevision expected = before == null ? RecordRevision.initial() : before.revision();
        final StatisticAudit audit = new StatisticAudit(event.audit().actor(),
                event.audit().correlationId(), now);
        final PlayerStatistic updated = new PlayerStatistic(playerId, event.statisticId(), event.scope(),
                next, expected.next(), audit);
        final Result<PlayerStatistic> saved = store.save(unitOfWork, updated, expected);
        return saved.isFailure() ? Result.failure(saved.error().get())
                : Result.success(Outcome.applied(saved.requireValue()));
    }

    private static long aggregate(final StatisticDefinition.Aggregation aggregation,
                                  final long previous, final long delta) {
        if (aggregation == StatisticDefinition.Aggregation.SUM) { return Math.addExact(previous, delta); }
        if (aggregation == StatisticDefinition.Aggregation.MAXIMUM) { return Math.max(previous, delta); }
        return delta;
    }

    /** Atomic persistence port; SQL implementations reside only in {@code zbw-storage-sql}. */
    public interface Store {
        /** @return true only for the invocation that owns the durable key */
        Result<Boolean> claim(UnitOfWork unitOfWork, IdempotencyKey key, Instant now);
        /** @return current aggregate when present */
        Result<Optional<PlayerStatistic>> find(UnitOfWork unitOfWork, PlayerId playerId,
                                               StatisticId statisticId, StatisticScope scope);
        /** @return saved aggregate or an optimistic-conflict failure */
        Result<PlayerStatistic> save(UnitOfWork unitOfWork, PlayerStatistic value,
                                     RecordRevision expectedRevision);
    }

    /** Explicit observable outcome for idempotency and recovery callers. */
    public static final class Outcome {
        private final boolean duplicate;
        private final PlayerStatistic statistic;
        private Outcome(final boolean duplicate, final PlayerStatistic statistic) {
            this.duplicate = duplicate;
            this.statistic = statistic;
        }
        private static Outcome duplicateOutcome() { return new Outcome(true, null); }
        private static Outcome applied(final PlayerStatistic statistic) { return new Outcome(false, statistic); }
        /** @return whether an existing durable claim suppressed this invocation */ public boolean duplicate() { return duplicate; }
        /** @return committed aggregate when this invocation applied */ public Optional<PlayerStatistic> statistic() { return Optional.ofNullable(statistic); }
    }
}
