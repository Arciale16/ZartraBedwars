package io.zartra.bedwars.statistics.leaderboard;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Repository-backed, deterministic leaderboard query and bounded rebuild runtime. */
public final class StatisticsLeaderboardRuntime {
    private final Store store;

    /** Creates a runtime using an explicit storage adapter. */
    public StatisticsLeaderboardRuntime(final Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Loads, filters, ranks and pages existing aggregate rows without recalculating them. */
    public Result<Page> query(final UnitOfWork unitOfWork, final Query query) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        Objects.requireNonNull(query, "query");
        final Result<List<Aggregate>> loaded = store.load(unitOfWork, query);
        if (loaded.isFailure()) {
            return Result.failure(loaded.error().get());
        }
        final List<Aggregate> filtered = new ArrayList<Aggregate>();
        for (Aggregate aggregate : loaded.requireValue()) {
            final Aggregate checked = Objects.requireNonNull(aggregate, "stored aggregate");
            if (checked.kind() != query.kind() || !checked.statisticId().equals(query.statisticId())
                    || !checked.scope().equals(query.scope())
                    || (!query.subjectFilter().isEmpty()
                    && !query.subjectFilter().contains(checked.subject()))) {
                continue;
            }
            filtered.add(checked);
        }
        Collections.sort(filtered, aggregateComparator(query.sort()));
        final int from = Math.min(query.offset(), filtered.size());
        final int to = Math.min(from + query.pageSize(), filtered.size());
        final List<Entry> entries = new ArrayList<Entry>();
        for (int index = from; index < to; index++) {
            final Aggregate aggregate = filtered.get(index);
            entries.add(new Entry(aggregate.subject(), aggregate.value(), index + 1));
        }
        return Result.success(new Page(entries, query.offset(), query.pageSize(),
                to < filtered.size()));
    }

    /** Rebuilds a bounded persisted ranking view through the storage adapter. */
    public Result<RebuildResult> rebuild(final UnitOfWork unitOfWork,
                                         final RebuildRequest request) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        Objects.requireNonNull(request, "request");
        return store.rebuild(unitOfWork, request);
    }

    /** Checks one persisted ranking view without changing the underlying aggregates. */
    public Result<Consistency> check(final UnitOfWork unitOfWork, final Query query) {
        final Result<Page> page = query(unitOfWork, query);
        if (page.isFailure()) {
            return Result.failure(page.error().get());
        }
        int expectedRank = query.offset() + 1;
        for (Entry entry : page.requireValue().entries()) {
            if (entry.rank() != expectedRank++) {
                return Result.success(Consistency.inconsistent());
            }
        }
        return Result.success(Consistency.consistent(page.requireValue().entries().size()));
    }

    private static Comparator<Aggregate> aggregateComparator(final Sort sort) {
        final Comparator<Aggregate> values = new Comparator<Aggregate>() {
            @Override
            public int compare(final Aggregate left, final Aggregate right) {
                return Long.compare(left.value(), right.value());
            }
        };
        final Comparator<Aggregate> ordered = sort == Sort.DESCENDING ? values.reversed() : values;
        return new Comparator<Aggregate>() {
            @Override
            public int compare(final Aggregate left, final Aggregate right) {
                final int valueOrder = ordered.compare(left, right);
                return valueOrder != 0 ? valueOrder
                        : left.subject().stableKey().compareTo(right.subject().stableKey());
            }
        };
    }

    /** Aggregate ownership dimension. */
    public enum Kind {
        /** Player lifetime or scoped aggregate. */ PLAYER,
        /** Arena-defined team aggregate. */ TEAM,
        /** Player aggregate partitioned by an immutable season. */ SEASONAL
    }

    /** Explicit sorting direction. */
    public enum Sort {
        /** Highest values rank first. */ DESCENDING,
        /** Lowest values rank first. */ ASCENDING
    }

    /** Immutable typed ranking subject. */
    public static final class Subject {
        private final Kind kind;
        private final PlayerId playerId;
        private final DefinitionId teamId;

        private Subject(final Kind kind, final PlayerId playerId, final DefinitionId teamId) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.playerId = playerId;
            this.teamId = teamId;
            if ((kind == Kind.TEAM) != (teamId != null)
                    || (kind != Kind.TEAM) != (playerId != null)) {
                throw new IllegalArgumentException("subject identity does not match aggregate kind");
            }
        }

        /** @return player ranking subject */
        public static Subject player(final PlayerId playerId) {
            return new Subject(Kind.PLAYER, Objects.requireNonNull(playerId, "playerId"), null);
        }

        /** @return team ranking subject */
        public static Subject team(final DefinitionId teamId) {
            return new Subject(Kind.TEAM, null, Objects.requireNonNull(teamId, "teamId"));
        }

        /** @return seasonal player ranking subject */
        public static Subject seasonalPlayer(final PlayerId playerId) {
            return new Subject(Kind.SEASONAL, Objects.requireNonNull(playerId, "playerId"), null);
        }

        /** @return aggregate dimension */
        public Kind kind() { return kind; }
        /** @return player identity when this is a player or seasonal subject */
        public Optional<PlayerId> playerId() { return Optional.ofNullable(playerId); }
        /** @return team identity when this is a team subject */
        public Optional<DefinitionId> teamId() { return Optional.ofNullable(teamId); }
        /** @return deterministic storage and tie-breaking key */
        public String stableKey() { return playerId != null ? playerId.toString() : teamId.toString(); }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Subject)) {
                return false;
            }
            final Subject that = (Subject) other;
            return kind == that.kind && Objects.equals(playerId, that.playerId)
                    && Objects.equals(teamId, that.teamId);
        }

        @Override
        public int hashCode() { return Objects.hash(kind, playerId, teamId); }
    }

    /** Immutable persisted aggregate row returned by a repository adapter. */
    public static final class Aggregate {
        private final Subject subject;
        private final StatisticId statisticId;
        private final StatisticScope scope;
        private final long value;

        /** Creates a non-negative reusable aggregate view. */
        public Aggregate(final Subject subject, final StatisticId statisticId,
                         final StatisticScope scope, final long value) {
            this.subject = Objects.requireNonNull(subject, "subject");
            this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
            this.scope = Objects.requireNonNull(scope, "scope");
            if (value < 0) {
                throw new IllegalArgumentException("value must be non-negative");
            }
            this.value = value;
        }

        /** @return typed aggregate owner */ public Subject subject() { return subject; }
        /** @return statistic definition */ public StatisticId statisticId() { return statisticId; }
        /** @return scope or immutable season partition */ public StatisticScope scope() { return scope; }
        /** @return persisted aggregate value */ public long value() { return value; }
        /** @return aggregate dimension */ public Kind kind() { return subject.kind(); }
    }

    /** Immutable repository query with bounded pagination and an optional exact subject filter. */
    public static final class Query {
        private final Kind kind;
        private final StatisticId statisticId;
        private final StatisticScope scope;
        private final Sort sort;
        private final int offset;
        private final int pageSize;
        private final Set<Subject> subjectFilter;

        /** Creates a bounded ranking query. */
        public Query(final Kind kind, final StatisticId statisticId, final StatisticScope scope,
                     final Sort sort, final int offset, final int pageSize,
                     final Collection<Subject> subjectFilter) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
            this.scope = Objects.requireNonNull(scope, "scope");
            this.sort = Objects.requireNonNull(sort, "sort");
            if (offset < 0 || pageSize < 1 || pageSize > 100) {
                throw new IllegalArgumentException("invalid page bounds");
            }
            this.offset = offset;
            this.pageSize = pageSize;
            final LinkedHashSet<Subject> filter = new LinkedHashSet<Subject>(
                    Objects.requireNonNull(subjectFilter, "subjectFilter"));
            if (filter.contains(null)) {
                throw new IllegalArgumentException("subjectFilter must not contain null");
            }
            for (Subject subject : filter) {
                if (subject.kind() != kind) {
                    throw new IllegalArgumentException("subject filter kind does not match query");
                }
            }
            this.subjectFilter = Collections.unmodifiableSet(filter);
        }

        /** @return aggregate dimension */ public Kind kind() { return kind; }
        /** @return selected statistic */ public StatisticId statisticId() { return statisticId; }
        /** @return selected scope/season */ public StatisticScope scope() { return scope; }
        /** @return ordering rule */ public Sort sort() { return sort; }
        /** @return zero-based result offset */ public int offset() { return offset; }
        /** @return requested page bound */ public int pageSize() { return pageSize; }
        /** @return immutable exact subject filter */ public Set<Subject> subjectFilter() { return subjectFilter; }
    }

    /** Immutable ranked entry with a stable one-based absolute rank. */
    public static final class Entry {
        private final Subject subject;
        private final long value;
        private final int rank;
        /** Creates one ranked aggregate row. */
        public Entry(final Subject subject, final long value, final int rank) {
            this.subject = Objects.requireNonNull(subject, "subject");
            if (value < 0 || rank < 1) {
                throw new IllegalArgumentException("value must be non-negative and rank positive");
            }
            this.value = value;
            this.rank = rank;
        }
        /** @return typed subject */ public Subject subject() { return subject; }
        /** @return aggregate value */ public long value() { return value; }
        /** @return one-based absolute rank */ public int rank() { return rank; }
    }

    /** Immutable bounded response page. */
    public static final class Page {
        private final List<Entry> entries;
        private final int offset;
        private final int pageSize;
        private final boolean hasNext;
        /** Creates one bounded response page. */
        public Page(final Collection<Entry> entries, final int offset, final int pageSize,
                    final boolean hasNext) {
            if (offset < 0 || pageSize < 1 || pageSize > 100) {
                throw new IllegalArgumentException("invalid page bounds");
            }
            final List<Entry> copy = new ArrayList<Entry>(Objects.requireNonNull(entries, "entries"));
            if (copy.contains(null) || copy.size() > pageSize) {
                throw new IllegalArgumentException("entries are invalid for page bounds");
            }
            this.entries = Collections.unmodifiableList(copy);
            this.offset = offset;
            this.pageSize = pageSize;
            this.hasNext = hasNext;
        }
        /** @return immutable rank rows */ public List<Entry> entries() { return entries; }
        /** @return zero-based offset */ public int offset() { return offset; }
        /** @return requested bound */ public int pageSize() { return pageSize; }
        /** @return whether another page exists */ public boolean hasNext() { return hasNext; }
    }

    /** Explicit bounded persisted-ranking rebuild request. */
    public static final class RebuildRequest {
        private final Query query;
        private final int maximumRows;
        /** Creates a bounded rebuild request. */
        public RebuildRequest(final Query query, final int maximumRows) {
            this.query = Objects.requireNonNull(query, "query");
            if (maximumRows < 1 || maximumRows > 100000) {
                throw new IllegalArgumentException("maximumRows must be within 1..100000");
            }
            this.maximumRows = maximumRows;
        }
        /** @return target query dimensions */ public Query query() { return query; }
        /** @return explicit work bound */ public int maximumRows() { return maximumRows; }
    }

    /** Immutable rebuild result. */
    public static final class RebuildResult {
        private final int rebuiltRows;
        private final boolean truncated;
        /** Creates a non-negative bounded rebuild result. */
        public RebuildResult(final int rebuiltRows, final boolean truncated) {
            if (rebuiltRows < 0) {
                throw new IllegalArgumentException("rebuiltRows must be non-negative");
            }
            this.rebuiltRows = rebuiltRows;
            this.truncated = truncated;
        }
        /** @return number of persisted ranking rows rebuilt */ public int rebuiltRows() { return rebuiltRows; }
        /** @return whether the explicit bound stopped the rebuild */ public boolean truncated() { return truncated; }
    }

    /** Immutable persisted-view consistency result. */
    public static final class Consistency {
        private final boolean consistent;
        private final int checkedRows;
        private Consistency(final boolean consistent, final int checkedRows) {
            this.consistent = consistent;
            this.checkedRows = checkedRows;
        }
        private static Consistency consistent(final int checkedRows) {
            return new Consistency(true, checkedRows);
        }
        private static Consistency inconsistent() { return new Consistency(false, 0); }
        /** @return whether the checked ranking order and ranks are valid */ public boolean consistent() { return consistent; }
        /** @return number of rows checked when consistent */ public int checkedRows() { return checkedRows; }
    }

    /** SQL and future provider boundary; implementations own storage queries and rebuild writes. */
    public interface Store {
        /** @return matching persisted aggregate rows */
        Result<List<Aggregate>> load(UnitOfWork unitOfWork, Query query);
        /** @return bounded persisted-view rebuild result */
        Result<RebuildResult> rebuild(UnitOfWork unitOfWork, RebuildRequest request);
    }
}
