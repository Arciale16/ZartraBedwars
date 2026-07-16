package io.zartra.bedwars.paper.game;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** Explicit lifecycle for closed M08 input translation and projection cleanup. */
public final class PaperGameProjectionRuntime {
    private final JavaPlugin plugin;
    private final PaperGameEventTranslator translator;
    private final PaperLobbyProjection lobby;
    private final AtomicBoolean started = new AtomicBoolean();

    /** Creates an unstarted runtime around real sinks and projectors. */
    public PaperGameProjectionRuntime(final JavaPlugin plugin,
                                      final PaperGameEventTranslator.Sink sink,
                                      final PaperLobbyProjection lobby) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.translator = new PaperGameEventTranslator(Objects.requireNonNull(sink, "sink"));
        this.lobby = Objects.requireNonNull(lobby, "lobby");
    }

    /** Registers primary-thread event translation exactly once. */
    public void start() {
        requireOwner();
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("game projection runtime already started");
        }
        Bukkit.getPluginManager().registerEvents(translator, plugin);
    }

    /** Unregisters translation and removes every owned projection exactly once. */
    public void stop() {
        requireOwner();
        if (!started.compareAndSet(true, false)) { return; }
        HandlerList.unregisterAll(translator);
        lobby.close();
    }

    /** @return whether event translation is currently registered */ public boolean isStarted() { return started.get(); }
    private static void requireOwner() {
        if (!Bukkit.isPrimaryThread()) { throw new IllegalStateException("Paper runtime lifecycle requires primary thread"); }
    }
}
