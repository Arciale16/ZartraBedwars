package io.zartra.bedwars.proxy.api;

/** Proxy-routable workflow families; policies remain in their owner modules. */
public enum CrossServerFlowType {
    REMOTE_QUEUE,
    MATCH_HANDOFF,
    PRIVATE_GAME,
    SPECTATE,
    REPLAY_SPECTATE,
    REJOIN,
    PLAY_AGAIN,
    MAP_SELECTION
}
