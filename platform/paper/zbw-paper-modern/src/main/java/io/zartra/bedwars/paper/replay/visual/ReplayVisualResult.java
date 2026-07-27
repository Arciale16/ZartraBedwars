package io.zartra.bedwars.paper.replay.visual;

import java.util.Objects;
import java.util.Optional;

/** Sanitized result of deterministic visual reconstruction. */
public final class ReplayVisualResult {
    /** Stable projection outcomes. */
    public enum Status { APPLIED, UNCHANGED, CORRUPT, OVER_CAPACITY }

    private final Status status;
    private final ReplayVisualState state;

    private ReplayVisualResult(final Status status, final ReplayVisualState state) {
        this.status = Objects.requireNonNull(status, "status");
        this.state = state;
    }

    /** Creates a successful result. */
    public static ReplayVisualResult success(final Status status,
                                             final ReplayVisualState state) {
        if (status != Status.APPLIED && status != Status.UNCHANGED) {
            throw new IllegalArgumentException("success status required");
        }
        return new ReplayVisualResult(status, Objects.requireNonNull(state, "state"));
    }

    /** Creates a rejected result without exposing malformed payload details. */
    public static ReplayVisualResult rejected(final Status status) {
        if (status == Status.APPLIED || status == Status.UNCHANGED) {
            throw new IllegalArgumentException("rejection status required");
        }
        return new ReplayVisualResult(status, null);
    }

    /** @return stable status */ public Status status() { return status; }
    /** @return reconstructed state when successful */ public Optional<ReplayVisualState> state() {
        return Optional.ofNullable(state);
    }
}
