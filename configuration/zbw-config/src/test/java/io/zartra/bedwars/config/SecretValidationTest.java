package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.secret.SecretRef;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.Issue;
import io.zartra.bedwars.config.schema.ConfigurationModel.LogicalFile;
import io.zartra.bedwars.config.schema.ConfigurationModel.Severity;
import io.zartra.bedwars.config.schema.ConfigurationModel.Validator;
import io.zartra.bedwars.config.secret.SecretServices.Classification;
import io.zartra.bedwars.config.secret.SecretServices.DiagnosticExporter;
import io.zartra.bedwars.config.secret.SecretServices.DiagnosticField;
import io.zartra.bedwars.config.secret.SecretServices.ExportResult;
import io.zartra.bedwars.config.secret.SecretServices.Redactor;
import io.zartra.bedwars.config.secret.SecretServices.Resolution;
import io.zartra.bedwars.config.secret.SecretServices.Resolver;
import io.zartra.bedwars.config.secret.SecretServices.SecretSource;
import io.zartra.bedwars.config.validation.ConfigurationValidationService;
import io.zartra.bedwars.config.validation.ConfigurationValidationService.AggregateReport;
import io.zartra.bedwars.config.validation.ConfigurationValidationService.ExternalCheck;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SecretValidationTest {
    @Test void resolverUsesExplicitSourcesAndApprovedPriorityThenZeroizesSourceMaterial() {
        final char[] providerMaterial = "provider-value".toCharArray();
        final char[] environmentMaterial = "environment-value".toCharArray();
        final AtomicInteger providerCalls = new AtomicInteger();
        final SecretSource provider = source(SecretRef.Source.PROVIDER, providerMaterial, providerCalls);
        final SecretSource environment = source(SecretRef.Source.ENVIRONMENT, environmentMaterial,
                new AtomicInteger());
        final Resolver resolver = new Resolver(Arrays.asList(environment, provider));
        final Resolution preferred = resolver.resolvePreferred("DISCORD_KEY");
        assertTrue(preferred.isResolved());
        assertEquals(1, providerCalls.get());
        assertTrue(allZero(providerMaterial));
        assertEquals(Integer.valueOf("provider-value".length()), preferred.lease().get().use(
                secret -> Integer.valueOf(secret.length)));
        assertEquals("SecretLease[REDACTED]", preferred.lease().get().toString());
        preferred.lease().get().close();
        assertThrows(IllegalStateException.class,
                () -> preferred.lease().get().use(secret -> Integer.valueOf(secret.length)));

        final Resolution explicit = resolver.resolve(SecretRef.parse("environment:DISCORD_KEY"));
        assertTrue(explicit.isResolved());
        assertTrue(allZero(environmentMaterial));
        explicit.lease().get().close();
    }

    @Test void resolverReturnsTypedFailuresWithoutSecretOrExceptionText() {
        final SecretSource absent = new SecretSource() {
            @Override public SecretRef.Source source() { return SecretRef.Source.PROVIDER; }
            @Override public Optional<char[]> resolve(final String key) { return Optional.empty(); }
        };
        Resolver resolver = new Resolver(Collections.singletonList(absent));
        assertEquals("zartra:secret/not_found", resolver.resolvePreferred("MISSING").failure().get().toString());
        assertEquals("zartra:secret/source_unavailable",
                resolver.resolve(SecretRef.parse("environment:MISSING")).failure().get().toString());

        final char[] empty = new char[0];
        final SecretSource emptySource = source(SecretRef.Source.ENVIRONMENT, empty, new AtomicInteger());
        resolver = new Resolver(Collections.singletonList(emptySource));
        assertEquals("zartra:secret/empty",
                resolver.resolve(SecretRef.parse("environment:EMPTY")).failure().get().toString());

        final SecretSource broken = new SecretSource() {
            @Override public SecretRef.Source source() { return SecretRef.Source.PROVIDER; }
            @Override public Optional<char[]> resolve(final String key) {
                throw new IllegalStateException("super-secret-text");
            }
        };
        resolver = new Resolver(Collections.singletonList(broken));
        assertEquals("zartra:secret/resolution_failed",
                resolver.resolve(SecretRef.parse("provider:KEY")).failure().get().toString());
        assertThrows(IllegalArgumentException.class, () -> new Resolver(Arrays.asList(absent, broken)));
        final Resolver finalResolver = resolver;
        assertThrows(IllegalArgumentException.class, () -> finalResolver.resolvePreferred("!"));
    }

    @Test void redactorAndDiagnosticExporterAreSeededAllowlistOnlyAndDeterministic() {
        final char[] secret = "seeded-secret".toCharArray();
        final Redactor redactor = new Redactor(Collections.singletonList(secret));
        assertEquals("before [REDACTED] after [REDACTED]",
                redactor.redact("before seeded-secret after seeded-secret"));
        final DefinitionId health = DefinitionId.of("zartra", "health/config");
        final DefinitionId mode = DefinitionId.of("zartra", "health/mode");
        final DiagnosticExporter exporter = new DiagnosticExporter(Arrays.asList(health, mode), redactor);
        final ExportResult result = exporter.export(Arrays.asList(
                DiagnosticField.of(mode, Classification.PUBLIC, "shared"),
                DiagnosticField.of(health, Classification.OPERATOR, "healthy seeded-secret")));
        assertTrue(result.isSuccess());
        assertEquals("zartra:health/config=healthy [REDACTED]\nzartra:health/mode=shared\n",
                result.content().get());
        assertFalse(result.content().get().contains("seeded-secret"));
        assertFalse(exporter.export(Collections.singletonList(DiagnosticField.of(
                DefinitionId.of("zartra", "health/unlisted"), Classification.PUBLIC, "ok"))).isSuccess());
        assertFalse(exporter.export(Collections.singletonList(DiagnosticField.of(
                health, Classification.SENSITIVE, "value"))).isSuccess());
        redactor.close();
        assertThrows(IllegalStateException.class, () -> redactor.redact("value"));
    }

    @Test void diagnosticExporterRejectsSensitiveFieldNamesEvenWhenAllowlisted() {
        final DefinitionId secretField = DefinitionId.of("zartra", "health/secret-token");
        final Redactor redactor = new Redactor(Collections.<char[]>emptyList());
        final DiagnosticExporter exporter = new DiagnosticExporter(
                Collections.singletonList(secretField), redactor);
        assertEquals("zartra:diagnostic/field_denied", exporter.export(Collections.singletonList(
                DiagnosticField.of(secretField, Classification.PUBLIC, "masked")))
                .failure().get().toString());
        redactor.close();
    }

    @Test void startupAndManualValidationCoverAllFilesExternalFactsAndCrossRules() {
        final ConfigurationValidationService validService = new ConfigurationValidationService(
                new Validator(), Collections.emptyList());
        final AggregateReport valid = validService.validateStartup(file -> Optional.of(empty()),
                Collections.singletonList(ExternalCheck.of(
                        DefinitionId.of("zartra", "environment/java"), true, "No action required.")));
        assertTrue(valid.isValid());
        assertEquals(36, valid.reports().size());

        final ConfigurationValidationService crossFailure = new ConfigurationValidationService(
                new Validator(), Collections.singletonList(configurations -> Collections.singletonList(
                        Issue.of(Severity.ERROR, DefinitionId.of("zartra", "validation/duplicate_map_id"),
                                null, "Rename the duplicate stable map ID."))));
        assertFalse(crossFailure.validateStartup(file -> Optional.of(empty()),
                Collections.<ExternalCheck>emptyList()).isValid());

        final AggregateReport externalFailure = validService.validateStartup(file -> Optional.of(empty()),
                Collections.singletonList(ExternalCheck.of(
                        DefinitionId.of("zartra", "environment/filesystem_permissions"), false,
                        "Restrict configuration file permissions.")));
        assertFalse(externalFailure.isValid());
        assertEquals("zartra:environment/filesystem_permissions",
                externalFailure.globalIssues().get(0).code().toString());

        final AggregateReport missing = validService.validateStartup(file ->
                file == LogicalFile.CONFIG ? Optional.<Document>empty() : Optional.of(empty()),
                Collections.<ExternalCheck>emptyList());
        assertFalse(missing.isValid());
        assertEquals("zartra:validation/file_missing",
                missing.reports().get(LogicalFile.CONFIG).issues().get(0).code().toString());

        assertFalse(validService.validateManual(LogicalFile.CONFIG,
                Document.of(ConfigurationVersion.of(1), Collections.singletonMap(
                        ConfigurationKey.of("unknown.key"), "value"))).isValid());
    }

    @Test void validationBoundsInputAndConvertsSourceFailureToSafeIssue() {
        final ConfigurationValidationService service = new ConfigurationValidationService(
                new Validator(), Collections.emptyList());
        final Map<ConfigurationKey, String> tooMany = new HashMap<ConfigurationKey, String>();
        for (int index = 0; index <= 10000; index++) {
            tooMany.put(ConfigurationKey.of("x.k" + index), "value");
        }
        final AggregateReport bounded = service.validateManual(LogicalFile.CONFIG,
                Document.of(ConfigurationVersion.of(1), tooMany));
        assertEquals("zartra:validation/option_limit",
                bounded.reports().get(LogicalFile.CONFIG).issues().get(0).code().toString());
        final AggregateReport sourceFailure = service.validateStartup(file -> {
            if (file == LogicalFile.CONFIG) { throw new IllegalStateException("secret-path"); }
            return Optional.of(empty());
        }, Collections.<ExternalCheck>emptyList());
        assertEquals("zartra:validation/source_failed",
                sourceFailure.reports().get(LogicalFile.CONFIG).issues().get(0).code().toString());
    }

    private static SecretSource source(final SecretRef.Source source, final char[] material,
                                       final AtomicInteger calls) {
        return new SecretSource() {
            @Override public SecretRef.Source source() { return source; }
            @Override public Optional<char[]> resolve(final String key) {
                calls.incrementAndGet();
                return Optional.of(material);
            }
        };
    }
    private static boolean allZero(final char[] value) {
        for (char character : value) {
            if (character != '\0') { return false; }
        }
        return true;
    }
    private static Document empty() {
        return Document.of(ConfigurationVersion.of(1), Collections.<ConfigurationKey, String>emptyMap());
    }
}
