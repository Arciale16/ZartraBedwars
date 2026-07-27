package io.zartra.bedwars.paper.replay;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper scheduler and disconnect-listener adapter for the replay runtime bootstrap. */
public final class BukkitReplayRuntimeAdapter
        implements PaperReplayService.OwnerThread, ReplayRuntimeBootstrap.DisconnectRegistration {
    private final JavaPlugin plugin;

    /** Creates an adapter owned by one plugin lifecycle. */
    public BukkitReplayRuntimeAdapter(final JavaPlugin plugin) { this.plugin = Objects.requireNonNull(plugin, "plugin"); }
    @Override public void execute(final Runnable action) {
        Objects.requireNonNull(action, "action");
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
    @Override public boolean isOwnerThread() { return Bukkit.isPrimaryThread(); }
    @Override public AutoCloseable register(final Consumer<UUID> disconnect) {
        final QuitListener listener = new QuitListener(Objects.requireNonNull(disconnect, "disconnect"));
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        return () -> HandlerList.unregisterAll(listener);
    }

    private static final class QuitListener implements Listener {
        private final Consumer<UUID> disconnect;
        private QuitListener(final Consumer<UUID> disconnect) { this.disconnect = disconnect; }
        @EventHandler public void onQuit(final PlayerQuitEvent event) {
            disconnect.accept(BukkitReplayAudience.playerIdOf(event.getPlayer()));
        }
    }
}
