package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiIntegration;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiLifecycle;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiProviders;
import io.zartra.bedwars.paper.atlas.AtlasAudience;
import io.zartra.bedwars.paper.atlas.AtlasCommandRouter;
import io.zartra.bedwars.paper.atlas.AtlasPaperPort;
import io.zartra.bedwars.paper.atlas.AtlasRuntimeBootstrap;
import io.zartra.bedwars.paper.atlas.PaperAtlasService;
import io.zartra.bedwars.paper.replay.BukkitReplayRuntimeAdapter;
import io.zartra.bedwars.paper.replay.PaperReplayCommands;
import io.zartra.bedwars.paper.replay.PaperReplayService;
import io.zartra.bedwars.paper.replay.ReplayRuntimeBootstrap;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffAuditSink;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffCommandRouter;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffService;
import io.zartra.bedwars.paper.replay.staff.ReplayStaffStore;
import io.zartra.bedwars.paper.replay.viewer.BukkitReplayViewerPresentation;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerAdapter;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerBootstrap;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerCommandRouter;
import io.zartra.bedwars.paper.replay.visual.BukkitReplayVisualRenderer;
import io.zartra.bedwars.paper.replay.visual.ReplayVisualAdapter;
import io.zartra.bedwars.paper.replay.visual.ReplayVisualEngine;
import io.zartra.bedwars.replay.api.ReplayAccessPolicy;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Primary Paper 1.21.1 build 133 bootstrap for the M06 foundation only. */
public final class ZartraBedWarsPlugin extends JavaPlugin {
    private PaperFoundationRuntime runtime;
    private PlaceholderApiIntegration placeholderIntegration;
    private ReplayRuntimeBootstrap replayRuntime;
    private ReplayViewerBootstrap replayViewerRuntime;
    private ReplayViewerCommandRouter replayViewerCommands;
    private ReplayViewerAdapter replayViewerAdapter;
    private ReplayStaffCommandRouter replayStaffCommands;
    private ReplaySessionRepository replayRepository;
    private AtlasRuntimeBootstrap atlasRuntime;
    private AtlasCommandRouter atlasCommands;
    private PaperProviderIntegrationRuntime providerIntegrations;

    @Override public void onEnable() {
        saveDefaultConfig();
        try {
            final PaperFoundationSettings settings = PaperFoundationSettings.from(getConfig());
            runtime = new PaperFoundationRuntime(this, Bukkit.getWorldContainer().toPath(),
                    settings, TimeSource.SystemTimeSource.INSTANCE);
            runtime.start();
            providerIntegrations = new PaperProviderIntegrationRuntime();
            providerIntegrations.start();
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
        if (providerIntegrations != null) {
            providerIntegrations.close();
            providerIntegrations = null;
            getLogger().info("M21 optional provider integrations cleanup scheduled");
        }
        if (atlasRuntime != null) {
            atlasRuntime.stop();
            atlasRuntime = null;
            atlasCommands = null;
            getLogger().info("M18 Atlas Paper runtime cleanup complete");
        }
        if (replayViewerRuntime != null) {
            replayViewerRuntime.stop();
            replayViewerRuntime = null;
            replayViewerCommands = null;
            replayViewerAdapter = null;
            replayStaffCommands = null;
            replayRepository = null;
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
        final ReplayVisualAdapter visuals = new ReplayVisualAdapter(
                new ReplayVisualEngine(128, 256), new BukkitReplayVisualRenderer(), 2L);
        final ReplayViewerAdapter viewer = new ReplayViewerAdapter(commands,
                new BukkitReplayViewerPresentation(Bukkit::getPlayer), visuals,
                Bukkit::getCurrentTick);
        final ReplayViewerBootstrap viewerCandidate = new ReplayViewerBootstrap(viewer, adapter);
        try {
            viewerCandidate.start();
        } catch (RuntimeException failure) {
            candidate.stop();
            throw failure;
        }
        replayRuntime = candidate;
        replayRepository = repository;
        replayViewerRuntime = viewerCandidate;
        replayViewerAdapter = viewer;
        replayViewerCommands = new ReplayViewerCommandRouter(viewer);
        getLogger().info("M17 Paper replay runtime and viewer foundation installed");
        return commands;
    }

    /**
     * Installs M17 staff tools after the replay viewer runtime is composed.
     *
     * @param store asynchronous replay search/moderation provider
     * @param audit authoritative non-blocking audit sink
     * @param time injected audit time source
     * @return strict staff command router
     */
    public synchronized ReplayStaffCommandRouter installReplayStaffTools(
            final ReplayStaffStore store, final ReplayStaffAuditSink audit,
            final Supplier<Instant> time) {
        if (replayViewerAdapter == null) {
            throw new IllegalStateException("M17 replay viewer runtime is not installed");
        }
        if (replayStaffCommands != null) {
            throw new IllegalStateException("M17 replay staff tools already installed");
        }
        final ReplayStaffService service = new ReplayStaffService(
                store, replayRepository, audit, time);
        replayStaffCommands = new ReplayStaffCommandRouter(service, replayViewerAdapter);
        getLogger().info("M17 Paper replay staff tools installed");
        return replayStaffCommands;
    }

    /**
     * Installs the M18 Atlas adapter once its asynchronous application port is composed.
     *
     * @param port authoritative non-blocking Atlas application boundary
     * @return strict `/atlas` command router
     */
    public synchronized AtlasCommandRouter installAtlasRuntime(final AtlasPaperPort port) {
        if (atlasRuntime != null) {
            throw new IllegalStateException("M18 Atlas runtime already installed");
        }
        final PaperAtlasService service = new PaperAtlasService(port,
                command -> Bukkit.getScheduler().runTask(this, command));
        final AtlasRuntimeBootstrap candidate = new AtlasRuntimeBootstrap(service);
        atlasCommands = candidate.start();
        atlasRuntime = candidate;
        Objects.requireNonNull(getCommand("atlas"), "atlas command")
                .setExecutor((sender, command, label, arguments) -> {
                    final AtlasAudience audience = new AtlasAudience() {
                        @Override public UUID playerId() {
                            return UUID.nameUUIDFromBytes(("atlas:" + sender.getName())
                                    .getBytes(StandardCharsets.UTF_8));
                        }
                        @Override public boolean hasPermission(final String permission) {
                            return sender.hasPermission(permission);
                        }
                        @Override public void present(final String message) {
                            sender.sendMessage(message);
                        }
                    };
                    try {
                        atlasCommands.route(audience, arguments).whenComplete((result, failure) ->
                                Bukkit.getScheduler().runTask(this, () -> audience.present(
                                        failure == null ? "Atlas request accepted"
                                                : "Atlas request failed")));
                    } catch (SecurityException denied) {
                        audience.present("Atlas permission denied");
                    }
                    return true;
                });
        getLogger().info("M18 Atlas Paper adapter installed");
        return atlasCommands;
    }

    /**
     * Installs one optional M21 provider through manual constructor composition.
     *
     * @param provider isolated adapter backed by an operator-installed plugin
     * @return asynchronous adapter lifecycle result
     */
    public synchronized CompletionStage<Result<Provider.LifecycleState>>
            installProviderIntegration(final Provider provider) {
        if (providerIntegrations == null) {
            throw new IllegalStateException("M21 provider runtime is not active");
        }
        return providerIntegrations.install(provider);
    }

    /** @return installed `/atlas` router when Atlas ports are composed */
    public synchronized Optional<AtlasCommandRouter> atlasCommands() {
        return Optional.ofNullable(atlasCommands);
    }
    /** @return installed `/replay staff` router when staff ports are composed */
    public synchronized Optional<ReplayStaffCommandRouter> replayStaffCommands() {
        return Optional.ofNullable(replayStaffCommands);
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
