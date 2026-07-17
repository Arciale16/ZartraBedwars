package io.zartra.bedwars.command.paper;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.command.api.CommandModel;
import io.zartra.bedwars.command.api.PresentationActions;
import io.zartra.bedwars.ui.api.ConfirmationFramework;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Builds the unified neutral command trees from the same action catalogue used by GUIs. */
public final class UnifiedCommandTreeFactory {
    /** Typed optional protected target argument. */
    public static final CommandModel.ArgumentKey<DefinitionId> TARGET =
            CommandModel.ArgumentKey.of("target", DefinitionId.class);
    /** Typed optional confirmation token argument. */
    public static final CommandModel.ArgumentKey<String> CONFIRMATION =
            CommandModel.ArgumentKey.of("confirmation", String.class);
    private final PresentationActions.Registry actions;
    private final ConfirmationFramework confirmations;

    /** Creates a factory around the shared action and confirmation services. */
    public UnifiedCommandTreeFactory(final PresentationActions.Registry actions,
                                     final ConfirmationFramework confirmations) {
        this.actions = Objects.requireNonNull(actions, "actions");
        this.confirmations = Objects.requireNonNull(confirmations, "confirmations");
    }

    /** @return one immutable root per plugin command label */
    public Map<String, CommandModel.Node> create(final Collection<PresentationActions.Definition> catalogue) {
        final Map<String, MutableNode> roots = new LinkedHashMap<String, MutableNode>();
        for (PresentationActions.Definition definition : Objects.requireNonNull(catalogue, "catalogue")) {
            final String[] tokens = definition.commandPath().substring(1).split(" ");
            MutableNode node = roots.computeIfAbsent(tokens[0], key -> new MutableNode(key, null));
            for (int index = 1; index < tokens.length; index++) {
                node = node.children.computeIfAbsent(tokens[index], key -> new MutableNode(key, null));
            }
            if (node.definition != null) { throw new IllegalArgumentException("duplicate command path " + definition.commandPath()); }
            node.definition = definition;
        }
        final Map<String, CommandModel.Node> built = new LinkedHashMap<String, CommandModel.Node>();
        for (Map.Entry<String, MutableNode> entry : roots.entrySet()) {
            built.put(entry.getKey(), build(entry.getValue(), entry.getKey()));
        }
        return java.util.Collections.unmodifiableMap(built);
    }

    private CommandModel.Node build(final MutableNode source, final String path) {
        final PresentationActions.Definition definition = source.definition;
        final CommandModel.CommandId id = definition == null
                ? CommandModel.CommandId.of("zartra", "command/" + path.replace(' ', '/'))
                : CommandModel.CommandId.parse(definition.id().value().toString());
        final CommandModel.Node.Builder builder = CommandModel.Node.builder(id, source.label)
                .messages(MessageKey.of("command." + path.replace(' ', '.') + ".help"),
                        MessageKey.of("command." + path.replace(' ', '.') + ".usage"))
                .permission(definition == null ? PermissionNode.of("zartrabedwars.command.use")
                        : definition.permission())
                .timeout(Duration.ofSeconds(10L));
        if (path.startsWith("deposit")) { builder.senderRule(CommandModel.SenderRule.PLAYER_ONLY); }
        for (MutableNode child : source.children.values()) { builder.child(build(child, path + ' ' + child.label)); }
        if (definition != null) {
            builder.argument(CommandModel.ArgumentSpec.optional(TARGET, CommandModel.Parsers.definitionId()));
            if (definition.destructive()) {
                builder.argument(CommandModel.ArgumentSpec.optional(CONFIRMATION, CommandModel.Parsers.word()));
            }
            builder.target(arguments -> arguments.find(TARGET).orElse(
                    DefinitionId.of("zartra", "presentation/global")));
            builder.executor(context -> invoke(definition, context));
        }
        return builder.build();
    }

    private CompletionStage<CommandModel.Result> invoke(final PresentationActions.Definition definition,
                                                         final CommandModel.ExecutionContext context) {
        final PresentationActions.Request request = new PresentationActions.Request(
                context.subject().authorization(), definition.id(), context.target(), 0L,
                context.correlationId(), context.arguments(), PresentationActions.Surface.COMMAND);
        if (definition.destructive()) {
            final java.util.Optional<String> token = context.arguments().find(CONFIRMATION);
            if (!token.isPresent()) {
                final ConfirmationFramework.Intent issued = confirmations.issue(request.actor(),
                        request.action(), request.target(), request.revision(), request.correlationId());
                final LocalizationService.Parameters parameters = LocalizationService.Parameters.of(
                        java.util.Collections.singletonList(LocalizationService.Parameter.text(
                                "confirmation", issued.id().toString())));
                return java.util.concurrent.CompletableFuture.completedFuture(CommandModel.Result.of(
                        CommandModel.Result.Status.CONFLICT,
                        MessageKey.of("command.confirmation.required"), parameters));
            }
            final ConfirmationFramework.Decision decision;
            try {
                decision = confirmations.consume(ConfirmationFramework.ConfirmationId.parse(token.get()),
                        request.actor(), request.action(), request.target(), request.revision());
            } catch (RuntimeException malformed) {
                return java.util.concurrent.CompletableFuture.completedFuture(CommandModel.Result.simple(
                        CommandModel.Result.Status.INVALID, "command.confirmation.invalid"));
            }
            if (decision.verdict() != ConfirmationFramework.Verdict.CONFIRMED) {
                return java.util.concurrent.CompletableFuture.completedFuture(CommandModel.Result.simple(
                        CommandModel.Result.Status.FORBIDDEN, "command.confirmation.rejected"));
            }
        }
        return actions.execute(request).thenApply(UnifiedCommandTreeFactory::result);
    }

    private static CommandModel.Result result(final PresentationActions.Response response) {
        final CommandModel.Result.Status status = switch (response.status()) {
            case SUCCESS -> CommandModel.Result.Status.SUCCESS;
            case INVALID -> CommandModel.Result.Status.INVALID;
            case FORBIDDEN -> CommandModel.Result.Status.FORBIDDEN;
            case CONFLICT -> CommandModel.Result.Status.CONFLICT;
            case CANCELLED -> CommandModel.Result.Status.CANCELLED;
            case TIMEOUT -> CommandModel.Result.Status.TIMEOUT;
            case NOT_FOUND, ERROR -> CommandModel.Result.Status.ERROR;
        };
        return CommandModel.Result.of(status, response.message(), response.parameters());
    }

    private static final class MutableNode {
        private final String label;
        private final Map<String, MutableNode> children = new LinkedHashMap<String, MutableNode>();
        private PresentationActions.Definition definition;
        private MutableNode(final String label, final PresentationActions.Definition definition) { this.label = label;
         this.definition = definition;
        }
    }
}
