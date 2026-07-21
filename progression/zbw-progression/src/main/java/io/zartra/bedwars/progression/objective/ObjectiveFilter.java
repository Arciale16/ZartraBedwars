package io.zartra.bedwars.progression.objective;

import java.util.Objects;

/** Immutable typed qualifier applied before an objective accepts an event. */
public final class ObjectiveFilter {
    /** Supported neutral filter dimensions. */
    public enum Dimension {
        /** Game mode identity. */ MODE, /** Arena identity. */ ARENA,
        /** Player group identity. */ GROUP, /** Team identity. */ TEAM,
        /** Item identity. */ ITEM, /** Victim classification. */ VICTIM,
        /** Match validity classification. */ MATCH_VALIDITY,
        /** Extension-defined namespaced qualifier. */ CUSTOM
    }

    private final Dimension dimension;
    private final String expectedValue;

    /** Creates a validated filter. */
    public ObjectiveFilter(final Dimension dimension, final String expectedValue) {
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        if (expectedValue == null || expectedValue.trim().isEmpty() || expectedValue.length() > 128) {
            throw new IllegalArgumentException("expectedValue must contain 1..128 characters");
        }
        this.expectedValue = expectedValue;
    }

    /** @return filter dimension */ public Dimension dimension() { return dimension; }
    /** @return expected canonical value */ public String expectedValue() { return expectedValue; }
}
