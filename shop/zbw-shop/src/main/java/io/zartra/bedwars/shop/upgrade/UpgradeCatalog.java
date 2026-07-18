package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable extension-safe upgrade catalogue. */
public final class UpgradeCatalog {
    private final Map<DefinitionId, UpgradeDefinition> definitions;
    /** Creates a catalogue and rejects duplicate IDs. */
    public UpgradeCatalog(final Collection<UpgradeDefinition> definitions) {
        final Map<DefinitionId, UpgradeDefinition> copy = new TreeMap<DefinitionId, UpgradeDefinition>();
        for (UpgradeDefinition definition : Objects.requireNonNull(definitions, "definitions")) {
            final UpgradeDefinition checked = Objects.requireNonNull(definition, "definition");
            if (copy.put(checked.id(), checked) != null) { throw new IllegalArgumentException("duplicate upgrade ID"); }
        }
        if (copy.isEmpty() || copy.size() > 256) { throw new IllegalArgumentException("catalogue size is invalid"); }
        for (UpgradeDefinition definition : copy.values()) {
            for (UpgradeDefinition.Level level : definition.levels()) {
                for (Map.Entry<DefinitionId, Integer> dependency : level.dependencies().entrySet()) {
                    final UpgradeDefinition target = copy.get(dependency.getKey());
                    if (target == null || target.maximumLevel() < dependency.getValue()
                            || dependency.getKey().equals(definition.id())) {
                        throw new IllegalArgumentException("invalid upgrade dependency");
                    }
                }
            }
        }
        this.definitions = Collections.unmodifiableMap(copy);
    }
    /** @return definition by ID */ public Optional<UpgradeDefinition> find(final DefinitionId id) { return Optional.ofNullable(definitions.get(Objects.requireNonNull(id, "id"))); }
    /** @return definitions ordered by ID */ public List<UpgradeDefinition> definitions() { return Collections.unmodifiableList(new ArrayList<UpgradeDefinition>(definitions.values())); }
}
