package io.zartra.bedwars.redis.api;
/** Fail-safe distributed operating mode. */ public enum DegradationMode {
NORMAL, LOCAL_ONLY, READ_ONLY, CROSS_NODE_PAUSED }
