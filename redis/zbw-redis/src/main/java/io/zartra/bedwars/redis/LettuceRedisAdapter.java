package io.zartra.bedwars.redis;


import io.lettuce.core.codec.ByteArrayCodec;

import io.lettuce.core.RedisClient;

import io.lettuce.core.codec.RedisCodec;

import io.lettuce.core.RedisURI;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;

import io.lettuce.core.SetArgs;

import io.lettuce.core.codec.StringCodec;

import io.lettuce.core.api.StatefulRedisConnection;

import io.lettuce.core.api.async.RedisAsyncCommands;

import io.zartra.bedwars.redis.api.DeduplicationKey;

import io.zartra.bedwars.redis.api.FencingToken;

import io.zartra.bedwars.redis.api.RedisAvailability;

import io.zartra.bedwars.redis.api.RedisHealth;

import io.zartra.bedwars.redis.api.RedisKey;

import io.zartra.bedwars.redis.api.DegradationMode;

import java.nio.charset.StandardCharsets;

import java.time.Clock;

import java.time.Instant;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;

import java.util.Objects;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.CompletionStage;

import java.util.concurrent.RejectedExecutionException;

import java.util.concurrent.ThreadFactory;

import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.ArrayBlockingQueue;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.Function;


/** Bounded nonblocking Lettuce lifecycle and coordination adapter. */
public final class LettuceRedisAdapter implements AutoCloseable {
    private static final byte[] ONE = new byte[] {1};

    private static final String ACQUIRE_SCRIPT = "local token=redis.call('incr',KEYS[2]); if redis.call('set',KEYS[1],ARGV[1]..':'..token,'NX','PX',ARGV[2]) then return token else return 0 end";

    private static final String RENEW_SCRIPT = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('pexpire',KEYS[1],ARGV[2]) else return 0 end";

    private final RedisAdapterConfig config;
 private final Clock clock;
 private final RedisCircuitBreaker breaker;
 private final RedisClient client;
 private final ThreadPoolExecutor callbackExecutor;
 private final List<StatefulRedisConnection<String, byte[]>> connections = Collections.synchronizedList(new ArrayList<StatefulRedisConnection<String, byte[]>>());
 private final AtomicInteger cursor = new AtomicInteger();

    /** Creates an unopened adapter;
 no connection or thread is started by construction. */
    public LettuceRedisAdapter(final RedisAdapterConfig config, final Clock clock, final RedisCircuitBreaker breaker) {
        this.config = Objects.requireNonNull(config, "config");
 this.clock = Objects.requireNonNull(clock, "clock");
 this.breaker = Objects.requireNonNull(breaker, "breaker");

        client = RedisClient.create(redisUri());

        callbackExecutor = new ThreadPoolExecutor(1, Math.min(4, config.connections()), 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(config.queueCapacity()), new NamedThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    }
    /** Opens the fixed pool asynchronously, never blocking the caller. */
    public CompletionStage<Void> start() {
        final List<CompletableFuture<?>> futures = new ArrayList<CompletableFuture<?>>();

        final RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

        for (int i = 0; i < config.connections(); i++) {
            futures.add(client.connectAsync(codec, redisUri()).toCompletableFuture().thenAccept(connections::add));

        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));

    }
    /** Executes against a pooled connection and shifts completion off Lettuce event loops. */
    public <T> CompletionStage<T> execute(final Function<RedisAsyncCommands<String, byte[]>, CompletionStage<T>> operation) {
        if (!breaker.allowRequest()) { return failed(new IllegalStateException("Redis circuit open"));
 }
        if (connections.isEmpty()) { return failed(new IllegalStateException("Redis adapter not started"));
 }
        final CompletableFuture<T> result = new CompletableFuture<T>();

        try {
            callbackExecutor.execute(new Runnable() { @Override public void run() {
                final int index = Math.floorMod(cursor.getAndIncrement(), connections.size());
                try {
                    operation.apply(connections.get(index).async()).whenCompleteAsync((value, failure) -> {
                        if (failure == null) {
                            breaker.success();
                            result.complete(value);
                        } else {
                            breaker.failure();
                            result.completeExceptionally(failure);
                        }
                    }, callbackExecutor);
                } catch (RuntimeException failure) {
                    breaker.failure();
                    result.completeExceptionally(failure);
                }
            } });

        } catch (RejectedExecutionException full) { result.completeExceptionally(new RejectedExecutionException("Redis queue capacity exceeded", full));
 }
        return result;

    }
    /** Uses Pub/Sub only for disposable invalidation/notification bytes. */
    public CompletionStage<Long> publish(final RedisKey channel, final byte[] payload) { requireKey(channel);
 final byte[] checked = copyPayload(payload);
 return execute(commands -> commands.publish(channel.qualified(), checked).toCompletableFuture());
 }
    /** Creates a stream consumer group while preserving existing records. */
    public CompletionStage<String> createConsumerGroup(final RedisKey stream, final String group) {
        requireKey(stream);
        return execute(commands -> commands.xgroupCreate(
                XReadArgs.StreamOffset.from(stream.qualified(), "0-0"), group,
                XGroupCreateArgs.Builder.mkstream()).toCompletableFuture());
    }

    /** Reads one bounded batch from a consumer group. */
    @SuppressWarnings("unchecked") // Lettuce exposes stream offsets through a generic varargs API.
    public CompletionStage<List<StreamMessage<String, byte[]>>> readGroup(
            final RedisKey stream, final String group, final String consumer, final int count) {
        requireKey(stream);
        if (count < 1 || count > 1000) {
            return failed(new IllegalArgumentException("stream read count outside 1..1000"));
        }
        return execute(commands -> commands.xreadgroup(
                Consumer.from(group, consumer), XReadArgs.Builder.count(count),
                XReadArgs.StreamOffset.lastConsumed(stream.qualified())).toCompletableFuture());
    }

    /** Acknowledges records only after successful infrastructure processing. */
    public CompletionStage<Long> acknowledge(
            final RedisKey stream, final String group, final String... ids) {
        requireKey(stream);
        if (ids == null || ids.length == 0) {
            return failed(new IllegalArgumentException("at least one stream ID required"));
        }
        return execute(commands -> commands.xack(stream.qualified(), group, ids).toCompletableFuture());
    }
    /** Records an operation ID for 24 hours;
 false means duplicate. */
    public CompletionStage<Boolean> deduplicate(final DeduplicationKey key) { requireKey(key.asRedisKey());
 return execute(commands -> commands.set(key.toString(), ONE, SetArgs.Builder.nx().px(RedisDeduplicationStore.RETENTION.toMillis())).toCompletableFuture().thenApply("OK"::equals));
 }
    /** Acquires a fenced short lease atomically;
 zero means conflict. */
    public CompletionStage<FencingToken> acquireLease(final RedisKey lease, final String holder, final long ttlMillis) {
        requireKey(lease);
 if (ttlMillis < 1) { return failed(new IllegalArgumentException("positive lease TTL required"));
 }
        final String fence = lease.qualified() + ":fence";

        return execute(commands -> commands.eval(ACQUIRE_SCRIPT, ScriptOutputType.INTEGER, new String[] {lease.qualified(), fence}, holder.getBytes(StandardCharsets.UTF_8), Long.toString(ttlMillis).getBytes(StandardCharsets.UTF_8)).toCompletableFuture().thenApply(value -> ((Number) value).longValue() == 0L ? null : FencingToken.of(((Number) value).longValue())));

    }
    /** Renews only the exact holder+fencing epoch. */
    public CompletionStage<Boolean> renewLease(final RedisKey lease, final String holder, final FencingToken token, final long ttlMillis) {
        requireKey(lease);
 final byte[] expected = (holder + ":" + token.value()).getBytes(StandardCharsets.UTF_8);

        return execute(commands -> commands.eval(RENEW_SCRIPT, ScriptOutputType.INTEGER, new String[] {lease.qualified()}, expected, Long.toString(ttlMillis).getBytes(StandardCharsets.UTF_8)).toCompletableFuture().thenApply(value -> ((Number) value).longValue() == 1L));

    }
    /** Reports sanitized health asynchronously. */
    public CompletionStage<RedisHealth> health() {
        if (connections.isEmpty()) { return CompletableFuture.completedFuture(RedisHealth.of(RedisAvailability.UNAVAILABLE, DegradationMode.CROSS_NODE_PAUSED, "not_started", callbackExecutor.getQueue().size(), Instant.ofEpochMilli(clock.millis())));
 }
        return execute(commands -> commands.ping().toCompletableFuture()).handle((pong, failure) -> failure == null && "PONG".equals(pong) ? RedisHealth.of(RedisAvailability.AVAILABLE, DegradationMode.NORMAL, "ok", callbackExecutor.getQueue().size(), Instant.ofEpochMilli(clock.millis())) : RedisHealth.of(RedisAvailability.DEGRADED, DegradationMode.CROSS_NODE_PAUSED, "ping_failed", callbackExecutor.getQueue().size(), Instant.ofEpochMilli(clock.millis())));

    }
    private RedisURI redisUri() { final RedisURI value = RedisURI.create(config.uri().toString());
 value.setTimeout(config.commandTimeout());
 return value;
 }
    private void requireKey(final RedisKey key) { if (!config.namespace().equals(Objects.requireNonNull(key, "key").namespace())) { throw new SecurityException("foreign Redis namespace");
 } }
    private static byte[] copyPayload(final byte[] payload) { if (payload == null || payload.length > 256 * 1024) { throw new IllegalArgumentException("invalid Redis payload size");
 } return java.util.Arrays.copyOf(payload, payload.length);
 }
    private static <T> CompletionStage<T> failed(final Throwable failure) { final CompletableFuture<T> result = new CompletableFuture<T>();
 result.completeExceptionally(failure);
 return result;
 }
    /** Initiates nonblocking connection/client shutdown and stops the bounded callback executor. */
    @Override public void close() { synchronized (connections) { for (StatefulRedisConnection<String, byte[]> connection : connections) { connection.closeAsync();
 } connections.clear();
 } client.shutdownAsync();
 callbackExecutor.shutdownNow();
 }
    private static final class NamedThreadFactory implements ThreadFactory { private final AtomicInteger sequence = new AtomicInteger();
 @Override public Thread newThread(final Runnable task) { final Thread thread = new Thread(task, "zbw-redis-callback-" + sequence.incrementAndGet());
 thread.setDaemon(true);
 return thread;
 } }
}
