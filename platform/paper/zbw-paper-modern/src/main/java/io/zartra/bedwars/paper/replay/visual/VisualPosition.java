package io.zartra.bedwars.paper.replay.visual;

import java.util.Objects;

/** Immutable world position used by the Paper replay visual projection. */
public final class VisualPosition {
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /** Creates a finite position. */
    public VisualPosition(final String world, final double x, final double y, final double z,
                          final float yaw, final float pitch) {
        if (world == null || world.trim().isEmpty()) {
            throw new IllegalArgumentException("world must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("position components must be finite");
        }
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /** @return world identity */ public String world() { return world; }
    /** @return X coordinate */ public double x() { return x; }
    /** @return Y coordinate */ public double y() { return y; }
    /** @return Z coordinate */ public double z() { return z; }
    /** @return yaw */ public float yaw() { return yaw; }
    /** @return pitch */ public float pitch() { return pitch; }

    @Override public boolean equals(final Object other) {
        if (!(other instanceof VisualPosition)) { return false; }
        final VisualPosition value = (VisualPosition) other;
        return world.equals(value.world) && Double.compare(x, value.x) == 0
                && Double.compare(y, value.y) == 0 && Double.compare(z, value.z) == 0
                && Float.compare(yaw, value.yaw) == 0 && Float.compare(pitch, value.pitch) == 0;
    }

    @Override public int hashCode() {
        return Objects.hash(world, x, y, z, yaw, pitch);
    }
}
