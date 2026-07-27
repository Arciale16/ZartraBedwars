package io.zartra.bedwars.atlas.api;

/** Stable Atlas case lifecycle states (ZBW-ATLAS-003/005/012). */
public enum AtlasCaseStatus {
    /** Case exists and is eligible for queue admission. */ OPEN,
    /** One reviewer owns a bounded reservation. */ RESERVED,
    /** A reviewer is actively inspecting evidence. */ UNDER_REVIEW,
    /** Community input is complete and staff disposition is pending. */ AWAITING_STAFF,
    /** Authorized staff completed the case. */ CLOSED,
    /** Closed evidence is retained outside active queues. */ ARCHIVED
}
