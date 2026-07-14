package io.zartra.bedwars.domain.generator;

import io.zartra.bedwars.api.content.ContentRegistry;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.util.Objects;

/** Immutable foundational definition shared by native and custom generator types. */
public final class GeneratorDefinition implements ContentRegistry.Definition<GeneratorTypeId> {
    private final GeneratorTypeId id;
    private final ResourceId outputResource;
    private final int schemaVersion;
    private GeneratorDefinition(final GeneratorTypeId id, final ResourceId outputResource, final int schemaVersion) {
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        this.id = Objects.requireNonNull(id, "id");
        this.outputResource = Objects.requireNonNull(outputResource, "outputResource");
        this.schemaVersion = schemaVersion;
    }
    /** @return immutable generator definition */ public static GeneratorDefinition of(final GeneratorTypeId id, final ResourceId outputResource, final int schemaVersion) { return new GeneratorDefinition(id, outputResource, schemaVersion); }
    @Override public GeneratorTypeId id() { return id; }
    /** @return generated native or custom resource */ public ResourceId outputResource() { return outputResource; }
    @Override public int schemaVersion() { return schemaVersion; }
}
