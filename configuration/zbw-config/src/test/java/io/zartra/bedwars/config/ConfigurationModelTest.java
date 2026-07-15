package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.api.configuration.ReloadTarget;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.config.schema.ConfigurationModel.Constraint;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.InitialCatalog;
import io.zartra.bedwars.config.schema.ConfigurationModel.LogicalFile;
import io.zartra.bedwars.config.schema.ConfigurationModel.OptionDefinition;
import io.zartra.bedwars.config.schema.ConfigurationModel.OptionMetadata;
import io.zartra.bedwars.config.schema.ConfigurationModel.ReferenceGenerator;
import io.zartra.bedwars.config.schema.ConfigurationModel.ResourceOverrides;
import io.zartra.bedwars.config.schema.ConfigurationModel.Schema;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValidationReport;
import io.zartra.bedwars.config.schema.ConfigurationModel.Validator;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValueTypes;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfigurationModelTest {
    private final Validator validator = new Validator();

    @Test void catalogueCoversEveryLogicalFileAndEveryOptionHasCompleteMetadata() {
        final List<Schema> schemas = InitialCatalog.schemas();
        assertEquals(36, schemas.size());
        assertEquals(LogicalFile.values().length, schemas.size());
        for (Schema schema : schemas) {
            assertEquals(ConfigurationVersion.of(1), schema.version());
            assertFalse(schema.definitions().isEmpty());
            assertTrue(schema.definition(ConfigurationKey.of("meta.schema-version")).isPresent());
            for (OptionDefinition<?> definition : schema.definitions()) {
                final OptionMetadata metadata = definition.metadata();
                assertFalse(metadata.purpose().isEmpty());
                assertFalse(metadata.defaultDescription().isEmpty());
                assertFalse(metadata.acceptedValues().isEmpty());
                assertFalse(metadata.example().isEmpty());
                assertNotNull(metadata.dependencies());
                assertNotNull(metadata.incompatibilities());
                assertFalse(metadata.performanceImpact().isEmpty());
                assertFalse(metadata.securityImpact().isEmpty());
                assertNotNull(metadata.reloadTarget());
                assertFalse(metadata.compatibility().isEmpty());
                assertFalse(metadata.deprecation().isEmpty());
                assertFalse(metadata.migration().isEmpty());
            }
            assertTrue(validator.validate(schema, emptyDocument()).isValid(), schema.file().path());
        }
        assertEquals("integrations/discord.yml", LogicalFile.DISCORD.path());
        assertEquals(ReloadTarget.MESSAGES, LogicalFile.MESSAGES.reloadTarget());
    }

    @Test void resourceScarcityPresetsAreIndependentBoundedAndCustomResourceAware() {
        final Schema modes = InitialCatalog.schema(LogicalFile.MODES);
        assertEquals(33, modes.definitions().size());
        final String[] presets = {"scarce", "reduced", "normal", "abundant", "extreme"};
        final String[] defaults = {"0.50", "0.75", "1.00", "1.50", "2.50"};
        for (int index = 0; index < presets.length; index++) {
            for (String resource : Arrays.asList("iron", "gold", "diamond", "emerald", "custom-default")) {
                final OptionDefinition<?> option = modes.definition(ConfigurationKey.of(
                        "private-games.resource-scarcity.presets." + presets[index] + '.' + resource)).get();
                assertEquals(0, new BigDecimal(defaults[index]).compareTo(
                        (BigDecimal) option.defaultValue().get()));
            }
            assertTrue(modes.definition(ConfigurationKey.of(
                    "private-games.resource-scarcity.presets." + presets[index]
                            + ".custom-overrides")).isPresent());
        }
        final ResourceOverrides overrides = ResourceOverrides.parse(
                "extension:ruby=1.25,zartra:copper=0.50");
        assertEquals(new BigDecimal("0.5"), overrides.values().get(ResourceId.parse("zartra:copper")));
        assertEquals("extension:ruby=1.25,zartra:copper=0.5", overrides.toString());
        assertEquals(overrides, ResourceOverrides.parse(overrides.toString()));
        assertThrows(IllegalArgumentException.class,
                () -> ResourceOverrides.parse("zartra:copper=0.5,zartra:copper=1.0"));
        assertThrows(IllegalArgumentException.class,
                () -> ResourceOverrides.parse("zartra:copper=0.09"));
        assertThrows(IllegalArgumentException.class, () -> ResourceOverrides.parse("broken"));
        assertThrows(IllegalArgumentException.class, () -> ResourceOverrides.parse(null));
    }

    @Test void strictValidatorRejectsUnknownMalformedRangeVersionAndDependencyFailures() {
        final Schema database = InitialCatalog.schema(LogicalFile.DATABASE);
        assertIssue(database, values("unknown.key", "value"), "zartra:config/unknown_key");
        assertIssue(database, values("database.enabled", "TRUE"), "zartra:config/malformed");
        assertIssue(database, values("database.enabled", "true"), "zartra:config/missing_dependency");
        final ValidationReport version = validator.validate(database,
                Document.of(ConfigurationVersion.of(2), Collections.<ConfigurationKey, String>emptyMap()));
        assertEquals("zartra:config/version_mismatch", version.issues().get(0).code().toString());

        final Schema modes = InitialCatalog.schema(LogicalFile.MODES);
        assertIssue(modes, values("private-games.resource-scarcity.presets.scarce.iron", "8.0"),
                "zartra:config/range");
        assertIssue(modes, values("private-games.resource-scarcity.presets.scarce.iron", "secret-value"),
                "zartra:config/malformed");
        assertFalse(validator.validate(InitialCatalog.schema(LogicalFile.DISCORD),
                Document.of(ConfigurationVersion.of(1), values("webhook.url-ref", "https://secret")))
                .isValid());
    }

    @Test void customSchemasEnforceRequiredIncompatibleDuplicateAndDefaultRules() {
        final OptionDefinition<Boolean> required = OptionDefinition.required(ConfigurationKey.of("feature.required"),
                ValueTypes.BOOLEAN, always(), metadata("required", Collections.<ConfigurationKey>emptyList(),
                        Collections.<ConfigurationKey>emptyList()));
        final OptionDefinition<Boolean> left = OptionDefinition.withDefault(ConfigurationKey.of("feature.left"),
                ValueTypes.BOOLEAN, Boolean.FALSE, always(), metadata("left",
                        Collections.<ConfigurationKey>emptyList(),
                        Collections.singletonList(ConfigurationKey.of("feature.right"))));
        final OptionDefinition<Boolean> right = OptionDefinition.withDefault(ConfigurationKey.of("feature.right"),
                ValueTypes.BOOLEAN, Boolean.FALSE, always(), metadata("right",
                        Collections.<ConfigurationKey>emptyList(), Collections.<ConfigurationKey>emptyList()));
        final Schema schema = Schema.of(LogicalFile.CONFIG, ConfigurationVersion.of(1),
                Arrays.<OptionDefinition<?>>asList(required, left, right));
        assertIssue(schema, Collections.<ConfigurationKey, String>emptyMap(),
                "zartra:config/missing_required");
        final Map<ConfigurationKey, String> incompatible = new HashMap<ConfigurationKey, String>();
        incompatible.put(required.key(), "true");
        incompatible.put(left.key(), "true");
        incompatible.put(right.key(), "true");
        assertIssue(schema, incompatible, "zartra:config/incompatible_options");
        assertThrows(IllegalArgumentException.class, () -> Schema.of(LogicalFile.CONFIG,
                ConfigurationVersion.of(1), Arrays.<OptionDefinition<?>>asList(left, left)));
        assertThrows(IllegalArgumentException.class, () -> Schema.of(LogicalFile.CONFIG,
                ConfigurationVersion.of(1), Collections.<OptionDefinition<?>>emptyList()));
        final Constraint<Boolean> never = new Constraint<Boolean>() {
            @Override public Optional<DefinitionId> validate(final Boolean value) {
                return Optional.of(DefinitionId.of("zartra", "config/rejected"));
            }
        };
        assertThrows(IllegalArgumentException.class, () -> OptionDefinition.withDefault(
                ConfigurationKey.of("bad.default"), ValueTypes.BOOLEAN, Boolean.TRUE, never,
                metadata("bad", Collections.<ConfigurationKey>emptyList(),
                        Collections.<ConfigurationKey>emptyList())));
    }

    @Test void valueTypesAndReferenceGenerationAreDeterministicAndSecretSafe() {
        assertEquals(Boolean.TRUE, ValueTypes.BOOLEAN.parse("true"));
        assertEquals(Integer.valueOf(5), ValueTypes.INTEGER.parse("5"));
        assertEquals("1.25", ValueTypes.DECIMAL.render(ValueTypes.DECIMAL.parse("1.250")));
        assertEquals(Duration.ofMinutes(5), ValueTypes.DURATION.parse("PT5M"));
        assertEquals(DefinitionId.of("zartra", "test"), ValueTypes.DEFINITION_ID.parse("zartra:test"));
        assertThrows(IllegalArgumentException.class, () -> ValueTypes.BOOLEAN.parse("yes"));
        assertThrows(IllegalArgumentException.class, () -> ValueTypes.INTEGER.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> ValueTypes.DURATION.parse("soon"));
        assertThrows(IllegalArgumentException.class, () -> ValueTypes.oneOf(Collections.<String>emptyList()));

        final ReferenceGenerator generator = new ReferenceGenerator();
        final String discord = generator.commentedConfiguration(InitialCatalog.schema(LogicalFile.DISCORD));
        assertTrue(discord.contains("# Logical file: integrations/discord.yml"));
        assertTrue(discord.contains("enabled = false"));
        assertTrue(discord.contains("# webhook.url-ref = environment:ZBW_SECRET_REFERENCE"));
        assertFalse(discord.contains("bot-token"));
        final String first = generator.markdown(InitialCatalog.schemas());
        assertEquals(first, generator.markdown(InitialCatalog.schemas()));
        assertTrue(first.contains("## `compatibility.yml`"));
    }

    private void assertIssue(final Schema schema, final Map<ConfigurationKey, String> values,
                             final String code) {
        final ValidationReport report = validator.validate(schema,
                Document.of(ConfigurationVersion.of(1), values));
        assertFalse(report.isValid());
        assertTrue(report.issues().stream().anyMatch(issue -> code.equals(issue.code().toString())),
                report.issues().toString());
    }
    private static Document emptyDocument() {
        return Document.of(ConfigurationVersion.of(1), Collections.<ConfigurationKey, String>emptyMap());
    }
    private static Map<ConfigurationKey, String> values(final String key, final String value) {
        return Collections.singletonMap(ConfigurationKey.of(key), value);
    }
    private static Constraint<Boolean> always() {
        return new Constraint<Boolean>() {
            @Override public Optional<DefinitionId> validate(final Boolean value) { return Optional.empty(); }
        };
    }
    private static OptionMetadata metadata(final String purpose,
                                           final List<ConfigurationKey> dependencies,
                                           final List<ConfigurationKey> incompatibilities) {
        return OptionMetadata.builder().purpose(purpose).defaultDescription("false")
                .acceptedValues("true or false").example("true").dependencies(dependencies)
                .incompatibilities(incompatibilities).reloadTarget(ReloadTarget.CORE).build();
    }
}
