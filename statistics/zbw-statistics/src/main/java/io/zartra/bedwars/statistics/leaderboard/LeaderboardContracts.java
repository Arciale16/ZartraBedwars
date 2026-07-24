package io.zartra.bedwars.statistics.leaderboard;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Deterministic leaderboard definitions, entries and bounded result pages. */
public final class LeaderboardContracts {
    private LeaderboardContracts() { }
    /** Ordering supported by this neutral foundation. */ public enum Sort { DESCENDING, ASCENDING }
    /** Immutable definition that names both a statistic and a segregating scope. */ public static final class Definition { private final StatisticId statisticId; private final StatisticScope scope; private final Sort sort;
        /** Creates a deterministic leaderboard definition. */ public Definition(final StatisticId statisticId, final StatisticScope scope, final Sort sort) { this.statisticId = Objects.requireNonNull(statisticId, "statisticId"); this.scope = Objects.requireNonNull(scope, "scope"); this.sort = Objects.requireNonNull(sort, "sort"); }
        /** @return ranked statistic */ public StatisticId statisticId() { return statisticId; } /** @return segregating scope */ public StatisticScope scope() { return scope; } /** @return sort rule */ public Sort sort() { return sort; }
    }
    /** Immutable rank row with deterministic player-identity tie breaking. */ public static final class Entry { private final PlayerId playerId; private final long value; private final int rank;
        /** Creates a non-negative ranked row. */ public Entry(final PlayerId playerId, final long value, final int rank) { this.playerId = Objects.requireNonNull(playerId, "playerId"); if (value < 0 || rank < 1) { throw new IllegalArgumentException("value must be non-negative and rank positive"); } this.value = value; this.rank = rank; }
        /** @return player identity */ public PlayerId playerId() { return playerId; } /** @return aggregate value */ public long value() { return value; } /** @return one-based rank */ public int rank() { return rank; }
    }
    /** Immutable bounded page with no implicit database query behavior. */ public static final class Page { private final List<Entry> entries; private final int offset; private final int pageSize; private final boolean hasNext;
        /** Creates a bounded page. */ public Page(final List<Entry> entries, final int offset, final int pageSize, final boolean hasNext) { if (offset < 0 || pageSize < 1 || pageSize > 100) { throw new IllegalArgumentException("invalid page bounds"); } final List<Entry> copy = new ArrayList<Entry>(Objects.requireNonNull(entries, "entries")); if (copy.size() > pageSize) { throw new IllegalArgumentException("entries exceed page size"); } this.entries = Collections.unmodifiableList(copy); this.offset = offset; this.pageSize = pageSize; this.hasNext = hasNext; }
        /** @return immutable ordered entries */ public List<Entry> entries() { return entries; } /** @return zero-based offset */ public int offset() { return offset; } /** @return requested bound */ public int pageSize() { return pageSize; } /** @return whether a subsequent page exists */ public boolean hasNext() { return hasNext; }
    }
    /** Creates the canonical comparator used by cache and storage adapters. */ public static Comparator<Entry> comparator(final Sort sort) { final Comparator<Entry> values = Comparator.comparingLong(Entry::value); final Comparator<Entry> ordered = sort == Sort.DESCENDING ? values.reversed() : values; return ordered.thenComparing(entry -> entry.playerId().toString()); }
}
