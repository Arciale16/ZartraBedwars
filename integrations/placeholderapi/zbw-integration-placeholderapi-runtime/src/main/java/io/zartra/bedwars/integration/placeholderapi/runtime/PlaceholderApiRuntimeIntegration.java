package io.zartra.bedwars.integration.placeholderapi.runtime;

import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiLifecycle;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderExpansionIntegration;
import org.bukkit.plugin.Plugin;

/**
 * Runtime-only adapter that depends on external PlaceholderAPI at compile time.
 */
public final class PlaceholderApiRuntimeIntegration {

    private PlaceholderApiRuntimeIntegration() {
    }

    public static Object initialize(final Plugin plugin, final PlaceholderApiLifecycle lifecycle) {
        final PlaceholderExpansionIntegration integration = new PlaceholderExpansionIntegration(lifecycle);
        if (!integration.register(plugin)) {
            return null;
        }

        final PlaceholderExpansionIntegration.PluginExpansion expansion =
                new PlaceholderExpansionIntegration.PluginExpansion(lifecycle.registry());
        expansion.register();
        return new RuntimeHandle(expansion);
    }

    public static void close(final Object handle) {
        if (!(handle instanceof RuntimeHandle)) {
            return;
        }
        final RuntimeHandle runtimeHandle = (RuntimeHandle) handle;
        runtimeHandle.close();
    }

    private static final class RuntimeHandle implements AutoCloseable {
        private final PlaceholderExpansionIntegration.PluginExpansion expansion;

        RuntimeHandle(final PlaceholderExpansionIntegration.PluginExpansion expansion) {
            this.expansion = expansion;
        }

        @Override
        public void close() {
            expansion.unregister();
        }
    }
}
