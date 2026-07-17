package io.zartra.bedwars.command.api;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.CommandModel.ArgumentSpec;
import io.zartra.bedwars.command.api.CommandModel.Arguments;
import io.zartra.bedwars.command.api.CommandModel.ExecutionContext;
import io.zartra.bedwars.command.api.CommandModel.Node;
import io.zartra.bedwars.command.api.CommandModel.ParseResult;
import io.zartra.bedwars.command.api.CommandModel.Result;
import io.zartra.bedwars.command.api.CommandModel.Subject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Platform-neutral command dispatcher with exact authorization, bounded completion, cooldowns,
 * cooperative cancellation and structured audit. Instances are thread-safe. Executors run only
 * through the supplied supervisor and must not block an owner thread.
 */
public final class CommandFramework {
    private static final int MAX_TOKENS = 64;
    private static final int MAX_COMPLETIONS = 100;
    private final Node root;
    private final AuthorizationService authorization;
    private final TimeSource time;
    private final ExecutionSupervisor supervisor;
    private final AuditSink audit;
    private final Map<CooldownKey, Instant> cooldowns = new LinkedHashMap<CooldownKey, Instant>();
    private final int maximumCooldowns;

    /** Creates a bounded dispatcher around one immutable root command. */
    public CommandFramework(final Node root, final AuthorizationService authorization,
                            final TimeSource time, final ExecutionSupervisor supervisor,
                            final AuditSink audit, final int maximumCooldowns) {
        this.root = Objects.requireNonNull(root, "root");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.time = Objects.requireNonNull(time, "time");
        this.supervisor = Objects.requireNonNull(supervisor, "supervisor");
        this.audit = Objects.requireNonNull(audit, "audit");
        if (maximumCooldowns < 1 || maximumCooldowns > 100000) {
            throw new IllegalArgumentException("maximumCooldowns outside supported range");
        }
        this.maximumCooldowns = maximumCooldowns;
    }

    /**
     * Parses, authorizes and schedules one invocation. Parsing and policy evaluation are bounded;
     * the returned handle permits cancellation without exposing an implementation future.
     */
    public Execution execute(final Subject subject, final List<String> rawTokens) {
        Objects.requireNonNull(subject, "subject");
        final List<String> tokens = copyTokens(rawTokens);
        final CorrelationId correlation = CorrelationId.random();
        Node selected = root;
        int cursor = 0;
        while (cursor < tokens.size()) {
            final Optional<Node> child = selected.child(tokens.get(cursor));
            if (!child.isPresent()) { break; }
            selected = child.get();
            cursor++;
        }
        if (!selected.senderRule().accepts(subject)) {
            return immediate(correlation, Result.simple(Result.Status.SENDER_REJECTED,
                    "command.sender.rejected"));
        }
        if (!selected.executor().isPresent()) {
            return immediate(correlation, Result.simple(Result.Status.HELP, selected.help().value()));
        }
        final ParsedArguments parsed = parse(selected.arguments(), tokens, cursor);
        if (!parsed.success) {
            return immediate(correlation, Result.of(Result.Status.INVALID, parsed.error,
                    io.zartra.bedwars.api.localization.LocalizationService.Parameters.empty()));
        }
        if (parsed.consumed != tokens.size()) {
            return immediate(correlation, Result.simple(Result.Status.USAGE, selected.usage().value()));
        }
        final DefinitionId target;
        try {
            target = Objects.requireNonNull(selected.targetResolver().resolve(parsed.arguments),
                    "resolved target");
        } catch (RuntimeException failure) {
            return immediate(correlation, Result.simple(Result.Status.INVALID,
                    "command.target.invalid"));
        }
        if (!authorization.authorize(AuthorizationRequest.of(subject.authorization(),
                selected.permission(), target)).isAllowed()) {
            audit.record(AuditRecord.denied(correlation, selected, subject, target, time.now()));
            return immediate(correlation, Result.simple(Result.Status.FORBIDDEN,
                    "command.permission.denied"));
        }
        final Instant started = time.now();
        final CooldownKey cooldownKey = new CooldownKey(subject.authorization().toString(), selected.id());
        if (!acquireCooldown(cooldownKey, started, selected.cooldown())) {
            return immediate(correlation, Result.simple(Result.Status.COOLDOWN,
                    "command.cooldown.active"));
        }
        final CancellationSource cancellation = new CancellationSource();
        final Instant deadline = started.plus(selected.timeout());
        final ExecutionContext context = new ExecutionContext(subject, parsed.arguments, target,
                correlation, cancellation, deadline);
        audit.record(AuditRecord.started(correlation, selected, subject, target, started));
        final Node executableNode = selected;
        final CompletionStage<Result> scheduled;
        try {
            scheduled = supervisor.submit(executableNode, context, executableNode.executor().get());
        } catch (RuntimeException failure) {
            releaseCooldown(cooldownKey);
            audit.record(AuditRecord.failed(correlation, selected, subject, target, time.now()));
            return immediate(correlation, Result.simple(Result.Status.ERROR,
                    "command.execution.rejected"));
        }
        if (scheduled == null) {
            releaseCooldown(cooldownKey);
            throw new IllegalStateException("execution supervisor returned null");
        }
        final CompletableFuture<Result> result = new CompletableFuture<Result>();
        scheduled.whenComplete((value, failure) -> {
            final Result resolved;
            if (cancellation.isCancellationRequested()) {
                resolved = Result.simple(Result.Status.CANCELLED, "command.execution.cancelled");
            } else if (time.now().isAfter(deadline)) {
                resolved = Result.simple(Result.Status.TIMEOUT, "command.execution.timeout");
            } else if (failure != null || value == null) {
                resolved = Result.simple(Result.Status.ERROR, "command.execution.failed");
            } else {
                resolved = value;
            }
            audit.record(AuditRecord.completed(correlation, executableNode, subject, target,
                    resolved.status(), time.now()));
            result.complete(resolved);
        });
        return new Execution(correlation, result, cancellation);
    }

    /** Returns deterministic permission-filtered completion candidates without executing code. */
    public List<String> complete(final Subject subject, final List<String> rawTokens) {
        Objects.requireNonNull(subject, "subject");
        final List<String> tokens = copyTokens(rawTokens);
        Node selected = root;
        int cursor = 0;
        while (cursor < Math.max(0, tokens.size() - 1)) {
            final Optional<Node> child = selected.child(tokens.get(cursor));
            if (!child.isPresent()) { return Collections.emptyList(); }
            selected = child.get();
            cursor++;
        }
        final String prefix = tokens.isEmpty() ? "" : tokens.get(tokens.size() - 1);
        final List<String> candidates = new ArrayList<String>();
        if (cursor < selected.arguments().size()) {
            candidates.addAll(selected.arguments().get(cursor).parser().complete(prefix));
        } else {
            for (Node child : selected.children()) {
                final DefinitionId target = child.targetResolver().resolve(Arguments.empty());
                if (child.senderRule().accepts(subject)
                        && authorization.authorize(AuthorizationRequest.of(subject.authorization(),
                        child.permission(), target)).isAllowed()
                        && child.label().startsWith(prefix)) {
                    candidates.add(child.label());
                }
            }
        }
        Collections.sort(candidates);
        return Collections.unmodifiableList(new ArrayList<String>(candidates.subList(0,
                Math.min(candidates.size(), MAX_COMPLETIONS))));
    }

    /** @return immutable depth-first command inventory used by documentation and parity checks */
    public List<InventoryEntry> inventory() {
        final List<InventoryEntry> result = new ArrayList<InventoryEntry>();
        inventory(root, root.label(), result);
        return Collections.unmodifiableList(result);
    }

    private void inventory(final Node node, final String path, final List<InventoryEntry> entries) {
        entries.add(new InventoryEntry(node.id(), path, node.permission(), node.senderRule(),
                node.help(), node.usage(), node.executor().isPresent()));
        for (Node child : node.children()) { inventory(child, path + " " + child.label(), entries); }
    }

    private synchronized boolean acquireCooldown(final CooldownKey key, final Instant now,
                                                 final Duration duration) {
        cooldowns.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        final Instant previous = cooldowns.get(key);
        if (previous != null && previous.isAfter(now)) { return false; }
        if (!duration.isZero()) {
            if (cooldowns.size() >= maximumCooldowns) {
                final CooldownKey oldest = Collections.min(cooldowns.entrySet(),
                        Comparator.comparing(Map.Entry::getValue)).getKey();
                cooldowns.remove(oldest);
            }
            cooldowns.put(key, now.plus(duration));
        }
        return true;
    }

    private synchronized void releaseCooldown(final CooldownKey key) { cooldowns.remove(key); }

    private static ParsedArguments parse(final List<ArgumentSpec<?>> specifications,
                                         final List<String> tokens, final int start) {
        final Arguments.Builder builder = Arguments.builder();
        int cursor = start;
        for (ArgumentSpec<?> specification : specifications) {
            if (cursor >= tokens.size()) {
                if (specification.required()) {
                    return ParsedArguments.failure(io.zartra.bedwars.api.localization.MessageKey.of(
                            "command.argument.missing"));
                }
                continue;
            }
            final ParseResult<?> parsed = specification.parser().parse(tokens.get(cursor));
            if (!parsed.isSuccess()) {
                return ParsedArguments.failure(parsed.error().orElseThrow(
                        () -> new IllegalStateException("failed parse without error")));
            }
            put(builder, specification, parsed.value().orElseThrow(
                    () -> new IllegalStateException("successful parse without value")));
            cursor++;
        }
        return ParsedArguments.success(builder.build(), cursor);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void put(final Arguments.Builder builder, final ArgumentSpec specification,
                            final Object value) {
        builder.put(specification.key(), value);
    }

    private static List<String> copyTokens(final List<String> values) {
        Objects.requireNonNull(values, "tokens");
        if (values.size() > MAX_TOKENS) { throw new IllegalArgumentException("too many command tokens"); }
        final List<String> copy = new ArrayList<String>(values.size());
        for (String value : values) {
            if (value == null || value.length() > 256) { throw new IllegalArgumentException("invalid command token"); }
            copy.add(value.toLowerCase(java.util.Locale.ROOT));
        }
        return Collections.unmodifiableList(copy);
    }

    private static Execution immediate(final CorrelationId correlation, final Result result) {
        return new Execution(correlation, CompletableFuture.completedFuture(result),
                new CancellationSource());
    }

    /** Bounded asynchronous execution port supplied by a runtime module. */
    public interface ExecutionSupervisor {
        /** @return eventual result; implementations enforce the node deadline and capacity */
        CompletionStage<Result> submit(Node node, ExecutionContext context,
                                       CommandModel.Executor executor);
    }

    /** Secret-free command audit port. */
    public interface AuditSink { /** Records one immutable lifecycle event. */ void record(AuditRecord record); }

    /** Immutable command audit event containing identities but no raw input or credentials. */
    public static final class AuditRecord {
        private final CorrelationId correlationId;
        private final CommandModel.CommandId commandId;
        private final String subject;
        private final DefinitionId target;
        private final Phase phase;
        private final Result.Status status;
        private final Instant occurredAt;
        private AuditRecord(final CorrelationId correlationId, final Node node, final Subject subject,
                            final DefinitionId target, final Phase phase, final Result.Status status,
                            final Instant occurredAt) {
            this.correlationId = correlationId;
            commandId = node.id();
            this.subject = subject.authorization().toString();
            this.target = target;
            this.phase = phase;
            this.status = status;
            this.occurredAt = occurredAt;
        }
        private static AuditRecord started(final CorrelationId id, final Node node,
                                           final Subject subject, final DefinitionId target,
                                           final Instant at) {
            return new AuditRecord(id, node, subject, target, Phase.STARTED, null, at);
        }
        private static AuditRecord denied(final CorrelationId id, final Node node,
                                          final Subject subject, final DefinitionId target,
                                          final Instant at) {
            return new AuditRecord(id, node, subject, target, Phase.DENIED,
                    Result.Status.FORBIDDEN, at);
        }
        private static AuditRecord failed(final CorrelationId id, final Node node,
                                          final Subject subject, final DefinitionId target,
                                          final Instant at) {
            return new AuditRecord(id, node, subject, target, Phase.COMPLETED,
                    Result.Status.ERROR, at);
        }
        private static AuditRecord completed(final CorrelationId id, final Node node,
                                             final Subject subject, final DefinitionId target,
                                             final Result.Status status, final Instant at) {
            return new AuditRecord(id, node, subject, target, Phase.COMPLETED, status, at);
        }
        /** @return correlation identity */ public CorrelationId correlationId() { return correlationId; }
        /** @return command identity */ public CommandModel.CommandId commandId() { return commandId; }
        /** @return canonical authenticated subject identity */ public String subject() { return subject; }
        /** @return protected target */ public DefinitionId target() { return target; }
        /** @return lifecycle phase */ public Phase phase() { return phase; }
        /** @return terminal status when present */ public Optional<Result.Status> status() { return Optional.ofNullable(status); }
        /** @return timestamp */ public Instant occurredAt() { return occurredAt; }
        /** Audit lifecycle phases. */ public enum Phase { /** Accepted. */ STARTED, /** Denied. */ DENIED, /** Terminal. */ COMPLETED }
    }

    /** One cancellable execution returned to a platform adapter. */
    public static final class Execution {
        private final CorrelationId correlationId;
        private final CompletionStage<Result> result;
        private final CancellationSource cancellation;
        private Execution(final CorrelationId correlationId, final CompletionStage<Result> result,
                          final CancellationSource cancellation) {
            this.correlationId = correlationId;
            this.result = result;
            this.cancellation = cancellation;
        }
        /** @return correlation identity */ public CorrelationId correlationId() { return correlationId; }
        /** @return eventual structured result */ public CompletionStage<Result> result() { return result; }
        /** Requests cooperative cancellation. */ public void cancel() { cancellation.cancel(); }
    }

    /** Immutable documentation and parity projection for one command node. */
    public static final class InventoryEntry {
        private final CommandModel.CommandId id;
        private final String path;
        private final io.zartra.bedwars.api.authorization.PermissionNode permission;
        private final CommandModel.SenderRule senderRule;
        private final io.zartra.bedwars.api.localization.MessageKey help;
        private final io.zartra.bedwars.api.localization.MessageKey usage;
        private final boolean executable;
        private InventoryEntry(final CommandModel.CommandId id, final String path,
                               final io.zartra.bedwars.api.authorization.PermissionNode permission,
                               final CommandModel.SenderRule senderRule,
                               final io.zartra.bedwars.api.localization.MessageKey help,
                               final io.zartra.bedwars.api.localization.MessageKey usage,
                               final boolean executable) {
            this.id = id;
            this.path = path;
            this.permission = permission;
            this.senderRule = senderRule;
            this.help = help;
            this.usage = usage;
            this.executable = executable;
        }
        /** @return stable ID */ public CommandModel.CommandId id() { return id; }
        /** @return canonical path */ public String path() { return path; }
        /** @return permission */ public io.zartra.bedwars.api.authorization.PermissionNode permission() { return permission; }
        /** @return sender constraint */ public CommandModel.SenderRule senderRule() { return senderRule; }
        /** @return help key */ public io.zartra.bedwars.api.localization.MessageKey help() { return help; }
        /** @return usage key */ public io.zartra.bedwars.api.localization.MessageKey usage() { return usage; }
        /** @return whether executable */ public boolean executable() { return executable; }
    }

    private static final class CancellationSource implements CancellationToken {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        @Override public boolean isCancellationRequested() { return cancelled.get(); }
        private void cancel() { cancelled.set(true); }
    }

    private static final class ParsedArguments {
        private final boolean success;
        private final Arguments arguments;
        private final int consumed;
        private final io.zartra.bedwars.api.localization.MessageKey error;
        private ParsedArguments(final boolean success, final Arguments arguments, final int consumed,
                                final io.zartra.bedwars.api.localization.MessageKey error) {
            this.success = success;
            this.arguments = arguments;
            this.consumed = consumed;
            this.error = error;
        }
        private static ParsedArguments success(final Arguments arguments, final int consumed) {
            return new ParsedArguments(true, arguments, consumed, null);
        }
        private static ParsedArguments failure(final io.zartra.bedwars.api.localization.MessageKey error) {
            return new ParsedArguments(false, Arguments.empty(), 0, error);
        }
    }

    private static final class CooldownKey {
        private final String subject;
        private final CommandModel.CommandId command;
        private CooldownKey(final String subject, final CommandModel.CommandId command) {
            this.subject = subject;
            this.command = command;
        }
        @Override public int hashCode() { return Objects.hash(subject, command); }
        @Override public boolean equals(final Object other) {
            if (!(other instanceof CooldownKey)) { return false; }
            final CooldownKey that = (CooldownKey) other;
            return subject.equals(that.subject) && command.equals(that.command);
        }
    }
}
