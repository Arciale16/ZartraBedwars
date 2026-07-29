package io.zartra.bedwars.cloudnet;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.proxy.api.BackendCapabilities;
import io.zartra.bedwars.proxy.api.BackendId;
import io.zartra.bedwars.proxy.api.BackendRegistration;
import io.zartra.bedwars.proxy.api.BackendStatus;
import io.zartra.bedwars.proxy.api.CapacitySnapshot;
import io.zartra.bedwars.proxy.api.HealthSnapshot;
import io.zartra.bedwars.proxy.api.InstanceEpoch;
import io.zartra.bedwars.redis.api.DegradationMode;
import io.zartra.bedwars.redis.api.FencingToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Coordinates CloudNet lifecycle with M19 fencing and M20 backend projections.
 *
 * <p>The coordinator starts, drains and replaces services only. It deliberately exposes no
 * matchmaking, destination selection, reservation or game-lifecycle operation.</p>
 */
public final class CloudNetServiceCoordinator {
    private static final int MAX_OBSERVED_SERVICES = 4096;
    private static final DefinitionId DEGRADED =
            DefinitionId.of("zartra", "cloudnet/distributed-degraded");
    private final CloudNetServiceAdapter adapter;
    private final CloudNetCoordinationPort.Redis redis;
    private final CloudNetCoordinationPort.Proxy proxy;
    private final TimeSource timeSource;
    private final Map<DefinitionId, CloudNetServiceMetadata> observed =
            new LinkedHashMap<DefinitionId, CloudNetServiceMetadata>();
    private FencingToken lastFence;
    private long operationSequence;

    /**
     * Creates the service-only coordinator.
     *
     * @param adapter CloudNet lifecycle adapter
     * @param redis M19 lease/degradation boundary
     * @param proxy M20 registry projection boundary
     * @param timeSource deterministic clock
     */
    public CloudNetServiceCoordinator(
            final CloudNetServiceAdapter adapter,
            final CloudNetCoordinationPort.Redis redis,
            final CloudNetCoordinationPort.Proxy proxy,
            final TimeSource timeSource) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.redis = Objects.requireNonNull(redis, "redis");
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }

    /**
     * Reconciles discovery and one bounded scaling policy.
     *
     * @param policy deterministic scaling policy
     * @param policyState previous policy state
     * @param kind service purpose
     * @param templateId CloudNet template
     * @param serviceCapacity capacity of each newly started service
     * @param deadline operation deadline
     * @return reconciliation result and next policy state
     */
    public CompletionStage<Result<Reconciliation>> reconcile(
            final CloudNetScalingPolicy policy,
            final CloudNetScalingPolicy.State policyState,
            final ServiceDiscoveryProvider.ServiceKind kind,
            final DefinitionId templateId,
            final int serviceCapacity,
            final Instant deadline) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(policyState, "policyState");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(templateId, "templateId");
        Objects.requireNonNull(deadline, "deadline");
        if (serviceCapacity < 1 || serviceCapacity > 4096) {
            throw new IllegalArgumentException("serviceCapacity must be 1..4096");
        }
        return adapter.discoverMetadata().thenCompose(result -> {
            if (result.isFailure()) {
                return CompletableFuture.completedFuture(Result.failure(
                        result.error().get()));
            }
            final List<CloudNetServiceMetadata> all = result.requireValue();
            acceptObservations(all);
            final List<CloudNetServiceMetadata> selected =
                    select(all, kind, templateId);
            final CloudNetScalingPolicy.Evaluation evaluation =
                    policy.evaluate(selected, policyState, timeSource.now());
            if (evaluation.actions() == 0) {
                return CompletableFuture.completedFuture(Result.success(
                        new Reconciliation(evaluation.nextState(), 0, false)));
            }
            if (redis.degradationMode() != DegradationMode.NORMAL) {
                return CompletableFuture.completedFuture(degraded());
            }
            final DefinitionId operation = nextOperationId();
            return redis.acquire(operation, deadline).thenCompose(lease -> {
                if (lease.isFailure()) {
                    return CompletableFuture.completedFuture(Result.failure(
                            lease.error().get()));
                }
                if (!acceptFence(lease.requireValue())) {
                    return CompletableFuture.completedFuture(staleFence());
                }
                if (evaluation.direction() == CloudNetScalingPolicy.Direction.UP) {
                    return startServices(evaluation, kind, templateId, serviceCapacity, deadline);
                }
                return drainServices(evaluation, selected, deadline);
            });
        });
    }

    /**
     * Replaces a crashed service after accepting only fresh metadata and a fenced lease.
     *
     * @param crashed fresh crashed-service metadata
     * @param deadline replacement deadline
     * @return replacement service result
     */
    public CompletionStage<Result<ServiceDiscoveryProvider.ServiceSnapshot>> replaceCrashed(
            final CloudNetServiceMetadata crashed, final Instant deadline) {
        Objects.requireNonNull(crashed, "crashed");
        Objects.requireNonNull(deadline, "deadline");
        if (crashed.state() != ServiceDiscoveryProvider.ServiceState.OFFLINE
                || !acceptObservation(crashed)) {
            return CompletableFuture.completedFuture(Result.failure(ApiError.of(
                    DefinitionId.of("zartra", "cloudnet/stale-crash"),
                    "cloudnet.stale_crash", ApiError.RetryDisposition.PERMANENT)));
        }
        if (redis.degradationMode() != DegradationMode.NORMAL) {
            return CompletableFuture.completedFuture(degradedSnapshot());
        }
        final DefinitionId operation = nextOperationId();
        return redis.acquire(operation, deadline).thenCompose(lease -> {
            if (lease.isFailure()) {
                return CompletableFuture.completedFuture(Result.failure(lease.error().get()));
            }
            if (!acceptFence(lease.requireValue())) {
                return CompletableFuture.completedFuture(staleFenceSnapshot());
            }
            proxy.remove(registration(crashed));
            final ServiceDiscoveryProvider.ServiceRequest request =
                    new ServiceDiscoveryProvider.ServiceRequest(
                            IdempotencyKey.parse(operation.toString()),
                            crashed.kind(), crashed.templateId(),
                            Math.max(1, crashed.capacity()), deadline);
            return adapter.request(request);
        });
    }

    private CompletionStage<Result<Reconciliation>> startServices(
            final CloudNetScalingPolicy.Evaluation evaluation,
            final ServiceDiscoveryProvider.ServiceKind kind,
            final DefinitionId templateId,
            final int serviceCapacity,
            final Instant deadline) {
        CompletionStage<Result<ServiceDiscoveryProvider.ServiceSnapshot>> stage =
                CompletableFuture.completedFuture(Result.success(
                        new ServiceDiscoveryProvider.ServiceSnapshot(
                                DefinitionId.of("zartra", "cloudnet/pending"),
                                kind, ServiceDiscoveryProvider.ServiceState.STARTING,
                                0, 0, 1)));
        for (int index = 0; index < evaluation.actions(); index++) {
            final DefinitionId operation = nextOperationId();
            final ServiceDiscoveryProvider.ServiceRequest request =
                    new ServiceDiscoveryProvider.ServiceRequest(
                            IdempotencyKey.parse(operation.toString()),
                            kind, templateId, serviceCapacity, deadline);
            stage = stage.thenCompose(previous -> previous.isFailure()
                    ? CompletableFuture.completedFuture(previous)
                    : adapter.request(request));
        }
        return stage.thenApply(result -> result.isFailure()
                ? Result.failure(result.error().get())
                : Result.success(new Reconciliation(
                        evaluation.nextState(), evaluation.actions(), false)));
    }

    private CompletionStage<Result<Reconciliation>> drainServices(
            final CloudNetScalingPolicy.Evaluation evaluation,
            final List<CloudNetServiceMetadata> selected,
            final Instant deadline) {
        final List<CloudNetServiceMetadata> candidates =
                new ArrayList<CloudNetServiceMetadata>();
        for (CloudNetServiceMetadata service : selected) {
            if (service.state() == ServiceDiscoveryProvider.ServiceState.ONLINE
                    && service.occupancy() == 0) {
                candidates.add(service);
            }
        }
        candidates.sort(Comparator
                .comparingInt(CloudNetServiceMetadata::occupancy)
                .thenComparing(CloudNetServiceMetadata::serviceId,
                        Comparator.comparing(DefinitionId::toString)));
        CompletionStage<Result<Boolean>> stage =
                CompletableFuture.completedFuture(Result.success(Boolean.TRUE));
        final int actions = Math.min(evaluation.actions(), candidates.size());
        for (int index = 0; index < actions; index++) {
            final CloudNetServiceMetadata candidate = candidates.get(index);
            stage = stage.thenCompose(previous -> previous.isFailure()
                    ? CompletableFuture.completedFuture(previous)
                    : adapter.drain(candidate.serviceId(), deadline));
        }
        return stage.thenApply(result -> result.isFailure()
                ? Result.failure(result.error().get())
                : Result.success(new Reconciliation(
                        evaluation.nextState(), actions, false)));
    }

    private synchronized void acceptObservations(
            final List<CloudNetServiceMetadata> services) {
        for (CloudNetServiceMetadata service : services) {
            if (acceptObservation(service)) {
                publish(service);
            }
        }
    }

    private synchronized boolean acceptObservation(final CloudNetServiceMetadata service) {
        final CloudNetServiceMetadata prior = observed.get(service.serviceId());
        if (prior != null && !service.supersedes(prior)) {
            return false;
        }
        observed.put(service.serviceId(), service);
        if (observed.size() > MAX_OBSERVED_SERVICES) {
            observed.remove(observed.keySet().iterator().next());
        }
        return true;
    }

    private synchronized boolean acceptFence(final FencingToken fence) {
        if (lastFence != null && !fence.isNewerThan(lastFence)) {
            return false;
        }
        lastFence = fence;
        return true;
    }

    private void publish(final CloudNetServiceMetadata service) {
        final BackendRegistration registration = registration(service);
        if (service.state() == ServiceDiscoveryProvider.ServiceState.OFFLINE) {
            proxy.remove(registration);
            return;
        }
        proxy.publish(registration,
                CapacitySnapshot.of(Math.max(1, service.capacity()),
                        service.occupancy(), 0),
                HealthSnapshot.of(health(service.state()),
                        "cloudnet." + service.state().name().toLowerCase(),
                        service.observedAt()));
    }

    private static BackendRegistration registration(final CloudNetServiceMetadata service) {
        return BackendRegistration.of(
                BackendId.of(service.backendId()),
                InstanceEpoch.of(service.epoch()),
                BackendCapabilities.of(Collections.singletonList(
                        service.kind().name().toLowerCase())),
                status(service.state()), service.observedAt());
    }

    private static BackendStatus status(final ServiceDiscoveryProvider.ServiceState state) {
        switch (state) {
            case ONLINE: return BackendStatus.ONLINE;
            case DRAINING: return BackendStatus.DRAINING;
            case UNHEALTHY: return BackendStatus.UNHEALTHY;
            default: return BackendStatus.OFFLINE;
        }
    }

    private static HealthSnapshot.State health(
            final ServiceDiscoveryProvider.ServiceState state) {
        return state == ServiceDiscoveryProvider.ServiceState.ONLINE
                ? HealthSnapshot.State.HEALTHY
                : state == ServiceDiscoveryProvider.ServiceState.DRAINING
                ? HealthSnapshot.State.DEGRADED : HealthSnapshot.State.UNHEALTHY;
    }

    private static List<CloudNetServiceMetadata> select(
            final List<CloudNetServiceMetadata> services,
            final ServiceDiscoveryProvider.ServiceKind kind,
            final DefinitionId templateId) {
        final List<CloudNetServiceMetadata> selected =
                new ArrayList<CloudNetServiceMetadata>();
        for (CloudNetServiceMetadata service : services) {
            if (service.kind() == kind && service.templateId().equals(templateId)) {
                selected.add(service);
            }
        }
        return selected;
    }

    private synchronized DefinitionId nextOperationId() {
        operationSequence++;
        return DefinitionId.of("zartra", "cloudnet/reconcile-" + operationSequence);
    }

    private static Result<Reconciliation> degraded() {
        return Result.failure(ApiError.of(DEGRADED, "cloudnet.distributed_degraded",
                ApiError.RetryDisposition.RETRYABLE));
    }

    private static Result<ServiceDiscoveryProvider.ServiceSnapshot> degradedSnapshot() {
        return Result.failure(ApiError.of(DEGRADED, "cloudnet.distributed_degraded",
                ApiError.RetryDisposition.RETRYABLE));
    }

    private static Result<Reconciliation> staleFence() {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "cloudnet/stale-fence"),
                "cloudnet.stale_fence", ApiError.RetryDisposition.PERMANENT));
    }

    private static Result<ServiceDiscoveryProvider.ServiceSnapshot> staleFenceSnapshot() {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "cloudnet/stale-fence"),
                "cloudnet.stale_fence", ApiError.RetryDisposition.PERMANENT));
    }

    /** Immutable reconciliation diagnostic. */
    public static final class Reconciliation {
        private final CloudNetScalingPolicy.State policyState;
        private final int actions;
        private final boolean degraded;
        private Reconciliation(
                final CloudNetScalingPolicy.State policyState,
                final int actions,
                final boolean degraded) {
            this.policyState = policyState;
            this.actions = actions;
            this.degraded = degraded;
        }
        /** @return next deterministic policy state */
        public CloudNetScalingPolicy.State policyState() { return policyState; }
        /** @return completed bounded action count */
        public int actions() { return actions; }
        /** @return whether the reconciliation used degraded behavior */
        public boolean degraded() { return degraded; }
    }
}
