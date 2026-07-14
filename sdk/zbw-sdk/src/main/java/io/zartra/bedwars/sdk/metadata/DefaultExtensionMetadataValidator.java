package io.zartra.bedwars.sdk.metadata;

import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.extension.ExtensionValidation;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ExtensionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Thread-safe deterministic implementation of the extension metadata contract. */
public final class DefaultExtensionMetadataValidator implements ExtensionValidation.Validator {
    private static final String ENTRYPOINT = "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+";

    @Override
    public ExtensionValidation.Report validate(final ExtensionMetadata metadata,
                                               final ExtensionValidation.Target target) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(target, "target");
        final List<ExtensionValidation.Issue> issues = new ArrayList<ExtensionValidation.Issue>();
        if (metadata.schemaVersion() != ExtensionMetadata.CURRENT_SCHEMA_VERSION) {
            issues.add(error("schema_version", "schema-version", "extension.metadata.schema.unsupported"));
        }
        if (metadata.displayName().trim().isEmpty() || metadata.displayName().length() > 64) {
            issues.add(error("display_name", "display-name", "extension.metadata.display_name.invalid"));
        }
        if (!metadata.entrypoint().matches(ENTRYPOINT)) {
            issues.add(error("entrypoint", "entrypoint", "extension.metadata.entrypoint.invalid"));
        }
        if (!metadata.apiVersions().contains(target.apiVersion())) {
            issues.add(error("api_unsupported", "api-versions", "extension.metadata.api.unsupported"));
        }
        if (!metadata.productVersions().contains(target.productVersion())) {
            issues.add(error("product_unsupported", "product-versions", "extension.metadata.product.unsupported"));
        }
        if (!metadata.minecraftVersions().contains(target.minecraftVersion())) {
            issues.add(error("minecraft_unsupported", "minecraft-versions", "extension.metadata.minecraft.unsupported"));
        }
        final Set<ExtensionId> dependencies = new HashSet<ExtensionId>();
        for (int index = 0; index < metadata.dependencies().size(); index++) {
            final ExtensionMetadata.Dependency dependency = metadata.dependencies().get(index);
            final String field = "dependencies[" + index + "]";
            if (dependency.id().equals(metadata.id())) {
                issues.add(error("self_dependency", field, "extension.metadata.dependency.self"));
            }
            if (!dependencies.add(dependency.id())) {
                issues.add(error("duplicate_dependency", field, "extension.metadata.dependency.duplicate"));
            }
        }
        duplicateValues(metadata.permissions(), "permissions", issues);
        duplicateValues(metadata.configurationKeys(), "configuration-keys", issues);
        return ExtensionValidation.Report.of(issues);
    }

    @Override
    public ExtensionValidation.Report validateCatalog(final Collection<ExtensionMetadata> metadata,
                                                      final ExtensionValidation.Target target) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(target, "target");
        final List<ExtensionMetadata> ordered = new ArrayList<ExtensionMetadata>();
        for (ExtensionMetadata value : metadata) { ordered.add(Objects.requireNonNull(value, "metadata entry")); }
        Collections.sort(ordered, (left, right) -> left.id().compareTo(right.id()));
        final List<ExtensionValidation.Issue> issues = new ArrayList<ExtensionValidation.Issue>();
        final Map<ExtensionId, ExtensionMetadata> byId = new HashMap<ExtensionId, ExtensionMetadata>();
        for (int index = 0; index < ordered.size(); index++) {
            final ExtensionMetadata value = ordered.get(index);
            for (ExtensionValidation.Issue issue : validate(value, target).issues()) {
                issues.add(ExtensionValidation.Issue.of(issue.severity(), issue.code(),
                        "catalog[" + index + "]." + issue.field(), issue.messageKey()));
            }
            if (byId.put(value.id(), value) != null) {
                issues.add(error("duplicate_extension", "catalog[" + index + "].id",
                        "extension.metadata.id.duplicate"));
            }
        }
        for (int index = 0; index < ordered.size(); index++) {
            final ExtensionMetadata owner = ordered.get(index);
            for (int dependencyIndex = 0; dependencyIndex < owner.dependencies().size(); dependencyIndex++) {
                final ExtensionMetadata.Dependency dependency = owner.dependencies().get(dependencyIndex);
                final ExtensionMetadata resolved = byId.get(dependency.id());
                final String field = "catalog[" + index + "].dependencies[" + dependencyIndex + "]";
                if (resolved == null && !dependency.optional()) {
                    issues.add(error("dependency_missing", field, "extension.metadata.dependency.missing"));
                } else if (resolved != null && !dependency.versions().contains(resolved.version())) {
                    issues.add(error("dependency_version", field, "extension.metadata.dependency.version"));
                }
            }
        }
        return ExtensionValidation.Report.of(issues);
    }

    private static void duplicateValues(final List<?> values, final String field,
                                        final List<ExtensionValidation.Issue> issues) {
        final Set<Object> unique = new HashSet<Object>();
        for (int index = 0; index < values.size(); index++) {
            if (!unique.add(values.get(index))) {
                issues.add(error("duplicate_value", field + '[' + index + ']',
                        "extension.metadata.value.duplicate"));
            }
        }
    }

    private static ExtensionValidation.Issue error(final String code, final String field,
                                                    final String messageKey) {
        return ExtensionValidation.Issue.of(ExtensionValidation.Severity.ERROR,
                DefinitionId.of("zartra", "extension_metadata/" + code), field, messageKey);
    }
}
