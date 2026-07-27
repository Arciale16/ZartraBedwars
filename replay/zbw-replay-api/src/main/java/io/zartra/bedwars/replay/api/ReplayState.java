package io.zartra.bedwars.replay.api;

/** Lifecycle state of a replay recording foundation. */
public enum ReplayState {
    /** Metadata exists and recording has not started. */ CREATED,
    /** Ordered events may be appended. */ RECORDING,
    /** Recording closed successfully and is immutable. */ COMPLETED,
    /** Completed replay is retained outside the active set. */ ARCHIVED,
    /** Recording terminated with a sanitized failure reason. */ FAILED
}
