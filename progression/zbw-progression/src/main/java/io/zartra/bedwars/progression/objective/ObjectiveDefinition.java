package io.zartra.bedwars.progression.objective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, versioned objective definition shared by quests, achievements and passes. */
public final class ObjectiveDefinition {
    /** How qualifying progress is composed. */
    public enum Composition {
        /** A single counter. */ SINGLE,
        /** Every child must complete. */ ALL,
        /** Any child may complete. */ ANY,
        /** Children complete in declared order. */ SEQUENTIAL
    }

    /** Ownership scope for accumulated progress. */
    public enum Scope {
        /** One player. */ PLAYER,
        /** One party. */ PARTY,
        /** One team. */ TEAM,
        /** One configured community. */ COMMUNITY,
        /** Global aggregate. */ GLOBAL
    }

    private final ObjectiveId id;
    private final int version;
    private final ObjectiveEventType eventType;
    private final long target;
    private final Composition composition;
    private final Scope scope;
    private final List<ObjectiveFilter> filters;
    private final List<ObjectiveId> children;

    /** Creates a validated objective definition. */
    public ObjectiveDefinition(final ObjectiveId id, final int version, final ObjectiveEventType eventType,
                               final long target, final Composition composition, final Scope scope,
                               final List<ObjectiveFilter> filters, final List<ObjectiveId> children) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (target < 1) {
            throw new IllegalArgumentException("target must be positive");
        }
        this.version = version;
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.target = target;
        this.composition = Objects.requireNonNull(composition, "composition");
        this.scope = Objects.requireNonNull(scope, "scope");
        final List<ObjectiveFilter> filterCopy =
                new ArrayList<ObjectiveFilter>(Objects.requireNonNull(filters, "filters"));
        if (filterCopy.contains(null)) {
            throw new IllegalArgumentException("filters must not contain null");
        }
        this.filters = Collections.unmodifiableList(filterCopy);
        final List<ObjectiveId> copy = new ArrayList<ObjectiveId>(Objects.requireNonNull(children, "children"));
        if (copy.contains(null)) {
            throw new IllegalArgumentException("children must not contain null");
        }
        if (composition == Composition.SINGLE && !copy.isEmpty()) {
            throw new IllegalArgumentException("single objectives cannot have children");
        }
        if (composition != Composition.SINGLE && copy.size() < 2) {
            throw new IllegalArgumentException("composite objectives require at least two children");
        }
        this.children = Collections.unmodifiableList(copy);
    }

    /** @return objective identity */ public ObjectiveId id() { return id; }
    /** @return schema version */ public int version() { return version; }
    /** @return allow-listed event type */ public ObjectiveEventType eventType() { return eventType; }
    /** @return required progress */ public long target() { return target; }
    /** @return composition policy */ public Composition composition() { return composition; }
    /** @return progress ownership scope */ public Scope scope() { return scope; }
    /** @return immutable event filters */ public List<ObjectiveFilter> filters() { return filters; }
    /** @return immutable child identities */ public List<ObjectiveId> children() { return children; }
}
