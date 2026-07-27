package io.zartra.bedwars.statistics.model;

/** Stable categories used for filtering and policy assignment. */
public enum StatisticCategory {
    /** Match result facts such as wins and losses. */
    MATCH,
    /** Combat facts such as kills and deaths. */
    COMBAT,
    /** Objective facts such as beds destroyed. */
    OBJECTIVE,
    /** Match resource collection facts. */
    RESOURCE,
    /** Shop and economic facts. */
    ECONOMY,
    /** Current and historical streak facts. */
    STREAK,
    /** Registered extension-owned fact. */
    CUSTOM
}
