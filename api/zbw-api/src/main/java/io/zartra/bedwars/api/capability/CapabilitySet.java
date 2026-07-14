package io.zartra.bedwars.api.capability;

import io.zartra.bedwars.api.identity.CapabilityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable, deterministically ordered set of declared capabilities. */
public final class CapabilitySet {
    private static final CapabilitySet EMPTY = new CapabilitySet(Collections.<CapabilityId>emptyList());
    private final List<CapabilityId> values;

    private CapabilitySet(final Collection<CapabilityId> values) {
        final Set<CapabilityId> unique = new LinkedHashSet<CapabilityId>();
        for (CapabilityId value : values) {
            if (!unique.add(Objects.requireNonNull(value, "capability"))) {
                throw new DuplicateCapabilityException(value);
            }
        }
        final List<CapabilityId> sorted = new ArrayList<CapabilityId>(unique);
        Collections.sort(sorted);
        this.values = Collections.unmodifiableList(sorted);
    }

    /** @return shared empty set */
    public static CapabilitySet empty() { return EMPTY; }
    /** @return immutable set containing the supplied unique IDs */
    public static CapabilitySet of(final Collection<CapabilityId> values) {
        return new CapabilitySet(Objects.requireNonNull(values, "values"));
    }
    /** @return immutable sorted capability IDs */
    public List<CapabilityId> values() { return values; }
    /** @return whether the capability is present */
    public boolean contains(final CapabilityId capability) { return values.contains(Objects.requireNonNull(capability, "capability")); }
    /** @return whether every required capability is present */
    public boolean containsAll(final CapabilitySet required) { return values.containsAll(required.values); }
    /** @return number of capabilities */
    public int size() { return values.size(); }
    @Override public int hashCode() { return values.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof CapabilitySet && values.equals(((CapabilitySet) other).values);
    }

    /** Typed duplicate-ID failure for capability declarations. */
    public static final class DuplicateCapabilityException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String capability;
        private DuplicateCapabilityException(final CapabilityId capability) {
            super("Duplicate capability: " + capability);
            this.capability = capability.toString();
        }
        /** @return duplicated capability */ public CapabilityId capability() { return CapabilityId.parse(capability); }
    }
}
