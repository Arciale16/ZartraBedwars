package io.zartra.bedwars.atlas.api;

/** Stable Atlas case lifecycle states (ZBW-ATLAS-003/005/012). */
public enum AtlasCaseStatus {
    CREATED, OPEN, REVIEWING, VERDICT_PENDING, RESOLVED, ARCHIVED, INVALID,
    RESERVED, UNDER_REVIEW, AWAITING_STAFF, CLOSED
}
