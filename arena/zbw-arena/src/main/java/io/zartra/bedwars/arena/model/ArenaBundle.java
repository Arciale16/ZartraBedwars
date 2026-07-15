package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.MapId;
import java.util.Objects;

/** Atomic durable aggregate containing one arena and its stable map metadata. */
public final class ArenaBundle {
    private final ArenaDefinition arena;
    private final MapDefinition map;

    /** Creates a bundle only when both definitions reference the same map identity. */
    public ArenaBundle(final ArenaDefinition arena, final MapDefinition map) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.map = Objects.requireNonNull(map, "map");
        if (!arena.mapId().equals(map.id())) {
            throw new IllegalArgumentException("arena and map identities do not match");
        }
    }
    /** @return arena definition */ public ArenaDefinition arena() { return arena; }
    /** @return map metadata */ public MapDefinition map() { return map; }
    /** @return arena identity */ public ArenaId arenaId() { return arena.id(); }
    /** @return map identity */ public MapId mapId() { return map.id(); }
    @Override public int hashCode() { return Objects.hash(arena, map); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaBundle)) { return false; }
        final ArenaBundle that = (ArenaBundle) other;
        return Objects.deepEquals(new Object[] {arena, map}, new Object[] {that.arena, that.map});
    }
}
