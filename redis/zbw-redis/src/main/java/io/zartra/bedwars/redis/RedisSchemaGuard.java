package io.zartra.bedwars.redis;


import io.zartra.bedwars.redis.api.RedisKey;

import io.zartra.bedwars.redis.api.RedisNamespace;

import io.zartra.bedwars.redis.api.SchemaVersion;

import java.util.Collections;

import java.util.HashSet;

import java.util.Objects;

import java.util.Set;


/** Fail-closed namespace and rolling-schema guard. */
public final class RedisSchemaGuard {
    private final RedisNamespace namespace;

    private final Set<SchemaVersion> accepted;

    /** Creates a guard with an explicit finite accepted schema set. */
    public RedisSchemaGuard(final RedisNamespace namespace, final Set<SchemaVersion> accepted) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");

        if (accepted == null || accepted.isEmpty() || !accepted.contains(namespace.schema())) {
            throw new IllegalArgumentException("active schema must be accepted");

        }
        this.accepted = Collections.unmodifiableSet(new HashSet<SchemaVersion>(accepted));

    }
    /** Rejects foreign namespace or unknown schema. */
    public void requireAccepted(final RedisKey key, final SchemaVersion schema) {
        if (!namespace.equals(Objects.requireNonNull(key, "key").namespace())) {
            throw new SecurityException("foreign Redis namespace");

        }
        if (!accepted.contains(Objects.requireNonNull(schema, "schema"))) {
            throw new IllegalArgumentException("unknown Redis schema");

        }
    }
    /** Returns active namespace. */ public RedisNamespace namespace() { return namespace;
 }
}
