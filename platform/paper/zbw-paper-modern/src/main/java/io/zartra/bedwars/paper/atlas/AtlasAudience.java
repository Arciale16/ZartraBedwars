package io.zartra.bedwars.paper.atlas;

import java.util.UUID;

/** Paper-neutral audience boundary used by commands and GUI presentation. */
public interface AtlasAudience {
    UUID playerId();
    boolean hasPermission(String permission);
    void present(String message);
}
