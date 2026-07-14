package io.zartra.bedwars.sdk.metadata;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.extension.ExtensionValidation;
import io.zartra.bedwars.api.extension.MinecraftVersion;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ExtensionId;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the deterministic {@code zartrabedwars-extension.properties} schema from a character
 * stream. The restricted format is one UTF-8 {@code key=value} per line, with {@code #} comments;
 * escapes, continuation lines and duplicate keys are rejected.
 */
public final class ExtensionMetadataReader {
    private static final Set<String> ALLOWED = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "schema-version", "id", "display-name", "version", "entrypoint", "api-versions",
            "product-versions", "minecraft-min", "minecraft-max", "dependencies", "required-apis",
            "provided-capabilities", "permissions", "configuration-keys")));
    private static final Set<String> REQUIRED = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "schema-version", "id", "display-name", "version", "entrypoint", "api-versions",
            "product-versions", "minecraft-min", "minecraft-max")));

    /**
     * Reads metadata without opening files or loading classes.
     *
     * @param source character source owned by the caller
     * @return metadata or a deterministic validation report
     */
    public ReadResult read(final Reader source) {
        Objects.requireNonNull(source, "source");
        final Map<String, String> values = new LinkedHashMap<String, String>();
        final List<ExtensionValidation.Issue> issues = new ArrayList<ExtensionValidation.Issue>();
        try {
            final BufferedReader lines = new BufferedReader(source);
            String line;
            int lineNumber = 0;
            while ((line = lines.readLine()) != null) {
                lineNumber++;
                final String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) { continue; }
                final int separator = line.indexOf('=');
                if (separator <= 0) {
                    issues.add(issue("syntax", "line[" + lineNumber + "]", "extension.metadata.reader.syntax"));
                    continue;
                }
                final String key = line.substring(0, separator).trim();
                final String value = line.substring(separator + 1).trim();
                if (!ALLOWED.contains(key)) {
                    issues.add(issue("unknown_key", "line[" + lineNumber + "]", "extension.metadata.reader.unknown_key"));
                } else if (values.put(key, value) != null) {
                    issues.add(issue("duplicate_key", "line[" + lineNumber + "]", "extension.metadata.reader.duplicate_key"));
                }
            }
        } catch (IOException exception) {
            issues.add(issue("io", "document", "extension.metadata.reader.io"));
        }
        for (String required : REQUIRED) {
            if (!values.containsKey(required) || values.get(required).isEmpty()) {
                issues.add(issue("missing_key", required, "extension.metadata.reader.missing_key"));
            }
        }
        if (!issues.isEmpty()) { return ReadResult.invalid(issues); }
        try {
            final ExtensionMetadata metadata = ExtensionMetadata.builder()
                    .schemaVersion(Integer.parseInt(values.get("schema-version")))
                    .id(ExtensionId.parse(values.get("id")))
                    .displayName(values.get("display-name"))
                    .version(SemanticVersion.parse(values.get("version")))
                    .entrypoint(values.get("entrypoint"))
                    .apiVersions(VersionRange.parse(values.get("api-versions")))
                    .productVersions(VersionRange.parse(values.get("product-versions")))
                    .minecraftVersions(ExtensionMetadata.MinecraftRange.inclusive(
                            MinecraftVersion.parse(values.get("minecraft-min")),
                            MinecraftVersion.parse(values.get("minecraft-max"))))
                    .dependencies(dependencies(values.get("dependencies")))
                    .requiredApis(capabilities(values.get("required-apis")))
                    .providedCapabilities(capabilities(values.get("provided-capabilities")))
                    .permissions(permissions(values.get("permissions")))
                    .configurationKeys(configurationKeys(values.get("configuration-keys")))
                    .build();
            return ReadResult.valid(metadata);
        } catch (RuntimeException exception) {
            issues.add(issue("malformed_value", "document", "extension.metadata.reader.malformed_value"));
            return ReadResult.invalid(issues);
        }
    }

    private static CapabilitySet capabilities(final String value) {
        final List<CapabilityId> parsed = new ArrayList<CapabilityId>();
        for (String item : commaValues(value)) { parsed.add(CapabilityId.parse(item)); }
        return CapabilitySet.of(parsed);
    }

    private static List<ExtensionMetadata.PermissionNode> permissions(final String value) {
        final List<ExtensionMetadata.PermissionNode> parsed = new ArrayList<ExtensionMetadata.PermissionNode>();
        for (String item : commaValues(value)) { parsed.add(ExtensionMetadata.PermissionNode.of(item)); }
        return parsed;
    }

    private static List<ExtensionMetadata.ConfigurationKey> configurationKeys(final String value) {
        final List<ExtensionMetadata.ConfigurationKey> parsed = new ArrayList<ExtensionMetadata.ConfigurationKey>();
        for (String item : commaValues(value)) { parsed.add(ExtensionMetadata.ConfigurationKey.of(item)); }
        return parsed;
    }

    private static List<String> commaValues(final String value) {
        if (value == null || value.trim().isEmpty()) { return Collections.emptyList(); }
        final List<String> result = new ArrayList<String>();
        for (String item : value.split(",")) {
            if (item.trim().isEmpty()) { throw new IllegalArgumentException("Empty list entry"); }
            result.add(item.trim());
        }
        return result;
    }

    private static List<ExtensionMetadata.Dependency> dependencies(final String value) {
        if (value == null || value.trim().isEmpty()) { return Collections.emptyList(); }
        final List<ExtensionMetadata.Dependency> result = new ArrayList<ExtensionMetadata.Dependency>();
        for (String item : value.split(";")) {
            final String trimmed = item.trim();
            final boolean optional = trimmed.endsWith("?");
            final String declaration = optional ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
            final int separator = declaration.indexOf('@');
            if (separator <= 0 || separator == declaration.length() - 1) { throw new IllegalArgumentException("Malformed dependency"); }
            result.add(ExtensionMetadata.Dependency.of(ExtensionId.parse(declaration.substring(0, separator)),
                    VersionRange.parse(declaration.substring(separator + 1)), optional));
        }
        return result;
    }

    private static ExtensionValidation.Issue issue(final String code, final String field, final String messageKey) {
        return ExtensionValidation.Issue.of(ExtensionValidation.Severity.ERROR,
                DefinitionId.of("zartra", "extension_reader/" + code), field, messageKey);
    }

    /** Immutable result of parsing one metadata document. */
    public static final class ReadResult {
        private final ExtensionMetadata metadata;
        private final ExtensionValidation.Report report;
        private ReadResult(final ExtensionMetadata metadata, final ExtensionValidation.Report report) {
            this.metadata = metadata;
            this.report = report;
        }
        private static ReadResult valid(final ExtensionMetadata metadata) { return new ReadResult(metadata, ExtensionValidation.Report.valid()); }
        private static ReadResult invalid(final List<ExtensionValidation.Issue> issues) { return new ReadResult(null, ExtensionValidation.Report.of(issues)); }
        /** @return metadata when parsing succeeded */ public Optional<ExtensionMetadata> metadata() { return Optional.ofNullable(metadata); }
        /** @return deterministic parsing report */ public ExtensionValidation.Report report() { return report; }
        /** @return whether parsing produced metadata */ public boolean isValid() { return metadata != null && report.isValid(); }
    }
}
