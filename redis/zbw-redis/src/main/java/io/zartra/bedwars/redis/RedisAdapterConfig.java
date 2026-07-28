package io.zartra.bedwars.redis;


import io.zartra.bedwars.redis.api.RedisNamespace;

import java.net.URI;

import java.time.Duration;

import java.util.Objects;


/** Validated immutable Redis adapter limits and connection settings. */
public final class RedisAdapterConfig {
    /** Hard backend connection ceiling. */ public static final int MAX_CONNECTIONS = 16;

    /** Hard queued operation ceiling. */ public static final int MAX_QUEUE = 5000;

    private final URI uri;

    private final RedisNamespace namespace;

    private final int connections;

    private final int queueCapacity;

    private final Duration commandTimeout;

    private RedisAdapterConfig(final URI uri, final RedisNamespace namespace, final int connections,
                               final int queueCapacity, final Duration commandTimeout) {
        this.uri = Objects.requireNonNull(uri, "uri");

        this.namespace = Objects.requireNonNull(namespace, "namespace");

        this.commandTimeout = Objects.requireNonNull(commandTimeout, "commandTimeout");

        if (!("redis".equals(uri.getScheme()) || "rediss".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("Redis URI scheme required");

        }
        if (connections < 1 || connections > MAX_CONNECTIONS) {
            throw new IllegalArgumentException("connections outside 1..16");

        }
        if (queueCapacity < 1 || queueCapacity > MAX_QUEUE) {
            throw new IllegalArgumentException("queue capacity outside 1..5000");

        }
        if (commandTimeout.isNegative() || commandTimeout.isZero()) {
            throw new IllegalArgumentException("command timeout must be positive");

        }
        this.connections = connections;

        this.queueCapacity = queueCapacity;

    }
    /** Creates validated adapter settings. */
    public static RedisAdapterConfig of(final URI uri, final RedisNamespace namespace,
                                        final int connections, final int queueCapacity,
                                        final Duration commandTimeout) {
        return new RedisAdapterConfig(uri, namespace, connections, queueCapacity, commandTimeout);

    }
    /** Returns endpoint URI;
 callers must redact credentials. */ public URI uri() { return uri;
 }
    /** Returns enforced namespace. */ public RedisNamespace namespace() { return namespace;
 }
    /** Returns backend connection count. */ public int connections() { return connections;
 }
    /** Returns queue capacity. */ public int queueCapacity() { return queueCapacity;
 }
    /** Returns command timeout. */ public Duration commandTimeout() { return commandTimeout;
 }
}
