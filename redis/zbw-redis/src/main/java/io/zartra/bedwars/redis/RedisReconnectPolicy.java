package io.zartra.bedwars.redis;


import java.time.Duration;

import java.util.Objects;

import java.util.Random;


/** Three-attempt exponential reconnect policy with bounded jitter. */
public final class RedisReconnectPolicy {
    /** Maximum automatic attempts. */ public static final int MAX_ATTEMPTS = 3;

    private final Random random;

    /** Creates a policy with an injected deterministic entropy source. */ public RedisReconnectPolicy(final Random random) { this.random = Objects.requireNonNull(random, "random");
 }
    /** Returns bounded delay for attempt 1..3. */
    public Duration delay(final int attempt) {
        if (attempt < 1 || attempt > MAX_ATTEMPTS) { throw new IllegalArgumentException("attempt outside retry budget");
 }
        final long base = attempt == 1 ? 50L : attempt == 2 ? 150L : 450L;

        return Duration.ofMillis(base + random.nextInt((int) (base / 2L + 1L)));

    }
}
