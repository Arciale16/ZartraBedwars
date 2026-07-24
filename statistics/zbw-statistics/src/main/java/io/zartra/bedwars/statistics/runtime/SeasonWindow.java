package io.zartra.bedwars.statistics.runtime;

import io.zartra.bedwars.statistics.model.StatisticScope;
import java.time.Instant;
import java.util.Objects;

/** Immutable, versioned half-open season boundary that cannot mutate historical windows. */
public final class SeasonWindow implements AggregateProjectionContracts.SeasonBoundary {
    private final StatisticScope seasonId;
    private final int definitionVersion;
    private final Instant startsAt;
    private final Instant endsAt;

    /** Creates one non-empty, deterministic season window. */
    public SeasonWindow(final StatisticScope seasonId, final int definitionVersion,
                        final Instant startsAt, final Instant endsAt) {
        this.seasonId = Objects.requireNonNull(seasonId, "seasonId");
        if (definitionVersion < 1) {
            throw new IllegalArgumentException("definitionVersion must be positive");
        }
        this.definitionVersion = definitionVersion;
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt");
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("season must have a positive duration");
        }
    }

    @Override
    public boolean contains(final Instant timestamp) {
        final Instant value = Objects.requireNonNull(timestamp, "timestamp");
        return !value.isBefore(startsAt) && value.isBefore(endsAt);
    }

    @Override
    public StatisticScope seasonId() {
        return seasonId;
    }

    /** @return immutable version of this season definition */
    public int definitionVersion() {
        return definitionVersion;
    }

    /** @return inclusive season start */
    public Instant startsAt() {
        return startsAt;
    }

    /** @return exclusive season end */
    public Instant endsAt() {
        return endsAt;
    }
}
