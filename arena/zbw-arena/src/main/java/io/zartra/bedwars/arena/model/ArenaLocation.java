package io.zartra.bedwars.arena.model;

import java.util.Objects;

/** Immutable world-local position and orientation used by setup and validation. */
public final class ArenaLocation {
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    private ArenaLocation(final double x, final double y, final double z,
                          final float yaw, final float pitch) {
        finite(x, "x");
        finite(y, "y");
        finite(z, "z");
        finite(yaw, "yaw");
        finite(pitch, "pitch");
        if (pitch < -90.0F || pitch > 90.0F) {
            throw new IllegalArgumentException("pitch must be between -90 and 90");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = normalizeYaw(yaw);
        this.pitch = pitch;
    }

    /** @return a validated immutable location */
    public static ArenaLocation of(final double x, final double y, final double z,
                                   final float yaw, final float pitch) {
        return new ArenaLocation(x, y, z, yaw, pitch);
    }

    private static float normalizeYaw(final float value) {
        float normalized = value % 360.0F;
        if (normalized < -180.0F) { normalized += 360.0F; }
        if (normalized >= 180.0F) { normalized -= 360.0F; }
        return normalized;
    }

    private static void finite(final double value, final String label) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }

    /** @return x coordinate */ public double x() { return x; }
    /** @return y coordinate */ public double y() { return y; }
    /** @return z coordinate */ public double z() { return z; }
    /** @return normalized yaw */ public float yaw() { return yaw; }
    /** @return pitch */ public float pitch() { return pitch; }

    @Override public int hashCode() { return Objects.hash(x, y, z, yaw, pitch); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaLocation)) { return false; }
        final ArenaLocation that = (ArenaLocation) other;
        return Objects.deepEquals(new Object[] {x, y, z, yaw, pitch},
                new Object[] {that.x, that.y, that.z, that.yaw, that.pitch});
    }
}
