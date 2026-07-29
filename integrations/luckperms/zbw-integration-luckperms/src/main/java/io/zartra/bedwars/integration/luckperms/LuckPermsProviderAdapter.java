package io.zartra.bedwars.integration.luckperms;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.permission.ContextQuery;
import io.zartra.bedwars.api.integration.permission.MetaSnapshot;
import io.zartra.bedwars.api.integration.permission.PermissionProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/** Optional LuckPerms context-aware permission and allow-listed metadata adapter. */
public final class LuckPermsProviderAdapter implements PermissionProvider {
    private static final int MAX_INVALIDATION_VERSIONS = 4096;
    private final Gateway gateway;
    private final TimeSource timeSource;
    private final OptionalProviderLifecycle lifecycle;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<PlayerId, Long> invalidationVersions =
            Collections.synchronizedMap(new LinkedHashMap<PlayerId, Long>(64, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                @Override protected boolean removeEldestEntry(final Map.Entry<PlayerId, Long> row) {
                    return size() > MAX_INVALIDATION_VERSIONS;
                }
            });

    /**
     * Creates the adapter around an operator-supplied LuckPerms binding.
     *
     * @param gateway nonblocking LuckPerms query boundary
     * @param probe plugin presence and API compatibility
     * @param timeSource observation clock
     */
    public LuckPermsProviderAdapter(final Gateway gateway,
                                    final OptionalProviderLifecycle.Probe probe,
                                    final TimeSource timeSource) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        lifecycle = new OptionalProviderLifecycle(ProviderId.of("zartra", "luckperms"),
                SemanticVersion.parse("5.4.0"),
                CapabilitySet.of(Arrays.asList(
                        CapabilityId.of("zartra", "permission"),
                        CapabilityId.of("zartra", "permission-meta"))),
                timeSource, "provider.luckperms", probe);
    }

    @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
    @Override public Health health() { return lifecycle.health(); }
    @Override public CompletionStage<Result<LifecycleState>> start() { return lifecycle.start(); }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        return lifecycle.drain(deadline);
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() { return lifecycle.stop(); }

    @Override
    public CompletionStage<Result<Boolean>> hasPermission(
            final ContextQuery query, final PermissionNode permission) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(permission, "permission");
        if (!lifecycle.available()) { return unavailable(); }
        if (!query.deadline().isAfter(timeSource.now())) { return expired(); }
        return gateway.hasPermission(query, permission).handle((value, failure) ->
                failure == null ? Result.success(Boolean.TRUE.equals(value)) : failure());
    }

    @Override
    public CompletionStage<Result<MetaSnapshot>> metadata(final ContextQuery query) {
        Objects.requireNonNull(query, "query");
        if (!lifecycle.available()) { return unavailable(); }
        if (!query.deadline().isAfter(timeSource.now())) { return expired(); }
        return gateway.metadata(query).handle((value, failure) -> {
            if (failure != null) { return failure(); }
            final long providerVersion = Math.max(value.version(),
                    invalidationVersions.getOrDefault(query.playerId(), 0L));
            return Result.success(new MetaSnapshot(query.playerId(), value.prefix(),
                    value.suffix(), value.metadata(), providerVersion, timeSource.now()));
        });
    }

    /** Invalidates cached vendor projections for one player without owning profile data. */
    public void invalidate(final PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId");
        invalidationVersions.put(playerId, sequence.incrementAndGet());
        gateway.invalidate(playerId);
    }

    private static <T> CompletionStage<Result<T>> unavailable() {
        return CompletableFuture.completedFuture(failure());
    }

    private static <T> CompletionStage<Result<T>> expired() {
        return CompletableFuture.completedFuture(Result.failure(ApiError.of(
                DefinitionId.of("zartra", "provider/deadline-expired"),
                "provider.deadline_expired", ApiError.RetryDisposition.PERMANENT)));
    }

    private static <T> Result<T> failure() {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "provider/luckperms-unavailable"),
                "provider.luckperms_unavailable", ApiError.RetryDisposition.RETRYABLE));
    }

    /** Narrow runtime binding implemented against the operator-installed LuckPerms service. */
    public interface Gateway {
        /** @return asynchronous context-aware permission decision */
        CompletionStage<Boolean> hasPermission(ContextQuery query, PermissionNode permission);
        /** @return asynchronous allow-listed metadata projection */
        CompletionStage<Metadata> metadata(ContextQuery query);
        /** Removes vendor-side cached projections for one player. */
        void invalidate(PlayerId playerId);
    }

    /** Immutable gateway metadata before public projection. */
    public static final class Metadata {
        private final String prefix;
        private final String suffix;
        private final Map<String, String> metadata;
        private final long version;

        /** @param prefix display prefix @param suffix display suffix
         * @param metadata allow-listed values @param version provider version */
        public Metadata(final String prefix, final String suffix,
                        final Map<String, String> metadata, final long version) {
            this.prefix = Objects.requireNonNull(prefix, "prefix");
            this.suffix = Objects.requireNonNull(suffix, "suffix");
            this.metadata = Collections.unmodifiableMap(
                    new LinkedHashMap<String, String>(
                            Objects.requireNonNull(metadata, "metadata")));
            if (version < 0) { throw new IllegalArgumentException("version must be non-negative"); }
            this.version = version;
        }
        /** @return prefix */ public String prefix() { return prefix; }
        /** @return suffix */ public String suffix() { return suffix; }
        /** @return immutable metadata */ public Map<String, String> metadata() { return metadata; }
        /** @return provider version */ public long version() { return version; }
    }
}
