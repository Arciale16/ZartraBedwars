package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Safe builder for the ten required server compatibility semantic categories. */
public final class CompatibilityMappings {
    private CompatibilityMappings() { throw new AssertionError("No instances"); }

    /**
     * Builds the complete bounded mapping set for one adapter profile.
     *
     * @param rendererPrefix safe adapter renderer prefix
     * @param values exactly ten safe platform values in {@link SemanticKey.Kind} order
     * @return immutable mappings
     */
    public static List<CompatibilityMapping> complete(
            final String rendererPrefix, final String... values) {
        if (rendererPrefix == null || !rendererPrefix.matches("[a-z0-9_/-]{1,48}")) {
            throw new IllegalArgumentException("invalid renderer prefix");
        }
        if (values == null || values.length != SemanticKey.Kind.values().length) {
            throw new IllegalArgumentException("one value is required for every semantic kind");
        }
        final List<CompatibilityMapping> mappings =
                new ArrayList<CompatibilityMapping>(values.length);
        final SemanticKey.Kind[] kinds = SemanticKey.Kind.values();
        for (int index = 0; index < kinds.length; index++) {
            final SemanticKey.Kind kind = kinds[index];
            final String key = kind == SemanticKey.Kind.USER_INTERFACE
                    ? "ui/inventory" : kind.name().toLowerCase(java.util.Locale.ROOT) + "/primary";
            mappings.add(new CompatibilityMapping(
                    SemanticKey.of(kind, DefinitionId.of("zartra", key)),
                    DefinitionId.of("zartra", rendererPrefix + "/"
                            + kind.name().toLowerCase(java.util.Locale.ROOT)),
                    values[index]));
        }
        return Collections.unmodifiableList(mappings);
    }
}
