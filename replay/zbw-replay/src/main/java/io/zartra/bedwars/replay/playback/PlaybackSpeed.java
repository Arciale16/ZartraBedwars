package io.zartra.bedwars.replay.playback;

/** Immutable bounded playback multiplier (ZBW-REPLAY-004). */
public final class PlaybackSpeed {
    /** Normal real-time playback speed. */
    public static final PlaybackSpeed NORMAL = new PlaybackSpeed(1.0D);
    private static final double MINIMUM = 0.10D;
    private static final double MAXIMUM = 4.0D;

    private final double multiplier;

    private PlaybackSpeed(final double multiplier) {
        this.multiplier = multiplier;
    }

    /** Creates a safe custom multiplier in the inclusive 0.10..4.00 range. */
    public static PlaybackSpeed of(final double multiplier) {
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)
                || multiplier < MINIMUM || multiplier > MAXIMUM) {
            throw new IllegalArgumentException("playback speed must be finite and within 0.10..4.00");
        }
        return multiplier == 1.0D ? NORMAL : new PlaybackSpeed(multiplier);
    }

    /** Returns the time multiplier. */
    public double multiplier() {
        return multiplier;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof PlaybackSpeed
                && Double.compare(multiplier, ((PlaybackSpeed) other).multiplier) == 0;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(multiplier).hashCode();
    }

    @Override
    public String toString() {
        return multiplier + "x";
    }
}
