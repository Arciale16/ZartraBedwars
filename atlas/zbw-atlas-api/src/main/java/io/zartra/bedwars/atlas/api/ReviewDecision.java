package io.zartra.bedwars.atlas.api;

/** Reviewer action recorded at submission time. */
public enum ReviewDecision {
    /** Reviewer supplied an evidence-based verdict. */ VERDICT,
    /** Reviewer released the case without evaluating it. */ SKIP,
    /** Reviewer inspected evidence but could not reach a verdict. */ ABSTAIN
}
