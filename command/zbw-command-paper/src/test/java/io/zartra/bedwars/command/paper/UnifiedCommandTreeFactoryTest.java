package io.zartra.bedwars.command.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.command.api.CommandModel;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.ui.api.AdminDashboard;
import io.zartra.bedwars.ui.api.ConfirmationFramework;
import io.zartra.bedwars.ui.api.PresentationParity;
import io.zartra.bedwars.ui.api.UiModel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UnifiedCommandTreeFactoryTest {
    @Test void generatedTreesPagesAndRegistryHaveCompleteParity() {
        List<PresentationActions.Definition> catalog = PresentationActions.Catalog.standard();
        AtomicInteger calls = new AtomicInteger();
        Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings = bindings(catalog, request -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(PresentationActions.Response.simple(PresentationActions.Response.Status.SUCCESS, "presentation.success", request.revision() + 1));
        });
        PresentationActions.Registry registry = new PresentationActions.Registry(catalog, bindings);
        ConfirmationFramework confirmations = confirmations(catalog);
        Map<String, CommandModel.Node> roots = new UnifiedCommandTreeFactory(registry, confirmations).create(catalog);
        assertEquals(2, roots.size());
         assertTrue(roots.containsKey("zbw"));
        assertTrue(roots.containsKey("deposit"));
        List<CommandFramework.InventoryEntry> inventory = new ArrayList<CommandFramework.InventoryEntry>();
        for (CommandModel.Node root : roots.values()) { inventory.addAll(framework(root).inventory()); }
        List<UiModel.PageDefinition> pages = AdminDashboard.pages(catalog, definition -> new UiModel.PageDefinition(definition.pageId(), MessageKey.of("ui.action.title"), (viewer, query) -> CompletableFuture.completedFuture(new UiModel.PageState(1, UiModel.PageState.Status.EMPTY, 0, 1, Collections.<UiModel.Component>emptyList(), MessageKey.of("ui.empty"))), Collections.singletonList(UiModel.Interaction.PRIMARY)));
        assertTrue(PresentationParity.validate(catalog, inventory, pages).valid());
        CommandModel.Result result = framework(roots.get("zbw")).execute(player(), java.util.Arrays.asList("arena", "list")).result().toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.SUCCESS, result.status());
        assertEquals(1, calls.get());
    }

    @Test void destructiveCommandsRequireSingleUseBoundConfirmation() {
        List<PresentationActions.Definition> catalog = PresentationActions.Catalog.standard();
        AtomicInteger calls = new AtomicInteger();
        PresentationActions.Registry registry = new PresentationActions.Registry(catalog,
                bindings(catalog, request -> {
                    calls.incrementAndGet();
                    return CompletableFuture.completedFuture(PresentationActions.Response.simple(
                            PresentationActions.Response.Status.SUCCESS,
                            "presentation.success", 1));
                }));
        CommandFramework framework = framework(new UnifiedCommandTreeFactory(registry, confirmations(catalog)).create(catalog).get("zbw"));
        CommandModel.Result prompt = framework.execute(player(), java.util.Arrays.asList("arena", "delete", "zartra:arena/test")).result().toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.CONFLICT, prompt.status());
        String token = prompt.parameters().find("confirmation").orElseThrow().value();
        CommandModel.Result accepted = framework.execute(player(), java.util.Arrays.asList("arena", "delete", "zartra:arena/test", token)).result().toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.SUCCESS, accepted.status());
        assertEquals(1, calls.get());
        CommandModel.Result replay = framework.execute(player(), java.util.Arrays.asList("arena", "delete", "zartra:arena/test", token)).result().toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.FORBIDDEN, replay.status());
        CommandModel.Result malformed = framework.execute(player(), java.util.Arrays.asList(
                "arena", "delete", "zartra:arena/test", "not-a-token"))
                .result().toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.INVALID, malformed.status());
    }

    private static Map<PresentationActions.ActionId, PresentationActions.UseCase> bindings(List<PresentationActions.Definition> definitions, PresentationActions.UseCase useCase) { Map<PresentationActions.ActionId, PresentationActions.UseCase> result = new LinkedHashMap<>();
     for (PresentationActions.Definition definition : definitions) { result.put(definition.id(), useCase);
     } return result;
    }
    private static ConfirmationFramework confirmations(List<PresentationActions.Definition> catalog) { return new ConfirmationFramework(request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "allowed")), TimeSource.FixedTimeSource.at(Instant.parse("2026-01-01T00:00:00Z")), UUID::randomUUID, record -> { }, Duration.ofSeconds(30), 128, catalog); }
    private static CommandFramework framework(CommandModel.Node root) { return new CommandFramework(root, request -> AuthorizationDecision.allow(DefinitionId.of("zartra", "allowed")), TimeSource.FixedTimeSource.at(Instant.parse("2026-01-01T00:00:00Z")), (node, context, executor) -> executor.execute(context), record -> { }, 128); }
    private static CommandModel.Subject player() { PlayerId id = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
     return CommandModel.Subject.player(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER, DefinitionId.of("zartra", "player/test")), id, LocaleId.parse("en"));
    }
}
