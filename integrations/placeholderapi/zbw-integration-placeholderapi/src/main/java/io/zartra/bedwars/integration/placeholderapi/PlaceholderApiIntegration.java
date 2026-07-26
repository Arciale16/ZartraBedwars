package io.zartra.bedwars.integration.placeholderapi;

import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderRegistry;
import java.util.Objects;
import org.bukkit.plugin.Plugin;

/**
 * Lifecycle entrypoint for PlaceholderAPI integration.
 *
 * The integration is optional and remains disabled when PlaceholderAPI is absent.
 */
public final class PlaceholderApiIntegration {

    private final PlaceholderApiLifecycle lifecycle;
    private PlaceholderExpansionIntegration.PluginExpansion expansion;
    private boolean registered;

    public PlaceholderApiIntegration(final PlaceholderApiLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public boolean initialize(final Plugin plugin) {
        final PlaceholderRegistry registry = lifecycle.registry();
        if (!new PlaceholderExpansionIntegration(lifecycle).register(plugin)) {
            return false;
        }
        if (!isPlaceholderApiAvailable()) {
            return false;
        }
        expansion = new PlaceholderExpansionIntegration.PluginExpansion(registry);
        registered = true;
        expansion.register();
        return true;
    }

    public void close() {
        if (expansion != null) {
            expansion.unregister();
        }
        registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }

    private static boolean isPlaceholderApiAvailable() {
        return tryLoad("me.clip.placeholderapi.PlaceholderAPI");
    }

    private static boolean tryLoad(final String name) {
        try {
            Class.forName(name, false, PlaceholderApiIntegration.class.getClassLoader());
            return true;
        } catch (final Exception exception) {
            return false;
        }
    }
}
