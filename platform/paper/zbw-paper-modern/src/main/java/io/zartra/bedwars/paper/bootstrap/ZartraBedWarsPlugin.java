package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.time.TimeSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Primary Paper 1.21.1 build 133 bootstrap for the M06 foundation only. */
public final class ZartraBedWarsPlugin extends JavaPlugin {
    private PaperFoundationRuntime runtime;

    @Override public void onEnable() {
        saveDefaultConfig();
        try {
            final PaperFoundationSettings settings = PaperFoundationSettings.from(getConfig());
            runtime = new PaperFoundationRuntime(this, Bukkit.getWorldContainer().toPath(),
                    settings, TimeSource.SystemTimeSource.INSTANCE);
            runtime.start();
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
        if (runtime != null) {
            runtime.stop();
            getLogger().info("M06 foundation shutdown initiated without owner-thread blocking");
        }
    }
}
