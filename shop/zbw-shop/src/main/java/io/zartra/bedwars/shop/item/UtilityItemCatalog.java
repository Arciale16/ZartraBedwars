package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable extension-safe utility-item action registry. */
public final class UtilityItemCatalog {
    private final Map<DefinitionId, UtilityItemDefinition> definitions;
    /** Creates a non-empty registry and rejects duplicate IDs. */
    public UtilityItemCatalog(final Collection<UtilityItemDefinition> definitions) {
        final Map<DefinitionId, UtilityItemDefinition> copy = new TreeMap<DefinitionId, UtilityItemDefinition>();
        for (UtilityItemDefinition definition : Objects.requireNonNull(definitions, "definitions")) {
            final UtilityItemDefinition checked = Objects.requireNonNull(definition, "definition");
            if (copy.put(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate utility action " + checked.id());
            }
        }
        if (copy.isEmpty()) { throw new IllegalArgumentException("utility catalog is empty"); }
        this.definitions = Collections.unmodifiableMap(copy);
    }
    /** @return registered action */
    public Optional<UtilityItemDefinition> definition(final DefinitionId id) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(id, "id")));
    }
    /** @return deterministic immutable definitions */ public Collection<UtilityItemDefinition> definitions() { return definitions.values(); }
}
