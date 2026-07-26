package io.zartra.bedwars.integration.placeholderapi;

/**
 * Pure Java entry point for bootstrapping PlaceholderAPI from the Paper runtime.
 */
public final class PlaceholderBootstrap {

    private PlaceholderBootstrap() {
    }

    public static boolean canStart(final Object plugin, final PlaceholderApiProviders providers) {
        try {
            final PlaceholderApiIntegration integration = new PlaceholderApiIntegration(new PlaceholderApiLifecycle(providers));
            return integration.initialize(plugin);
        } catch (final Exception e) {
            return false;
        }
    }
}
