package io.zartra.bedwars.paper.replay;

import java.util.Objects;
import java.util.UUID;

/** Immutable externally visible spectator replay session. */
public final class SpectatorReplaySession {
    private final UUID playerId;
    private final ReplayRuntimeContext context;

    SpectatorReplaySession(final UUID playerId, final ReplayRuntimeContext context) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.context = Objects.requireNonNull(context, "context");
    }

    /** @return spectator player identity */ public UUID playerId() { return playerId; }
    /** @return current replay runtime context */ public ReplayRuntimeContext context() { return context; }
}
