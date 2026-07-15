package io.zartra.bedwars.config.schema;

import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.api.configuration.ReloadTarget;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.secret.SecretRef;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Namespace for the immutable M03 configuration model and its deterministic services. */
public final class ConfigurationModel {
    private ConfigurationModel() { throw new AssertionError("No instances"); }

    /** The complete logical configuration-file inventory required by the PRD. */
    public enum LogicalFile {
        /** Core settings. */ CONFIG("config.yml", ReloadTarget.CORE),
        /** Deployment topology. */ DEPLOYMENT("deployment.yml", ReloadTarget.CORE),
        /** Database connection policy. */ DATABASE("database.yml", ReloadTarget.CORE),
        /** Redis connection policy. */ REDIS("redis.yml", ReloadTarget.CORE),
        /** Proxy connection policy. */ PROXY("proxy.yml", ReloadTarget.CORE),
        /** CloudNet connection policy. */ CLOUDNET("cloudnet.yml", ReloadTarget.CORE),
        /** Arena definitions. */ ARENAS("arenas.yml", ReloadTarget.ARENAS),
        /** Map definitions. */ MAPS("maps.yml", ReloadTarget.ARENAS),
        /** Game-mode and private-game definitions. */ MODES("modes.yml", ReloadTarget.ARENAS),
        /** Shop definitions. */ SHOPS("shops.yml", ReloadTarget.SHOP),
        /** Upgrade definitions. */ UPGRADES("upgrades.yml", ReloadTarget.SHOP),
        /** Generator definitions. */ GENERATORS("generators.yml", ReloadTarget.ARENAS),
        /** Gameplay-item definitions. */ ITEMS("items.yml", ReloadTarget.SHOP),
        /** Quest definitions. */ QUESTS("quests.yml", ReloadTarget.QUESTS),
        /** Achievement definitions. */ ACHIEVEMENTS("achievements.yml", ReloadTarget.QUESTS),
        /** Challenge definitions. */ CHALLENGES("challenges.yml", ReloadTarget.QUESTS),
        /** Battle-pass definitions. */ BATTLEPASS("battlepass.yml", ReloadTarget.QUESTS),
        /** Cosmetic definitions. */ COSMETICS("cosmetics.yml", ReloadTarget.COSMETICS),
        /** Content-pack selection and provenance. */ CONTENT("content.yml", ReloadTarget.CORE),
        /** Reward definitions. */ REWARDS("rewards.yml", ReloadTarget.QUESTS),
        /** Statistics visibility and policy. */ STATISTICS("statistics.yml", ReloadTarget.CORE),
        /** Placeholder definitions. */ PLACEHOLDERS("placeholders.yml", ReloadTarget.PLACEHOLDERS),
        /** Replay retention declarations. */ REPLAY("replay.yml", ReloadTarget.CORE),
        /** Atlas retention and visibility declarations. */ ATLAS("atlas.yml", ReloadTarget.CORE),
        /** Anticheat integration declarations. */ ANTICHEAT("anticheat.yml", ReloadTarget.INTEGRATIONS),
        /** Party provider declarations. */ PARTIES("parties.yml", ReloadTarget.INTEGRATIONS),
        /** NPC provider declarations. */ NPCS("npcs.yml", ReloadTarget.INTEGRATIONS),
        /** Hologram provider declarations. */ HOLOGRAMS("holograms.yml", ReloadTarget.INTEGRATIONS),
        /** GUI definitions. */ GUI("gui.yml", ReloadTarget.GUI),
        /** Localized message settings. */ MESSAGES("messages.yml", ReloadTarget.MESSAGES),
        /** Authorization settings. */ PERMISSIONS("permissions.yml", ReloadTarget.PERMISSIONS),
        /** Version fallback declarations. */ COMPATIBILITY("compatibility.yml", ReloadTarget.COMPATIBILITY),
        /** Performance budgets. */ PERFORMANCE("performance.yml", ReloadTarget.CORE),
        /** Security, privacy and script policy. */ SECURITY("security.yml", ReloadTarget.SECURITY),
        /** General integration declarations. */ INTEGRATIONS("integrations.yml", ReloadTarget.INTEGRATIONS),
        /** Discord provider declaration. */ DISCORD("integrations/discord.yml", ReloadTarget.INTEGRATIONS);

        private final String path;
        private final ReloadTarget target;
        LogicalFile(final String path, final ReloadTarget target) {
            this.path = path;
            this.target = target;
        }
        /** @return repository-relative logical path */ public String path() { return path; }
        /** @return default reload classification */ public ReloadTarget reloadTarget() { return target; }
    }

    /** Parser and canonical renderer for one option value type. */
    public interface ValueType<T> {
        /** @return parsed immutable value @throws IllegalArgumentException for malformed input */
        T parse(String serialized);
        /** @return canonical representation suitable for generated configuration */
        String render(T value);
        /** @return stable human-readable type name */
        String name();
    }

    /** Additional semantic constraint applied after type parsing. */
    public interface Constraint<T> {
        /** @return empty when valid, or a stable issue code when invalid */
        Optional<DefinitionId> validate(T value);
    }

    /** Approved built-in option value types. */
    public static final class ValueTypes {
        /** Strict lower-case boolean type. */
        public static final ValueType<Boolean> BOOLEAN = new ValueType<Boolean>() {
            @Override public Boolean parse(final String value) {
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw new IllegalArgumentException("Boolean must be true or false");
                }
                return Boolean.valueOf(value);
            }
            @Override public String render(final Boolean value) { return value.toString(); }
            @Override public String name() { return "boolean"; }
        };
        /** Bounded 32-bit integer type. */
        public static final ValueType<Integer> INTEGER = new ValueType<Integer>() {
            @Override public Integer parse(final String value) {
                try { return Integer.valueOf(value); }
                catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Integer is malformed", exception);
                }
            }
            @Override public String render(final Integer value) { return value.toString(); }
            @Override public String name() { return "integer"; }
        };
        /** Bounded decimal type. */
        public static final ValueType<BigDecimal> DECIMAL = new ValueType<BigDecimal>() {
            @Override public BigDecimal parse(final String value) {
                try {
                    final BigDecimal parsed = new BigDecimal(value).stripTrailingZeros();
                    if (parsed.precision() > 34 || Math.abs(parsed.scale()) > 12) {
                        throw new IllegalArgumentException("Decimal exceeds precision bounds");
                    }
                    return parsed;
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Decimal is malformed", exception);
                }
            }
            @Override public String render(final BigDecimal value) { return value.toPlainString(); }
            @Override public String name() { return "decimal"; }
        };
        /** ISO-8601 duration type. */
        public static final ValueType<Duration> DURATION = new ValueType<Duration>() {
            @Override public Duration parse(final String value) {
                try { return Duration.parse(value); }
                catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Duration is malformed", exception);
                }
            }
            @Override public String render(final Duration value) { return value.toString(); }
            @Override public String name() { return "duration"; }
        };
        /** Stable namespaced identifier type. */
        public static final ValueType<DefinitionId> DEFINITION_ID = new ValueType<DefinitionId>() {
            @Override public DefinitionId parse(final String value) { return DefinitionId.parse(value); }
            @Override public String render(final DefinitionId value) { return value.toString(); }
            @Override public String name() { return "definition-id"; }
        };
        /** Normalized locale type. */
        public static final ValueType<LocaleId> LOCALE = new ValueType<LocaleId>() {
            @Override public LocaleId parse(final String value) { return LocaleId.parse(value); }
            @Override public String render(final LocaleId value) { return value.toString(); }
            @Override public String name() { return "locale"; }
        };
        /** Secret-reference-only type. */
        public static final ValueType<SecretRef> SECRET_REF = new ValueType<SecretRef>() {
            @Override public SecretRef parse(final String value) { return SecretRef.parse(value); }
            @Override public String render(final SecretRef value) { return value.toString(); }
            @Override public String name() { return "secret-reference"; }
        };
        /** Resource-to-multiplier override collection type. */
        public static final ValueType<ResourceOverrides> RESOURCE_OVERRIDES = new ValueType<ResourceOverrides>() {
            @Override public ResourceOverrides parse(final String value) { return ResourceOverrides.parse(value); }
            @Override public String render(final ResourceOverrides value) { return value.toString(); }
            @Override public String name() { return "resource-multiplier-overrides"; }
        };

        private ValueTypes() { throw new AssertionError("No instances"); }

        /** @return strict value type accepting only the supplied canonical strings */
        public static ValueType<String> oneOf(final Collection<String> accepted) {
            if (accepted == null || accepted.isEmpty()) {
                throw new IllegalArgumentException("At least one non-null value is required");
            }
            final Set<String> collected = new TreeSet<String>();
            for (String value : accepted) {
                collected.add(Objects.requireNonNull(value, "acceptedValue"));
            }
            final Set<String> values = Collections.unmodifiableSet(collected);
            return new ValueType<String>() {
                @Override public String parse(final String value) {
                    if (!values.contains(value)) { throw new IllegalArgumentException("Value is not accepted"); }
                    return value;
                }
                @Override public String render(final String value) {
                    if (!values.contains(value)) { throw new IllegalArgumentException("Value is not accepted"); }
                    return value;
                }
                @Override public String name() { return "enum"; }
            };
        }
    }

    /** Complete documentation and operational metadata for one option. */
    public static final class OptionMetadata {
        private final String purpose;
        private final String defaultDescription;
        private final String acceptedValues;
        private final String example;
        private final List<ConfigurationKey> dependencies;
        private final List<ConfigurationKey> incompatibilities;
        private final String performanceImpact;
        private final String securityImpact;
        private final ReloadTarget reloadTarget;
        private final boolean restartRequired;
        private final String compatibility;
        private final String deprecation;
        private final String migration;

        private OptionMetadata(final Builder builder) {
            purpose = text(builder.purpose, "purpose");
            defaultDescription = text(builder.defaultDescription, "defaultDescription");
            acceptedValues = text(builder.acceptedValues, "acceptedValues");
            example = text(builder.example, "example");
            dependencies = immutableKeys(builder.dependencies, "dependency");
            incompatibilities = immutableKeys(builder.incompatibilities, "incompatibility");
            performanceImpact = text(builder.performanceImpact, "performanceImpact");
            securityImpact = text(builder.securityImpact, "securityImpact");
            reloadTarget = Objects.requireNonNull(builder.reloadTarget, "reloadTarget");
            restartRequired = builder.restartRequired;
            compatibility = text(builder.compatibility, "compatibility");
            deprecation = text(builder.deprecation, "deprecation");
            migration = text(builder.migration, "migration");
        }

        private static String text(final String value, final String label) {
            if (value == null || value.trim().isEmpty()) { throw new IllegalArgumentException(label + " is required"); }
            return value;
        }
        private static List<ConfigurationKey> immutableKeys(final Collection<ConfigurationKey> values,
                                                             final String label) {
            final List<ConfigurationKey> copy = new ArrayList<ConfigurationKey>();
            for (ConfigurationKey value : Objects.requireNonNull(values, label + "s")) {
                copy.add(Objects.requireNonNull(value, label));
            }
            Collections.sort(copy);
            return Collections.unmodifiableList(copy);
        }
        /** @return new metadata builder */ public static Builder builder() { return new Builder(); }
        /** @return purpose */ public String purpose() { return purpose; }
        /** @return documented default */ public String defaultDescription() { return defaultDescription; }
        /** @return accepted values */ public String acceptedValues() { return acceptedValues; }
        /** @return safe example */ public String example() { return example; }
        /** @return enabling dependencies */ public List<ConfigurationKey> dependencies() { return dependencies; }
        /** @return mutually incompatible options */ public List<ConfigurationKey> incompatibilities() { return incompatibilities; }
        /** @return performance impact */ public String performanceImpact() { return performanceImpact; }
        /** @return security impact */ public String securityImpact() { return securityImpact; }
        /** @return targeted reload group */ public ReloadTarget reloadTarget() { return reloadTarget; }
        /** @return whether a process restart is mandatory */ public boolean restartRequired() { return restartRequired; }
        /** @return compatibility notes */ public String compatibility() { return compatibility; }
        /** @return deprecation status */ public String deprecation() { return deprecation; }
        /** @return migration behavior */ public String migration() { return migration; }

        /** Builder used only while declaring an immutable schema. */
        public static final class Builder {
            private String purpose;
            private String defaultDescription;
            private String acceptedValues;
            private String example;
            private Collection<ConfigurationKey> dependencies = Collections.emptyList();
            private Collection<ConfigurationKey> incompatibilities = Collections.emptyList();
            private String performanceImpact = "No material impact at the documented default.";
            private String securityImpact = "No sensitive data; standard configuration authorization applies.";
            private ReloadTarget reloadTarget = ReloadTarget.CORE;
            private boolean restartRequired;
            private String compatibility = "Version-neutral M03 declaration; runtime support is certified separately.";
            private String deprecation = "Active; not deprecated.";
            private String migration = "Preserved by stable key and deterministic schema migrations.";
            private Builder() { }
            /** @return this builder */ public Builder purpose(final String value) {
                purpose = value;
                return this;
            }
            /** @return this builder */ public Builder defaultDescription(final String value) {
                defaultDescription = value;
                return this;
            }
            /** @return this builder */ public Builder acceptedValues(final String value) {
                acceptedValues = value;
                return this;
            }
            /** @return this builder */ public Builder example(final String value) {
                example = value;
                return this;
            }
            /** @return this builder */ public Builder dependencies(final Collection<ConfigurationKey> value) {
                dependencies = value;
                return this;
            }
            /** @return this builder */ public Builder incompatibilities(final Collection<ConfigurationKey> value) {
                incompatibilities = value;
                return this;
            }
            /** @return this builder */ public Builder performanceImpact(final String value) {
                performanceImpact = value;
                return this;
            }
            /** @return this builder */ public Builder securityImpact(final String value) {
                securityImpact = value;
                return this;
            }
            /** @return this builder */ public Builder reloadTarget(final ReloadTarget value) {
                reloadTarget = value;
                return this;
            }
            /** @return this builder */ public Builder restartRequired(final boolean value) {
                restartRequired = value;
                return this;
            }
            /** @return this builder */ public Builder compatibility(final String value) {
                compatibility = value;
                return this;
            }
            /** @return this builder */ public Builder deprecation(final String value) {
                deprecation = value;
                return this;
            }
            /** @return this builder */ public Builder migration(final String value) {
                migration = value;
                return this;
            }
            /** @return immutable complete metadata */ public OptionMetadata build() { return new OptionMetadata(this); }
        }
    }

    /** Typed immutable option definition. */
    public static final class OptionDefinition<T> {
        private final ConfigurationKey key;
        private final ValueType<T> type;
        private final T defaultValue;
        private final boolean required;
        private final Constraint<T> constraint;
        private final OptionMetadata metadata;

        private OptionDefinition(final ConfigurationKey key, final ValueType<T> type,
                                 final T defaultValue, final boolean required,
                                 final Constraint<T> constraint, final OptionMetadata metadata) {
            this.key = Objects.requireNonNull(key, "key");
            this.type = Objects.requireNonNull(type, "type");
            this.defaultValue = defaultValue;
            this.required = required;
            this.constraint = Objects.requireNonNull(constraint, "constraint");
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            if (required && defaultValue != null) {
                throw new IllegalArgumentException("Required options may not hide behind defaults");
            }
            if (defaultValue != null && constraint.validate(defaultValue).isPresent()) {
                throw new IllegalArgumentException("Default value violates its constraint");
            }
        }
        /** @return optional option with a validated default */
        public static <T> OptionDefinition<T> withDefault(final ConfigurationKey key,
                final ValueType<T> type, final T defaultValue, final Constraint<T> constraint,
                final OptionMetadata metadata) {
            return new OptionDefinition<T>(key, type, Objects.requireNonNull(defaultValue, "defaultValue"),
                    false, constraint, metadata);
        }
        /** @return optional option without a default */
        public static <T> OptionDefinition<T> optional(final ConfigurationKey key,
                final ValueType<T> type, final Constraint<T> constraint, final OptionMetadata metadata) {
            return new OptionDefinition<T>(key, type, null, false, constraint, metadata);
        }
        /** @return required option without a default */
        public static <T> OptionDefinition<T> required(final ConfigurationKey key,
                final ValueType<T> type, final Constraint<T> constraint, final OptionMetadata metadata) {
            return new OptionDefinition<T>(key, type, null, true, constraint, metadata);
        }
        /** @return stable option key */ public ConfigurationKey key() { return key; }
        /** @return typed parser/renderer */ public ValueType<T> type() { return type; }
        /** @return optional default */ public Optional<T> defaultValue() { return Optional.ofNullable(defaultValue); }
        /** @return whether an explicit value is required */ public boolean required() { return required; }
        /** @return semantic constraint */ public Constraint<T> constraint() { return constraint; }
        /** @return complete documentation metadata */ public OptionMetadata metadata() { return metadata; }
    }

    /** Immutable logical-file schema. */
    public static final class Schema {
        private final LogicalFile file;
        private final ConfigurationVersion version;
        private final Map<ConfigurationKey, OptionDefinition<?>> definitions;

        private Schema(final LogicalFile file, final ConfigurationVersion version,
                       final Collection<OptionDefinition<?>> definitions) {
            this.file = Objects.requireNonNull(file, "file");
            this.version = Objects.requireNonNull(version, "version");
            final Map<ConfigurationKey, OptionDefinition<?>> collected =
                    new TreeMap<ConfigurationKey, OptionDefinition<?>>();
            for (OptionDefinition<?> definition : Objects.requireNonNull(definitions, "definitions")) {
                final OptionDefinition<?> checked = Objects.requireNonNull(definition, "definition");
                if (collected.put(checked.key(), checked) != null) {
                    throw new IllegalArgumentException("Duplicate configuration key: " + checked.key());
                }
            }
            if (collected.isEmpty()) { throw new IllegalArgumentException("Schema must define options"); }
            this.definitions = Collections.unmodifiableMap(collected);
        }
        /** @return immutable schema with duplicate-key rejection */
        public static Schema of(final LogicalFile file, final ConfigurationVersion version,
                                final Collection<OptionDefinition<?>> definitions) {
            return new Schema(file, version, definitions);
        }
        /** @return logical file */ public LogicalFile file() { return file; }
        /** @return current schema version */ public ConfigurationVersion version() { return version; }
        /** @return deterministic immutable definitions */ public List<OptionDefinition<?>> definitions() {
            return Collections.unmodifiableList(new ArrayList<OptionDefinition<?>>(definitions.values()));
        }
        /** @return definition for a key */ public Optional<OptionDefinition<?>> definition(final ConfigurationKey key) {
            return Optional.ofNullable(definitions.get(Objects.requireNonNull(key, "key")));
        }
    }

    /** Immutable untrusted serialized configuration document supplied by an injected adapter. */
    public static final class Document {
        private final ConfigurationVersion version;
        private final Map<ConfigurationKey, String> values;
        private Document(final ConfigurationVersion version, final Map<ConfigurationKey, String> values) {
            this.version = Objects.requireNonNull(version, "version");
            final Map<ConfigurationKey, String> copy = new TreeMap<ConfigurationKey, String>();
            for (Map.Entry<ConfigurationKey, String> entry : Objects.requireNonNull(values, "values").entrySet()) {
                if (copy.put(Objects.requireNonNull(entry.getKey(), "key"),
                        Objects.requireNonNull(entry.getValue(), "value")) != null) {
                    throw new IllegalArgumentException("Duplicate configuration key");
                }
            }
            this.values = Collections.unmodifiableMap(copy);
        }
        /** @return immutable external document */
        public static Document of(final ConfigurationVersion version,
                                  final Map<ConfigurationKey, String> values) {
            return new Document(version, values);
        }
        /** @return source schema version */ public ConfigurationVersion version() { return version; }
        /** @return deterministic serialized values */ public Map<ConfigurationKey, String> values() { return values; }
        /** @return copy with a replaced value */
        public Document with(final ConfigurationKey key, final String value) {
            final Map<ConfigurationKey, String> changed = new TreeMap<ConfigurationKey, String>(values);
            changed.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return new Document(version, changed);
        }
        /** @return copy with a new version */
        public Document atVersion(final ConfigurationVersion newVersion) {
            return new Document(newVersion, values);
        }
    }

    /** One deterministic validation issue that never contains an offending value. */
    public static final class Issue implements Comparable<Issue> {
        private final Severity severity;
        private final DefinitionId code;
        private final ConfigurationKey key;
        private final String detail;
        private Issue(final Severity severity, final DefinitionId code,
                      final ConfigurationKey key, final String detail) {
            this.severity = Objects.requireNonNull(severity, "severity");
            this.code = Objects.requireNonNull(code, "code");
            this.key = key;
            this.detail = Objects.requireNonNull(detail, "detail");
        }
        /** @return validation issue */
        public static Issue of(final Severity severity, final DefinitionId code,
                               final ConfigurationKey key, final String detail) {
            return new Issue(severity, code, key, detail);
        }
        /** @return severity */ public Severity severity() { return severity; }
        /** @return stable issue code */ public DefinitionId code() { return code; }
        /** @return affected key, empty for document-wide issues */ public Optional<ConfigurationKey> key() { return Optional.ofNullable(key); }
        /** @return actionable secret-free detail */ public String detail() { return detail; }
        @Override public int compareTo(final Issue other) {
            int result = severity.compareTo(other.severity);
            if (result == 0) { result = code.compareTo(other.code); }
            if (result == 0) { result = String.valueOf(key).compareTo(String.valueOf(other.key)); }
            if (result == 0) { result = detail.compareTo(other.detail); }
            return result;
        }
    }

    /** Validation severity. */
    public enum Severity {
        /** Prevents publication. */ ERROR,
        /** Safe advisory that does not weaken validation. */ WARNING
    }

    /** Immutable parsed configuration snapshot. */
    public static final class ValidatedConfiguration {
        private final Schema schema;
        private final Map<ConfigurationKey, Object> values;
        private ValidatedConfiguration(final Schema schema, final Map<ConfigurationKey, Object> values) {
            this.schema = schema;
            this.values = Collections.unmodifiableMap(new TreeMap<ConfigurationKey, Object>(values));
        }
        /** @return originating schema */ public Schema schema() { return schema; }
        /** @return typed value or empty when an optional option was omitted */
        public <T> Optional<T> value(final OptionDefinition<T> definition) {
            Objects.requireNonNull(definition, "definition");
            final Object value = values.get(definition.key());
            return value == null ? Optional.<T>empty() : Optional.of(definition.type().parse(definition.type().render(cast(value))));
        }
        @SuppressWarnings("unchecked")
        private static <T> T cast(final Object value) { return (T) value; }
        /** @return keys whose effective typed values differ */
        public Set<ConfigurationKey> changedKeys(final ValidatedConfiguration other) {
            Objects.requireNonNull(other, "other");
            final Set<ConfigurationKey> keys = new TreeSet<ConfigurationKey>();
            keys.addAll(values.keySet());
            keys.addAll(other.values.keySet());
            final Set<ConfigurationKey> changed = new TreeSet<ConfigurationKey>();
            for (ConfigurationKey key : keys) {
                if (!Objects.equals(values.get(key), other.values.get(key))) { changed.add(key); }
            }
            return Collections.unmodifiableSet(changed);
        }
    }

    /** Immutable deterministic validation report. */
    public static final class ValidationReport {
        private final List<Issue> issues;
        private final ValidatedConfiguration configuration;
        private ValidationReport(final Collection<Issue> issues,
                                 final ValidatedConfiguration configuration) {
            final List<Issue> copy = new ArrayList<Issue>(issues);
            Collections.sort(copy);
            this.issues = Collections.unmodifiableList(copy);
            this.configuration = configuration;
        }
        /** @return successful validation report */
        public static ValidationReport valid(final ValidatedConfiguration configuration,
                                             final Collection<Issue> warnings) {
            return new ValidationReport(warnings, Objects.requireNonNull(configuration, "configuration"));
        }
        /** @return rejected validation report */
        public static ValidationReport invalid(final Collection<Issue> issues) {
            return new ValidationReport(issues, null);
        }
        /** @return deterministic issues */ public List<Issue> issues() { return issues; }
        /** @return whether no error prevents publication */ public boolean isValid() { return configuration != null; }
        /** @return parsed snapshot only when valid */ public Optional<ValidatedConfiguration> configuration() { return Optional.ofNullable(configuration); }
    }

    /** Strict deterministic validator for startup, manual validation and reload preparation. */
    public static final class Validator {
        /** @return complete validation report; unknown and invalid input is never accepted */
        public ValidationReport validate(final Schema schema, final Document document) {
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(document, "document");
            final List<Issue> issues = new ArrayList<Issue>();
            if (!schema.version().equals(document.version())) {
                issues.add(issue("config/version_mismatch", null,
                        document.version().compareTo(schema.version()) < 0
                                ? "A deterministic migration is required before validation."
                                : "The document version is newer than this runtime."));
            }
            for (ConfigurationKey key : document.values().keySet()) {
                if (!schema.definition(key).isPresent()) {
                    issues.add(issue("config/unknown_key", key,
                            "Remove the unknown key or install a schema that declares it."));
                }
            }
            final Map<ConfigurationKey, Object> parsed = new TreeMap<ConfigurationKey, Object>();
            for (OptionDefinition<?> definition : schema.definitions()) {
                parse(definition, document, parsed, issues);
            }
            validateRelations(schema, document, issues);
            if (!issues.isEmpty()) { return ValidationReport.invalid(issues); }
            return ValidationReport.valid(new ValidatedConfiguration(schema, parsed), Collections.<Issue>emptyList());
        }

        private static <T> void parse(final OptionDefinition<T> definition, final Document document,
                                      final Map<ConfigurationKey, Object> parsed,
                                      final List<Issue> issues) {
            final String serialized = document.values().get(definition.key());
            if (serialized == null) {
                if (definition.defaultValue().isPresent()) {
                    parsed.put(definition.key(), definition.defaultValue().get());
                } else if (definition.required()) {
                    issues.add(issue("config/missing_required", definition.key(),
                            "Provide the required option through its documented source."));
                }
                return;
            }
            try {
                final T value = definition.type().parse(serialized);
                final Optional<DefinitionId> constraint = definition.constraint().validate(value);
                if (constraint.isPresent()) {
                    issues.add(Issue.of(Severity.ERROR, constraint.get(), definition.key(),
                            "The parsed value violates the documented accepted range."));
                } else {
                    parsed.put(definition.key(), value);
                }
            } catch (RuntimeException exception) {
                issues.add(issue("config/malformed", definition.key(),
                        "The value does not match type " + definition.type().name() + '.'));
            }
        }

        private static void validateRelations(final Schema schema, final Document document,
                                              final List<Issue> issues) {
            for (OptionDefinition<?> definition : schema.definitions()) {
                final String serialized = document.values().get(definition.key());
                final boolean enabled = serialized != null && !serialized.isEmpty() && !"false".equals(serialized);
                if (!enabled) { continue; }
                for (ConfigurationKey dependency : definition.metadata().dependencies()) {
                    if (!document.values().containsKey(dependency)
                            || document.values().get(dependency).isEmpty()
                            || "false".equals(document.values().get(dependency))) {
                        issues.add(issue("config/missing_dependency", definition.key(),
                                "The option requires " + dependency + '.'));
                    }
                }
                for (ConfigurationKey incompatible : definition.metadata().incompatibilities()) {
                    if (document.values().containsKey(incompatible)
                            && !"false".equals(document.values().get(incompatible))) {
                        issues.add(issue("config/incompatible_options", definition.key(),
                                "The option is incompatible with " + incompatible + '.'));
                    }
                }
            }
        }
        private static Issue issue(final String code, final ConfigurationKey key, final String detail) {
            return Issue.of(Severity.ERROR, DefinitionId.of("zartra", code), key, detail);
        }
    }

    /** Deterministic comments, examples and Markdown reference generator. */
    public static final class ReferenceGenerator {
        /** @return fully commented canonical configuration for one logical file */
        public String commentedConfiguration(final Schema schema) {
            final StringBuilder output = new StringBuilder();
            output.append("# ZartraBedWars generated configuration reference\n")
                    .append("# Logical file: ").append(schema.file().path()).append('\n')
                    .append("# Schema version: ").append(schema.version()).append("\n\n");
            for (OptionDefinition<?> definition : schema.definitions()) {
                final OptionMetadata metadata = definition.metadata();
                output.append("# ").append(metadata.purpose()).append('\n')
                        .append("# Type: ").append(definition.type().name()).append('\n')
                        .append("# Default: ").append(metadata.defaultDescription()).append('\n')
                        .append("# Accepted: ").append(metadata.acceptedValues()).append('\n')
                        .append("# Dependencies: ").append(keys(metadata.dependencies())).append('\n')
                        .append("# Incompatible: ").append(keys(metadata.incompatibilities())).append('\n')
                        .append("# Reload: ").append(metadata.reloadTarget())
                        .append("; restart required: ").append(metadata.restartRequired()).append('\n')
                        .append("# Performance: ").append(metadata.performanceImpact()).append('\n')
                        .append("# Security: ").append(metadata.securityImpact()).append('\n')
                        .append("# Compatibility: ").append(metadata.compatibility()).append('\n')
                        .append("# Deprecation: ").append(metadata.deprecation()).append('\n')
                        .append("# Migration: ").append(metadata.migration()).append('\n');
                if (definition.defaultValue().isPresent()) {
                    output.append(definition.key()).append(" = ")
                            .append(render(definition, definition.defaultValue().get())).append("\n\n");
                } else {
                    output.append("# ").append(definition.key()).append(" = ")
                            .append(metadata.example()).append("\n\n");
                }
            }
            return output.toString();
        }
        /** @return deterministic Markdown option reference for all schemas */
        public String markdown(final Collection<Schema> schemas) {
            final StringBuilder output = new StringBuilder("# M03 Configuration Reference\n\n");
            for (Schema schema : schemas) {
                output.append("## `").append(schema.file().path()).append("`\n\n")
                        .append("Schema version: `").append(schema.version()).append("`.\n\n")
                        .append("| Key | Type | Default | Reload | Restart | Purpose |\n")
                        .append("|---|---|---|---|---|---|\n");
                for (OptionDefinition<?> definition : schema.definitions()) {
                    output.append('|').append('`').append(definition.key()).append('`').append('|')
                            .append(definition.type().name()).append('|')
                            .append(definition.metadata().defaultDescription()).append('|')
                            .append(definition.metadata().reloadTarget()).append('|')
                            .append(definition.metadata().restartRequired()).append('|')
                            .append(definition.metadata().purpose()).append("|\n");
                }
                output.append('\n');
            }
            return output.toString();
        }
        private static String keys(final List<ConfigurationKey> keys) {
            if (keys.isEmpty()) { return "none"; }
            final StringBuilder result = new StringBuilder();
            for (ConfigurationKey key : keys) {
                if (result.length() > 0) { result.append(", "); }
                result.append(key);
            }
            return result.toString();
        }
        private static <T> String render(final OptionDefinition<T> definition, final Object value) {
            return definition.type().render(cast(value));
        }
        @SuppressWarnings("unchecked")
        private static <T> T cast(final Object value) { return (T) value; }
    }

    /** Immutable custom-resource multiplier overrides for Resource Scarcity presets. */
    public static final class ResourceOverrides {
        private final Map<ResourceId, BigDecimal> values;
        private ResourceOverrides(final Map<ResourceId, BigDecimal> values) {
            this.values = Collections.unmodifiableMap(new TreeMap<ResourceId, BigDecimal>(values));
        }
        /** @return parsed comma-separated {@code resource:id=multiplier} entries */
        public static ResourceOverrides parse(final String value) {
            final Map<ResourceId, BigDecimal> parsed = new TreeMap<ResourceId, BigDecimal>();
            if (value == null) { throw new IllegalArgumentException("Overrides must not be null"); }
            if (value.isEmpty()) { return new ResourceOverrides(parsed); }
            for (String entry : value.split(",", -1)) {
                final int separator = entry.lastIndexOf('=');
                if (separator < 1 || separator == entry.length() - 1) {
                    throw new IllegalArgumentException("Malformed resource override");
                }
                final ResourceId resource = ResourceId.parse(entry.substring(0, separator));
                final BigDecimal multiplier = boundedMultiplier(entry.substring(separator + 1));
                if (parsed.put(resource, multiplier) != null) {
                    throw new IllegalArgumentException("Duplicate resource override");
                }
            }
            return new ResourceOverrides(parsed);
        }
        /** @return deterministic immutable multipliers */ public Map<ResourceId, BigDecimal> values() { return values; }
        @Override public String toString() {
            final StringBuilder output = new StringBuilder();
            for (Map.Entry<ResourceId, BigDecimal> entry : values.entrySet()) {
                if (output.length() > 0) { output.append(','); }
                output.append(entry.getKey()).append('=').append(entry.getValue().toPlainString());
            }
            return output.toString();
        }
        @Override public int hashCode() { return values.hashCode(); }
        @Override public boolean equals(final Object other) {
            return this == other || other instanceof ResourceOverrides
                    && values.equals(((ResourceOverrides) other).values);
        }
    }

    /** Complete initial M03 schema catalogue. */
    public static final class InitialCatalog {
        private static final ConfigurationVersion VERSION = ConfigurationVersion.of(1);
        private InitialCatalog() { throw new AssertionError("No instances"); }

        /** @return all 36 logical schemas in canonical enum order */
        public static List<Schema> schemas() {
            final List<Schema> schemas = new ArrayList<Schema>();
            for (LogicalFile file : LogicalFile.values()) {
                schemas.add(Schema.of(file, VERSION, definitions(file)));
            }
            return Collections.unmodifiableList(schemas);
        }
        /** @return schema for one logical file */
        public static Schema schema(final LogicalFile file) {
            return Schema.of(file, VERSION, definitions(Objects.requireNonNull(file, "file")));
        }

        private static List<OptionDefinition<?>> definitions(final LogicalFile file) {
            final List<OptionDefinition<?>> options = new ArrayList<OptionDefinition<?>>();
            options.add(integer(file, "meta.schema-version", 1, "Version of this logical document.",
                    1, 1, true));
            switch (file) {
                case CONFIG:
                    config(options, file);
                    break;
                case DEPLOYMENT:
                    deployment(options, file);
                    break;
                case DATABASE:
                    connection(options, file, "database");
                    break;
                case REDIS:
                    connection(options, file, "redis");
                    break;
                case PROXY:
                    connection(options, file, "proxy");
                    break;
                case CLOUDNET:
                    connection(options, file, "cloudnet");
                    break;
                case MODES:
                    modes(options, file);
                    break;
                case GENERATORS:
                    generators(options, file);
                    break;
                case SHOPS:
                    profile(options, file, "shop", "zartra:balance/shop-standard-v1");
                    break;
                case QUESTS:
                    profile(options, file, "quest", "zartra:quests/starter-v1");
                    break;
                case ACHIEVEMENTS:
                    profile(options, file, "achievement", "zartra:achievements/starter-v1");
                    break;
                case BATTLEPASS:
                    profile(options, file, "battle-pass", "zartra:battlepass/starter-v1");
                    break;
                case COSMETICS:
                    cosmetics(options, file);
                    break;
                case CONTENT:
                    content(options, file);
                    break;
                case STATISTICS:
                    statistics(options, file);
                    break;
                case REPLAY:
                    retention(options, file, "replay", "P30D");
                    break;
                case ATLAS:
                    retention(options, file, "atlas", "P180D");
                    break;
                case MESSAGES:
                    messages(options, file);
                    break;
                case PERMISSIONS:
                    permissions(options, file);
                    break;
                case COMPATIBILITY:
                    compatibility(options, file);
                    break;
                case PERFORMANCE:
                    performance(options, file);
                    break;
                case SECURITY:
                    security(options, file);
                    break;
                case DISCORD:
                    discord(options, file);
                    break;
                default:
                    genericFoundation(options, file);
                    break;
            }
            return options;
        }

        private static void config(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(locale(file, "localization.server-locale", "en-US",
                    "Default server language selected when no player preference exists."));
            options.add(locale(file, "localization.fallback-locale", "en-US",
                    "Complete fallback language used when a selected catalog lacks a key."));
            options.add(bool(file, "validation.reject-unknown", true,
                    "Reject unknown options instead of ignoring them.", false));
        }
        private static void deployment(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(enumeration(file, "deployment.mode", "shared-server",
                    Arrays.asList("shared-server", "scalable-proxy"),
                    "Select the approved deployment topology.", true));
            options.add(enumeration(file, "network.authority", "local",
                    Arrays.asList("local", "network-service"),
                    "Select the single writer for network-visible state.", true));
            options.add(identifier(file, "network.instance-id", "zartra:instance/local",
                    "Stable backend identity used in authenticated envelopes.", true));
            options.add(secret(file, "network.authority-secret-ref",
                    "Reference for authenticated network authority envelopes.", true));
        }
        private static void connection(final List<OptionDefinition<?>> options, final LogicalFile file,
                                       final String provider) {
            final ConfigurationKey credential = ConfigurationKey.of(provider + ".credential-ref");
            options.add(bool(file, provider + ".enabled", false,
                    "Enable the optional " + provider + " adapter after later-milestone validation.", true,
                    Collections.singletonList(credential)));
            options.add(secret(file, provider + ".credential-ref",
                    "Reference to protected " + provider + " credentials or private endpoint.", true));
        }
        private static void modes(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(identifier(file, "balance.profile-id", "zartra:balance/modes-standard-v1",
                    "Original game-mode balance profile identity.", false));
            final String[] presets = {"scarce", "reduced", "normal", "abundant", "extreme"};
            final String[] defaults = {"0.50", "0.75", "1.00", "1.50", "2.50"};
            for (int index = 0; index < presets.length; index++) {
                final String prefix = "private-games.resource-scarcity.presets." + presets[index] + '.';
                for (String resource : Arrays.asList("iron", "gold", "diamond", "emerald", "custom-default")) {
                    options.add(decimal(file, prefix + resource, defaults[index],
                            "Independent " + resource + " generation multiplier for the "
                                    + presets[index] + " Resource Scarcity preset.", "0.10", "5.00"));
                }
                options.add(overrides(file, prefix + "custom-overrides",
                        "Per-resource overrides for custom generators in the " + presets[index] + " preset."));
            }
            options.add(enumeration(file, "private-games.resource-scarcity.change-policy", "countdown-locked",
                    Arrays.asList("countdown-locked", "dynamic-rate-safe"),
                    "Lock preset changes at countdown by default; dynamic changes require safe generator capability.",
                    false));
        }
        private static void generators(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(bool(file, "custom-resources.enabled", true,
                    "Allow namespaced custom generator resources in typed profiles.", false));
            options.add(decimal(file, "generation.minimum-multiplier", "0.10",
                    "Minimum accepted private-game resource multiplier.", "0.10", "5.00"));
            options.add(decimal(file, "generation.maximum-multiplier", "5.00",
                    "Maximum accepted private-game resource multiplier.", "0.10", "5.00"));
        }
        private static void profile(final List<OptionDefinition<?>> options, final LogicalFile file,
                                    final String family, final String defaultId) {
            options.add(identifier(file, "balance.profile-id", defaultId,
                    "Original configurable " + family + " starter profile identity.", false));
        }
        private static void cosmetics(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(integer(file, "catalogue.minimum-original-definitions", 300,
                    "Release gate for original or properly licensed production cosmetics.", 300, 100000, false));
            options.add(bool(file, "catalogue.custom-definitions", true,
                    "Allow validated custom cosmetic definitions and rarities.", false));
            options.add(enumeration(file, "rendering.packet-support", "capability-adapter",
                    Arrays.asList("capability-adapter", "platform-safe"),
                    "Route packet-backed cosmetics through later compatibility capabilities.", false));
            options.add(identifier(file, "balance.profile-id", "zartra:cosmetics/starter-v1",
                    "Original cosmetic catalogue and rarity balance profile.", false));
        }
        private static void content(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(identifier(file, "active-pack-id", "zartra:content/starter-v1",
                    "Active versioned content-pack identity.", true));
            options.add(bool(file, "provenance.require-approved-assets", true,
                    "Reject production assets without approved provenance manifest rows.", true));
            options.add(bool(file, "provenance.allow-proprietary-copy", false,
                    "Must remain false; proprietary code, assets, names and exact balance content are prohibited.",
                    true));
        }
        private static void statistics(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(enumeration(file, "privacy.default-visibility", "private",
                    Arrays.asList("private", "friends", "party", "public"),
                    "Privacy-first default for new player statistics and profiles.", false));
            options.add(bool(file, "privacy.require-consent-for-external", true,
                    "Require verified consent before external linked-account statistics access.", false));
        }
        private static void retention(final List<OptionDefinition<?>> options, final LogicalFile file,
                                      final String family, final String defaultValue) {
            options.add(duration(file, "retention.standard", defaultValue,
                    "Default retention for " + family + " records that are not held evidence."));
            options.add(bool(file, "retention.legal-hold-overrides-deletion", true,
                    "Preserve protected, reported, Atlas or legally held evidence from automatic deletion.", true));
        }
        private static void messages(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(locale(file, "catalog.default-locale", "en-US", "Default complete message catalog."));
            options.add(locale(file, "catalog.fallback-locale", "en-US", "Final complete fallback catalog."));
            options.add(bool(file, "catalog.require-complete", true,
                    "Reject live locale switches to incomplete catalogs.", false));
            options.add(enumeration(file, "formatting.model", "neutral-components",
                    Arrays.asList("neutral-components", "legacy-text"),
                    "Keep message templates platform-neutral until compatibility rendering.", false));
        }
        private static void permissions(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(bool(file, "authorization.default-deny", true,
                    "Deny every action that lacks an exact canonical grant.", true));
            options.add(bool(file, "authorization.parent-node-inheritance", false,
                    "Prevent implicit parent or wildcard permission escalation.", true));
            options.add(bool(file, "authorization.legacy-aliases", true,
                    "Resolve documented migration aliases to exact canonical nodes.", false));
            options.add(bool(file, "authorization.audit-decisions", true,
                    "Emit secret-free authorization decision metadata to the later audit sink.", false));
        }
        private static void compatibility(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(enumeration(file, "unsupported-data.policy", "reject-last-known-good",
                    Arrays.asList("reject-last-known-good"),
                    "Reject unsupported declarations and retain the last known good snapshot.", false));
            options.add(identifier(file, "legacy.material.default", "zartra:legacy/material-safe",
                    "Configurable Minecraft 1.8 material fallback semantic ID.", false));
            options.add(identifier(file, "legacy.particle.default", "zartra:legacy/particle-safe",
                    "Configurable Minecraft 1.8 particle fallback semantic ID.", false));
            options.add(identifier(file, "legacy.sound.default", "zartra:legacy/sound-safe",
                    "Configurable Minecraft 1.8 sound fallback semantic ID.", false));
            options.add(identifier(file, "legacy.text.default", "zartra:legacy/text-safe",
                    "Configurable Minecraft 1.8 text/action fallback semantic ID.", false));
            options.add(bool(file, "decorative-suppression.require-diagnostic", true,
                    "Record every unavoidable purely decorative suppression without disabling gameplay.", false));
        }
        private static void performance(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(integer(file, "configuration.maximum-options-per-file", 10000,
                    "Bound validation and generated-reference work per logical file.", 1, 100000, true));
            options.add(duration(file, "configuration.manual-validation-budget", "PT5S",
                    "Operator-visible synchronous M03 validation budget."));
        }
        private static void security(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(bool(file, "scripts.enabled", false,
                    "Keep the declarative scripting DSL disabled until later capability-isolated runtime work.", true));
            options.add(enumeration(file, "scripts.authorization", "explicit-capabilities",
                    Arrays.asList("explicit-capabilities"),
                    "Require declared least-privilege capabilities for every script action.", true));
            options.add(integer(file, "scripts.maximum-instructions", 10000,
                    "Bound future declarative script work per invocation.", 1, 1000000, true));
            options.add(bool(file, "diagnostics.allowlist-only", true,
                    "Export only explicitly allowlisted diagnostic fields after redaction.", true));
            options.add(bool(file, "diagnostics.include-secrets", false,
                    "Must remain false; secret material and private endpoints never enter exports.", true));
            options.add(enumeration(file, "secrets.resolution-order", "provider-environment-protected-file",
                    Arrays.asList("provider-environment-protected-file"),
                    "Use the approved secret-provider, environment and explicitly protected-file order.", true));
            options.add(enumeration(file, "privacy.default-visibility", "private",
                    Arrays.asList("private", "friends", "party", "public"),
                    "Privacy-first default for new user-visible records.", false));
            options.add(duration(file, "privacy.account-link-challenge-retention", "PT10M",
                    "Maximum retention for unconsumed external account-link challenges."));
            options.add(duration(file, "privacy.audit-retention", "P365D",
                    "Default retention for security audit outcomes unless legal policy differs."));
        }
        private static void discord(final List<OptionDefinition<?>> options, final LogicalFile file) {
            final ConfigurationKey provider = ConfigurationKey.of("provider.type");
            options.add(bool(file, "enabled", false,
                    "Keep Discord optional and disabled by default.", false,
                    Collections.singletonList(provider)));
            options.add(enumeration(file, "provider.type", "disabled",
                    Arrays.asList("disabled", "embedded-webhook", "external-bot", "custom"),
                    "Select a provider; M03 implements only the safe disabled provider.", false));
            options.add(secret(file, "webhook.url-ref",
                    "Reference to an embedded provider webhook URL; never the URL itself.", false));
            options.add(secret(file, "external.service-key-ref",
                    "Reference to external-bot service credentials; never a Discord bot token.", false));
            options.add(bool(file, "privacy.require-consent", true,
                    "Require consent for linked-account data and external identities.", false));
        }
        private static void genericFoundation(final List<OptionDefinition<?>> options, final LogicalFile file) {
            options.add(bool(file, "foundation.strict-validation", true,
                    "Reserve this logical surface under strict versioned validation until its feature milestone.",
                    false));
        }

        private static OptionDefinition<Boolean> bool(final LogicalFile file, final String key,
                                                       final boolean value, final String purpose,
                                                       final boolean restart) {
            return bool(file, key, value, purpose, restart, Collections.<ConfigurationKey>emptyList());
        }
        private static OptionDefinition<Boolean> bool(final LogicalFile file, final String key,
                                                       final boolean value, final String purpose,
                                                       final boolean restart,
                                                       final List<ConfigurationKey> dependencies) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.BOOLEAN,
                    Boolean.valueOf(value), always(), metadata(file, purpose, Boolean.toString(value),
                            "true or false", Boolean.toString(value), restart, dependencies));
        }
        private static OptionDefinition<Integer> integer(final LogicalFile file, final String key,
                                                          final int value, final String purpose,
                                                          final int minimum, final int maximum,
                                                          final boolean restart) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.INTEGER,
                    Integer.valueOf(value), range(minimum, maximum), metadata(file, purpose,
                            Integer.toString(value), minimum + " through " + maximum,
                            Integer.toString(value), restart, Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionDefinition<BigDecimal> decimal(final LogicalFile file, final String key,
                                                             final String value, final String purpose,
                                                             final String minimum, final String maximum) {
            final BigDecimal parsed = new BigDecimal(value);
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.DECIMAL, parsed,
                    decimalRange(minimum, maximum), metadata(file, purpose, value,
                            minimum + " through " + maximum, value, false,
                            Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionDefinition<Duration> duration(final LogicalFile file, final String key,
                                                            final String value, final String purpose) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.DURATION,
                    Duration.parse(value), positiveDuration(), metadata(file, purpose, value,
                            "positive ISO-8601 duration", value, false,
                            Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionDefinition<LocaleId> locale(final LogicalFile file, final String key,
                                                          final String value, final String purpose) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.LOCALE,
                    LocaleId.parse(value), always(), metadata(file, purpose, value,
                            "language or language-region", value, false,
                            Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionDefinition<DefinitionId> identifier(final LogicalFile file,
                                                                  final String key,
                                                                  final String value,
                                                                  final String purpose,
                                                                  final boolean restart) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.DEFINITION_ID,
                    DefinitionId.parse(value), always(), metadata(file, purpose, value,
                            "namespaced stable ID", value, restart,
                            Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionDefinition<String> enumeration(final LogicalFile file, final String key,
                                                             final String value,
                                                             final List<String> accepted,
                                                             final String purpose,
                                                             final boolean restart) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.oneOf(accepted),
                    value, always(), metadata(file, purpose, value, String.join(", ", accepted),
                            value, restart, Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionDefinition<SecretRef> secret(final LogicalFile file, final String key,
                                                           final String purpose,
                                                           final boolean restart) {
            final OptionMetadata metadata = OptionMetadata.builder().purpose(purpose)
                    .defaultDescription("unset; provider remains unavailable")
                    .acceptedValues("provider:, environment: or explicitly approved protected_file: reference")
                    .example("environment:ZBW_SECRET_REFERENCE")
                    .reloadTarget(file.reloadTarget()).restartRequired(restart)
                    .performanceImpact("Resolution is bounded and occurs outside gameplay paths.")
                    .securityImpact("Contains only a locator; resolved material is redacted and zeroized.")
                    .compatibility("Version-neutral secret-reference contract.")
                    .migration("Reference locator is preserved; secret material is never migrated in normal config.")
                    .build();
            return OptionDefinition.optional(ConfigurationKey.of(key), ValueTypes.SECRET_REF,
                    always(), metadata);
        }
        private static OptionDefinition<ResourceOverrides> overrides(final LogicalFile file,
                                                                      final String key,
                                                                      final String purpose) {
            return OptionDefinition.withDefault(ConfigurationKey.of(key), ValueTypes.RESOURCE_OVERRIDES,
                    ResourceOverrides.parse(""), always(), metadata(file, purpose, "empty",
                            "comma-separated resource:id=0.10..5.00", "zartra:copper=0.50", false,
                            Collections.<ConfigurationKey>emptyList()));
        }
        private static OptionMetadata metadata(final LogicalFile file, final String purpose,
                                                final String defaultValue, final String accepted,
                                                final String example, final boolean restart,
                                                final Collection<ConfigurationKey> dependencies) {
            return OptionMetadata.builder().purpose(purpose).defaultDescription(defaultValue)
                    .acceptedValues(accepted).example(example).dependencies(dependencies)
                    .reloadTarget(file.reloadTarget()).restartRequired(restart)
                    .build();
        }
        private static <T> Constraint<T> always() {
            return new Constraint<T>() {
                @Override public Optional<DefinitionId> validate(final T value) {
                    return Optional.empty();
                }
            };
        }
        private static Constraint<Integer> range(final int minimum, final int maximum) {
            return new Constraint<Integer>() {
                @Override public Optional<DefinitionId> validate(final Integer value) {
                    return value.intValue() < minimum || value.intValue() > maximum
                            ? Optional.of(DefinitionId.of("zartra", "config/range"))
                            : Optional.<DefinitionId>empty();
                }
            };
        }
        private static Constraint<BigDecimal> decimalRange(final String minimum, final String maximum) {
            final BigDecimal lower = new BigDecimal(minimum);
            final BigDecimal upper = new BigDecimal(maximum);
            return new Constraint<BigDecimal>() {
                @Override public Optional<DefinitionId> validate(final BigDecimal value) {
                    return value.compareTo(lower) < 0 || value.compareTo(upper) > 0
                            ? Optional.of(DefinitionId.of("zartra", "config/range"))
                            : Optional.<DefinitionId>empty();
                }
            };
        }
        private static Constraint<Duration> positiveDuration() {
            return new Constraint<Duration>() {
                @Override public Optional<DefinitionId> validate(final Duration value) {
                    return value.isZero() || value.isNegative()
                            ? Optional.of(DefinitionId.of("zartra", "config/range"))
                            : Optional.<DefinitionId>empty();
                }
            };
        }
    }

    private static BigDecimal boundedMultiplier(final String value) {
        final BigDecimal parsed;
        try { parsed = new BigDecimal(value).stripTrailingZeros(); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Multiplier is malformed", exception);
        }
        if (parsed.compareTo(new BigDecimal("0.10")) < 0
                || parsed.compareTo(new BigDecimal("5.00")) > 0) {
            throw new IllegalArgumentException("Multiplier is outside 0.10 through 5.00");
        }
        return parsed;
    }
}
