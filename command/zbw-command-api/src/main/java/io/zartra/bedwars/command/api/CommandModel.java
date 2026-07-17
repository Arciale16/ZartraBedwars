package io.zartra.bedwars.command.api;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/** Immutable values and extension points used by the neutral command framework. */
public final class CommandModel {
    private CommandModel() { throw new AssertionError("No instances"); }

    /** Stable namespaced command identity independent of labels and aliases. */
    public static final class CommandId implements Comparable<CommandId> {
        private final DefinitionId value;
        private CommandId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return a validated command identity */
        public static CommandId of(final String namespace, final String path) {
            return new CommandId(DefinitionId.of(namespace, path));
        }
        /** @return a parsed command identity */
        public static CommandId parse(final String value) {
            return new CommandId(DefinitionId.parse(value));
        }
        /** @return underlying stable definition identity */ public DefinitionId value() { return value; }
        @Override public int compareTo(final CommandId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public String toString() { return value.toString(); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof CommandId && value.equals(((CommandId) other).value); }
    }

    /** Typed argument key; the runtime type is checked at insertion and lookup. */
    public static final class ArgumentKey<T> {
        private final String name;
        private final Class<T> type;
        private ArgumentKey(final String name, final Class<T> type) {
            if (name == null || !name.matches("[a-z][a-z0-9_.-]{0,63}")) {
                throw new IllegalArgumentException("invalid argument name");
            }
            this.name = name;
            this.type = Objects.requireNonNull(type, "type");
        }
        /** @return a validated typed argument key */
        public static <T> ArgumentKey<T> of(final String name, final Class<T> type) {
            return new ArgumentKey<T>(name, type);
        }
        /** @return stable argument name */ public String name() { return name; }
        /** @return runtime argument type */ public Class<T> type() { return type; }
        @Override public int hashCode() { return Objects.hash(name, type); }
        @Override public boolean equals(final Object other) {
            if (!(other instanceof ArgumentKey)) { return false; }
            final ArgumentKey<?> that = (ArgumentKey<?>) other;
            return name.equals(that.name) && type.equals(that.type);
        }
    }

    /** Immutable typed parsed arguments. */
    public static final class Arguments {
        private final Map<ArgumentKey<?>, Object> values;
        private Arguments(final Map<ArgumentKey<?>, Object> source) {
            values = Collections.unmodifiableMap(new LinkedHashMap<ArgumentKey<?>, Object>(source));
        }
        /** @return empty arguments */ public static Arguments empty() { return new Arguments(Collections.<ArgumentKey<?>, Object>emptyMap()); }
        /** @return a new mutable builder */ public static Builder builder() { return new Builder(); }
        /** @return the required typed value */
        public <T> T require(final ArgumentKey<T> key) {
            final Object value = values.get(Objects.requireNonNull(key, "key"));
            if (value == null) { throw new IllegalArgumentException("missing argument " + key.name()); }
            return key.type().cast(value);
        }
        /** @return optional typed value */
        public <T> Optional<T> find(final ArgumentKey<T> key) {
            return Optional.ofNullable(key.type().cast(values.get(Objects.requireNonNull(key, "key"))));
        }
        /** @return immutable argument names */
        public Set<String> names() {
            final Set<String> names = new LinkedHashSet<String>();
            for (ArgumentKey<?> key : values.keySet()) { names.add(key.name()); }
            return Collections.unmodifiableSet(names);
        }

        /** Builder enforcing unique typed argument keys. */
        public static final class Builder {
            private final Map<ArgumentKey<?>, Object> values = new LinkedHashMap<ArgumentKey<?>, Object>();
            /** Adds one non-null value. */
            public <T> Builder put(final ArgumentKey<T> key, final T value) {
                final ArgumentKey<T> checked = Objects.requireNonNull(key, "key");
                final T cast = checked.type().cast(Objects.requireNonNull(value, "value"));
                if (values.put(checked, cast) != null) { throw new IllegalArgumentException("duplicate argument " + checked.name()); }
                return this;
            }
            /** @return immutable arguments */ public Arguments build() { return new Arguments(values); }
        }
    }

    /** Platform-neutral argument parser with side-effect-free completion. */
    public interface ArgumentParser<T> {
        /** @return typed parse outcome for one token */ ParseResult<T> parse(String token);
        /** @return bounded deterministic completion candidates */ List<String> complete(String prefix);
    }

    /** Immutable parse success or localized validation failure. */
    public static final class ParseResult<T> {
        private final T value;
        private final MessageKey error;
        private ParseResult(final T value, final MessageKey error) {
            this.value = value;
            this.error = error;
        }
        /** @return successful parse */ public static <T> ParseResult<T> success(final T value) { return new ParseResult<T>(Objects.requireNonNull(value, "value"), null); }
        /** @return failed parse */ public static <T> ParseResult<T> failure(final MessageKey error) { return new ParseResult<T>(null, Objects.requireNonNull(error, "error")); }
        /** @return whether parsing succeeded */ public boolean isSuccess() { return error == null; }
        /** @return parsed value */ public Optional<T> value() { return Optional.ofNullable(value); }
        /** @return localized error key */ public Optional<MessageKey> error() { return Optional.ofNullable(error); }
    }

    /** Built-in strict parsers used by the generated command inventory. */
    public static final class Parsers {
        private static final MessageKey INVALID = MessageKey.of("command.argument.invalid");
        private Parsers() { throw new AssertionError("No instances"); }
        /** @return non-blank bounded word parser */
        public static ArgumentParser<String> word() {
            return new ArgumentParser<String>() {
                @Override public ParseResult<String> parse(final String token) {
                    return token != null && !token.trim().isEmpty() && token.length() <= 128
                            ? ParseResult.success(token) : ParseResult.<String>failure(INVALID);
                }
                @Override public List<String> complete(final String prefix) { return Collections.emptyList(); }
            };
        }
        /** @return bounded integer parser */
        public static ArgumentParser<Integer> integer(final int minimum, final int maximum) {
            if (maximum < minimum) { throw new IllegalArgumentException("invalid integer range"); }
            return new ArgumentParser<Integer>() {
                @Override public ParseResult<Integer> parse(final String token) {
                    try {
                        final int value = Integer.parseInt(token);
                        return value >= minimum && value <= maximum ? ParseResult.success(Integer.valueOf(value)) : ParseResult.<Integer>failure(INVALID);
                    } catch (RuntimeException failure) { return ParseResult.failure(INVALID); }
                }
                @Override public List<String> complete(final String prefix) { return Collections.emptyList(); }
            };
        }
        /** @return namespaced definition-ID parser */
        public static ArgumentParser<DefinitionId> definitionId() {
            return mapped(new Mapper<DefinitionId>() {
                @Override public DefinitionId map(final String value) {
                    return DefinitionId.parse(value);
                }
            });
        }
        /** @return UUID arena-ID parser */
        public static ArgumentParser<ArenaId> arenaId() {
            return mapped(new Mapper<ArenaId>() {
                @Override public ArenaId map(final String value) { return ArenaId.parse(value); }
            });
        }
        /** @return UUID match-ID parser */
        public static ArgumentParser<MatchId> matchId() {
            return mapped(new Mapper<MatchId>() {
                @Override public MatchId map(final String value) { return MatchId.parse(value); }
            });
        }
        private static <T> ArgumentParser<T> mapped(final Mapper<T> mapper) {
            return new ArgumentParser<T>() {
                @Override public ParseResult<T> parse(final String token) {
                    try { return ParseResult.success(mapper.map(token)); }
                    catch (RuntimeException failure) { return ParseResult.failure(INVALID); }
                }
                @Override public List<String> complete(final String prefix) { return Collections.emptyList(); }
            };
        }
        private interface Mapper<T> { T map(String value); }
    }

    /** Authenticated neutral sender; no platform sender object is retained. */
    public static final class Subject {
        private final AuthorizationSubject authorization;
        private final Kind kind;
        private final PlayerId playerId;
        private final LocaleId locale;
        private Subject(final AuthorizationSubject authorization, final Kind kind, final PlayerId playerId, final LocaleId locale) {
            this.authorization = Objects.requireNonNull(authorization, "authorization");
            this.kind = Objects.requireNonNull(kind, "kind");
            if ((kind == Kind.PLAYER) != (playerId != null)) { throw new IllegalArgumentException("player identity does not match sender kind"); }
            this.playerId = playerId;
            this.locale = Objects.requireNonNull(locale, "locale");
        }
        /** @return player sender */ public static Subject player(final AuthorizationSubject subject, final PlayerId playerId, final LocaleId locale) { return new Subject(subject, Kind.PLAYER, Objects.requireNonNull(playerId, "playerId"), locale); }
        /** @return console sender */ public static Subject console(final AuthorizationSubject subject, final LocaleId locale) { return new Subject(subject, Kind.CONSOLE, null, locale); }
        /** @return authorization identity */ public AuthorizationSubject authorization() { return authorization; }
        /** @return sender kind */ public Kind kind() { return kind; }
        /** @return player identity when applicable */ public Optional<PlayerId> playerId() { return Optional.ofNullable(playerId); }
        /** @return requested locale */ public LocaleId locale() { return locale; }
        /** Supported command sender kinds. */ public enum Kind { /** Authenticated player. */ PLAYER, /** Local console. */ CONSOLE }
    }

    /** Sender eligibility declared by a command node. */
    public enum SenderRule {
        /** Player or console. */ ANY,
        /** Player only. */ PLAYER_ONLY,
        /** Console only. */ CONSOLE_ONLY;
        /** @return whether the sender is eligible */
        public boolean accepts(final Subject subject) {
            return this == ANY || this == PLAYER_ONLY && subject.kind() == Subject.Kind.PLAYER || this == CONSOLE_ONLY && subject.kind() == Subject.Kind.CONSOLE;
        }
    }

    /** One typed argument declaration in a command path. */
    public static final class ArgumentSpec<T> {
        private final ArgumentKey<T> key;
        private final ArgumentParser<T> parser;
        private final boolean required;
        private ArgumentSpec(final ArgumentKey<T> key, final ArgumentParser<T> parser, final boolean required) {
            this.key = Objects.requireNonNull(key, "key");
            this.parser = Objects.requireNonNull(parser, "parser");
            this.required = required;
        }
        /** @return required argument declaration */ public static <T> ArgumentSpec<T> required(final ArgumentKey<T> key, final ArgumentParser<T> parser) { return new ArgumentSpec<T>(key, parser, true); }
        /** @return optional argument declaration */ public static <T> ArgumentSpec<T> optional(final ArgumentKey<T> key, final ArgumentParser<T> parser) { return new ArgumentSpec<T>(key, parser, false); }
        /** @return argument key */ public ArgumentKey<T> key() { return key; }
        /** @return parser */ public ArgumentParser<T> parser() { return parser; }
        /** @return whether input is required */ public boolean required() { return required; }
    }

    /** Immutable command execution context. */
    public static final class ExecutionContext {
        private final Subject subject;
        private final Arguments arguments;
        private final DefinitionId target;
        private final CorrelationId correlationId;
        private final CancellationToken cancellationToken;
        private final Instant deadline;
        /** Creates a context after parsing and authorization. */
        public ExecutionContext(final Subject subject, final Arguments arguments, final DefinitionId target,
                                final CorrelationId correlationId, final CancellationToken cancellationToken,
                                final Instant deadline) {
            this.subject = Objects.requireNonNull(subject, "subject");
            this.arguments = Objects.requireNonNull(arguments, "arguments");
            this.target = Objects.requireNonNull(target, "target");
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
            this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
            this.deadline = Objects.requireNonNull(deadline, "deadline");
        }
        /** @return sender */ public Subject subject() { return subject; }
        /** @return parsed arguments */ public Arguments arguments() { return arguments; }
        /** @return protected target */ public DefinitionId target() { return target; }
        /** @return audit correlation */ public CorrelationId correlationId() { return correlationId; }
        /** @return cooperative cancellation */ public CancellationToken cancellationToken() { return cancellationToken; }
        /** @return execution deadline */ public Instant deadline() { return deadline; }
    }

    /** Structured localized command result. */
    public static final class Result {
        private final Status status;
        private final MessageKey message;
        private final LocalizationService.Parameters parameters;
        private Result(final Status status, final MessageKey message, final LocalizationService.Parameters parameters) {
            this.status = Objects.requireNonNull(status, "status");
            this.message = Objects.requireNonNull(message, "message");
            this.parameters = Objects.requireNonNull(parameters, "parameters");
        }
        /** @return a structured result */ public static Result of(final Status status, final MessageKey message, final LocalizationService.Parameters parameters) { return new Result(status, message, parameters); }
        /** @return simple result without parameters */ public static Result simple(final Status status, final String messageKey) { return of(status, MessageKey.of(messageKey), LocalizationService.Parameters.empty()); }
        /** @return status */ public Status status() { return status; }
        /** @return localization key */ public MessageKey message() { return message; }
        /** @return safe typed parameters */ public LocalizationService.Parameters parameters() { return parameters; }
        /** Result classifications used uniformly by command and GUI presentation. */
        public enum Status { /** Mutation or query succeeded. */ SUCCESS, /** Help rendered. */ HELP, /** Usage rendered. */ USAGE, /** Invalid input. */ INVALID, /** Sender kind rejected. */ SENDER_REJECTED, /** Permission denied. */ FORBIDDEN, /** Cooldown active. */ COOLDOWN, /** Cancelled. */ CANCELLED, /** Deadline elapsed. */ TIMEOUT, /** Conflict or stale state. */ CONFLICT, /** Typed operational error. */ ERROR }
    }

    /** Extension executor; it must never block a Minecraft owner thread. */
    public interface Executor { /** @return non-null eventual structured result */ CompletionStage<Result> execute(ExecutionContext context); }

    /** Resolves the protected target from already validated arguments. */
    public interface TargetResolver { /** @return non-null stable target */ DefinitionId resolve(Arguments arguments); }

    /** Immutable command node in a validated tree. */
    public static final class Node {
        private final CommandId id;
        private final String label;
        private final Set<String> aliases;
        private final MessageKey help;
        private final MessageKey usage;
        private final PermissionNode permission;
        private final SenderRule senderRule;
        private final Duration cooldown;
        private final Duration timeout;
        private final List<ArgumentSpec<?>> arguments;
        private final List<Node> children;
        private final TargetResolver targetResolver;
        private final Executor executor;
        private Node(final Builder builder) {
            id = Objects.requireNonNull(builder.id, "id");
            label = label(builder.label);
            aliases = immutableLabels(builder.aliases, label);
            help = Objects.requireNonNull(builder.help, "help");
            usage = Objects.requireNonNull(builder.usage, "usage");
            permission = Objects.requireNonNull(builder.permission, "permission");
            senderRule = Objects.requireNonNull(builder.senderRule, "senderRule");
            cooldown = positiveOrZero(builder.cooldown, "cooldown");
            timeout = positive(builder.timeout, "timeout");
            arguments = Collections.unmodifiableList(new ArrayList<ArgumentSpec<?>>(builder.arguments));
            children = children(builder.children);
            targetResolver = Objects.requireNonNull(builder.targetResolver, "targetResolver");
            executor = builder.executor;
            boolean optionalSeen = false;
            final Set<String> keys = new LinkedHashSet<String>();
            for (ArgumentSpec<?> argument : arguments) {
                if (!keys.add(argument.key().name()) || optionalSeen && argument.required()) { throw new IllegalArgumentException("invalid command arguments"); }
                optionalSeen |= !argument.required();
            }
            if (executor == null && children.isEmpty()) { throw new IllegalArgumentException("leaf command requires an executor"); }
        }
        /** @return builder for one node */ public static Builder builder(final CommandId id, final String label) { return new Builder(id, label); }
        /** @return stable identity */ public CommandId id() { return id; }
        /** @return primary label */ public String label() { return label; }
        /** @return immutable aliases */ public Set<String> aliases() { return aliases; }
        /** @return localized help key */ public MessageKey help() { return help; }
        /** @return localized usage key */ public MessageKey usage() { return usage; }
        /** @return exact permission */ public PermissionNode permission() { return permission; }
        /** @return sender rule */ public SenderRule senderRule() { return senderRule; }
        /** @return cooldown */ public Duration cooldown() { return cooldown; }
        /** @return execution timeout */ public Duration timeout() { return timeout; }
        /** @return immutable arguments */ public List<ArgumentSpec<?>> arguments() { return arguments; }
        /** @return immutable children */ public List<Node> children() { return children; }
        /** @return target resolver */ public TargetResolver targetResolver() { return targetResolver; }
        /** @return executor when this node is executable */ public Optional<Executor> executor() { return Optional.ofNullable(executor); }
        /** @return matching child for primary label or alias */
        public Optional<Node> child(final String token) {
            for (Node child : children) { if (child.label.equals(token) || child.aliases.contains(token)) { return Optional.of(child); } }
            return Optional.empty();
        }
        private static String label(final String value) {
            if (value == null || !value.matches("[a-z][a-z0-9_-]{0,31}")) { throw new IllegalArgumentException("invalid command label"); }
            return value;
        }
        private static Set<String> immutableLabels(final Collection<String> values, final String primary) {
            final Set<String> copy = new LinkedHashSet<String>();
            for (String value : Objects.requireNonNull(values, "aliases")) {
                final String checked = label(value);
                if (primary.equals(checked) || !copy.add(checked)) { throw new IllegalArgumentException("duplicate command alias"); }
            }
            return Collections.unmodifiableSet(copy);
        }
        private static List<Node> children(final Collection<Node> values) {
            final List<Node> copy = new ArrayList<Node>();
            final Set<String> labels = new LinkedHashSet<String>();
            for (Node value : Objects.requireNonNull(values, "children")) {
                final Node child = Objects.requireNonNull(value, "child");
                if (!labels.add(child.label)) { throw new IllegalArgumentException("duplicate child label"); }
                for (String alias : child.aliases) { if (!labels.add(alias)) { throw new IllegalArgumentException("duplicate child alias"); } }
                copy.add(child);
            }
            return Collections.unmodifiableList(copy);
        }
        private static Duration positive(final Duration value, final String label) {
            if (value == null || value.isZero() || value.isNegative()) { throw new IllegalArgumentException(label + " must be positive"); }
            return value;
        }
        private static Duration positiveOrZero(final Duration value, final String label) {
            if (value == null || value.isNegative()) { throw new IllegalArgumentException(label + " must not be negative"); }
            return value;
        }

        /** Fluent construction helper validated by {@link #build()}. */
        public static final class Builder {
            private final CommandId id;
            private final String label;
            private final List<String> aliases = new ArrayList<String>();
            private MessageKey help = MessageKey.of("command.help");
            private MessageKey usage = MessageKey.of("command.usage");
            private PermissionNode permission = PermissionNode.of("zartrabedwars.command.use");
            private SenderRule senderRule = SenderRule.ANY;
            private Duration cooldown = Duration.ZERO;
            private Duration timeout = Duration.ofSeconds(5L);
            private final List<ArgumentSpec<?>> arguments = new ArrayList<ArgumentSpec<?>>();
            private final List<Node> children = new ArrayList<Node>();
            private TargetResolver targetResolver = new TargetResolver() {
                @Override public DefinitionId resolve(final Arguments ignored) {
                    return DefinitionId.of("zartra", "presentation/global");
                }
            };
            private Executor executor;
            private Builder(final CommandId id, final String label) {
                this.id = Objects.requireNonNull(id, "id");
                this.label = label;
            }
            /** Adds an alias. */ public Builder alias(final String value) {
                aliases.add(value);
                return this;
            }
            /** Sets localized help and usage keys. */
            public Builder messages(final MessageKey helpKey, final MessageKey usageKey) {
                help = helpKey;
                usage = usageKey;
                return this;
            }
            /** Sets exact permission. */ public Builder permission(final PermissionNode value) {
                permission = value;
                return this;
            }
            /** Sets sender rule. */ public Builder senderRule(final SenderRule value) {
                senderRule = value;
                return this;
            }
            /** Sets cooldown. */ public Builder cooldown(final Duration value) {
                cooldown = value;
                return this;
            }
            /** Sets timeout. */ public Builder timeout(final Duration value) {
                timeout = value;
                return this;
            }
            /** Adds an argument. */ public Builder argument(final ArgumentSpec<?> value) {
                arguments.add(Objects.requireNonNull(value, "argument"));
                return this;
            }
            /** Adds a subcommand. */ public Builder child(final Node value) {
                children.add(Objects.requireNonNull(value, "child"));
                return this;
            }
            /** Sets target resolver. */ public Builder target(final TargetResolver value) {
                targetResolver = value;
                return this;
            }
            /** Sets executor. */ public Builder executor(final Executor value) {
                executor = value;
                return this;
            }
            /** @return immutable validated node */ public Node build() { return new Node(this); }
        }
    }
}
