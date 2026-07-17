package io.zartra.bedwars.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CommandFrameworkTest {
    private static final DefinitionId TARGET = DefinitionId.of("zartra", "target/test");
    private static final CommandModel.ArgumentKey<Integer> COUNT = CommandModel.ArgumentKey.of("count", Integer.class);

    @Test void primitivesValidateTypeIdentityAndParsers() {
        assertEquals(CommandModel.CommandId.of("zartra", "command/test"), CommandModel.CommandId.parse("zartra:command/test"));
        assertThrows(IllegalArgumentException.class, () -> CommandModel.ArgumentKey.of("Bad", String.class));
        CommandModel.Arguments arguments = CommandModel.Arguments.builder().put(COUNT, 4).build();
        assertEquals(4, arguments.require(COUNT));
        assertEquals(Collections.singleton("count"), arguments.names());
        assertFalse(arguments.find(CommandModel.ArgumentKey.of("missing", String.class)).isPresent());
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Arguments.builder().put(COUNT, 1).put(COUNT, 2));
        assertTrue(CommandModel.Parsers.integer(1, 5).parse("3").isSuccess());
        assertFalse(CommandModel.Parsers.integer(1, 5).parse("9").isSuccess());
        assertFalse(CommandModel.Parsers.integer(1, 5).parse("bad").isSuccess());
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Parsers.integer(2, 1));
        assertTrue(CommandModel.Parsers.definitionId().parse("zartra:test").isSuccess());
        assertFalse(CommandModel.Parsers.definitionId().parse("bad").isSuccess());
        assertTrue(CommandModel.Parsers.arenaId().parse(UUID.randomUUID().toString()).isSuccess());
        assertTrue(CommandModel.Parsers.matchId().parse(UUID.randomUUID().toString()).isSuccess());
        assertFalse(CommandModel.Parsers.word().parse(" ").isSuccess());
        assertEquals(4, arguments.find(COUNT).orElseThrow(AssertionError::new));
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Arguments.empty().require(COUNT));
    }

    @Test void nodeValidationSenderAndInventoryAreDeterministic() {
        CommandModel.Subject player = player();
        CommandModel.Subject console = console();
        assertTrue(CommandModel.SenderRule.ANY.accepts(player));
        assertTrue(CommandModel.SenderRule.PLAYER_ONLY.accepts(player));
        assertFalse(CommandModel.SenderRule.PLAYER_ONLY.accepts(console));
        assertTrue(CommandModel.SenderRule.CONSOLE_ONLY.accepts(console));
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Node.builder(id("leaf"), "Bad").executor(success()).build());
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Node.builder(id("leaf"), "leaf").alias("leaf").executor(success()).build());
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Node.builder(id("leaf"), "leaf").build());
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Node.builder(id("leaf"), "leaf").timeout(Duration.ZERO).executor(success()).build());
        CommandModel.Node duplicate = leaf("same", CommandModel.SenderRule.ANY, success());
        assertThrows(IllegalArgumentException.class, () -> CommandModel.Node.builder(id("root"), "root").child(duplicate).child(duplicate).build());
    }

    @Test void executionCoversSuccessHelpUsageInvalidDenialSenderAndCooldown() {
        MutableTime time = new MutableTime();
        List<CommandFramework.AuditRecord> audit = new ArrayList<CommandFramework.AuditRecord>();
        CommandModel.Node run = CommandModel.Node.builder(id("run"), "run")
                .permission(PermissionNode.of("zartrabedwars.test.run"))
                .argument(CommandModel.ArgumentSpec.required(COUNT, CommandModel.Parsers.integer(1, 5)))
                .cooldown(Duration.ofSeconds(5)).target(values -> TARGET).executor(success()).build();
        CommandModel.Node playerOnly = leaf("player", CommandModel.SenderRule.PLAYER_ONLY, success());
        CommandModel.Node branch = CommandModel.Node.builder(id("branch"), "branch").child(run).child(playerOnly).build();
        CommandFramework framework = framework(branch, allow(true), time, direct(), audit::add);
        assertEquals(CommandModel.Result.Status.SUCCESS, framework.execute(player(), Arrays.asList("run", "2")).result().toCompletableFuture().join().status());
        assertEquals(CommandModel.Result.Status.COOLDOWN, framework.execute(player(), Arrays.asList("run", "2")).result().toCompletableFuture().join().status());
        assertEquals(CommandModel.Result.Status.INVALID, framework.execute(player(), Arrays.asList("run", "bad")).result().toCompletableFuture().join().status());
        assertEquals(CommandModel.Result.Status.USAGE, framework.execute(player(), Arrays.asList("run", "2", "extra")).result().toCompletableFuture().join().status());
        assertEquals(CommandModel.Result.Status.HELP, framework.execute(player(), Collections.<String>emptyList()).result().toCompletableFuture().join().status());
        assertEquals(CommandModel.Result.Status.SENDER_REJECTED, framework.execute(console(), Collections.singletonList("player")).result().toCompletableFuture().join().status());
        assertEquals(2, audit.size());
        assertEquals(CommandFramework.AuditRecord.Phase.STARTED, audit.get(0).phase());
        assertEquals(CommandModel.Result.Status.SUCCESS,
                audit.get(1).status().orElseThrow(AssertionError::new));
        CommandFramework denied = framework(branch, allow(false), time, direct(), audit::add);
        assertEquals(CommandModel.Result.Status.FORBIDDEN, denied.execute(player(), Arrays.asList("run", "2")).result().toCompletableFuture().join().status());
        assertEquals(CommandFramework.AuditRecord.Phase.DENIED, audit.get(2).phase());
    }

    @Test void cancellationTimeoutFailureAndSupervisorRejectionAreStructured() {
        MutableTime time = new MutableTime();
        CompletableFuture<CommandModel.Result> pending = new CompletableFuture<CommandModel.Result>();
        CommandModel.Node node = leaf("wait", CommandModel.SenderRule.ANY, context -> pending);
        CommandFramework framework = framework(node, allow(true), time, direct(), record -> { });
        CommandFramework.Execution execution = framework.execute(player(), Collections.<String>emptyList());
        execution.cancel();
        pending.complete(CommandModel.Result.simple(CommandModel.Result.Status.SUCCESS, "command.ok"));
        assertEquals(CommandModel.Result.Status.CANCELLED, execution.result().toCompletableFuture().join().status());
        CompletableFuture<CommandModel.Result> delayed = new CompletableFuture<CommandModel.Result>();
        framework = framework(leaf("wait", CommandModel.SenderRule.ANY, context -> delayed), allow(true), time, direct(), record -> { });
        CommandFramework.Execution timeout = framework.execute(player(), Collections.<String>emptyList());
        time.now = time.now.plusSeconds(11);
        delayed.complete(CommandModel.Result.simple(CommandModel.Result.Status.SUCCESS, "command.ok"));
        assertEquals(CommandModel.Result.Status.TIMEOUT, timeout.result().toCompletableFuture().join().status());
        CommandFramework.ExecutionSupervisor rejected = (n, c, e) -> { throw new IllegalStateException("closed"); };
        assertEquals(CommandModel.Result.Status.ERROR, framework(leaf("x", CommandModel.SenderRule.ANY, success()), allow(true), time, rejected, record -> { }).execute(player(), Collections.<String>emptyList()).result().toCompletableFuture().join().status());
        CommandModel.Node badTarget = CommandModel.Node.builder(id("bad"), "bad").target(values -> { throw new IllegalArgumentException(); }).executor(success()).build();
        assertEquals(CommandModel.Result.Status.INVALID, framework(badTarget, allow(true), time, direct(), record -> { }).execute(player(), Collections.<String>emptyList()).result().toCompletableFuture().join().status());
    }

    @Test void completionIsAuthorizedSortedBoundedAndInventoryIsComplete() {
        CommandModel.Node alpha = leaf("alpha", CommandModel.SenderRule.ANY, success());
        CommandModel.Node beta = leaf("beta", CommandModel.SenderRule.ANY, success());
        CommandModel.Node root = CommandModel.Node.builder(id("root"), "root").child(beta).child(alpha).build();
        CommandFramework allowed = framework(root, allow(true), new MutableTime(), direct(), record -> { });
        assertEquals(Arrays.asList("alpha", "beta"), allowed.complete(player(), Collections.singletonList("")));
        assertEquals(Collections.singletonList("beta"), allowed.complete(player(), Collections.singletonList("b")));
        assertEquals(3, allowed.inventory().size());
        assertTrue(allowed.inventory().get(1).executable());
        assertTrue(framework(root, allow(false), new MutableTime(), direct(), record -> { }).complete(player(), Collections.singletonList("")).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> allowed.execute(player(), Collections.nCopies(65, "x")));
    }

    private static CommandFramework framework(CommandModel.Node root, AuthorizationService auth, TimeSource time,
                                              CommandFramework.ExecutionSupervisor supervisor, CommandFramework.AuditSink audit) {
        return new CommandFramework(root, auth, time, supervisor, audit, 8);
    }
    private static CommandFramework.ExecutionSupervisor direct() { return (node, context, executor) -> executor.execute(context); }
    private static AuthorizationService allow(boolean value) { return request -> value ? AuthorizationDecision.allow(DefinitionId.of("zartra", "allowed")) : AuthorizationDecision.deny(DefinitionId.of("zartra", "denied")); }
    private static CommandModel.Node leaf(String label, CommandModel.SenderRule sender, CommandModel.Executor executor) { return CommandModel.Node.builder(id(label), label).senderRule(sender).target(values -> TARGET).executor(executor).build(); }
    private static CommandModel.CommandId id(String path) { return CommandModel.CommandId.of("zartra", "command/" + path); }
    private static CommandModel.Executor success() { return context -> CompletableFuture.completedFuture(CommandModel.Result.simple(CommandModel.Result.Status.SUCCESS, "command.success")); }
    private static CommandModel.Subject player() {
        PlayerId id = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return CommandModel.Subject.player(AuthorizationSubject.of(
                AuthorizationSubject.Kind.PLAYER, DefinitionId.of("zartra", "player/one")),
                id, LocaleId.parse("en"));
    }
    private static CommandModel.Subject console() { return CommandModel.Subject.console(AuthorizationSubject.of(AuthorizationSubject.Kind.CONSOLE, DefinitionId.of("zartra", "console/local")), LocaleId.parse("en")); }
    private static final class MutableTime implements TimeSource {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public Instant now() { return now; }
    }
}
