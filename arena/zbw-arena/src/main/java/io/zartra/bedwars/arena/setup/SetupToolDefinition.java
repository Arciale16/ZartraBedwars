package io.zartra.bedwars.arena.setup;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable setup-hotbar action definition; M09 owns its eventual item rendering. */
public final class SetupToolDefinition implements Comparable<SetupToolDefinition> {
    private final DefinitionId id;
    private final SetupMutation.Kind action;
    private final int slot;
    private final DefinitionId labelKey;
    private final DefinitionId descriptionKey;

    /** Creates one bounded semantic hotbar tool definition. */
    public SetupToolDefinition(final DefinitionId id, final SetupMutation.Kind action,
                               final int slot, final DefinitionId labelKey,
                               final DefinitionId descriptionKey) {
        this.id = Objects.requireNonNull(id, "id");
        this.action = Objects.requireNonNull(action, "action");
        if (slot < 0 || slot > 8) { throw new IllegalArgumentException("slot must be between 0 and 8"); }
        this.slot = slot;
        this.labelKey = Objects.requireNonNull(labelKey, "labelKey");
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey");
    }
    /** @return tool identity */ public DefinitionId id() { return id; }
    /** @return setup action identity */ public SetupMutation.Kind action() { return action; }
    /** @return preferred hotbar slot */ public int slot() { return slot; }
    /** @return localized label key */ public DefinitionId labelKey() { return labelKey; }
    /** @return localized description key */ public DefinitionId descriptionKey() { return descriptionKey; }
    @Override public int compareTo(final SetupToolDefinition other) { return Integer.compare(slot, Objects.requireNonNull(other, "other").slot); }
}
