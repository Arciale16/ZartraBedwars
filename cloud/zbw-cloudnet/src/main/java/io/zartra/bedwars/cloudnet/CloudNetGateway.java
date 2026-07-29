package io.zartra.bedwars.cloudnet;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Narrow operator-supplied CloudNet binding.
 *
 * <p>The repository intentionally does not redistribute or compile against a CloudNet server
 * binary. A runtime composition layer maps the installed API to this nonblocking boundary.</p>
 */
public interface CloudNetGateway {
    /** @return asynchronously discovered, secret-free metadata */
    CompletionStage<List<CloudNetServiceMetadata>> discover();
    /** @return asynchronously started service metadata */
    CompletionStage<CloudNetServiceMetadata> start(
            ServiceDiscoveryProvider.ServiceRequest request);
    /** @return whether the service entered drain before the deadline */
    CompletionStage<Boolean> drain(DefinitionId serviceId, Instant deadline);
    /** @return whether the service stopped before the deadline */
    CompletionStage<Boolean> stop(DefinitionId serviceId, Instant deadline);
}
