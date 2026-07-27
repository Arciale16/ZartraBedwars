package io.zartra.bedwars.statistics.model;

import java.util.Objects;

/** Versioned, immutable definition describing how one statistic is aggregated. */
public final class StatisticDefinition {
    /** Supported deterministic aggregation modes. */
    public enum Aggregation { SUM, MAXIMUM, LATEST }
    private final StatisticId id;
    private final StatisticCategory category;
    private final int version;
    private final Aggregation aggregation;
    private final StatisticAudit audit;
    /** Creates a validated definition. */
    public StatisticDefinition(final StatisticId id, final StatisticCategory category, final int version,
                               final Aggregation aggregation, final StatisticAudit audit) {
        this.id = Objects.requireNonNull(id, "id");
        this.category = Objects.requireNonNull(category, "category");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.aggregation = Objects.requireNonNull(aggregation, "aggregation");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return stable definition identity */ public StatisticId id() { return id; }
    /** @return filtering category */ public StatisticCategory category() { return category; }
    /** @return schema version */ public int version() { return version; }
    /** @return aggregation policy */ public Aggregation aggregation() { return aggregation; }
    /** @return immutable audit metadata */ public StatisticAudit audit() { return audit; }
}
