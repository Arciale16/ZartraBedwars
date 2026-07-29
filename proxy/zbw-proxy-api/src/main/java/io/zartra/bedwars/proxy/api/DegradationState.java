package io.zartra.bedwars.proxy.api;

/** Fail-closed proxy degradation state. */ public enum DegradationState{
NORMAL,LOCAL_ONLY,RESERVATIONS_PAUSED,DRAINING,OFFLINE}
