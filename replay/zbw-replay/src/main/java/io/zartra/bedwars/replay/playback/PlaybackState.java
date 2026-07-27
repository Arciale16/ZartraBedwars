package io.zartra.bedwars.replay.playback;

/** Lifecycle of one platform-neutral replay playback (ZBW-REPLAY-004). */
public enum PlaybackState {
    CREATED,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    SEEKING,
    COMPLETED,
    FAILED
}
