package io.zartra.bedwars.integration.placeholderapi;

import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderContext;
import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderRegistry;
import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderResult;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Bridges Neutral placeholder resolution with PlaceholderAPI runtime expansion.
 */
public final class PlaceholderExpansionIntegration {

    private final PlaceholderApiLifecycle lifecycle;
    private volatile boolean loaded;

    public PlaceholderExpansionIntegration(final PlaceholderApiLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public boolean register(final Plugin plugin) {
        if (!isPlaceholderApiPresent()) {
            return false;
        }
        loaded = true;
        return true;
    }

    public void unregister() {
        loaded = false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    private static boolean isPlaceholderApiPresent() {
        return tryLoadClass("me.clip.placeholderapi.PlaceholderAPI")
                && tryLoadClass("me.clip.placeholderapi.expansion.PlaceholderExpansion");
    }

    private static boolean tryLoadClass(final String name) {
        try {
            Class.forName(name, false, PlaceholderExpansionIntegration.class.getClassLoader());
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public String resolve(final String identifier, final Player player, final PlaceholderRegistry registry) {
        if (!loaded) {
            return "-";
        }
        final UUID playerId = player == null ? null : player.getUniqueId();
        final PlaceholderResult result = registry.resolve(
                PlaceholderContext.of(PlaceholderLifecycle.NAMESPACE, playerId, normalize(identifier), "-", true)
        );
        return result.value();
    }

    private String normalize(final String identifier) {
        if (identifier == null) {
            return "-";
        }
        return identifier.toLowerCase();
    }

    public static final class PluginExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
        private final PlaceholderRegistry registry;

        public PluginExpansion(final PlaceholderRegistry registry) {
            this.registry = registry;
        }

        @Override
        public String getIdentifier() {
            return PlaceholderLifecycle.NAMESPACE;
        }

        @Override
        public String getAuthor() {
            return "Zartra";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public String onRequest(final OfflinePlayer player, final String params) {
            final UUID playerId = player == null ? null : player.getUniqueId();
            return registry.resolve(PlaceholderContext.of(PlaceholderLifecycle.NAMESPACE, playerId, normalize(params), "-", true)).value();
        }

        private String normalize(final String value) {
            return value == null ? "-" : value.toLowerCase();
        }
    }
}

/**
 * Backward-compatible minimal lifecycle alias kept for tests and external wiring.
 */
class PlaceholderLifecycle {
    static final String NAMESPACE = PlaceholderApiLifecycle.NAMESPACE;
}
