package io.zartra.bedwars.shop.generator;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;

/** Thread-safe deterministic runtime for one bounded match-local generator. */
public final class GeneratorRuntime {
    /** Hard bound on catch-up work performed by one call. */
    public static final int MAX_GENERATIONS_PER_TICK = 128;
    private final MatchId matchId;
    private final GeneratorConfiguration configuration;
    private final Deque<GeneratorBatch> pending = new ArrayDeque<GeneratorBatch>();
    private GeneratorState.Lifecycle lifecycle = GeneratorState.Lifecycle.STOPPED;
    private long sequence;
    private int pendingUnits;
    private Instant nextGeneration;

    /** Creates a stopped runtime. */
    public GeneratorRuntime(final MatchId matchId, final GeneratorConfiguration configuration) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }
    /** Starts at a deterministic logical instant. Repeated starts are harmless. */
    public synchronized void start(final MatchSnapshot match, final Instant now) {
        requireMatch(match);
        if (match.state() != MatchSnapshot.State.PLAYING) {
            throw new IllegalStateException("generators may start only while a match is playing");
        }
        if (lifecycle == GeneratorState.Lifecycle.CLEANED) {
            throw new IllegalStateException("cleaned generator cannot restart");
        }
        if (lifecycle == GeneratorState.Lifecycle.STOPPED) {
            lifecycle = GeneratorState.Lifecycle.RUNNING;
            nextGeneration = Objects.requireNonNull(now, "now").plus(configuration.interval());
        }
    }
    /** Enables or disables scheduling without discarding pending deliveries. */
    public synchronized void setRunning(final boolean enabled, final Instant now) {
        if (lifecycle == GeneratorState.Lifecycle.CLEANED) { throw new IllegalStateException("generator is cleaned"); }
        if (enabled && lifecycle == GeneratorState.Lifecycle.STOPPED) {
            lifecycle = GeneratorState.Lifecycle.RUNNING;
            nextGeneration = Objects.requireNonNull(now, "now").plus(configuration.interval());
        } else if (!enabled) {
            lifecycle = GeneratorState.Lifecycle.STOPPED;
            nextGeneration = null;
        }
    }
    /** Generates due batches and attempts queued deliveries; work is strictly bounded. */
    public synchronized int tick(final MatchSnapshot match, final Instant now,
                                 final ResourceDeliveryPort delivery) {
        requireMatch(match);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(delivery, "delivery");
        if (match.state() != MatchSnapshot.State.PLAYING) {
            cleanup();
            return 0;
        }
        if (lifecycle != GeneratorState.Lifecycle.RUNNING || !configuration.enabled()) { return 0; }
        int generated = 0;
        while (generated < MAX_GENERATIONS_PER_TICK && !now.isBefore(nextGeneration)
                && pendingUnits + configuration.amount() <= configuration.capacity()) {
            sequence++;
            final String generatorDigest = UUID.nameUUIDFromBytes(configuration.id().toString()
                    .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
            final IdempotencyKey key = IdempotencyKey.of("zartra", "gen/"
                    + matchId.toString().replace("-", "") + "/" + generatorDigest + "/" + sequence);
            pending.addLast(new GeneratorBatch(key, matchId, configuration.id(),
                    configuration.resource(), sequence, configuration.amount(), nextGeneration));
            pendingUnits += configuration.amount();
            nextGeneration = nextGeneration.plus(configuration.interval());
            generated++;
        }
        deliverPending(delivery);
        return generated;
    }
    private void deliverPending(final ResourceDeliveryPort delivery) {
        int attempts = 0;
        while (!pending.isEmpty() && attempts++ < MAX_GENERATIONS_PER_TICK) {
            final GeneratorBatch batch = pending.peekFirst();
            final ResourceDeliveryPort.Result result = Objects.requireNonNull(
                    delivery.deliver(configuration, batch), "delivery result");
            if (result == ResourceDeliveryPort.Result.RETRY) { return; }
            pending.removeFirst();
            pendingUnits -= batch.amount();
        }
    }
    /** Permanently removes scheduling and queued match resources on match cleanup/reset. */
    public synchronized void cleanup() {
        pending.clear();
        pendingUnits = 0;
        nextGeneration = null;
        lifecycle = GeneratorState.Lifecycle.CLEANED;
    }
    /** @return immutable state snapshot */
    public synchronized GeneratorState state() {
        return new GeneratorState(lifecycle, sequence, pendingUnits, nextGeneration);
    }
    /** @return immutable configuration */ public GeneratorConfiguration configuration() { return configuration; }
    private void requireMatch(final MatchSnapshot match) {
        if (!matchId.equals(Objects.requireNonNull(match, "match").matchId())) {
            throw new IllegalArgumentException("match identity does not own this generator");
        }
    }
}
