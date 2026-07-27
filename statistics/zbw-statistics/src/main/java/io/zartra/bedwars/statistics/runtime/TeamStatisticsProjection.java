package io.zartra.bedwars.statistics.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.TeamStatistic;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Deterministic, caller-transaction-bound team aggregate projection. */
public final class TeamStatisticsProjection {
    private final Store store;

    /** Creates a projection with an explicit durable store. */
    public TeamStatisticsProjection(final Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Claims and saves one immutable team aggregate without committing the caller transaction. */
    public Result<Outcome> project(final UnitOfWork unitOfWork, final TeamStatistic value,
                                   final RecordRevision expected, final IdempotencyKey key,
                                   final Instant occurredAt) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(occurredAt, "occurredAt");
        final Result<Boolean> claimed = store.claim(unitOfWork, key, occurredAt);
        if (claimed.isFailure()) {
            return Result.failure(claimed.error().get());
        }
        if (!claimed.requireValue()) {
            return Result.success(Outcome.duplicate());
        }
        final Result<TeamStatistic> saved = store.save(unitOfWork, value, expected);
        if (saved.isFailure()) {
            return Result.success(Outcome.conflict());
        }
        return Result.success(Outcome.applied(saved.requireValue()));
    }

    /** Durable team-store boundary implemented only by SQL adapters. */
    public interface Store {
        /** @return true only for the first durable claim */
        Result<Boolean> claim(UnitOfWork unitOfWork, IdempotencyKey key, Instant occurredAt);

        /** @return saved state or an optimistic conflict */
        Result<TeamStatistic> save(UnitOfWork unitOfWork, TeamStatistic value,
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
        private final TeamStatistic value;

        private Outcome(final Status status, final TeamStatistic value) {
            this.status = status;
            this.value = value;
        }

        private static Outcome applied(final TeamStatistic value) {
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
        public Optional<TeamStatistic> value() {
            return Optional.ofNullable(value);
        }
    }
}
