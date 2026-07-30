package io.zartra.bedwars.compat.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Selects immutable semantic-key subsets from a complete mapping profile. */
public final class CompatibilityMappingKeys {
    private CompatibilityMappingKeys() { throw new AssertionError("No instances"); }

    /** @return keys whose kinds match the requested categories */
    public static Set<SemanticKey> of(
            final List<CompatibilityMapping> mappings, final SemanticKey.Kind... kinds) {
        if (mappings == null || kinds == null) {
            throw new NullPointerException("mappings and kinds");
        }
        final Set<SemanticKey.Kind> requested = new LinkedHashSet<SemanticKey.Kind>();
        Collections.addAll(requested, kinds);
        final Set<SemanticKey> keys = new LinkedHashSet<SemanticKey>();
        for (CompatibilityMapping mapping : mappings) {
            if (requested.contains(mapping.semanticKey().kind())) {
                keys.add(mapping.semanticKey());
            }
        }
        return Collections.unmodifiableSet(keys);
    }
}
