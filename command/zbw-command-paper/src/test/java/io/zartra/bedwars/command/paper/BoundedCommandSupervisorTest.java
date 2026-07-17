package io.zartra.bedwars.command.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.command.api.CommandModel;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BoundedCommandSupervisorTest {
    @Test void validatesBoundsPrefixAndCloseDeadline() {
        assertThrows(IllegalArgumentException.class,
                () -> new BoundedCommandSupervisor(0, 1, "invalid"));
        assertThrows(IllegalArgumentException.class,
                () -> new BoundedCommandSupervisor(1, 0, "invalid"));
        assertThrows(IllegalArgumentException.class,
                () -> new BoundedCommandSupervisor(1, 1, "invalid prefix"));
        BoundedCommandSupervisor supervisor = new BoundedCommandSupervisor(1, 1, "valid");
        assertThrows(IllegalArgumentException.class, () -> supervisor.close(Duration.ofSeconds(-1)));
        supervisor.close();
    }
    @Test void executesReportsHealthAndDrains() {
        BoundedCommandSupervisor supervisor = new BoundedCommandSupervisor(1, 2, "zbw-test");
        CommandModel.Result result = supervisor.submit(node(), context(Instant.now().plusSeconds(5)), value -> CompletableFuture.completedFuture(CommandModel.Result.simple(CommandModel.Result.Status.SUCCESS, "command.success"))).toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.SUCCESS, result.status());
         assertEquals(0, supervisor.inFlight());
        assertEquals(0, supervisor.queued());
        assertTrue(supervisor.close(Duration.ofSeconds(1)));
        assertTrue(supervisor.isClosed());
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> supervisor.submit(node(), context(Instant.now().plusSeconds(1)), value -> new CompletableFuture<>()));
    }

    @Test void mapsFailuresAndTimeoutsWithoutLeakingAccounting() {
        BoundedCommandSupervisor supervisor = new BoundedCommandSupervisor(1, 1, "zbw-failure");
        CommandModel.Result failed = supervisor.submit(node(), context(Instant.now().plusSeconds(2)), value -> { throw new IllegalStateException(); }).toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.ERROR, failed.status());
        CommandModel.Result timed = supervisor.submit(node(), context(Instant.now()), value -> new CompletableFuture<>()).toCompletableFuture().join();
        assertEquals(CommandModel.Result.Status.TIMEOUT, timed.status());
        assertEquals(0, supervisor.inFlight());
        supervisor.close();
    }
    private static CommandModel.Node node() { return CommandModel.Node.builder(CommandModel.CommandId.of("zartra", "command/test"), "test").executor(value -> CompletableFuture.completedFuture(CommandModel.Result.simple(CommandModel.Result.Status.SUCCESS, "command.success"))).build(); }
    private static CommandModel.ExecutionContext context(Instant deadline) { CommandModel.Subject subject = CommandModel.Subject.console(AuthorizationSubject.of(AuthorizationSubject.Kind.CONSOLE, DefinitionId.of("zartra", "console/local")), LocaleId.parse("en")); return new CommandModel.ExecutionContext(subject, CommandModel.Arguments.empty(), DefinitionId.of("zartra", "target"), CorrelationId.random(), () -> false, deadline); }
}
