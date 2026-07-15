package io.zartra.bedwars.observability.health;

import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.time.TimeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Thread-safe, capacity-bounded registry of non-blocking health sources. */
public final class BoundedHealthRegistry {
    private static final DefinitionId SOURCE_FAILED =
            DefinitionId.of("zartra", "health/source-failed");
    private final int maximumSources;
    private final TimeSource timeSource;
    private final Map<DefinitionId, Health.Source> sources =
            new TreeMap<DefinitionId, Health.Source>();
    /** Creates a registry. */
    public BoundedHealthRegistry(final int maximumSources, final TimeSource timeSource) {
        if (maximumSources < 1) { throw new IllegalArgumentException("maximumSources must be positive"); }
        this.maximumSources = maximumSources;
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }
    /** Registers one unique source. */
    public synchronized void register(final Health.Source source) {
        Objects.requireNonNull(source, "source");
        final DefinitionId id = Objects.requireNonNull(source.id(), "source.id");
        if (sources.containsKey(id)) { throw new IllegalArgumentException("duplicate health source ID"); }
        if (sources.size() >= maximumSources) { throw new IllegalStateException("health capacity exhausted"); }
        sources.put(id, source);
    }
    /** Removes one source during shutdown. */
    public synchronized boolean unregister(final DefinitionId id) {
        return sources.remove(Objects.requireNonNull(id, "id")) != null;
    }
    /** @return sorted immutable snapshots; failing sources are isolated as unavailable */
    public synchronized List<Health.Snapshot> snapshots() {
        final List<Health.Snapshot> result = new ArrayList<Health.Snapshot>(sources.size());
        for (Map.Entry<DefinitionId, Health.Source> entry : sources.entrySet()) {
            Health.Snapshot snapshot;
            try {
                snapshot = Objects.requireNonNull(entry.getValue().snapshot(), "source snapshot");
            } catch (RuntimeException exception) {
                snapshot = new Health.Snapshot(entry.getKey(), Health.Status.UNAVAILABLE,
                        SOURCE_FAILED, timeSource.now());
            }
            if (!entry.getKey().equals(snapshot.componentId())) {
                throw new IllegalStateException("health source returned another component ID");
            }
            result.add(snapshot);
        }
        return Collections.unmodifiableList(result);
    }
    /** @return source count */ public synchronized int size() { return sources.size(); }
}
