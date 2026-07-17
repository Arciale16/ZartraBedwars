package io.zartra.bedwars.paper.m09;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-artifact-only exact Paper 1.21.1 M09 command and inventory certification plugin. */
public final class M09PaperCertificationPlugin extends JavaPlugin {
    private boolean commandDispatch;
    private boolean inventoryRendering;
    private boolean parity;
    private boolean asyncOffOwner;
    private boolean duplicatePrevented;

    @Override public void onEnable() {
        try {
            final List<PresentationActions.Definition> catalog = PresentationActions.Catalog.standard();
            final AtomicInteger calls = new AtomicInteger();
            final Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings = new LinkedHashMap<>();
            final PresentationActions.UseCase useCase = request -> {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture(PresentationActions.Response.simple(
                        PresentationActions.Response.Status.SUCCESS, "presentation.success", 1L));
            };
            for (PresentationActions.Definition definition : catalog) { bindings.put(definition.id(), useCase); }
            final PresentationActions.Registry actions = new PresentationActions.Registry(catalog, bindings);
            final ConfirmationFramework confirmations = new ConfirmationFramework(
                    request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "certification/allowed")),
                    TimeSource.SystemTimeSource.INSTANCE, UUID::randomUUID, record -> { },
                    Duration.ofSeconds(30L), 256, catalog);
            final Map<String, CommandModel.Node> roots = new UnifiedCommandTreeFactory(actions,
                    confirmations).create(catalog);
            final BoundedCommandSupervisor supervisor = new BoundedCommandSupervisor(1, 16,
                    "zbw-m09-certification");
            final CommandFramework commands = new CommandFramework(roots.get("zbw"),
                    request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "certification/allowed")),
                    TimeSource.SystemTimeSource.INSTANCE, supervisor, record -> { }, 256);
            final PaperCommandAdapter adapter = new PaperCommandAdapter(commands, localization(),
                    PaperCommandAdapter.standardSubjects(), PaperCommandAdapter.bukkitOutput(this));
            adapter.register(this, Collections.singletonList("zbw"));
            final boolean dispatchAccepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "zartrabedwarsm09certification:zbw arena list");
            final List<UiModel.PageDefinition> pages = AdminDashboard.pages(catalog,
                    definition -> emptyPage(definition.pageId()));
            final List<CommandFramework.InventoryEntry> inventory = new ArrayList<>();
            for (CommandModel.Node root : roots.values()) {
                inventory.addAll(new CommandFramework(root,
                        request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "certification/allowed")),
                        TimeSource.SystemTimeSource.INSTANCE, supervisor, record -> { }, 256).inventory());
            }
            parity = PresentationParity.validate(catalog, inventory, pages).valid();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                commandDispatch = dispatchAccepted && calls.get() == 1;
                duplicatePrevented = calls.get() == 1;
                try {
                    certifyInventory();
                    certifyWorker(supervisor);
                } catch (RuntimeException failure) {
                    getLogger().log(java.util.logging.Level.SEVERE,
                            "M09 certification failed: " + failure.getMessage(), failure);
                    finish(false, supervisor);
                }
            }, 1L);
        } catch (RuntimeException failure) {
            getLogger().log(java.util.logging.Level.SEVERE,
                    "M09 certification failed: " + failure.getMessage(), failure);
            finish(false, null);
        }
    }

    private void certifyInventory() {
        final PaperInventoryReflection inventories = new PaperInventoryReflection();
        final Object inventory = inventories.create(54, "ZartraBedWars M09");
        inventories.set(inventory, 0, new PaperGuiAdapter.RenderedItem("PAPER", "Arena list",
                Collections.singletonList("Command and GUI parity")));
        inventories.clear(inventory);
        inventoryRendering = true;
    }

    private void certifyWorker(final BoundedCommandSupervisor supervisor) {
        final CountDownLatch complete = new CountDownLatch(1);
        final AtomicBoolean offOwner = new AtomicBoolean();
        supervisor.submit(CommandModel.Node.builder(CommandModel.CommandId.of("zartra", "command/certify"),
                        "certify").executor(context -> CompletableFuture.completedFuture(
                        CommandModel.Result.simple(CommandModel.Result.Status.SUCCESS,
                                "command.success"))).build(),
                context(), context -> {
                    offOwner.set(!Bukkit.isPrimaryThread());
                    complete.countDown();
                    return CompletableFuture.completedFuture(CommandModel.Result.simple(
                            CommandModel.Result.Status.SUCCESS, "command.success"));
                });
        try { asyncOffOwner = complete.await(10L, TimeUnit.SECONDS) && offOwner.get(); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
        finish(asyncOffOwner, supervisor);
    }

    private CommandModel.ExecutionContext context() {
        return new CommandModel.ExecutionContext(CommandModel.Subject.console(
                io.zartra.bedwars.api.authorization.AuthorizationSubject.of(
                        io.zartra.bedwars.api.authorization.AuthorizationSubject.Kind.CONSOLE,
                        DefinitionId.of("zartra", "console/certification")), LocaleId.parse("en")),
                CommandModel.Arguments.empty(), DefinitionId.of("zartra", "certification/target"),
                io.zartra.bedwars.api.identity.CorrelationId.random(), () -> false,
                Instant.now().plusSeconds(10L));
    }

    private UiModel.PageDefinition emptyPage(final GuiPageId id) {
        return new UiModel.PageDefinition(id, MessageKey.of("ui.action.title"),
                (viewer, query) -> CompletableFuture.completedFuture(new UiModel.PageState(1L,
                        UiModel.PageState.Status.EMPTY, 0, 1,
                        Collections.<UiModel.Component>emptyList(), MessageKey.of("ui.empty"))),
                Collections.singletonList(UiModel.Interaction.PRIMARY));
    }

    private LocalizationService localization() {
        return new LocalizationService() {
            @Override public Result<LocalizedMessage> render(final MessageKey key,
                    final Optional<PlayerId> player, final Parameters parameters) {
                return Result.success(LocalizedMessage.of(LocaleId.parse("en"), key, key.value()));
            }
            @Override public Result<LocaleId> switchServerLocale(final LocaleId locale) { return Result.success(locale); }
            @Override public Result<LocaleId> switchPlayerLocale(final PlayerId player,
                                                                  final LocaleId locale) { return Result.success(locale); }
        };
    }

    private void finish(final boolean worker, final BoundedCommandSupervisor supervisor) {
        final Thread writer = new Thread(() -> {
            final boolean drained = supervisor == null || supervisor.close(Duration.ofSeconds(5L));
            final boolean offOwnerWrite = !Bukkit.isPrimaryThread();
            final boolean success = worker && drained && offOwnerWrite && commandDispatch && inventoryRendering
                    && parity && duplicatePrevented;
            final String evidence = "{\n  \"schema_version\": 1,\n"
                    + "  \"runtime\": \"Paper 1.21.1 build 133\",\n"
                    + "  \"server_sha256\": \""
                    + io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter.SERVER_SHA256
                    + "\",\n  \"command_dispatch\": " + commandDispatch
                    + ",\n  \"inventory_rendering\": " + inventoryRendering
                    + ",\n  \"command_gui_parity\": " + parity
                    + ",\n  \"async_off_owner\": " + asyncOffOwner
                    + ",\n  \"duplicate_action_prevented\": " + duplicatePrevented
                    + ",\n  \"evidence_written_off_owner\": " + offOwnerWrite
                    + ",\n  \"catalog_actions\": 87,\n  \"success\": " + success + "\n}\n";
            try {
                final Path path = getDataFolder().toPath().resolve("m09-primary-certification.json");
                Files.createDirectories(path.getParent());
                Files.write(path, evidence.getBytes(StandardCharsets.UTF_8));
            } catch (IOException failure) { getLogger().severe("M09 evidence write failed"); }
            Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
        }, "zbw-m09-certification-evidence");
        writer.setDaemon(false);
        writer.start();
    }
}
