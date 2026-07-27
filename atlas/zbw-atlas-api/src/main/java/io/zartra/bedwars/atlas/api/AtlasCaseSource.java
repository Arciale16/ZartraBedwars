package io.zartra.bedwars.atlas.api;

/** Privacy-neutral source family that caused a case to be created. */
public enum AtlasCaseSource {
    /** A player submitted a report through an authorized reporting boundary. */ REPORT,
    /** Existing M17 replay evidence triggered case creation. */ REPLAY_EVIDENCE,
    /** A trusted internal detector or manual staff signal triggered case creation. */ INTERNAL_SIGNAL
}
