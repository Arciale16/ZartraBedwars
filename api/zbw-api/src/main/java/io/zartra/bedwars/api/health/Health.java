package io.zartra.bedwars.api.health;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Public health and bounded-cardinality metric contracts. */
public final class Health {
    private Health() { throw new AssertionError("No instances"); }
    /** Health severity. */
    public enum Status {
        /** Meets objectives. */ HEALTHY,
        /** Usable outside a non-critical objective. */ DEGRADED,
        /** Temporarily unavailable. */ UNAVAILABLE
    }
    /** Thread-safe non-blocking source. */
    public interface Source {
        /** @return stable component ID */ DefinitionId id();
        /** @return fast secret-safe snapshot */ Snapshot snapshot();
    }
    /** Immutable health snapshot. */
    public static final class Snapshot {
        private final DefinitionId componentId;
        private final Status status;
        private final DefinitionId reasonCode;
        private final Instant observedAt;
        /** Creates validated health metadata. */
        public Snapshot(final DefinitionId componentId, final Status status,
                        final DefinitionId reasonCode, final Instant observedAt) {
            this.componentId = Objects.requireNonNull(componentId, "componentId");
            this.status = Objects.requireNonNull(status, "status");
            this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
            this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
        }
        /** @return component ID */ public DefinitionId componentId() { return componentId; }
        /** @return severity */ public Status status() { return status; }
        /** @return stable reason */ public DefinitionId reasonCode() { return reasonCode; }
        /** @return observation time */ public Instant observedAt() { return observedAt; }
    }
    /** Immutable metric sample whose dimensions were bounded by a registry. */
    public static final class Metric {
        private final DefinitionId metricId;
        private final long value;
        private final Map<String, String> dimensions;
        /** Creates one validated sample. */
        public Metric(final DefinitionId metricId, final long value,
                      final Map<String, String> dimensions) {
            this.metricId = Objects.requireNonNull(metricId, "metricId");
            final Map<String, String> copy = new TreeMap<String, String>();
            for (Map.Entry<String, String> entry
                    : Objects.requireNonNull(dimensions, "dimensions").entrySet()) {
                if (!valid(entry.getKey()) || !valid(entry.getValue())) {
                    throw new IllegalArgumentException("metric dimensions require safe bounded labels");
                }
                copy.put(entry.getKey(), entry.getValue());
            }
            this.value = value;
            this.dimensions = Collections.unmodifiableMap(copy);
        }
        private static boolean valid(final String value) {
            return value != null && value.matches("[a-zA-Z0-9_.-]{1,64}");
        }
        /** @return metric ID */ public DefinitionId metricId() { return metricId; }
        /** @return value */ public long value() { return value; }
        /** @return sorted dimensions */ public Map<String, String> dimensions() { return dimensions; }
    }
}
