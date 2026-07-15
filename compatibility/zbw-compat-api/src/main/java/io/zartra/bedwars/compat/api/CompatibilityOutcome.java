package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;
import java.util.Optional;

/** Immutable supported, fallback, degraded or unsupported compatibility decision. */
public final class CompatibilityOutcome {
    private final State state;
    private final CompatibilityMapping mapping;
    private final DefinitionId reason;
    private final boolean decorativeSuppression;

    private CompatibilityOutcome(final State state, final CompatibilityMapping mapping,
                                 final DefinitionId reason, final boolean suppression) {
        this.state = Objects.requireNonNull(state, "state");
        this.mapping = mapping;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.decorativeSuppression = suppression;
        if (state == State.UNSUPPORTED && mapping != null) {
            throw new IllegalArgumentException("unsupported outcome cannot carry a mapping");
        }
        if (state != State.UNSUPPORTED && mapping == null) {
            throw new IllegalArgumentException("usable outcome requires a mapping");
        }
        if (suppression && state != State.DEGRADED) {
            throw new IllegalArgumentException("only degraded outcomes suppress decoration");
        }
    }

    /** @return native supported outcome */
    public static CompatibilityOutcome supported(final CompatibilityMapping mapping) {
        return new CompatibilityOutcome(State.SUPPORTED, mapping,
                DefinitionId.of("zartra", "compat/supported"), false);
    }
    /** @return functionally equivalent fallback outcome */
    public static CompatibilityOutcome fallback(final CompatibilityMapping mapping,
                                                final DefinitionId reason) {
        return new CompatibilityOutcome(State.FALLBACK, mapping, reason, false);
    }
    /** @return gameplay-preserving degraded outcome */
    public static CompatibilityOutcome degraded(final CompatibilityMapping mapping,
                                                final DefinitionId reason,
                                                final boolean decorativeSuppression) {
        return new CompatibilityOutcome(State.DEGRADED, mapping, reason, decorativeSuppression);
    }
    /** @return explicit unsupported outcome */
    public static CompatibilityOutcome unsupported(final DefinitionId reason) {
        return new CompatibilityOutcome(State.UNSUPPORTED, null, reason, false);
    }

    /** @return resolution state */ public State state() { return state; }
    /** @return selected mapping, empty only when unsupported */ public Optional<CompatibilityMapping> mapping() { return Optional.ofNullable(mapping); }
    /** @return stable localization-safe reason */ public DefinitionId reason() { return reason; }
    /** @return whether a purely decorative sub-effect was suppressed */ public boolean decorativeSuppression() { return decorativeSuppression; }
    /** @return true when gameplay behavior remains available */ public boolean gameplayPreserved() { return state != State.UNSUPPORTED; }

    /** Compatibility resolution classification. */
    public enum State { SUPPORTED, FALLBACK, DEGRADED, UNSUPPORTED }
}
