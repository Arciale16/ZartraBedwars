package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.addon.LeaveDelayPolicy;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Translates primary Paper player events into typed M08 inputs without game decisions. */
public final class PaperGameEventTranslator implements Listener {
    private final Sink sink;

    /** Creates a listener whose sink owns all state and policy decisions. */
    public PaperGameEventTranslator(final Sink sink) { this.sink = Objects.requireNonNull(sink, "sink"); }

    /** Translates disconnect only; reconnect admission is composed by the later login boundary. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        sink.disconnected(playerId(event, "getPlayer"));
    }

    /** Translates material movement, ignoring head rotation. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        if (event.hasChangedPosition()) {
            sink.leaveDelaySignal(playerId(event, "getPlayer"),
                    LeaveDelayPolicy.Signal.MOVEMENT);
        }
    }

    /** Translates received damage for leave-delay cancellation and game input policy. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(final EntityDamageEvent event) {
        final Object entity = PaperReflection.invoke(event, "getEntity", new Class<?>[0]);
        if (PaperReflection.PLAYER.isInstance(entity)) {
            sink.leaveDelaySignal(id(entity), LeaveDelayPolicy.Signal.DAMAGE);
        }
    }

    /** Translates death; final-kill and respawn decisions remain in the game application. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event) {
        final PlayerId playerId = playerId(event, "getEntity");
        sink.died(playerId);
        sink.leaveDelaySignal(playerId,
                LeaveDelayPolicy.Signal.DEATH);
    }

    private static PlayerId playerId(final Object event, final String accessor) {
        return id(PaperReflection.invoke(event, accessor, new Class<?>[0]));
    }
    private static PlayerId id(final Object player) {
        return PlayerId.of((java.util.UUID) PaperReflection.invoke(player,
                "getUniqueId", new Class<?>[0]));
    }

    /** Typed input sink implemented by the game composition root. */
    public interface Sink {
        /** Records a disconnect input. */ void disconnected(PlayerId playerId);
        /** Records a death input. */ void died(PlayerId playerId);
        /** Records one leave-delay cancellation signal. */ void leaveDelaySignal(PlayerId playerId, LeaveDelayPolicy.Signal signal);
    }
}
