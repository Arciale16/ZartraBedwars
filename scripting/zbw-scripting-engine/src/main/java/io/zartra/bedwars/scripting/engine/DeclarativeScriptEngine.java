package io.zartra.bedwars.scripting.engine;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.scripting.api.ScriptActionId;
import io.zartra.bedwars.scripting.api.ScriptId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Disabled-by-default interpreter for immutable declarative action graphs.
 *
 * <p>Execution occurs only through the injected bounded M05 scheduler. Handlers are registered
 * Java capabilities;
        scripts cannot load code or access host services except through a handler's
 * deliberately narrow contract. Cancellation and deadlines are cooperative at every node.</p>
 */
public final class DeclarativeScriptEngine {
    private static final DefinitionId OPERATION = DefinitionId.of("zartra", "script/execute");
    private static final DefinitionId OWNER = DefinitionId.of("zartra", "script/engine");
    private static final PermissionNode EXECUTE_PERMISSION =
            PermissionNode.of("zartrabedwars.script.execute");
    private final SchedulerPort scheduler;
    private final AuthorizationService authorization;
    private final Policy policy;
    private final Map<DefinitionId, ActionHandler> handlers;
    private final AuditSink audit;

    /** Creates an engine with an immutable action allowlist. */
    public DeclarativeScriptEngine(final SchedulerPort scheduler,
                                   final AuthorizationService authorization,
                                   final Policy policy,
                                   final Collection<ActionHandler> handlers,
                                   final AuditSink audit) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.audit = Objects.requireNonNull(audit, "audit");
        final Map<DefinitionId, ActionHandler> collected = new LinkedHashMap<DefinitionId, ActionHandler>();
        for (ActionHandler handler : Objects.requireNonNull(handlers, "handlers")) {
            final ActionHandler checked = Objects.requireNonNull(handler, "handler");
            if (collected.put(checked.actionId(), checked) != null) {
                throw new IllegalArgumentException("duplicate action handler");
            }
        }
        this.handlers = Collections.unmodifiableMap(collected);
    }

    /** Validates then submits one execution to the bounded scheduler. */
    public SchedulerPort.TaskHandle<ExecutionResult> execute(final ScriptGraph graph,
                                                              final ScriptActionId entry,
                                                              final ExecutionInput input) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(input, "input");
        final Validation validation = validate(graph, entry);
        final TaskDescriptor descriptor = TaskDescriptor.of(TaskId.random(), OPERATION, OWNER,
                input.correlationId(), policy.deadline(), true);
        return scheduler.submit(descriptor, context -> {
            if (!validation.valid()) {
                final ExecutionResult rejected = ExecutionResult.rejected(validation.code(), 0);
                audit.record(AuditRecord.of(graph.id(), entry, input.correlationId(), rejected));
                return rejected;
            }
            if (!policy.enabled()) {
                final ExecutionResult disabled = ExecutionResult.rejected("disabled", 0);
                audit.record(AuditRecord.of(graph.id(), entry, input.correlationId(), disabled));
                return disabled;
            }
            if (!authorization.authorize(AuthorizationRequest.of(
                    input.subject(), EXECUTE_PERMISSION, graph.id().value())).isAllowed()) {
                final ExecutionResult denied = ExecutionResult.rejected("authorization_denied", 0);
                audit.record(AuditRecord.of(graph.id(), entry, input.correlationId(), denied));
                return denied;
            }
            final Budget budget = new Budget(policy.maximumOperations());
            final ExecutionResult result;
            try {
                result = run(graph, entry, input, context.cancellationToken(), budget, 1);
            } catch (Exception failure) {
                final ExecutionResult rejected =
                        ExecutionResult.rejected("handler_exception", budget.used());
                audit.record(AuditRecord.of(graph.id(), entry, input.correlationId(), rejected));
                return rejected;
            }
            audit.record(AuditRecord.of(graph.id(), entry, input.correlationId(), result));
            return result;
        });
    }

    /** Performs deterministic structural and security validation without execution. */
    public Validation validate(final ScriptGraph graph, final ScriptActionId entry) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(entry, "entry");
        if (graph.schemaVersion() != policy.schemaVersion()) { return Validation.failure("unsupported_schema");
        }
        if (graph.nodes().size() > policy.maximumNodes()) { return Validation.failure("node_limit");
        }
        if (!graph.nodes().containsKey(entry)) { return Validation.failure("unknown_entry");
        }
        for (Node node : graph.nodes().values()) {
            if (!policy.allowedActions().contains(node.actionId())) { return Validation.failure("action_denied");
        }
            if (!policy.allowedCapabilities().containsAll(node.capabilities())) { return Validation.failure("capability_denied");
        }
            if (!handlers.containsKey(node.actionId())) { return Validation.failure("handler_missing");
        }
            for (ScriptActionId child : node.next()) {
                if (!graph.nodes().containsKey(child)) { return Validation.failure("unknown_child");
        }
            }
        }
        return depth(graph, entry, new LinkedHashSet<ScriptActionId>(), 1) > policy.maximumDepth()
                ? Validation.failure("depth_limit") : Validation.success();
    }

    private int depth(final ScriptGraph graph, final ScriptActionId id,
                      final Set<ScriptActionId> path, final int current) {
        if (!path.add(id)) { return policy.maximumDepth() + 1;
        }
        int maximum = current;
        for (ScriptActionId child : graph.nodes().get(id).next()) {
            maximum = Math.max(maximum, depth(graph, child,
                    new LinkedHashSet<ScriptActionId>(path), current + 1));
        }
        return maximum;
    }

    private ExecutionResult run(final ScriptGraph graph, final ScriptActionId id,
                                final ExecutionInput input, final CancellationToken cancellation,
                                final Budget budget, final int depth) throws Exception {
        if (cancellation.isCancellationRequested()) { return ExecutionResult.rejected("cancelled", budget.used());
        }
        if (depth > policy.maximumDepth()) { return ExecutionResult.rejected("depth_limit", budget.used());
        }
        if (!budget.consume()) { return ExecutionResult.rejected("operation_limit", budget.used());
        }
        final Node node = graph.nodes().get(id);
        final ActionResult action = handlers.get(node.actionId()).execute(
                new ActionContext(input, cancellation, node.arguments()));
        if (!action.isSuccess()) { return ExecutionResult.rejected(action.code(), budget.used());
        }
        for (ScriptActionId child : node.next()) {
            final ExecutionResult childResult = run(graph, child, input, cancellation, budget, depth + 1);
            if (!childResult.success()) { return childResult;
        }
        }
        return ExecutionResult.success(budget.used());
    }

    /** Immutable versioned graph. */
    public static final class ScriptGraph {
        private final ScriptId id;
        private final int schemaVersion;
        private final Map<ScriptActionId, Node> nodes;
        /** Creates a graph with unique bounded nodes. */
        public ScriptGraph(final ScriptId id, final int schemaVersion, final Collection<Node> nodes) {
            this.id = Objects.requireNonNull(id, "id");
            if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive");
        }
            this.schemaVersion = schemaVersion;
            final Map<ScriptActionId, Node> values = new LinkedHashMap<ScriptActionId, Node>();
            for (Node node : Objects.requireNonNull(nodes, "nodes")) {
                final Node checked = Objects.requireNonNull(node, "node");
                if (values.put(checked.id(), checked) != null) { throw new IllegalArgumentException("duplicate node");
        }
            }
            if (values.isEmpty()) { throw new IllegalArgumentException("graph must contain nodes");
        }
            this.nodes = Collections.unmodifiableMap(values);
        }
        /** @return graph ID */ public ScriptId id() { return id;
        }
        /** @return exact schema */ public int schemaVersion() { return schemaVersion;
        }
        /** @return immutable nodes */ public Map<ScriptActionId, Node> nodes() { return nodes;
        }
    }

    /** One immutable allowlisted action invocation. */
    public static final class Node {
        private final ScriptActionId id;
        private final DefinitionId actionId;
        private final Set<DefinitionId> capabilities;
        private final Map<String, String> arguments;
        private final List<ScriptActionId> next;
        /** Creates a declarative node;
        argument text is bounded and control-character free. */
        public Node(final ScriptActionId id, final DefinitionId actionId,
                    final Collection<DefinitionId> capabilities, final Map<String, String> arguments,
                    final Collection<ScriptActionId> next) {
            this.id = Objects.requireNonNull(id, "id");
        this.actionId = Objects.requireNonNull(actionId, "actionId");
            this.capabilities = immutableSet(capabilities);
            final Map<String, String> safe = new LinkedHashMap<String, String>();
            for (Map.Entry<String, String> entry : Objects.requireNonNull(arguments, "arguments").entrySet()) {
                final String key = bounded(entry.getKey(), 64, "argument key");
                final String value = bounded(entry.getValue(), 1024, "argument value");
                if (safe.put(key, value) != null) { throw new IllegalArgumentException("duplicate argument");
        }
            }
            this.arguments = Collections.unmodifiableMap(safe);
            final List<ScriptActionId> children = new ArrayList<ScriptActionId>();
            for (ScriptActionId child : Objects.requireNonNull(next, "next")) { children.add(Objects.requireNonNull(child, "child"));
        }
            this.next = Collections.unmodifiableList(children);
        }
        /** @return node ID */ public ScriptActionId id() { return id;
        }
        /** @return registered action */ public DefinitionId actionId() { return actionId;
        }
        /** @return required capabilities */ public Set<DefinitionId> capabilities() { return capabilities;
        }
        /** @return immutable arguments */ public Map<String, String> arguments() { return arguments;
        }
        /** @return ordered children */ public List<ScriptActionId> next() { return next;
        }
    }

    /** Immutable security and quota policy. */
    public static final class Policy {
        private final boolean enabled;
        private final int schemaVersion, maximumNodes, maximumDepth, maximumOperations;
        private final Duration deadline;
        private final Set<DefinitionId> actions, capabilities;
        /** Creates a bounded policy. */
        public Policy(final boolean enabled, final int schemaVersion, final int maximumNodes,
                      final int maximumDepth, final int maximumOperations, final Duration deadline,
                      final Collection<DefinitionId> actions, final Collection<DefinitionId> capabilities) {
            if (schemaVersion < 1 || maximumNodes < 1 || maximumNodes > 4096 || maximumDepth < 1
                    || maximumDepth > 64 || maximumOperations < 1 || maximumOperations > 100000) {
                throw new IllegalArgumentException("invalid script limits");
            }
            this.deadline = Objects.requireNonNull(deadline, "deadline");
            if (deadline.isZero() || deadline.isNegative() || deadline.compareTo(Duration.ofSeconds(30)) > 0) {
                throw new IllegalArgumentException("deadline outside safe bounds");
            }
            this.enabled = enabled;
        this.schemaVersion = schemaVersion;
        this.maximumNodes = maximumNodes;
            this.maximumDepth = maximumDepth;
        this.maximumOperations = maximumOperations;
            this.actions = immutableSet(actions);
        this.capabilities = immutableSet(capabilities);
        }
        /** @return execution enabled */ public boolean enabled() { return enabled;
        }
        /** @return accepted schema */ public int schemaVersion() { return schemaVersion;
        }
        /** @return graph node bound */ public int maximumNodes() { return maximumNodes;
        }
        /** @return recursion bound */ public int maximumDepth() { return maximumDepth;
        }
        /** @return operation bound */ public int maximumOperations() { return maximumOperations;
        }
        /** @return queue plus execution deadline */ public Duration deadline() { return deadline;
        }
        /** @return action allowlist */ public Set<DefinitionId> allowedActions() { return actions;
        }
        /** @return capability allowlist */ public Set<DefinitionId> allowedCapabilities() { return capabilities;
        }
    }

    /** Narrow registered action implementation. */
    public interface ActionHandler {
        /** @return unique allowlisted action */ DefinitionId actionId();
        /** Executes without retaining context or invoking platform APIs. */ ActionResult execute(ActionContext context) throws Exception;
    }
    /** Non-blocking, secret-free audit sink. */ public interface AuditSink { /** Records terminal result. */ void record(AuditRecord record);
        }

    /** Immutable action context. */
    public static final class ActionContext {
        private final ExecutionInput input;
        private final CancellationToken cancellation;
        private final Map<String, String> arguments;
        private ActionContext(final ExecutionInput input, final CancellationToken cancellation, final Map<String, String> arguments) {
            this.input = input;
        this.cancellation = cancellation;
        this.arguments = arguments;
        }
        /** @return input */ public ExecutionInput input() { return input;
        }
        /** @return cancellation/deadline token */ public CancellationToken cancellation() { return cancellation;
        }
        /** @return immutable validated arguments */ public Map<String, String> arguments() { return arguments;
        }
    }
    /** Immutable caller input containing identifiers only. */
    public static final class ExecutionInput {
        private final AuthorizationSubject subject;
        private final CorrelationId correlationId;
        private final Map<String, String> values;
        /** Creates bounded non-secret input. */
        public ExecutionInput(final AuthorizationSubject subject,
                              final CorrelationId correlationId,
                              final Map<String, String> values) {
            this.subject = Objects.requireNonNull(subject, "subject");
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
            final Map<String, String> safe = new LinkedHashMap<String, String>();
            for (Map.Entry<String, String> entry : Objects.requireNonNull(values, "values").entrySet()) {
                safe.put(bounded(entry.getKey(), 64, "input key"), bounded(entry.getValue(), 1024, "input value"));
            }
            this.values = Collections.unmodifiableMap(safe);
        }
        /** @return authenticated execution subject */ public AuthorizationSubject subject() {
            return subject;
        }
        /** @return correlation ID */ public CorrelationId correlationId() { return correlationId;
        }
        /** @return immutable inputs */ public Map<String, String> values() { return values;
        }
    }
    /** Handler result. */
    public static final class ActionResult {
        private final boolean success;
        private final String code;
        private ActionResult(final boolean success, final String code) { this.success = success;
        this.code = code;
        }
        /** @return success */ public static ActionResult success() { return new ActionResult(true, "ok");
        }
        /** @return safe failure */ public static ActionResult failure(final String code) { return new ActionResult(false, safeCode(code));
        }
        /** @return successful */ public boolean isSuccess() { return success;
        }
        /** @return stable code */ public String code() { return code;
        }
    }
    /** Terminal execution result. */
    public static final class ExecutionResult {
        private final boolean success;
        private final String code;
        private final int operations;
        private ExecutionResult(final boolean success, final String code, final int operations) { this.success = success;
        this.code = code;
        this.operations = operations;
        }
        private static ExecutionResult success(final int operations) { return new ExecutionResult(true, "ok", operations);
        }
        private static ExecutionResult rejected(final String code, final int operations) { return new ExecutionResult(false, safeCode(code), operations);
        }
        /** @return successful */ public boolean success() { return success;
        }
        /** @return stable code */ public String code() { return code;
        }
        /** @return consumed operations */ public int operations() { return operations;
        }
    }
    /** Structural validation result. */
    public static final class Validation {
        private final boolean valid;
        private final String code;
        private Validation(final boolean valid, final String code) { this.valid = valid;
        this.code = code;
        }
        private static Validation success() { return new Validation(true, "ok");
        }
        private static Validation failure(final String code) { return new Validation(false, code);
        }
        /** @return valid */ public boolean valid() { return valid;
        }
        /** @return stable code */ public String code() { return code;
        }
    }
    /** Secret-free terminal audit record. */
    public static final class AuditRecord {
        private final ScriptId scriptId;
        private final ScriptActionId entry;
        private final CorrelationId correlation;
        private final boolean success;
        private final String code;
        private final int operations;
        private AuditRecord(final ScriptId scriptId, final ScriptActionId entry, final CorrelationId correlation,
                            final ExecutionResult result) {
            this.scriptId = scriptId;
        this.entry = entry;
        this.correlation = correlation;
            this.success = result.success();
        this.code = result.code();
        this.operations = result.operations();
        }
        private static AuditRecord of(final ScriptId scriptId, final ScriptActionId entry,
                                      final CorrelationId correlation, final ExecutionResult result) {
            return new AuditRecord(scriptId, entry, correlation, result);
        }
        /** @return script */ public ScriptId scriptId() { return scriptId;
        }
        /** @return entry */ public ScriptActionId entry() { return entry;
        }
        /** @return correlation */ public CorrelationId correlationId() { return correlation;
        }
        /** @return success */ public boolean success() { return success;
        }
        /** @return code */ public String code() { return code;
        }
        /** @return operations */ public int operations() { return operations;
        }
    }

    private static final class Budget { private final int maximum;
        private int used;
        private Budget(final int maximum) { this.maximum = maximum;
        } private boolean consume() { if (used >= maximum) { return false;
        } used++;
        return true;
        } private int used() { return used;
        } }
    private static Set<DefinitionId> immutableSet(final Collection<DefinitionId> source) {
        final Set<DefinitionId> result = new LinkedHashSet<DefinitionId>();
        for (DefinitionId value : Objects.requireNonNull(source, "values")) { if (!result.add(Objects.requireNonNull(value, "value"))) { throw new IllegalArgumentException("duplicate identity");
        } }
        return Collections.unmodifiableSet(result);
    }
    private static String bounded(final String value, final int maximum, final String label) {
        Objects.requireNonNull(value, label);
        if (value.isEmpty() || value.length() > maximum || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\0') >= 0) { throw new IllegalArgumentException(label + " is malformed");
        }
        return value;
    }
    private static String safeCode(final String code) {
        if (code == null || !code.matches("[a-z0-9_.-]{1,64}")) { throw new IllegalArgumentException("invalid result code");
        }
        return code;
    }
}
