package io.zartra.bedwars.paper.replay;

import java.util.UUID;

/** Paper-facing player boundary used without exposing Bukkit to replay core. */
public interface ReplayAudience {
    /** @return stable player identity */ UUID playerId();
    /** @return whether the connected player owns a permission */ boolean hasPermission(String permission);
    /** @return opaque state needed to restore the player after replay */ Object enterSpectatorReplay();
    /** Restores state captured by {@link #enterSpectatorReplay()}. */ void leaveSpectatorReplay(Object restoration);
}
