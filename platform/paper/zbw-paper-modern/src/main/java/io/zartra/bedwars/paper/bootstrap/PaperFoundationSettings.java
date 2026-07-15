package io.zartra.bedwars.paper.bootstrap;

import java.time.Duration;
import org.bukkit.configuration.file.FileConfiguration;

/** Immutable validated M06 runtime settings loaded from Paper configuration. */
public final class PaperFoundationSettings {
    private final int workers;
    private final int queueCapacity;
    private final int maximumInFlightWorlds;
    private final int maximumTrackedWorlds;
    private final Duration operationTimeout;

    private PaperFoundationSettings(final int workers, final int queueCapacity,
                                    final int maximumInFlightWorlds,
                                    final int maximumTrackedWorlds,
                                    final Duration operationTimeout) {
        this.workers = range(workers, 1, 8, "world.worker-count");
        this.queueCapacity = range(queueCapacity, 1, 256, "world.queue-capacity");
        this.maximumInFlightWorlds = range(maximumInFlightWorlds, 1, 64,
                "world.maximum-in-flight");
        this.maximumTrackedWorlds = range(maximumTrackedWorlds, maximumInFlightWorlds, 256,
                "world.maximum-tracked");
        final long seconds = operationTimeout.getSeconds();
        if (seconds < 1L || seconds > 300L || operationTimeout.getNano() != 0) {
            throw new IllegalArgumentException(
                    "world.operation-timeout-seconds must be a whole value between 1 and 300");
        }
        this.operationTimeout = Duration.ofSeconds(seconds);
    }

    /** Reads and validates all bounded M06 settings. */
    public static PaperFoundationSettings from(final FileConfiguration configuration) {
        return new PaperFoundationSettings(configuration.getInt("world.worker-count"),
                configuration.getInt("world.queue-capacity"),
                configuration.getInt("world.maximum-in-flight"),
                configuration.getInt("world.maximum-tracked"),
                Duration.ofSeconds(configuration.getInt("world.operation-timeout-seconds")));
    }

    /** Creates validated settings for composition tests and non-YAML embedding. */
    public static PaperFoundationSettings of(final int workers, final int queueCapacity,
                                             final int maximumInFlightWorlds,
                                             final int maximumTrackedWorlds,
                                             final Duration operationTimeout) {
        return new PaperFoundationSettings(workers, queueCapacity, maximumInFlightWorlds,
                maximumTrackedWorlds, operationTimeout);
    }

    private static int range(final int value, final int minimum, final int maximum,
                             final String key) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    /** @return bounded worker count */ public int workers() { return workers; }
    /** @return bounded scheduler queue */ public int queueCapacity() { return queueCapacity; }
    /** @return bounded concurrent operations */ public int maximumInFlightWorlds() { return maximumInFlightWorlds; }
    /** @return bounded neutral world snapshots */ public int maximumTrackedWorlds() { return maximumTrackedWorlds; }
    /** @return total operation deadline */ public Duration operationTimeout() { return operationTimeout; }
}
