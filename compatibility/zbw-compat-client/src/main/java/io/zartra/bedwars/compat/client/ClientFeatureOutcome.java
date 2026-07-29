package io.zartra.bedwars.compat.client;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable client feature decision with explicit information-preservation evidence. */
public final class ClientFeatureOutcome {
    private final ClientFeature feature;
    private final State state;
    private final DefinitionId reason;
    private final boolean informationPreserved;
    private final boolean decorativeSuppression;

    /**
     * Creates a validated client feature outcome.
     *
     * @param feature feature surface
     * @param state translation state
     * @param reason stable diagnostic/localization reason
     * @param informationPreserved whether all gameplay information remains available
     * @param decorativeSuppression whether only redundant decoration was suppressed
     */
    public ClientFeatureOutcome(final ClientFeature feature, final State state,
                                final DefinitionId reason,
                                final boolean informationPreserved,
                                final boolean decorativeSuppression) {
        this.feature = Objects.requireNonNull(feature, "feature");
        this.state = Objects.requireNonNull(state, "state");
        this.reason = Objects.requireNonNull(reason, "reason");
        if ((state == State.BLOCKED) == informationPreserved) {
            throw new IllegalArgumentException(
                    "blocked is the only non-preserving state");
        }
        if (decorativeSuppression && state != State.DEGRADED) {
            throw new IllegalArgumentException(
                    "only degraded outcomes suppress decoration");
        }
        this.informationPreserved = informationPreserved;
        this.decorativeSuppression = decorativeSuppression;
    }

    /** @return feature surface */ public ClientFeature feature() { return feature; }
    /** @return translation state */ public State state() { return state; }
    /** @return stable reason */ public DefinitionId reason() { return reason; }
    /** @return whether gameplay information remains available */
    public boolean informationPreserved() { return informationPreserved; }
    /** @return whether redundant decoration was explicitly suppressed */
    public boolean decorativeSuppression() { return decorativeSuppression; }

    @Override public int hashCode() {
        return Objects.hash(feature, state, reason,
                informationPreserved, decorativeSuppression);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ClientFeatureOutcome)) { return false; }
        final ClientFeatureOutcome that = (ClientFeatureOutcome) other;
        return feature == that.feature && state == that.state
                && reason.equals(that.reason)
                && informationPreserved == that.informationPreserved
                && decorativeSuppression == that.decorativeSuppression;
    }

    /** Client translation outcome states. */
    public enum State {
        /** Native client and server semantics match. */ NATIVE,
        /** Provider performs a direct protocol translation. */ TRANSLATED,
        /** An equivalent non-native presentation/input is selected. */ FALLBACK,
        /** Redundant decoration is reduced with information retained elsewhere. */ DEGRADED,
        /** The session must not activate because information would be lost. */ BLOCKED
    }
}
