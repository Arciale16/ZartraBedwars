package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable axis-aligned region with a stable identity. */
public final class ArenaRegion {
    private final DefinitionId id;
    private final ArenaLocation minimum;
    private final ArenaLocation maximum;

    private ArenaRegion(final DefinitionId id, final ArenaLocation first,
                        final ArenaLocation second) {
        this.id = Objects.requireNonNull(id, "id");
        final ArenaLocation a = Objects.requireNonNull(first, "first");
        final ArenaLocation b = Objects.requireNonNull(second, "second");
        this.minimum = ArenaLocation.of(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()),
                Math.min(a.z(), b.z()), 0.0F, 0.0F);
        this.maximum = ArenaLocation.of(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()),
                Math.max(a.z(), b.z()), 0.0F, 0.0F);
    }

    /** @return normalized region regardless of corner order */
    public static ArenaRegion between(final DefinitionId id, final ArenaLocation first,
                                      final ArenaLocation second) {
        return new ArenaRegion(id, first, second);
    }

    /** @return region identity */ public DefinitionId id() { return id; }
    /** @return inclusive minimum corner */ public ArenaLocation minimum() { return minimum; }
    /** @return inclusive maximum corner */ public ArenaLocation maximum() { return maximum; }
    /** @return whether the position lies inside the inclusive region */
    public boolean contains(final ArenaLocation position) {
        final ArenaLocation value = Objects.requireNonNull(position, "position");
        return value.x() >= minimum.x() && value.x() <= maximum.x()
                && value.y() >= minimum.y() && value.y() <= maximum.y()
                && value.z() >= minimum.z() && value.z() <= maximum.z();
    }

    @Override public int hashCode() { return Objects.hash(id, minimum, maximum); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaRegion)) { return false; }
        final ArenaRegion that = (ArenaRegion) other;
        return Objects.deepEquals(new Object[] {id, minimum, maximum},
                new Object[] {that.id, that.minimum, that.maximum});
    }
}
