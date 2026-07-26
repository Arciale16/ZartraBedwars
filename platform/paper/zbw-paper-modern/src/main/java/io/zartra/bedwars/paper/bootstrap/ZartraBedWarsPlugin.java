package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiIntegration;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiLifecycle;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiProviders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Primary Paper 1.21.1 build 133 bootstrap for the M06 foundation only. */
public final class ZartraBedWarsPlugin extends JavaPlugin {
    private PaperFoundationRuntime runtime;
    private PlaceholderApiIntegration placeholderIntegration;

    @Override public void onEnable() {
        saveDefaultConfig();
        try {
            final PaperFoundationSettings settings = PaperFoundationSettings.from(getConfig());
            runtime = new PaperFoundationRuntime(this, Bukkit.getWorldContainer().toPath(),
                    settings, TimeSource.SystemTimeSource.INSTANCE);
            runtime.start();
            initializePlaceholderApi();
            getLogger().info("M06 compatibility and world-provider foundation enabled");
            if ("true".equalsIgnoreCase(System.getenv("ZBW_M06_CERTIFY"))) {
                new PrimaryRuntimeCertification(this, runtime, settings.operationTimeout()).start();
            }
        } catch (RuntimeException failure) {
            getLogger().severe("M06 foundation configuration or startup failed");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override public void onDisable() {
        if (placeholderIntegration != null) {
            placeholderIntegration.close();
            placeholderIntegration = null;
            getLogger().info("PlaceholderAPI integration shut down");
        }
        if (runtime != null) {
            runtime.stop();
            getLogger().info("M06 foundation shutdown initiated without owner-thread blocking");
        }
    }

    private void initializePlaceholderApi() {
        try {
            final PlaceholderApiIntegration candidate = new PlaceholderApiIntegration(
                    new PlaceholderApiLifecycle(PlaceholderApiProviders.fallback())
            );
            if (candidate.initialize(this)) {
                placeholderIntegration = candidate;
                getLogger().info("PlaceholderAPI expansion initialized");
                return;
            }
            getLogger().info("PlaceholderAPI not available at startup; continuing with fallback placeholders");
        } catch (final Exception failure) {
            getLogger().warning("PlaceholderAPI integration disabled: " + failure.getClass().getSimpleName());
        }
    }
}
