package io.zartra.bedwars.redis.security;


import java.time.Clock;

import java.util.HashMap;

import java.util.Map;

import java.util.Objects;


/** Synchronized per-peer token bucket: 100/s with burst 200 by default. */
public final class RedisRateLimiter {
    private final Clock clock;
 private final int rate;
 private final int burst;
 private final Map<String, Bucket> buckets = new HashMap<String, Bucket>();

    /** Creates a limiter. */ public RedisRateLimiter(final Clock clock, final int rate, final int burst) { this.clock = Objects.requireNonNull(clock, "clock");
 if (rate < 1 || burst < rate) { throw new IllegalArgumentException("invalid rate policy");
 } this.rate = rate;
 this.burst = burst;
 }
    /** Attempts one admission. */ public synchronized boolean allow(final String peer) { final long now = clock.millis();
 Bucket bucket = buckets.get(peer);
 if (bucket == null) { bucket = new Bucket(burst, now);
 buckets.put(peer, bucket);
 } final long elapsed = now - bucket.at;
 if (elapsed > 0) { bucket.tokens = Math.min(burst, bucket.tokens + elapsed * rate / 1000L);
 bucket.at = now;
 } if (bucket.tokens < 1) { return false;
 } bucket.tokens--;
 return true;
 }
    private static final class Bucket { private long tokens;
 private long at;
 Bucket(final long tokens, final long at) { this.tokens = tokens;
 this.at = at;
 } }
}
