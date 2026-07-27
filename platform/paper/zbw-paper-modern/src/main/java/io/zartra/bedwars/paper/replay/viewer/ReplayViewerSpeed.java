package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.replay.playback.PlaybackSpeed;
import java.util.Locale;
import java.util.Objects;

/** Exact speed choices exposed by the replay UX. */
public enum ReplayViewerSpeed {
    QUARTER(0.25D),
    HALF(0.5D),
    NORMAL(1.0D),
    DOUBLE(2.0D),
    QUADRUPLE(4.0D);

    private final double multiplier;

    ReplayViewerSpeed(final double multiplier) {
        this.multiplier = multiplier;
    }

    /** Parses one exact user-facing speed token. */
    public static ReplayViewerSpeed parse(final String token) {
        if (token == null) { throw new IllegalArgumentException("speed must not be null"); }
        final String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (ReplayViewerSpeed speed : values()) {
            final String number = Double.toString(speed.multiplier);
            if (normalized.equals(number) || normalized.equals(number + "x")
                    || normalized.equals(number.replace(".0", ""))
                    || normalized.equals(number.replace(".0", "") + "x")
                    || speed == NORMAL && ("1".equals(normalized)
                    || "1x".equals(normalized))) {
                return speed;
            }
        }
        throw new IllegalArgumentException("unsupported replay speed");
    }

    /** Converts an exact playback-engine value into its supported UX choice. */
    public static ReplayViewerSpeed fromPlayback(final PlaybackSpeed playback) {
        Objects.requireNonNull(playback, "playback");
        for (ReplayViewerSpeed speed : values()) {
            if (Double.compare(speed.multiplier, playback.multiplier()) == 0) { return speed; }
        }
        throw new IllegalArgumentException("unsupported replay speed");
    }
    /** @return exact playback-engine value */
    public PlaybackSpeed playbackSpeed() { return PlaybackSpeed.of(multiplier); }
    /** @return user-facing multiplier */ public double multiplier() { return multiplier; }
}
