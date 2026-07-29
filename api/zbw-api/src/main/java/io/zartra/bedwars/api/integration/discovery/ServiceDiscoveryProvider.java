package io.zartra.bedwars.api.integration.discovery;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Dynamic service discovery port; routing and game policy remain with their owning modules. */
public interface ServiceDiscoveryProvider extends Provider {
    /** @return asynchronous deterministic service snapshots */
    CompletionStage<Result<List<ServiceSnapshot>>> discover();
    /** @param request bounded idempotent capacity request @return resulting service snapshot */
    CompletionStage<Result<ServiceSnapshot>> request(ServiceRequest request);
    /** @param serviceId service identity @param deadline drain deadline @return drain outcome */
    CompletionStage<Result<Boolean>> drain(DefinitionId serviceId, Instant deadline);
    /** @param serviceId service identity @param deadline stop deadline @return stop outcome */
    CompletionStage<Result<Boolean>> stop(DefinitionId serviceId, Instant deadline);

    /** Immutable dynamic service request. */
    final class ServiceRequest {
        private final IdempotencyKey operationId;
        private final ServiceKind kind;
        private final DefinitionId templateId;
        private final int capacity;
        private final Instant deadline;

        /**
         * Creates a dynamic service request.
         *
         * @param operationId idempotency identity
         * @param kind service purpose
         * @param templateId configured template identity
         * @param capacity positive bounded capacity
         * @param deadline operation deadline
         */
        public ServiceRequest(final IdempotencyKey operationId, final ServiceKind kind,
                              final DefinitionId templateId, final int capacity,
                              final Instant deadline) {
            this.operationId = Objects.requireNonNull(operationId, "operationId");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.templateId = Objects.requireNonNull(templateId, "templateId");
            this.deadline = Objects.requireNonNull(deadline, "deadline");
            if (capacity < 1 || capacity > 4096) {
                throw new IllegalArgumentException("capacity must be 1..4096");
            }
            this.capacity = capacity;
        }

        /** @return idempotency identity */
        public IdempotencyKey operationId() { return operationId; }
        /** @return service purpose */
        public ServiceKind kind() { return kind; }
        /** @return template identity */
        public DefinitionId templateId() { return templateId; }
        /** @return requested capacity */
        public int capacity() { return capacity; }
        /** @return operation deadline */
        public Instant deadline() { return deadline; }
    }

    /** Immutable, secret-free service snapshot. */
    final class ServiceSnapshot {
        private final DefinitionId serviceId;
        private final ServiceKind kind;
        private final ServiceState state;
        private final int capacity;
        private final int occupancy;
        private final long epoch;

        /**
         * Creates a service snapshot.
         *
         * @param serviceId service identity
         * @param kind service purpose
         * @param state lifecycle state
         * @param capacity non-negative capacity
         * @param occupancy occupancy no greater than capacity
         * @param epoch positive instance epoch
         */
        public ServiceSnapshot(final DefinitionId serviceId, final ServiceKind kind,
                               final ServiceState state, final int capacity,
                               final int occupancy, final long epoch) {
            this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.state = Objects.requireNonNull(state, "state");
            if (capacity < 0 || occupancy < 0 || occupancy > capacity || epoch < 1) {
                throw new IllegalArgumentException("invalid service capacity or epoch");
            }
            this.capacity = capacity;
            this.occupancy = occupancy;
            this.epoch = epoch;
        }

        /** @return service identity */
        public DefinitionId serviceId() { return serviceId; }
        /** @return service purpose */
        public ServiceKind kind() { return kind; }
        /** @return lifecycle state */
        public ServiceState state() { return state; }
        /** @return advertised capacity */
        public int capacity() { return capacity; }
        /** @return current occupancy */
        public int occupancy() { return occupancy; }
        /** @return instance epoch */
        public long epoch() { return epoch; }
    }

    /** Dynamic service purpose. */
    enum ServiceKind { ARENA, PRIVATE_GAME, REPLAY_VIEWER }
    /** Provider-neutral dynamic service state. */
    enum ServiceState { STARTING, ONLINE, DRAINING, UNHEALTHY, OFFLINE }
}
