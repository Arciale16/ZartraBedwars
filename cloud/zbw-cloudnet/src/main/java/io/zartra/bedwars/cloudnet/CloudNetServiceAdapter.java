package io.zartra.bedwars.cloudnet;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Optional CloudNet service discovery/lifecycle adapter with bounded callback execution. */
public final class CloudNetServiceAdapter implements ServiceDiscoveryProvider, AutoCloseable {
    private static final int MAX_TRACKED_OPERATIONS = 4096;
    private static final int MAX_DISCOVERED_SERVICES = 4096;
    private static final DefinitionId UNAVAILABLE =
            DefinitionId.of("zartra", "provider/cloudnet-unavailable");
    private final CloudNetGateway gateway;
    private final TimeSource timeSource;
    private final BoundedCloudExecutor executor;
    private final OptionalProviderLifecycle lifecycle;
    private final Map<String, CompletionStage<Result<ServiceSnapshot>>> operations =
            new LinkedHashMap<String, CompletionStage<Result<ServiceSnapshot>>>();

    /**
     * Creates the optional adapter.
     *
     * @param gateway nonblocking operator-provided CloudNet binding
     * @param probe installation and API compatibility probe
     * @param timeSource observation clock
     * @param executor bounded callback worker
     */
    public CloudNetServiceAdapter(
            final CloudNetGateway gateway,
            final OptionalProviderLifecycle.Probe probe,
            final TimeSource timeSource,
            final BoundedCloudExecutor executor) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        this.executor = Objects.requireNonNull(executor, "executor");
        lifecycle = new OptionalProviderLifecycle(
                ProviderId.of("zartra", "cloudnet"),
                SemanticVersion.parse("4.0.0"),
                CapabilitySet.of(Collections.singletonList(
                        CapabilityId.of("zartra", "service-discovery"))),
                timeSource, "provider.cloudnet", probe);
    }

    @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
    @Override public Health health() { return lifecycle.health(); }
    @Override public CompletionStage<Result<LifecycleState>> start() {
        return lifecycle.start();
    }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        return lifecycle.drain(deadline);
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() {
        return lifecycle.stop();
    }

    @Override
    public CompletionStage<Result<List<ServiceSnapshot>>> discover() {
        if (!lifecycle.available()) {
            return completedFailure();
        }
        return executor.submit(gateway::discover).handle((metadata, failure) -> {
            if (failure != null) {
                return failure();
            }
            try {
                final List<CloudNetServiceMetadata> sorted =
                        new ArrayList<CloudNetServiceMetadata>(metadata);
                Collections.sort(sorted);
                final List<ServiceSnapshot> snapshots = new ArrayList<ServiceSnapshot>();
                DefinitionId previous = null;
                for (CloudNetServiceMetadata service : sorted) {
                    if (previous != null && previous.equals(service.serviceId())) {
                        return invalid();
                    }
                    snapshots.add(service.snapshot());
                    previous = service.serviceId();
                }
                return Result.success(Collections.unmodifiableList(snapshots));
            } catch (RuntimeException malformed) {
                return invalid();
            }
        });
    }

    /** @return deterministic full metadata used by the lifecycle coordinator */
    public CompletionStage<Result<List<CloudNetServiceMetadata>>> discoverMetadata() {
        if (!lifecycle.available()) {
            return completedFailure();
        }
        return executor.submit(gateway::discover).handle((metadata, failure) -> {
            if (failure != null) {
                return failure();
            }
            try {
                final List<CloudNetServiceMetadata> sorted =
                        new ArrayList<CloudNetServiceMetadata>(metadata);
                Collections.sort(sorted);
                for (int index = 1; index < sorted.size(); index++) {
                    if (sorted.get(index - 1).serviceId().equals(sorted.get(index).serviceId())) {
                        return invalid();
                    }
                }
                return Result.success(Collections.unmodifiableList(sorted));
            } catch (RuntimeException malformed) {
                return invalid();
            }
        });
    }

    @Override
    public CompletionStage<Result<ServiceSnapshot>> request(final ServiceRequest request) {
        Objects.requireNonNull(request, "request");
        if (!lifecycle.available()) {
            return completedFailure();
        }
        if (!request.deadline().isAfter(timeSource.now())) {
            return CompletableFuture.completedFuture(Result.failure(ApiError.of(
                    DefinitionId.of("zartra", "provider/deadline-expired"),
                    "provider.deadline_expired", ApiError.RetryDisposition.PERMANENT)));
        }
        final String key = request.operationId().toString();
        synchronized (operations) {
            final CompletionStage<Result<ServiceSnapshot>> existing = operations.get(key);
            if (existing != null) {
                return existing;
            }
            if (operations.size() >= MAX_TRACKED_OPERATIONS) {
                final String oldest = operations.keySet().iterator().next();
                operations.remove(oldest);
            }
            final CompletionStage<Result<ServiceSnapshot>> created =
                    executor.submit(() -> gateway.start(request))
                            .handle((metadata, failure) -> {
                                if (failure != null) {
                                    return failure();
                                }
                                try {
                                    return Result.success(metadata.snapshot());
                                } catch (RuntimeException malformed) {
                                    return invalid();
                                }
                            });
            operations.put(key, created);
            return created;
        }
    }

    @Override
    public CompletionStage<Result<Boolean>> drain(
            final DefinitionId serviceId, final Instant deadline) {
        return lifecycleOperation(serviceId, deadline, true);
    }

    @Override
    public CompletionStage<Result<Boolean>> stop(
            final DefinitionId serviceId, final Instant deadline) {
        return lifecycleOperation(serviceId, deadline, false);
    }

    private CompletionStage<Result<Boolean>> lifecycleOperation(
            final DefinitionId serviceId, final Instant deadline, final boolean draining) {
        Objects.requireNonNull(serviceId, "serviceId");
        Objects.requireNonNull(deadline, "deadline");
        if (!lifecycle.available()) {
            return completedFailure();
        }
        if (!deadline.isAfter(timeSource.now())) {
            return CompletableFuture.completedFuture(Result.failure(ApiError.of(
                    DefinitionId.of("zartra", "provider/deadline-expired"),
                    "provider.deadline_expired", ApiError.RetryDisposition.PERMANENT)));
        }
        return executor.submit(() -> draining
                ? gateway.drain(serviceId, deadline) : gateway.stop(serviceId, deadline))
                .handle((accepted, failure) -> failure == null
                        ? Result.success(Boolean.TRUE.equals(accepted)) : failure());
    }

    private static List<CloudNetServiceMetadata> bounded(
            final List<CloudNetServiceMetadata> metadata) {
        if (metadata == null || metadata.size() > MAX_DISCOVERED_SERVICES) {
            throw new IllegalArgumentException("discovery result exceeds bound");
        }
        return new ArrayList<CloudNetServiceMetadata>(metadata);
    }

    private static <T> CompletionStage<Result<T>> completedFailure() {
        return CompletableFuture.completedFuture(failure());
    }

    private static <T> Result<T> failure() {
        return Result.failure(ApiError.of(UNAVAILABLE, "provider.cloudnet_unavailable",
                ApiError.RetryDisposition.RETRYABLE));
    }

    private static <T> Result<T> invalid() {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "provider/cloudnet-malformed-metadata"),
                "provider.cloudnet_malformed_metadata",
                ApiError.RetryDisposition.PERMANENT));
    }

    @Override public void close() { executor.close(); }
}
