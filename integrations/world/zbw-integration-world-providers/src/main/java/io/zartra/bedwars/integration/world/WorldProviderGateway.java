package io.zartra.bedwars.integration.world;

import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldProvider;
import java.util.concurrent.CompletionStage;

/**
 * Narrow vendor binding used by the optional M21 world-provider adapters.
 *
 * <p>The operator supplies an implementation against the independently installed provider API.
 * Capability probing is asynchronous. Plan construction performs no I/O and resource snapshots
 * are cached, fast and secret-free as required by the existing M06 {@link WorldProvider} SPI.</p>
 */
public interface WorldProviderGateway {
    /** @return asynchronous exact-version and capability compatibility result */
    CompletionStage<Boolean> compatible();

    /**
     * Builds a bounded provider plan without executing vendor or filesystem work.
     *
     * @param operation validated world operation
     * @return plan using the original operation
     */
    WorldProvider.Plan plan(WorldOperation operation);

    /**
     * Returns cached provider resource accounting.
     *
     * @param world world identity
     * @return non-null secret-free snapshot
     */
    WorldProvider.ResourceSnapshot snapshot(WorldKey world);
}
