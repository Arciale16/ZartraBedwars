package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.command.api.CommandModel;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.command.paper.BoundedCommandSupervisor;
import io.zartra.bedwars.command.paper.PaperCommandAdapter;
import io.zartra.bedwars.command.paper.UnifiedCommandTreeFactory;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiIntegration;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiLifecycle;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderApiProviders;
import io.zartra.bedwars.paper.atlas.AtlasAudience;
import io.zartra.bedwars.paper.atlas.AtlasCommandRouter;
import io.zartra.bedwars.paper.atlas.AtlasPaperPort;
import io.zartra.bedwars.paper.atlas.AtlasRuntimeBootstrap;
import io.zartra.bedwars.paper.atlas.PaperAtlasService;
import io.zartra.bedwars.paper.replay.BukkitReplayAudience;
import io.zartra.bedwars.paper.replay.BukkitReplayRuntimeAdapter;
import io.zartra.bedwars.paper.replay.PaperReplayCommands;
import io.zartra.bedwars.paper.replay.PaperReplayService;
import io.zartra.bedwars.paper.replay.ReplayAudience;
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
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.ui.api.ConfirmationFramework;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Primary Paper 1.21.1 application composition root. */
public final class ZartraBedWarsPlugin extends JavaPlugin {
    private PaperFoundationRuntime runtime;
    private BoundedCommandSupervisor commandSupervisor;
    private List<PaperCommandAdapter> commandAdapters = Collections.emptyList();
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
            installCommandRuntime();
            installAtlasRuntime(degradedAtlasPort());
            installReplayRuntime(degradedReplayRepository());
            installReplayStaffTools(degradedReplayStaffStore(), degradedReplayAudit(), Instant::now);
            registerReplayCommand();
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
        if (commandSupervisor != null) {
            commandSupervisor.close(Duration.ZERO);
            commandSupervisor = null;
            commandAdapters = Collections.emptyList();
        }
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
        getLogger().info("Replay runtime installed");
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
        final PluginCommand command = Objects.requireNonNull(getCommand("atlas"), "atlas command");
        command.setExecutor((sender, ignored, label, arguments) -> {
            final AtlasAudience audience = atlasAudience(sender);
            try {
                atlasCommands.route(audience, arguments).whenComplete((result, failure) ->
                        Bukkit.getScheduler().runTask(this, () -> audience.present(
                                failure == null ? atlasMessage(result)
                                        : "Atlas request failed")));
            } catch (SecurityException denied) {
                audience.present("Atlas permission denied");
            }
            return true;
        });
        command.setTabCompleter((sender, ignored, alias, arguments) ->
                arguments.length <= 1 ? atlasCommands.actions() : Collections.emptyList());
        getLogger().info("Atlas runtime installed");
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

    /** @return M21 provider compatibility check for registration with Plugin Doctor */
    public synchronized PluginDoctor.Check providerCompatibilityCheck() {
        if (providerIntegrations == null) {
            throw new IllegalStateException("M21 provider runtime is not active");
        }
        return providerIntegrations.compatibilityCheck();
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
    private void installCommandRuntime() {
        final List<PresentationActions.Definition> catalogue = PresentationActions.Catalog.standard();
        final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings =
                new LinkedHashMap<PresentationActions.ActionId, PresentationActions.UseCase>();
        for (PresentationActions.Definition definition : catalogue) {
            bindings.put(definition.id(), request -> CompletableFuture.completedFuture(
                    PresentationActions.Response.simple(PresentationActions.Response.Status.ERROR,
                            "presentation.runtime.unavailable", request.revision())));
        }
        final PresentationActions.Registry actions =
                new PresentationActions.Registry(catalogue, bindings);
        final ConfirmationFramework confirmations = new ConfirmationFramework(
                this::authorizePaperCommand, TimeSource.SystemTimeSource.INSTANCE,
                UUID::randomUUID, record -> getLogger().fine(
                        "Command confirmation " + record.action()),
                Duration.ofSeconds(30L), 512, catalogue);
        final Map<String, CommandModel.Node> roots =
                new UnifiedCommandTreeFactory(actions, confirmations).create(catalogue);
        commandSupervisor = new BoundedCommandSupervisor(2, 128, "zbw-command");
        final List<PaperCommandAdapter> installed = new ArrayList<PaperCommandAdapter>();
        installCommandAdapter("zbw", roots, installed);
        if (roots.containsKey("deposit")) {
            installCommandAdapter("deposit", roots, installed);
        }
        commandAdapters = Collections.unmodifiableList(installed);
        getLogger().info("ZartraBedWars command runtime installed");
    }

    private void installCommandAdapter(final String label,
                                       final Map<String, CommandModel.Node> roots,
                                       final List<PaperCommandAdapter> installed) {
        final CommandFramework framework = new CommandFramework(
                Objects.requireNonNull(roots.get(label), label + " command root"),
                this::authorizePaperCommand, TimeSource.SystemTimeSource.INSTANCE,
                commandSupervisor, record -> getLogger().fine(
                        "Command " + record.commandId() + " " + record.phase()), 512);
        final PaperCommandAdapter adapter = new PaperCommandAdapter(framework,
                fallbackLocalization(), PaperCommandAdapter.standardSubjects(),
                PaperCommandAdapter.bukkitOutput(this));
        adapter.register(this, Collections.singletonList(label));
        installed.add(adapter);
    }

    private AuthorizationDecision authorizePaperCommand(final AuthorizationRequest request) {
        if (request.subject().kind() == AuthorizationSubject.Kind.CONSOLE) {
            return AuthorizationDecision.allow(
                    DefinitionId.of("zartra", "authorization/paper-console"));
        }
        if (request.subject().kind() != AuthorizationSubject.Kind.PLAYER) {
            return AuthorizationDecision.deny(
                    DefinitionId.of("zartra", "authorization/unsupported-subject"));
        }
        final String path = request.subject().id().path();
        if (!path.startsWith("player/")) {
            return AuthorizationDecision.deny(
                    DefinitionId.of("zartra", "authorization/malformed-player"));
        }
        try {
            final Player player = Bukkit.getPlayer(UUID.fromString(path.substring("player/".length())));
            if (player != null && player.hasPermission(request.action().value())) {
                return AuthorizationDecision.allow(
                        DefinitionId.of("zartra", "authorization/paper-permission"));
            }
        } catch (IllegalArgumentException malformed) {
            getLogger().fine("Rejected malformed command subject");
        }
        return AuthorizationDecision.deny(
                DefinitionId.of("zartra", "authorization/paper-permission-denied"));
    }

    private void registerReplayCommand() {
        final PluginCommand command = Objects.requireNonNull(getCommand("replay"), "replay command");
        command.setExecutor((sender, ignored, label, arguments) -> {
            final ReplayAudience audience = replayAudience(sender);
            final List<String> tokens = java.util.Arrays.asList(arguments);
            final CompletionStage<?> result;
            try {
                if (!tokens.isEmpty() && "staff".equalsIgnoreCase(tokens.get(0))) {
                    result = replayStaffCommands.route(audience,
                            tokens.subList(1, tokens.size()));
                } else {
                    result = replayViewerCommands.route(audience, tokens);
                }
            } catch (SecurityException denied) {
                sender.sendMessage("Replay permission denied");
                return true;
            }
            result.whenComplete((value, failure) -> Bukkit.getScheduler().runTask(this,
                    () -> sender.sendMessage(failure == null
                            ? "Replay " + replayStatus(value)
                            : "Replay request failed")));
            return true;
        });
        command.setTabCompleter((sender, ignored, alias, arguments) -> {
            if (arguments.length <= 1) {
                return java.util.Arrays.asList("open", "info", "play", "pause", "speed",
                        "seek", "stop", "staff");
            }
            if ("staff".equalsIgnoreCase(arguments[0]) && arguments.length == 2) {
                return java.util.Arrays.asList("search", "inspect", "open", "mark",
                        "archive", "remove-invalid");
            }
            return Collections.emptyList();
        });
    }

    private static String replayStatus(final Object result) {
        if (result instanceof io.zartra.bedwars.paper.replay.viewer.ReplayViewerResult) {
            return ((io.zartra.bedwars.paper.replay.viewer.ReplayViewerResult) result)
                    .status().name().toLowerCase(Locale.ROOT);
        }
        if (result instanceof io.zartra.bedwars.paper.replay.staff.ReplayStaffResult) {
            return ((io.zartra.bedwars.paper.replay.staff.ReplayStaffResult) result)
                    .status().name().toLowerCase(Locale.ROOT);
        }
        return "request completed";
    }

    private static String atlasMessage(final Object result) {
        if (result instanceof List<?>) {
            return "Atlas cases: " + ((List<?>) result).size();
        }
        return "Atlas request accepted";
    }

    private static AtlasAudience atlasAudience(final CommandSender sender) {
        return new AtlasAudience() {
            @Override public UUID playerId() {
                return sender instanceof Player
                        ? ((Player) sender).getUniqueId()
                        : UUID.nameUUIDFromBytes(("atlas:" + sender.getName())
                                .getBytes(StandardCharsets.UTF_8));
            }
            @Override public boolean hasPermission(final String permission) {
                return sender.hasPermission(permission);
            }
            @Override public void present(final String message) {
                sender.sendMessage(message);
            }
        };
    }

    private static ReplayAudience replayAudience(final CommandSender sender) {
        if (sender instanceof Player) {
            return new BukkitReplayAudience(sender);
        }
        return new ReplayAudience() {
            @Override public UUID playerId() {
                return UUID.nameUUIDFromBytes(("replay:" + sender.getName())
                        .getBytes(StandardCharsets.UTF_8));
            }
            @Override public boolean hasPermission(final String permission) {
                return sender.hasPermission(permission);
            }
            @Override public Object enterSpectatorReplay() {
                return this;
            }
            @Override public void leaveSpectatorReplay(final Object restoration) {
                // Non-player senders have no Paper spectator state.
            }
        };
    }
    static ReplaySessionRepository degradedReplayRepository() {
        return new ReplaySessionRepository() {
            @Override public CompletionStage<Boolean> create(final ReplaySession session) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            @Override public CompletionStage<SaveResult> save(
                    final ReplaySession session, final ReplayState expectedState) {
                return CompletableFuture.completedFuture(SaveResult.NOT_FOUND);
            }
            @Override public CompletionStage<Optional<ReplaySession>> findSession(
                    final ReplayId replayId) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        };
    }

    static ReplayStaffStore degradedReplayStaffStore() {
        return new ReplayStaffStore() {
            @Override public CompletionStage<List<io.zartra.bedwars.paper.replay.staff.ReplayStaffRecord>>
                    search(final io.zartra.bedwars.paper.replay.staff.ReplayStaffQuery query) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }
            @Override public CompletionStage<Optional<io.zartra.bedwars.paper.replay.staff.ReplayStaffRecord>>
                    find(final ReplayId replayId) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            @Override public CompletionStage<Boolean> mark(
                    final ReplayId replayId, final boolean marked) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            @Override public CompletionStage<Boolean> removeInvalid(final ReplayId replayId) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
        };
    }

    static ReplayStaffAuditSink degradedReplayAudit() {
        return record -> CompletableFuture.completedFuture(null);
    }

    static AtlasPaperPort degradedAtlasPort() {
        return new AtlasPaperPort() {
            @Override public CompletionStage<List<io.zartra.bedwars.paper.atlas.AtlasCaseSummary>>
                    list(final UUID actorId) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }
            @Override public CompletionStage<io.zartra.bedwars.paper.atlas.AtlasView> open(
                    final UUID actorId,
                    final io.zartra.bedwars.atlas.api.AtlasCaseId caseId) {
                final CompletableFuture<io.zartra.bedwars.paper.atlas.AtlasView> failed =
                        new CompletableFuture<io.zartra.bedwars.paper.atlas.AtlasView>();
                failed.completeExceptionally(new IllegalStateException("Atlas storage unavailable"));
                return failed;
            }
            @Override public CompletionStage<Boolean> beginReview(
                    final UUID actorId,
                    final io.zartra.bedwars.atlas.api.AtlasCaseId caseId) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            @Override public CompletionStage<Boolean> submitVerdict(
                    final UUID actorId,
                    final io.zartra.bedwars.atlas.api.AtlasCaseId caseId,
                    final String verdict, final String reason) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            @Override public CompletionStage<Boolean> finalReview(
                    final UUID actorId,
                    final io.zartra.bedwars.atlas.api.AtlasCaseId caseId,
                    final String disposition) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
            @Override public CompletionStage<String> diagnostics() {
                return CompletableFuture.completedFuture("Atlas storage unavailable");
            }
        };
    }

    private static LocalizationService fallbackLocalization() {
        return new LocalizationService() {
            @Override public Result<LocalizedMessage> render(
                    final io.zartra.bedwars.api.localization.MessageKey key,
                    final Optional<PlayerId> player, final Parameters parameters) {
                return Result.success(LocalizedMessage.of(
                        LocaleId.parse("en"), key, key.value()));
            }
            @Override public Result<LocaleId> switchServerLocale(final LocaleId locale) {
                return Result.success(locale);
            }
            @Override public Result<LocaleId> switchPlayerLocale(
                    final PlayerId player, final LocaleId locale) {
                return Result.success(locale);
            }
        };
    }
}
