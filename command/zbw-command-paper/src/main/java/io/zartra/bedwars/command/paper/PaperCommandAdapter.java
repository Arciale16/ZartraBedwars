package io.zartra.bedwars.command.paper;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.command.api.CommandFramework;
import io.zartra.bedwars.command.api.CommandModel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/** Thin Paper translation adapter; all parsing, authorization and use-case logic remains neutral. */
public final class PaperCommandAdapter implements CommandExecutor, TabCompleter {
    private final CommandFramework framework;
    private final LocalizationService localization;
    private final SubjectTranslator subjects;
    private final OwnerThreadOutput output;

    /** Creates an adapter with injected translation and owner-thread output boundaries. */
    public PaperCommandAdapter(final CommandFramework framework,
                               final LocalizationService localization,
                               final SubjectTranslator subjects,
                               final OwnerThreadOutput output) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.localization = Objects.requireNonNull(localization, "localization");
        this.subjects = Objects.requireNonNull(subjects, "subjects");
        this.output = Objects.requireNonNull(output, "output");
    }

    /** Registers only command labels declared in plugin metadata; missing labels fail startup. */
    public void register(final JavaPlugin plugin, final List<String> labels) {
        Objects.requireNonNull(plugin, "plugin");
        for (String label : Objects.requireNonNull(labels, "labels")) {
            final PluginCommand command = plugin.getCommand(label);
            if (command == null) { throw new IllegalStateException("command missing from plugin metadata: " + label); }
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @Override public boolean onCommand(final CommandSender sender, final Command command,
                                       final String label, final String[] arguments) {
        final CommandModel.Subject subject = subjects.translate(sender);
        framework.execute(subject, Arrays.asList(arguments)).result().whenComplete((result, failure) -> {
            final CommandModel.Result resolved = failure == null && result != null ? result
                    : CommandModel.Result.simple(CommandModel.Result.Status.ERROR,
                    "command.execution.failed");
            final io.zartra.bedwars.api.result.Result<LocalizationService.LocalizedMessage> rendered =
                    localization.render(resolved.message(), subject.playerId(), resolved.parameters());
            final String text = rendered.isSuccess() ? rendered.requireValue().text()
                    : resolved.message().value();
            output.send(sender, text);
        });
        return true;
    }

    @Override public List<String> onTabComplete(final CommandSender sender, final Command command,
                                                final String alias, final String[] arguments) {
        try { return framework.complete(subjects.translate(sender), Arrays.asList(arguments)); }
        catch (RuntimeException failure) { return Collections.emptyList(); }
    }

    /** Translates a Paper sender to an authenticated neutral subject. */
    public interface SubjectTranslator { /** @return neutral subject with no retained sender reference */ CommandModel.Subject translate(CommandSender sender); }

    /** Schedules output on the Paper owner thread. */
    public interface OwnerThreadOutput { /** Sends already neutral, untrusted plain text. */ void send(CommandSender sender, String text); }

    /** Creates the primary-runtime sender translator. */
    public static SubjectTranslator standardSubjects() {
        return sender -> {
            if (PaperCommandReflection.isPlayer(sender)) {
                final java.util.UUID uniqueId = PaperCommandReflection.uniqueId(sender);
                final PlayerId playerId = PlayerId.of(uniqueId);
                return CommandModel.Subject.player(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                        DefinitionId.of("zartra", "player/" + uniqueId)), playerId,
                        LocaleId.parse(PaperCommandReflection.locale(sender)));
            }
            return CommandModel.Subject.console(AuthorizationSubject.of(AuthorizationSubject.Kind.CONSOLE,
                    DefinitionId.of("zartra", "console/local")), LocaleId.parse("en"));
        };
    }

    /** Creates owner-thread output backed by the Bukkit scheduler. */
    public static OwnerThreadOutput bukkitOutput(final JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return (sender, text) -> {
            final Runnable mutation = () -> PaperCommandReflection.send(sender, text);
            if (org.bukkit.Bukkit.isPrimaryThread()) { mutation.run(); }
            else { org.bukkit.Bukkit.getScheduler().runTask(plugin, mutation); }
        };
    }
}
