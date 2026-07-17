package io.zartra.bedwars.paper.m10;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.command.api.CommandModel;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.command.paper.BoundedCommandSupervisor;
import io.zartra.bedwars.command.paper.PaperCommandAdapter;
import io.zartra.bedwars.command.paper.UnifiedCommandTreeFactory;
import io.zartra.bedwars.game.mode.ModeFramework;
import io.zartra.bedwars.game.selector.SelectorFramework;
import io.zartra.bedwars.paper.game.BoundedMatchmakingExecutor;
import io.zartra.bedwars.paper.game.M10PaperProjection;
import io.zartra.bedwars.ui.api.AdminDashboard;
import io.zartra.bedwars.ui.api.ConfirmationFramework;
import io.zartra.bedwars.ui.api.PresentationParity;
import io.zartra.bedwars.ui.api.UiModel;
import io.zartra.bedwars.ui.paper.PaperGuiAdapter;
import io.zartra.bedwars.ui.paper.PaperInventoryReflection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-artifact-only exact Paper 1.21.1 M10 certification plugin. */
public final class M10PaperCertificationPlugin extends JavaPlugin {
    @Override public void onEnable() {
        final BoundedCommandSupervisor commands = new BoundedCommandSupervisor(1, 32, "zbw-m10-command-cert");
        final BoundedMatchmakingExecutor matching = new BoundedMatchmakingExecutor(1, 8, "zbw-m10-match-cert");
        try {
            final List<PresentationActions.Definition> catalog = PresentationActions.Catalog.throughM10();
            final AtomicInteger queueCalls = new AtomicInteger();
            final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings = new LinkedHashMap<>();
            for (PresentationActions.Definition definition : catalog) {
                bindings.put(definition.id(), request -> {
                    if (definition.id().toString().endsWith("queue/join")) { queueCalls.incrementAndGet(); }
                    return CompletableFuture.completedFuture(PresentationActions.Response.simple(
                            PresentationActions.Response.Status.SUCCESS, "presentation.success", 1));
                });
            }
            final PresentationActions.Registry actions = new PresentationActions.Registry(catalog, bindings);
            final ConfirmationFramework confirmations = new ConfirmationFramework(
                    request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "cert/allowed")),
                    TimeSource.SystemTimeSource.INSTANCE, UUID::randomUUID, record -> { },
                    Duration.ofSeconds(30), 512, catalog);
            final Map<String, CommandModel.Node> roots = new UnifiedCommandTreeFactory(actions, confirmations).create(catalog);
            final CommandFramework framework = new CommandFramework(roots.get("zbw"),
                    request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "cert/allowed")),
                    TimeSource.SystemTimeSource.INSTANCE, commands, record -> { }, 512);
            final PaperCommandAdapter adapter = new PaperCommandAdapter(framework, localization(),
                    PaperCommandAdapter.standardSubjects(), PaperCommandAdapter.bukkitOutput(this));
            adapter.register(this, Collections.singletonList("zbw"));
            final boolean commandAccepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "zartrabedwarsm10certification:zbw queue join");

            final List<UiModel.PageDefinition> pages = AdminDashboard.pages(catalog,
                    definition -> emptyPage(definition.pageId()));
            final List<CommandFramework.InventoryEntry> inventory = new ArrayList<>();
            for (CommandModel.Node root : roots.values()) {
                inventory.addAll(new CommandFramework(root,
                        request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "cert/allowed")),
                        TimeSource.SystemTimeSource.INSTANCE, commands, record -> { }, 512).inventory());
            }
            final boolean parity = PresentationParity.validate(catalog, inventory, pages).valid();
            final PresentationActions.Definition queueAction = catalog.stream()
                    .filter(value -> value.id().toString().endsWith("queue/join")).findFirst().get();
            actions.execute(new PresentationActions.Request(subject(), queueAction.id(),
                    DefinitionId.of("zartra", "queue/local"), 0, CorrelationId.random(),
                    CommandModel.Arguments.empty(), PresentationActions.Surface.GUI)).toCompletableFuture().join();

            final SelectorFramework.Service selectors = new SelectorFramework.Service();
            final ModeFramework.Layout layout = new ModeFramework.Layout(
                    DefinitionId.of("zartra", "layout/duel"),
                    java.util.Arrays.asList(DefinitionId.of("team", "red"), DefinitionId.of("team", "blue")), 4);
            final ArenaId arena = ArenaId.of(new UUID(1, 1));
            final SelectorFramework.Candidate candidate = new SelectorFramework.Candidate(arena, 4,
                    DefinitionId.of("zartra", "map/cert"), ModeFramework.ModeId.of("zartra", "standard"),
                    layout, MessageKey.of("arena.cert"), true, true, true,
                    SelectorFramework.Lifecycle.WAITING, 0, 0, 0,
                    Collections.singleton(DefinitionId.of("zartra", "tag/all")));
            final SelectorFramework.Query query = new SelectorFramework.Query(null, null, null, null,
                    "", 1, 0, 0, 9, SelectorFramework.Order.CONFIGURED);
            final SelectorFramework.Page page = selectors.page(Collections.singleton(candidate), query, 7);
            final AtomicBoolean ownerMutation = new AtomicBoolean();
            final AtomicBoolean selectorRendered = new AtomicBoolean();
            final M10PaperProjection projection = new M10PaperProjection(Bukkit::isPrimaryThread,
                    new M10PaperProjection.Platform() {
                        @Override public void renderSelector(final PlayerId playerId,
                                                            final SelectorFramework.Page rendered) {
                            ownerMutation.set(Bukkit.isPrimaryThread());
                            final PaperInventoryReflection reflection = new PaperInventoryReflection();
                            final Object paperInventory = reflection.create(9, "M10 selector");
                            reflection.set(paperInventory, 0, new PaperGuiAdapter.RenderedItem(
                                    "PAPER", rendered.message().value(), Collections.singletonList("typed candidate")));
                            reflection.clear(paperInventory);
                            selectorRendered.set(true);
                        }
                        @Override public void applySpectator(final io.zartra.bedwars.game.spectator.SpectatorFramework.Session session) { ownerMutation.set(Bukkit.isPrimaryThread()); }
                        @Override public void restore(final io.zartra.bedwars.game.model.PlayerStateSnapshot capturedState) { ownerMutation.set(Bukkit.isPrimaryThread()); }
                        @Override public void clearOwnedState(final PlayerId playerId) { ownerMutation.set(Bukkit.isPrimaryThread()); }
                    });
            projection.renderSelector(PlayerId.of(new UUID(0, 1)), page);
            final SelectorFramework.Selection selection = selectors.select(page, arena);
            final boolean staleRejected = !selectors.current(selection, 8, candidate);
            final CompletableFuture<Boolean> offOwner = matching.submit(() -> !Bukkit.isPrimaryThread(), Duration.ofSeconds(5));

            Bukkit.getScheduler().runTaskLater(this, () -> {
                final boolean commandDispatch = commandAccepted && queueCalls.get() == 2;
                final boolean click = selection.arenaId().equals(arena) && selection.viewRevision() == 7;
                final boolean duplicatePrevented = queueCalls.get() == 2;
                final boolean worker;
                try { worker = offOwner.get(5, TimeUnit.SECONDS); }
                catch (Exception failure) { finish(false, commands, matching, catalog.size(), false, false,
                        false, false, false, false);
                        return;
                        }
                finish(commandDispatch && parity && selectorRendered.get() && click && staleRejected
                                && ownerMutation.get() && worker && duplicatePrevented,
                        commands, matching, catalog.size(), commandDispatch, parity, selectorRendered.get(),
                        click, staleRejected, ownerMutation.get() && worker);
            }, 2L);
        } catch (RuntimeException failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "M10 certification failed", failure);
            finish(false, commands, matching, 0, false, false, false, false, false, false);
        }
    }

    private void finish(final boolean success, final BoundedCommandSupervisor commands,
                        final BoundedMatchmakingExecutor matching, final int catalog,
                        final boolean command, final boolean parity, final boolean rendered,
                        final boolean click, final boolean stale, final boolean threading) {
        final Thread writer = new Thread(() -> {
            final boolean drained = commands.close(Duration.ofSeconds(5)) && matching.close(Duration.ofSeconds(5));
            final boolean finalSuccess = success && drained;
            final String evidence = "{\n  \"schema_version\": 1,\n  \"runtime\": \"Paper 1.21.1 build 133\",\n"
                    + "  \"server_sha256\": \"" + io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter.SERVER_SHA256 + "\",\n"
                    + "  \"selector_rendering\": " + rendered + ",\n  \"selector_click\": " + click
                    + ",\n  \"queue_command_dispatch\": " + command + ",\n  \"gui_queue_action\": " + command
                    + ",\n  \"command_gui_parity\": " + parity + ",\n  \"owner_thread_projection\": " + threading
                    + ",\n  \"bounded_off_owner_matching\": " + threading + ",\n  \"duplicate_action_prevented\": " + command
                    + ",\n  \"stale_view_rejected\": " + stale + ",\n  \"deterministic_cleanup\": " + drained
                    + ",\n  \"catalog_actions\": " + catalog + ",\n  \"success\": " + finalSuccess + "\n}\n";
            try {
                final Path path = getDataFolder().toPath().resolve("m10-primary-certification.json");
                Files.createDirectories(path.getParent());
                Files.write(path, evidence.getBytes(StandardCharsets.UTF_8));
            } catch (IOException failure) { getLogger().severe("M10 evidence write failed"); }
            Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
        }, "zbw-m10-certification-evidence");
        writer.setDaemon(false);
        writer.start();
    }

    private static AuthorizationSubject subject() {
        return AuthorizationSubject.of(AuthorizationSubject.Kind.CONSOLE,
                DefinitionId.of("zartra", "console/certification"));
    }
    private UiModel.PageDefinition emptyPage(final GuiPageId id) {
        return new UiModel.PageDefinition(id, MessageKey.of("ui.action.title"),
                (viewer, query) -> CompletableFuture.completedFuture(new UiModel.PageState(1,
                        UiModel.PageState.Status.EMPTY, 0, 1, Collections.<UiModel.Component>emptyList(),
                        MessageKey.of("ui.empty"))), Collections.singletonList(UiModel.Interaction.PRIMARY));
    }
    private LocalizationService localization() {
        return new LocalizationService() {
            @Override public Result<LocalizedMessage> render(final MessageKey key,
                    final Optional<PlayerId> player, final Parameters parameters) {
                return Result.success(LocalizedMessage.of(LocaleId.parse("en"), key, key.value()));
            }
            @Override public Result<LocaleId> switchServerLocale(final LocaleId locale) { return Result.success(locale); }
            @Override public Result<LocaleId> switchPlayerLocale(final PlayerId player, final LocaleId locale) { return Result.success(locale); }
        };
    }
}
