package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable typed identifier for a version-neutral platform intent. */
public final class SemanticKey implements Comparable<SemanticKey> {
    private final Kind kind;
    private final DefinitionId id;

    private SemanticKey(final Kind kind, final DefinitionId id) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.id = Objects.requireNonNull(id, "id");
    }

    /** @return a semantic key whose category cannot be confused with another platform intent */
    public static SemanticKey of(final Kind kind, final DefinitionId id) {
        return new SemanticKey(kind, id);
    }

    /** @return semantic platform category */
    public Kind kind() { return kind; }
    /** @return stable namespaced semantic identity */
    public DefinitionId id() { return id; }

    @Override public int compareTo(final SemanticKey other) {
        final int type = kind.compareTo(Objects.requireNonNull(other, "other").kind);
        return type == 0 ? id.toString().compareTo(other.id.toString()) : type;
    }
    @Override public int hashCode() { return Objects.hash(kind, id); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof SemanticKey)) { return false; }
        final SemanticKey that = (SemanticKey) other;
        return kind == that.kind && id.equals(that.id);
    }
    @Override public String toString() { return kind.name().toLowerCase() + ":" + id; }

    /** Exhaustive M06 semantic capability categories. */
    public enum Kind {
        /** Block or inventory material. */ MATERIAL,
        /** Gameplay item presentation and identity. */ ITEM,
        /** Persistent item or entity metadata. */ METADATA,
        /** Informational or decorative sound. */ SOUND,
        /** Informational or decorative particle/effect. */ PARTICLE,
        /** Text, rich-component and action rendering. */ TEXT,
        /** Entity type, pose or presentation. */ ENTITY,
        /** Packet-only capability. */ PACKET,
        /** Inventory, boss-bar, title or input UI. */ USER_INTERFACE,
        /** Owner-thread or region scheduling capability. */ SCHEDULER
    }
}
