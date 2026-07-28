package io.zartra.bedwars.redis.coordination;

import io.zartra.bedwars.redis.api.FencingToken;
import io.zartra.bedwars.redis.api.RedisAvailability;
import io.zartra.bedwars.redis.api.RedisHealth;
import io.zartra.bedwars.redis.api.RedisKey;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Ephemeral Atlas reservation guard. SQL remains authoritative for assignment and submission.
 */
public final class AtlasReservationCoordinator {
    /** Redis lease operations required by the bridge. */
    public interface LeasePort {
        CompletionStage<FencingToken> acquire(
                RedisKey key, String holder, long ttlMillis);
        CompletionStage<Boolean> renew(
                RedisKey key, String holder, FencingToken token, long ttlMillis);
        CompletionStage<RedisHealth> health();
    }

    /** Result that explicitly distinguishes partition safety from contention. */
    public enum Status {
        ACQUIRED,
        CONFLICT,
        CROSS_NODE_PAUSED
    }

    /** Immutable coordination result; it never represents a durable Atlas reservation. */
    public static final class Result {
        private final Status status;
        private final FencingToken token;

        private Result(final Status status, final FencingToken token) {
            this.status = Objects.requireNonNull(status, "status");
            this.token = token;
        }

        /** @return result status */ public Status status() { return status; }
        /** @return fencing token, or null unless acquired */ public FencingToken token() {
            return token;
        }
    }

    private final LeasePort leases;
    private final int capacity;
    private final Map<RedisKey, FencingToken> accepted = new LinkedHashMap<RedisKey, FencingToken>();

    /** Creates a bounded coordinator. */
    public AtlasReservationCoordinator(final LeasePort leases, final int capacity) {
        this.leases = Objects.requireNonNull(leases, "leases");
        if (capacity < 1 || capacity > 10000) {
            throw new IllegalArgumentException("reservation capacity outside bounds");
        }
        this.capacity = capacity;
    }

    /** Stops unsafe cross-node reservation during degradation, otherwise obtains a fenced lease. */
    public CompletionStage<Result> reserve(final RedisKey caseKey, final String reviewer,
                                           final Duration ttl) {
        final RedisKey key = Objects.requireNonNull(caseKey, "caseKey");
        final String holder = requireReviewer(reviewer);
        final long ttlMillis = requireTtl(ttl);
        synchronized (this) {
            if (!accepted.containsKey(key) && accepted.size() >= capacity) {
                return completed(Status.CONFLICT, null);
            }
        }
        return leases.health().thenCompose(health -> {
            if (health.availability() != RedisAvailability.AVAILABLE) {
                return completed(Status.CROSS_NODE_PAUSED, null);
            }
            return leases.acquire(key, holder, ttlMillis).thenApply(token -> {
                if (token == null) {
                    return new Result(Status.CONFLICT, null);
                }
                synchronized (AtlasReservationCoordinator.this) {
                    final FencingToken previous = accepted.get(key);
                    if (previous != null && token.compareTo(previous) <= 0) {
                        return new Result(Status.CONFLICT, null);
                    }
                    accepted.put(key, token);
                }
                return new Result(Status.ACQUIRED, token);
            });
        }).exceptionally(failure -> new Result(Status.CROSS_NODE_PAUSED, null));
    }

    /** Renews only the latest locally accepted fencing epoch. */
    public CompletionStage<Boolean> renew(final RedisKey key, final String reviewer,
                                          final FencingToken token, final Duration ttl) {
        synchronized (this) {
            if (!Objects.requireNonNull(token, "token").equals(accepted.get(key))) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
        }
        return leases.renew(key, requireReviewer(reviewer), token, requireTtl(ttl))
                .exceptionally(failure -> Boolean.FALSE);
    }

    private static CompletionStage<Result> completed(
            final Status status, final FencingToken token) {
        return CompletableFuture.completedFuture(new Result(status, token));
    }

    private static String requireReviewer(final String reviewer) {
        final String checked = Objects.requireNonNull(reviewer, "reviewer").trim();
        if (checked.isEmpty() || checked.length() > 128) {
            throw new IllegalArgumentException("opaque reviewer reference outside bounds");
        }
        return checked;
    }

    private static long requireTtl(final Duration ttl) {
        final Duration checked = Objects.requireNonNull(ttl, "ttl");
        if (checked.isNegative() || checked.isZero()
                || checked.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException("reservation TTL outside bounds");
        }
        return checked.toMillis();
    }
}
