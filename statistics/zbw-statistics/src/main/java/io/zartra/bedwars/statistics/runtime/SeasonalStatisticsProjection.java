package io.zartra.bedwars.statistics.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.SeasonalStatistic;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Deterministic, caller-transaction-bound seasonal aggregate projection. */
public final class SeasonalStatisticsProjection {
    private final Store store;

    /** Creates a projection with an explicit durable store. */
    public SeasonalStatisticsProjection(final Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Projects an event only within its immutable season boundary. */
    public Result<Outcome> project(final UnitOfWork unitOfWork, final SeasonalStatistic value,
                                   final RecordRevision expected, final IdempotencyKey key,
                                   final Instant occurredAt, final SeasonWindow season) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(occurredAt, "occurredAt");
        final SeasonWindow window = Objects.requireNonNull(season, "season");
        if (!value.season().equals(window.seasonId()) || !window.contains(occurredAt)) {
            throw new IllegalArgumentException("event does not belong to the aggregate season");
        }
        final Result<Boolean> claimed = store.claim(unitOfWork, key, occurredAt);
        if (claimed.isFailure()) {
            return Result.failure(claimed.error().get());
        }
        if (!claimed.requireValue()) {
            return Result.success(Outcome.duplicate());
        }
        final Result<SeasonalStatistic> saved = store.save(unitOfWork, value, expected);
        if (saved.isFailure()) {
            return Result.success(Outcome.conflict());
        }
        return Result.success(Outcome.applied(saved.requireValue()));
    }

    /** Durable seasonal-store boundary implemented only by SQL adapters. */
    public interface Store {
        /** @return true only for the first durable claim */
        Result<Boolean> claim(UnitOfWork unitOfWork, IdempotencyKey key, Instant occurredAt);

        /** @return saved state or an optimistic conflict */
        Result<SeasonalStatistic> save(UnitOfWork unitOfWork, SeasonalStatistic value,
                                       RecordRevision expected);
    }

    /** Typed projection result for duplicate and revision-conflict recovery. */
    public static final class Outcome {
        /** Projection status. */
        public enum Status {
            /** The aggregate and event claim were saved. */
            APPLIED,
            /** The durable event claim already exists. */
            DUPLICATE,
            /** The expected aggregate revision did not match. */
            REVISION_CONFLICT
        }

        private final Status status;
        private final SeasonalStatistic value;

        private Outcome(final Status status, final SeasonalStatistic value) {
            this.status = status;
            this.value = value;
        }

        private static Outcome applied(final SeasonalStatistic value) {
            return new Outcome(Status.APPLIED, value);
        }

        private static Outcome duplicate() {
            return new Outcome(Status.DUPLICATE, null);
        }

        private static Outcome conflict() {
            return new Outcome(Status.REVISION_CONFLICT, null);
        }

        /** @return typed result */
        public Status status() {
            return status;
        }

        /** @return saved state when applied */
        public Optional<SeasonalStatistic> value() {
            return Optional.ofNullable(value);
        }
    }
}
