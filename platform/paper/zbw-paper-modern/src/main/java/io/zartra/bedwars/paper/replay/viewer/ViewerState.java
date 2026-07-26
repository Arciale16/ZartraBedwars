package io.zartra.bedwars.paper.replay.viewer;

/** Lifecycle of one Paper replay viewer session (ZBW-REPLAY-004). */
public enum ViewerState {
    CONNECTED,
    WATCHING,
    PAUSED,
    DISCONNECTED
}
