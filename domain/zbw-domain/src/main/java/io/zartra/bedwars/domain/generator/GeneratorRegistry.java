package io.zartra.bedwars.domain.generator;

import io.zartra.bedwars.api.content.ContentRegistry;
import io.zartra.bedwars.api.identity.GeneratorTypeId;

/** Public registry contract for native and extension-defined generator types. */
public interface GeneratorRegistry extends ContentRegistry<GeneratorTypeId, GeneratorDefinition> {
}
