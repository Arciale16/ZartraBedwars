package io.zartra.bedwars.sdk.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.extension.ExtensionValidation;
import io.zartra.bedwars.api.extension.MinecraftVersion;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExtensionMetadataValidationTest {
    private static final ExtensionValidation.Target TARGET = ExtensionValidation.Target.of(
            SemanticVersion.parse("1.0.0"), SemanticVersion.parse("0.1.0"),
            MinecraftVersion.parse("1.21.11"));
    private final ExtensionMetadataReader reader = new ExtensionMetadataReader();
    private final DefaultExtensionMetadataValidator validator = new DefaultExtensionMetadataValidator();

    @Test
    void validFixturesParseAndPassCompatibilityValidation() {
        final ExtensionMetadata base = read("extensions/valid/example.properties").metadata().get();
        final ExtensionMetadata optional = read("extensions/valid/optional-dependency.properties").metadata().get();
        assertTrue(validator.validate(base, TARGET).isValid());
        assertTrue(validator.validateCatalog(Arrays.asList(optional, base), TARGET).isValid());
        assertEquals("example:generator-pack", base.id().toString());
        assertEquals(1, optional.dependencies().size());
        assertTrue(optional.dependencies().get(0).optional());
    }

    @Test
    void malformedFixtureFailsBeforeMetadataConstruction() {
        final ExtensionMetadataReader.ReadResult malformed = read("extensions/invalid/malformed.properties");
        assertFalse(malformed.isValid());
        assertFalse(malformed.metadata().isPresent());
        final List<String> codes = malformed.report().issues().stream()
                .map(issue -> issue.code().toString()).collect(Collectors.toList());
        assertTrue(codes.contains("zartra:extension_reader/missing_key"));
        assertTrue(codes.contains("zartra:extension_reader/unknown_key"));
    }

    @Test
    void unsupportedApiVersionIsReportedDeterministically() {
        final ExtensionMetadata future = read("extensions/invalid/unsupported-api.properties").metadata().get();
        final ExtensionValidation.Report first = validator.validate(future, TARGET);
        final ExtensionValidation.Report second = validator.validate(future, TARGET);
        assertFalse(first.isValid());
        assertEquals(issueKeys(first), issueKeys(second));
        assertTrue(issueKeys(first).contains("api-versions=zartra:extension_metadata/api_unsupported"));
    }

    @Test
    void catalogRejectsDuplicateIdsAndMissingRequiredDependencies() {
        final ExtensionMetadata base = read("extensions/valid/example.properties").metadata().get();
        final ExtensionMetadata missing = read("extensions/invalid/required-missing.properties").metadata().get();
        final ExtensionValidation.Report duplicates = validator.validateCatalog(Arrays.asList(base, base, missing), TARGET);
        assertFalse(duplicates.isValid());
        assertTrue(issueKeys(duplicates).stream().anyMatch(value -> value.contains("duplicate_extension")));
        assertTrue(issueKeys(duplicates).stream().anyMatch(value -> value.contains("dependency_missing")));
    }

    @Test
    void duplicateDependencyIdIsRejected() {
        final ExtensionMetadata duplicate = read("extensions/invalid/duplicate-dependency.properties").metadata().get();
        final ExtensionValidation.Report report = validator.validate(duplicate, TARGET);
        assertFalse(report.isValid());
        assertTrue(issueKeys(report).stream().anyMatch(value -> value.contains("duplicate_dependency")));
    }

    private static List<String> issueKeys(final ExtensionValidation.Report report) {
        return report.issues().stream().map(issue -> issue.field() + '=' + issue.code())
                .collect(Collectors.toList());
    }

    private ExtensionMetadataReader.ReadResult read(final String resource) {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
        if (stream == null) { throw new AssertionError("Missing fixture: " + resource); }
        return reader.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
