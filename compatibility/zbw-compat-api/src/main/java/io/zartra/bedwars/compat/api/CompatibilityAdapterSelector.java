package io.zartra.bedwars.compat.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Deterministic exact-runtime selector that rejects absent and ambiguous adapters. */
public final class CompatibilityAdapterSelector {
    private final List<CompatibilityAdapter> adapters;

    /** Creates a selector with a stable ordering independent of discovery order. */
    public CompatibilityAdapterSelector(final Collection<? extends CompatibilityAdapter> adapters) {
        final List<CompatibilityAdapter> copy =
                new ArrayList<CompatibilityAdapter>(Objects.requireNonNull(adapters, "adapters"));
        if (copy.contains(null)) {
            throw new IllegalArgumentException("adapters cannot contain null");
        }
        Collections.sort(copy, Comparator.comparing(adapter ->
                adapter.descriptor().id().toString()));
        this.adapters = Collections.unmodifiableList(copy);
    }

    /**
     * Selects exactly one adapter matching every locked runtime field.
     *
     * @throws IllegalStateException when no adapter or more than one adapter matches
     */
    public CompatibilityAdapter select(final CompatibilityAdapter.RuntimeClaim runtime) {
        Objects.requireNonNull(runtime, "runtime");
        CompatibilityAdapter selected = null;
        for (CompatibilityAdapter adapter : adapters) {
            if (same(adapter.runtimeClaim(), runtime)) {
                if (selected != null) {
                    throw new IllegalStateException("duplicate compatibility adapter for exact runtime");
                }
                selected = adapter;
            }
        }
        if (selected == null) {
            throw new IllegalStateException("unsupported or unlocked server runtime");
        }
        return selected;
    }

    /** @return immutable deterministic adapter inventory */
    public List<CompatibilityAdapter> adapters() { return adapters; }

    private static boolean same(final CompatibilityAdapter.RuntimeClaim left,
                                final CompatibilityAdapter.RuntimeClaim right) {
        return left.platform().equals(right.platform())
                && left.minecraftVersion().equals(right.minecraftVersion())
                && left.build().equals(right.build())
                && left.sha256().equals(right.sha256());
    }
}
