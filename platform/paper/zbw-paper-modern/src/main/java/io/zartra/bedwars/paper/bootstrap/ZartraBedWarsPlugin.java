package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiIntegration;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiLifecycle;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiProviders;
import io.zartra.bedwars.paper.replay.BukkitReplayRuntimeAdapter;
import io.zartra.bedwars.paper.replay.PaperReplayCommands;
import io.zartra.bedwars.paper.replay.PaperReplayService;
import io.zartra.bedwars.paper.replay.ReplayRuntimeBootstrap;
import io.zartra.bedwars.paper.replay.viewer.BukkitReplayViewerPresentation;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerAdapter;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerBootstrap;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerCommandRouter;
import io.zartra.bedwars.replay.api.ReplayAccessPolicy;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Primary Paper 1.21.1 build 133 bootstrap for the M06 foundation only. */
public final class ZartraBedWarsPlugin extends JavaPlugin {
    private PaperFoundationRuntime runtime;
    private PlaceholderApiIntegration placeholderIntegration;
    private ReplayRuntimeBootstrap replayRuntime;
    private ReplayViewerBootstrap replayViewerRuntime;
    private ReplayViewerCommandRouter replayViewerCommands;

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
        if (replayViewerRuntime != null) {
            replayViewerRuntime.stop();
            replayViewerRuntime = null;
            replayViewerCommands = null;
            getLogger().info("M17 replay viewer cleanup complete");
        }
        if (replayRuntime != null) {
            replayRuntime.stop();
            replayRuntime = null;
            getLogger().info("M17 replay runtime cleanup scheduled");
        }
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

    /**
     * Installs the M17 replay runtime once an asynchronous repository is available.
     *
     * @param repository authoritative non-blocking replay-session boundary
     * @return typed replay command-service foundation
     */
    public synchronized PaperReplayCommands installReplayRuntime(
            final ReplaySessionRepository repository) {
        if (replayRuntime != null) {
            throw new IllegalStateException("M17 replay runtime already installed");
        }
        final BukkitReplayRuntimeAdapter adapter = new BukkitReplayRuntimeAdapter(this);
        final PaperReplayService service = new PaperReplayService(repository,
                new ReplayAccessPolicy(), new ReplayPlaybackEngine(), adapter);
        final ReplayRuntimeBootstrap candidate = new ReplayRuntimeBootstrap(service, adapter);
        candidate.start();
        final PaperReplayCommands commands = candidate.commands();
        final ReplayViewerAdapter viewer = new ReplayViewerAdapter(commands,
                new BukkitReplayViewerPresentation(Bukkit::getPlayer));
        final ReplayViewerBootstrap viewerCandidate = new ReplayViewerBootstrap(viewer, adapter);
        try {
            viewerCandidate.start();
        } catch (RuntimeException failure) {
            candidate.stop();
            throw failure;
        }
        replayRuntime = candidate;
        replayViewerRuntime = viewerCandidate;
        replayViewerCommands = new ReplayViewerCommandRouter(viewer);
        getLogger().info("M17 Paper replay runtime and viewer foundation installed");
        return commands;
    }

    /** @return installed `/replay` viewer router when replay storage is composed */
    public synchronized Optional<ReplayViewerCommandRouter> replayViewerCommands() {
        return Optional.ofNullable(replayViewerCommands);
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
