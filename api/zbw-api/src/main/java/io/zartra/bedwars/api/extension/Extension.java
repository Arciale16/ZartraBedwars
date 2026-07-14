package io.zartra.bedwars.api.extension;

import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.result.Result;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Public extension and registry contracts.
 *
 * <p>Concrete dependencies are supplied through constructor injection. Lifecycle callbacks run on
 * bounded extension workers and never receive Bukkit, storage, filesystem or configuration
 * objects through this interface.</p>
 */
public interface Extension {
    /** @return immutable metadata matching the validated descriptor */
    ExtensionMetadata metadata();
    /** @return asynchronous start result */
    CompletionStage<Result<State>> start();
    /** @return asynchronous bounded drain result */
    CompletionStage<Result<State>> drain(Duration deadline);
    /** @return asynchronous stop result */
    CompletionStage<Result<State>> stop();

    /** Extension lifecycle state. */
    enum State { LOADED, STARTING, RUNNING, DRAINING, STOPPED, FAILED }

    /** Read-only extension catalog. Implementations publish immutable snapshots. */
    interface Catalog {
        /** @return extension metadata by stable ID */
        Optional<ExtensionMetadata> find(io.zartra.bedwars.api.identity.ExtensionId id);
        /** @return immutable, deterministic metadata list */
        List<ExtensionMetadata> extensions();
    }

    /** Typed extension point contract; runtime registration is transactional in a later milestone. */
    interface Point<T> {
        /** @return capability identifying the extension point */ CapabilityId capability();
        /** @return public contract type; never an internal implementation class */ Class<T> contractType();
    }
}
