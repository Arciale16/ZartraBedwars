package io.zartra.bedwars.statistics.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.MatchStatistic;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Objects;

/** Deterministic, caller-transaction-bound match aggregate projection. */
public final class MatchStatisticsProjection {
    private final Store store;
    /** Creates a projection with an explicit durable store. */ public MatchStatisticsProjection(final Store store) { this.store = Objects.requireNonNull(store, "store");
    }
    /** Claims and saves one immutable match aggregate without committing the caller transaction. */
    public Result<Outcome> project(final UnitOfWork unit, final MatchStatistic value, final RecordRevision expected, final IdempotencyKey key, final Instant occurredAt) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(occurredAt, "occurredAt");
        final Result<Boolean> claimed = store.claim(unit, key, occurredAt);
        if (claimed.isFailure()) { return Result.failure(claimed.error().get());
        }
        if (!claimed.requireValue()) { return Result.success(Outcome.duplicate());
        }
        final Result<MatchStatistic> saved = store.save(unit, value, expected);
        return saved.isFailure() ? Result.success(Outcome.conflict()) : Result.success(Outcome.applied(saved.requireValue()));
    }
    /** Durable match-store boundary implemented only by SQL adapters. */ public interface Store { /** @return true only for first durable claim */ Result<Boolean> claim(UnitOfWork unit, IdempotencyKey key, Instant occurredAt);
    /** @return saved state or optimistic conflict */ Result<MatchStatistic> save(UnitOfWork unit, MatchStatistic value, RecordRevision expected);
    }
    /** Typed projection result for duplicate and revision-conflict recovery. */ public static final class Outcome { public enum Status { APPLIED, DUPLICATE, REVISION_CONFLICT } private final Status status;
    private final MatchStatistic value;
    private Outcome(final Status status, final MatchStatistic value) { this.status = status;
    this.value = value;
    } private static Outcome applied(final MatchStatistic value) { return new Outcome(Status.APPLIED, value);
    } private static Outcome duplicate() { return new Outcome(Status.DUPLICATE, null);
    } private static Outcome conflict() { return new Outcome(Status.REVISION_CONFLICT, null);
    } /** @return typed result */ public Status status() { return status;
    } /** @return saved state when applied */ public java.util.Optional<MatchStatistic> value() { return java.util.Optional.ofNullable(value);
    } }
}
