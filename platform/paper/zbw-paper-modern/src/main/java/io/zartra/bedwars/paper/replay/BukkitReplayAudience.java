package io.zartra.bedwars.paper.replay;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.GameMode;

/** Exact Paper player translation without leaking Paper types into replay contracts. */
public final class BukkitReplayAudience implements ReplayAudience {
    private final Object player;

    /** Wraps one connected Paper player. */
    public BukkitReplayAudience(final Object player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    @Override public UUID playerId() { return playerIdOf(player); }
    @Override public boolean hasPermission(final String permission) {
        return ((Boolean) invoke(player, "hasPermission", new Class<?>[] {String.class}, permission)).booleanValue();
    }
    @Override public Object enterSpectatorReplay() {
        final Object previous = invoke(player, "getGameMode", new Class<?>[0]);
        invoke(player, "setGameMode", new Class<?>[] {GameMode.class}, GameMode.SPECTATOR);
        return previous;
    }
    @Override public void leaveSpectatorReplay(final Object restoration) {
        final boolean online = ((Boolean) invoke(player, "isOnline", new Class<?>[0])).booleanValue();
        if (online && restoration instanceof GameMode) {
            invoke(player, "setGameMode", new Class<?>[] {GameMode.class}, restoration);
        }
    }

    static UUID playerIdOf(final Object player) {
        return (UUID) invoke(Objects.requireNonNull(player, "player"),
                "getUniqueId", new Class<?>[0]);
    }

    private static Object invoke(final Object target, final String name,
                                 final Class<?>[] parameterTypes, final Object... arguments) {
        try {
            final Method method = target.getClass().getMethod(name, parameterTypes);
            return method.invoke(target, arguments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException failure) {
            throw new IllegalStateException("Paper player operation unavailable: " + name, failure);
        }
    }
}
