package io.zartra.bedwars.atlas.api;

/** Stable built-in Atlas verdict identifiers (ZBW-ATLAS-006). */
public enum ReviewVerdict {
    /** Evidence clearly supports cheating. */ EVIDENTLY_CHEATING,
    /** Evidence is insufficient for a finding. */ INSUFFICIENT_EVIDENCE,
    /** Evidence supports no violation. */ NOT_CHEATING,
    /** Evidence supports cross-team cooperation. */ CROSS_TEAMING,
    /** Evidence supports prohibited boosting. */ BOOSTING,
    /** Evidence supports exploitation or bug abuse. */ EXPLOITING,
    /** Evidence supports another configured rule violation. */ OTHER_RULE_VIOLATION,
    /** Evidence cannot be trusted or decoded. */ INVALID_EVIDENCE,
    /** No substantive verdict was submitted. */ UNABLE_TO_REVIEW
}
