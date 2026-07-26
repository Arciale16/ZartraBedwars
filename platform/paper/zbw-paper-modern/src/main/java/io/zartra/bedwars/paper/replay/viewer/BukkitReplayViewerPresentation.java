package io.zartra.bedwars.paper.replay.viewer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

/** Minimal Paper message presentation; advanced visuals remain outside Phase 6. */
public final class BukkitReplayViewerPresentation implements ReplayViewerPresentation {
    private final PlayerLookup players;

    /** Creates a message presentation over a Paper player lookup. */
    public BukkitReplayViewerPresentation(final PlayerLookup players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override
    public void show(final ReplayViewerSession session) {
        Objects.requireNonNull(session, "session");
        send(session.viewerId(), "replay.viewer." + session.state().name().toLowerCase(
                java.util.Locale.ROOT));
    }

    @Override
    public void reject(final UUID viewerId, final ReplayViewerResult.Status status) {
        send(viewerId, "replay.viewer.error." + Objects.requireNonNull(status, "status")
                .name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void clear(final UUID viewerId) {
        send(viewerId, "replay.viewer.disconnected");
    }

    private void send(final UUID viewerId, final String messageKey) {
        final Object player = players.find(Objects.requireNonNull(viewerId, "viewerId"));
        if (player == null) {
            return;
        }
        try {
            final Method method = player.getClass().getMethod("sendMessage", String.class);
            method.invoke(player, messageKey);
        } catch (NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException failure) {
            throw new IllegalStateException("Paper viewer message operation unavailable", failure);
        }
    }

    /** Finds a currently connected opaque Paper player. */
    public interface PlayerLookup {
        /** @return Paper player or null when disconnected */
        Object find(UUID playerId);
    }
}
