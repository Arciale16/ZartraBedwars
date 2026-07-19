package io.zartra.bedwars.scripting.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskContext;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.scripting.api.ScriptActionId;
import io.zartra.bedwars.scripting.api.ScriptId;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeclarativeScriptEngineTest {
    private static final DefinitionId ACTION = DefinitionId.of("zartra", "action/test");
    private static final DefinitionId CAPABILITY = DefinitionId.of("zartra", "capability/test");
    private static final DefinitionId SUBJECT = DefinitionId.of("zartra", "subject/test");
    private static final ScriptActionId ENTRY = ScriptActionId.of("zartra", "entry");

    @Test void disabledByDefaultAndAuditAreFailClosed() throws Exception {
        final List<DeclarativeScriptEngine.AuditRecord> audits = new java.util.ArrayList<>();
        final DeclarativeScriptEngine engine = engine(false, 8, 8, context -> DeclarativeScriptEngine.ActionResult.success(), audits);
        final DeclarativeScriptEngine.ExecutionResult result = value(engine.execute(graph(node(ENTRY, Collections.emptyList())), ENTRY, input()));
        assertFalse(result.success());
        assertEquals("disabled", result.code());
        assertEquals(0, result.operations());
        assertEquals(1, audits.size());
        assertFalse(audits.get(0).success());
        assertEquals(ENTRY, audits.get(0).entry());
    }

    @Test void executesAllowlistedGraphDeterministically() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final ScriptActionId child = ScriptActionId.of("zartra", "child");
        final DeclarativeScriptEngine engine = engine(true, 8, 8, context -> { calls.incrementAndGet();
        assertEquals("v", context.arguments().get("k"));
        return DeclarativeScriptEngine.ActionResult.success();
        }, new java.util.ArrayList<>());
        final DeclarativeScriptEngine.ExecutionResult result = value(engine.execute(graph(node(ENTRY, Collections.singletonList(child)), node(child, Collections.emptyList())), ENTRY, input()));
        assertTrue(result.success());
        assertEquals(2, result.operations());
        assertEquals(2, calls.get());
    }

    @Test void rejectsMalformedAndUnauthorizedGraphs() {
        final DeclarativeScriptEngine engine = engine(true, 1, 2, context -> DeclarativeScriptEngine.ActionResult.success(), new java.util.ArrayList<>());
        assertEquals("unsupported_schema", engine.validate(new DeclarativeScriptEngine.ScriptGraph(ScriptId.of("zartra", "x"), 2, Collections.singletonList(node(ENTRY, Collections.emptyList()))), ENTRY).code());
        assertEquals("unknown_entry", engine.validate(graph(node(ENTRY, Collections.emptyList())), ScriptActionId.of("zartra", "missing")).code());
        final DeclarativeScriptEngine.Node denied = new DeclarativeScriptEngine.Node(ENTRY, DefinitionId.of("zartra", "action/denied"), Collections.singleton(CAPABILITY), Collections.singletonMap("k", "v"), Collections.emptyList());
        assertEquals("action_denied", engine.validate(graph(denied), ENTRY).code());
        final DeclarativeScriptEngine.Node badCapability = new DeclarativeScriptEngine.Node(ENTRY, ACTION, Collections.singleton(DefinitionId.of("zartra", "capability/denied")), Collections.singletonMap("k", "v"), Collections.emptyList());
        assertEquals("capability_denied", engine.validate(graph(badCapability), ENTRY).code());
        final ScriptActionId missing = ScriptActionId.of("zartra", "missing");
        assertEquals("unknown_child", engine.validate(graph(node(ENTRY, Collections.singletonList(missing))), ENTRY).code());
        final DeclarativeScriptEngine nodeLimited = new DeclarativeScriptEngine(
                new ImmediateScheduler(false), request -> allowed(),
                new DeclarativeScriptEngine.Policy(true, 1, 1, 8, 8, Duration.ofSeconds(1),
                        Collections.singleton(ACTION), Collections.singleton(CAPABILITY)),
                Collections.singleton(handler(context -> DeclarativeScriptEngine.ActionResult.success())),
                record -> { });
        assertEquals("node_limit", nodeLimited.validate(graph(
                node(ENTRY, Collections.singletonList(missing)),
                node(missing, Collections.emptyList())), ENTRY).code());
    }

    @Test void enforcesDepthOperationFailureAndCancellation() throws Exception {
        final ScriptActionId child = ScriptActionId.of("zartra", "child");
        final DeclarativeScriptEngine depth = engine(true, 8, 1, context -> DeclarativeScriptEngine.ActionResult.success(), new java.util.ArrayList<>());
        assertEquals("depth_limit", depth.validate(graph(node(ENTRY, Collections.singletonList(child)), node(child, Collections.emptyList())), ENTRY).code());
        final DeclarativeScriptEngine limited = engine(true, 1, 8, context -> DeclarativeScriptEngine.ActionResult.success(), new java.util.ArrayList<>());
        assertEquals("operation_limit", value(limited.execute(graph(node(ENTRY, Collections.singletonList(child)), node(child, Collections.emptyList())), ENTRY, input())).code());
        final DeclarativeScriptEngine failed = engine(true, 8, 8, context -> DeclarativeScriptEngine.ActionResult.failure("denied"), new java.util.ArrayList<>());
        assertEquals("denied", value(failed.execute(graph(node(ENTRY, Collections.emptyList())), ENTRY, input())).code());
        final ImmediateScheduler cancelled = new ImmediateScheduler(true);
        final DeclarativeScriptEngine cancelledEngine = new DeclarativeScriptEngine(
                cancelled, request -> allowed(), policy(true, 8, 8),
                Collections.singleton(handler(context -> DeclarativeScriptEngine.ActionResult.success())),
                record -> { });
        assertEquals("cancelled", value(cancelledEngine.execute(graph(node(ENTRY, Collections.emptyList())), ENTRY, input())).code());
    }

    @Test void authorizationAndHandlerExceptionsFailClosedAndAudit() throws Exception {
        final List<DeclarativeScriptEngine.AuditRecord> deniedAudits = new java.util.ArrayList<>();
        final DeclarativeScriptEngine denied = new DeclarativeScriptEngine(
                new ImmediateScheduler(false),
                request -> AuthorizationDecision.deny(DefinitionId.of("zartra", "reason/denied")),
                policy(true, 8, 8),
                Collections.singleton(handler(context -> DeclarativeScriptEngine.ActionResult.success())),
                deniedAudits::add);
        assertEquals("authorization_denied", value(denied.execute(
                graph(node(ENTRY, Collections.emptyList())), ENTRY, input())).code());
        assertEquals(1, deniedAudits.size());

        final List<DeclarativeScriptEngine.AuditRecord> failureAudits = new java.util.ArrayList<>();
        final DeclarativeScriptEngine failed = engine(true, 8, 8,
                context -> { throw new IllegalStateException("sensitive detail"); }, failureAudits);
        assertEquals("handler_exception", value(failed.execute(
                graph(node(ENTRY, Collections.emptyList())), ENTRY, input())).code());
        assertEquals("handler_exception", failureAudits.get(0).code());
    }

    @Test void constructorsRejectUnsafeOrUnboundedInput() {
        assertThrows(IllegalArgumentException.class, () -> new DeclarativeScriptEngine.Policy(true, 0, 1, 1, 1, Duration.ofSeconds(1), Collections.singleton(ACTION), Collections.singleton(CAPABILITY)));
        assertThrows(IllegalArgumentException.class, () -> new DeclarativeScriptEngine.Policy(true, 1, 0, 1, 1, Duration.ofSeconds(1), Collections.singleton(ACTION), Collections.singleton(CAPABILITY)));
        assertThrows(IllegalArgumentException.class, () -> new DeclarativeScriptEngine.Policy(true, 1, 1, 1, 1, Duration.ofSeconds(31), Collections.singleton(ACTION), Collections.singleton(CAPABILITY)));
        assertThrows(IllegalArgumentException.class, () -> new DeclarativeScriptEngine.ScriptGraph(ScriptId.of("zartra", "x"), 1, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new DeclarativeScriptEngine.Node(ENTRY, ACTION, Collections.singleton(CAPABILITY), Collections.singletonMap("x", "bad\nvalue"), Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> DeclarativeScriptEngine.ActionResult.failure("BAD CODE"));
        assertThrows(IllegalArgumentException.class, () -> new DeclarativeScriptEngine(
                new ImmediateScheduler(false), request -> allowed(), policy(true, 8, 8),
                Arrays.asList(handler(context -> DeclarativeScriptEngine.ActionResult.success()),
                        handler(context -> DeclarativeScriptEngine.ActionResult.success())),
                record -> { }));
    }

    private static DeclarativeScriptEngine engine(boolean enabled, int operations, int depth, Operation operation, List<DeclarativeScriptEngine.AuditRecord> audits) {
        return new DeclarativeScriptEngine(new ImmediateScheduler(false), request -> allowed(),
                policy(enabled, operations, depth), Collections.singleton(handler(operation)),
                audits::add);
    }
    private static DeclarativeScriptEngine.Policy policy(boolean enabled, int operations, int depth) { return new DeclarativeScriptEngine.Policy(enabled, 1, 64, depth, operations, Duration.ofSeconds(1), Collections.singleton(ACTION), Collections.singleton(CAPABILITY));
        }
    private static DeclarativeScriptEngine.ActionHandler handler(Operation operation) { return new DeclarativeScriptEngine.ActionHandler() { public DefinitionId actionId() { return ACTION;
        } public DeclarativeScriptEngine.ActionResult execute(DeclarativeScriptEngine.ActionContext context) throws Exception { return operation.run(context);
        } };
        }
    private static DeclarativeScriptEngine.Node node(ScriptActionId id, List<ScriptActionId> next) { return new DeclarativeScriptEngine.Node(id, ACTION, Collections.singleton(CAPABILITY), Collections.singletonMap("k", "v"), next);
        }
    private static DeclarativeScriptEngine.ScriptGraph graph(DeclarativeScriptEngine.Node... nodes) { return new DeclarativeScriptEngine.ScriptGraph(ScriptId.of("zartra", "test"), 1, Arrays.asList(nodes));
        }
    private static DeclarativeScriptEngine.ExecutionInput input() { return new DeclarativeScriptEngine.ExecutionInput(
                AuthorizationSubject.of(AuthorizationSubject.Kind.SERVICE, SUBJECT),
                CorrelationId.random(), Collections.singletonMap("player", "test"));
        }
    private static AuthorizationDecision allowed() { return AuthorizationDecision.allow(
                DefinitionId.of("zartra", "reason/allowed"));
        }
    private static DeclarativeScriptEngine.ExecutionResult value(SchedulerPort.TaskHandle<DeclarativeScriptEngine.ExecutionResult> handle) throws Exception { return handle.completion().toCompletableFuture().get().value().get();
        }
    private interface Operation { DeclarativeScriptEngine.ActionResult run(DeclarativeScriptEngine.ActionContext context) throws Exception;
        }

    private static final class ImmediateScheduler implements SchedulerPort {
        private final boolean cancelled;
        private ImmediateScheduler(boolean cancelled) { this.cancelled = cancelled;
        }
        public <T> TaskHandle<T> submit(TaskDescriptor descriptor, TaskOperation<T> operation) {
            final CompletableFuture<Outcome<T>> future = new CompletableFuture<>();
            try { future.complete(Outcome.success(operation.execute(new TaskContext(descriptor, () -> cancelled))));
        }
            catch (Exception failure) { future.completeExceptionally(failure);
        }
            return new TaskHandle<T>() { public io.zartra.bedwars.api.identity.TaskId taskId() { return descriptor.taskId();
        } public CompletionStage<Outcome<T>> completion() { return future;
        } public boolean cancel() { return false;
        } };
        }
        public void stopAdmission() { }
        public Snapshot snapshot() { return new Snapshot(0, 0, 0, 0, 0, 0, 0, true);
        }
    }
}
