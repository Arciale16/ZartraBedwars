package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.setup.MarkerProposal;
import io.zartra.bedwars.arena.setup.SetupSession;

/** Asynchronous-adapter boundary for marker discovery; discovery itself never mutates a draft. */
public interface MarkerDiscoveryPort {
    /** @return immutable proposal bound to the inspected session revision */
    Result<MarkerProposal> discover(SetupSession session);
}
