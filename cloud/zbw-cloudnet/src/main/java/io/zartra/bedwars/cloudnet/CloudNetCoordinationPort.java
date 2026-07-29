package io.zartra.bedwars.cloudnet;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.proxy.api.BackendRegistration;
import io.zartra.bedwars.proxy.api.CapacitySnapshot;
import io.zartra.bedwars.proxy.api.HealthSnapshot;
import io.zartra.bedwars.redis.api.DegradationMode;
import io.zartra.bedwars.redis.api.FencingToken;
import java.time.Instant;
import java.util.concurrent.CompletionStage;

/** Neutral M19/M20 coordination ports; neither routing nor durable domain ownership is exposed. */
public interface CloudNetCoordinationPort {
    /** Redis lease/fencing boundary used only to serialize service lifecycle actions. */
    interface Redis {
        /** @return current fail-safe distributed operating mode */
        DegradationMode degradationMode();
        /** @return a monotonic fencing token or typed failure */
        CompletionStage<Result<FencingToken>> acquire(
                DefinitionId operationId, Instant deadline);
    }

    /** Proxy registry projection boundary; no destination selection or reservations are exposed. */
    interface Proxy {
        /** Publishes a backend registration and its capacity/health projections. */
        void publish(
                BackendRegistration registration,
                CapacitySnapshot capacity,
                HealthSnapshot health);
        /** Removes a stopped backend epoch from routing eligibility. */
        void remove(BackendRegistration registration);
    }
}
