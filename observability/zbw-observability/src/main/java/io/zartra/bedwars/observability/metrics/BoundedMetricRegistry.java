package io.zartra.bedwars.observability.metrics;

import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe metric registry with hard series and dimension bounds. */
public final class BoundedMetricRegistry {
    private final int maximumSeries;
    private final int maximumDimensions;
    private final Map<Key, AtomicLong> values = new TreeMap<Key, AtomicLong>();
    /** Creates a registry. */
    public BoundedMetricRegistry(final int maximumSeries, final int maximumDimensions) {
        if (maximumSeries < 1 || maximumDimensions < 0 || maximumDimensions > 16) {
            throw new IllegalArgumentException("metric bounds are invalid");
        }
        this.maximumSeries = maximumSeries;
        this.maximumDimensions = maximumDimensions;
    }
    /** Increments one bounded counter. */
    public void increment(final DefinitionId id, final Map<String, String> dimensions,
                          final long delta) {
        value(id, dimensions).addAndGet(delta);
    }
    /** Sets one bounded gauge. */
    public void set(final DefinitionId id, final Map<String, String> dimensions, final long value) {
        value(id, dimensions).set(value);
    }
    /** @return deterministic immutable samples */
    public synchronized List<Health.Metric> snapshots() {
        final List<Health.Metric> result = new ArrayList<Health.Metric>(values.size());
        for (Map.Entry<Key, AtomicLong> entry : values.entrySet()) {
            result.add(new Health.Metric(entry.getKey().id, entry.getValue().get(),
                    entry.getKey().dimensions));
        }
        return Collections.unmodifiableList(result);
    }
    /** @return series count */ public synchronized int size() { return values.size(); }
    private synchronized AtomicLong value(final DefinitionId id,
                                          final Map<String, String> dimensions) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dimensions, "dimensions");
        if (dimensions.size() > maximumDimensions) {
            throw new IllegalArgumentException("metric dimension capacity exceeded");
        }
        final Health.Metric validated = new Health.Metric(id, 0L, dimensions);
        final Key key = new Key(id, validated.dimensions());
        AtomicLong result = values.get(key);
        if (result == null) {
            if (values.size() >= maximumSeries) {
                throw new IllegalStateException("metric series capacity exhausted");
            }
            result = new AtomicLong();
            values.put(key, result);
        }
        return result;
    }
    private static final class Key implements Comparable<Key> {
        private final DefinitionId id;
        private final Map<String, String> dimensions;
        private Key(final DefinitionId id, final Map<String, String> dimensions) {
            this.id = id;
            this.dimensions = dimensions;
        }
        @Override public int compareTo(final Key other) {
            final int order = id.compareTo(other.id);
            return order != 0 ? order : dimensions.toString().compareTo(other.dimensions.toString());
        }
    }
}
