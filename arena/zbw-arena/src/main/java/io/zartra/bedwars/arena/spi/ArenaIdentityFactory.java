package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.MapId;

/** Collision-resistant identity source injected at the application composition boundary. */
public interface ArenaIdentityFactory {
    /** @return a never-reused arena identity */ ArenaId newArenaId();
    /** @return a never-reused map identity */ MapId newMapId();

    /** Process-random implementation for production composition roots. */
    enum RandomIdentityFactory implements ArenaIdentityFactory {
        /** Shared stateless instance. */ INSTANCE;
        @Override public ArenaId newArenaId() { return ArenaId.random(); }
        @Override public MapId newMapId() { return MapId.random(); }
    }
}
