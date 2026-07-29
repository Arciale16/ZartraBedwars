package io.zartra.bedwars.cloudnet;

import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic bounded warm-pool scaling policy.
 *
 * <p>ZBW-ADDON-229: consecutive observations, cooldown and bounded actions prevent oscillation.</p>
 */
public final class CloudNetScalingPolicy {
    private final int minimumServices;
    private final int maximumServices;
    private final int warmCapacity;
    private final int scaleUpPercent;
    private final int scaleDownPercent;
    private final int requiredObservations;
    private final int maximumActions;
    private final Duration cooldown;

    /**
     * Creates a scaling policy.
     *
     * @param minimumServices minimum running services
     * @param maximumServices maximum running services
     * @param warmCapacity minimum free slots
     * @param scaleUpPercent utilization percentage that triggers scale-up
     * @param scaleDownPercent utilization percentage that triggers scale-down
     * @param requiredObservations consecutive observations required
     * @param maximumActions maximum actions per reconciliation
     * @param cooldown minimum interval between actions
     */
    public CloudNetScalingPolicy(
            final int minimumServices,
            final int maximumServices,
            final int warmCapacity,
            final int scaleUpPercent,
            final int scaleDownPercent,
            final int requiredObservations,
            final int maximumActions,
            final Duration cooldown) {
        if (minimumServices < 0 || maximumServices < 1 || minimumServices > maximumServices
                || warmCapacity < 0 || scaleDownPercent < 0 || scaleDownPercent > 100
                || scaleUpPercent < 0 || scaleUpPercent > 100
                || scaleDownPercent >= scaleUpPercent
                || requiredObservations < 1 || requiredObservations > 100
                || maximumActions < 1 || maximumActions > 16) {
            throw new IllegalArgumentException("invalid scaling bounds");
        }
        this.minimumServices = minimumServices;
        this.maximumServices = maximumServices;
        this.warmCapacity = warmCapacity;
        this.scaleUpPercent = scaleUpPercent;
        this.scaleDownPercent = scaleDownPercent;
        this.requiredObservations = requiredObservations;
        this.maximumActions = maximumActions;
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("cooldown must not be negative");
        }
    }

    /**
     * Evaluates an immutable observation.
     *
     * @param services deterministically discovered services
     * @param previous prior policy state
     * @param now evaluation time
     * @return decision and next state
     */
    public Evaluation evaluate(
            final List<CloudNetServiceMetadata> services,
            final State previous,
            final Instant now) {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(now, "now");
        int active = 0;
        int capacity = 0;
        int occupancy = 0;
        for (CloudNetServiceMetadata service : services) {
            if (service.state() != ServiceDiscoveryProvider.ServiceState.OFFLINE) {
                active++;
                capacity += service.capacity();
                occupancy += service.occupancy();
            }
        }
        final int utilization = capacity == 0 ? 100 : occupancy * 100 / capacity;
        final int free = capacity - occupancy;
        final boolean up = active < minimumServices
                || active < maximumServices
                && (free < warmCapacity || utilization >= scaleUpPercent);
        final boolean down = active > minimumServices
                && free > warmCapacity && utilization <= scaleDownPercent;
        final Direction direction = up ? Direction.UP : down ? Direction.DOWN : Direction.NONE;
        final int observations = direction == previous.direction()
                ? previous.observations() + 1 : direction == Direction.NONE ? 0 : 1;
        final boolean cooled = !now.isBefore(previous.lastActionAt().plus(cooldown));
        final int available = direction == Direction.UP
                ? maximumServices - active : active - minimumServices;
        final int actions = direction != Direction.NONE
                && observations >= requiredObservations && cooled
                ? Math.min(maximumActions, Math.max(0, available)) : 0;
        final State next = new State(direction, actions > 0 ? 0 : observations,
                actions > 0 ? now : previous.lastActionAt());
        return new Evaluation(direction, actions, next);
    }

    /** Scaling direction. */
    public enum Direction { UP, DOWN, NONE }

    /** Immutable policy memory. */
    public static final class State {
        private final Direction direction;
        private final int observations;
        private final Instant lastActionAt;
        /** Creates policy memory. */
        public State(
                final Direction direction, final int observations, final Instant lastActionAt) {
            this.direction = Objects.requireNonNull(direction, "direction");
            if (observations < 0) {
                throw new IllegalArgumentException("observations must not be negative");
            }
            this.observations = observations;
            this.lastActionAt = Objects.requireNonNull(lastActionAt, "lastActionAt");
        }
        /** @return prior direction */
        public Direction direction() { return direction; }
        /** @return consecutive observation count */
        public int observations() { return observations; }
        /** @return last action time */
        public Instant lastActionAt() { return lastActionAt; }
    }

    /** Immutable scaling evaluation. */
    public static final class Evaluation {
        private final Direction direction;
        private final int actions;
        private final State nextState;
        private Evaluation(final Direction direction, final int actions, final State nextState) {
            this.direction = direction;
            this.actions = actions;
            this.nextState = nextState;
        }
        /** @return selected direction */
        public Direction direction() { return direction; }
        /** @return bounded action count */
        public int actions() { return actions; }
        /** @return next policy memory */
        public State nextState() { return nextState; }
    }
}
