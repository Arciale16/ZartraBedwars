package io.zartra.bedwars.paper.world;

import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldProvider;

/** Narrow owner-thread-only Paper boundary used by the native world provider. */
interface PaperPlatform {
    /** @return whether the caller is the Paper owner thread */
    boolean isOwnerThread();

    /** Loads or creates a world on the owner thread. */
    boolean load(WorldKey world);

    /** Unloads a world without saving on the owner thread. */
    boolean unload(WorldKey world);

    /** Captures live resource counters on the owner thread. */
    WorldProvider.ResourceSnapshot resources(WorldKey world);
}
