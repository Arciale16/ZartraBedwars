package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable semantic hologram definition whose rendering belongs to platform adapters. */
public final class ArenaHologram implements Comparable<ArenaHologram> {
    private final DefinitionId id;
    private final ArenaLocation location;
    private final List<DefinitionId> messageKeys;

    /** Creates a bounded hologram made only of localization keys. */
    public ArenaHologram(final DefinitionId id, final ArenaLocation location,
                         final List<DefinitionId> messageKeys) {
        this.id = Objects.requireNonNull(id, "id");
        this.location = Objects.requireNonNull(location, "location");
        final List<DefinitionId> copy = new ArrayList<DefinitionId>(
                Objects.requireNonNull(messageKeys, "messageKeys"));
        if (copy.isEmpty() || copy.size() > 16 || copy.contains(null)) {
            throw new IllegalArgumentException("hologram requires one to sixteen message keys");
        }
        this.messageKeys = Collections.unmodifiableList(copy);
    }
    /** @return hologram identity */ public DefinitionId id() { return id; }
    /** @return hologram anchor */ public ArenaLocation location() { return location; }
    /** @return ordered localization message keys */ public List<DefinitionId> messageKeys() { return messageKeys; }
    @Override public int compareTo(final ArenaHologram other) { return id.compareTo(Objects.requireNonNull(other, "other").id); }
    @Override public int hashCode() { return Objects.hash(id, location, messageKeys); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaHologram)) { return false; }
        final ArenaHologram that = (ArenaHologram) other;
        return Objects.deepEquals(new Object[] {id, location, messageKeys},
                new Object[] {that.id, that.location, that.messageKeys});
    }
}
