package io.zartra.bedwars.compat.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete immutable parity report for one client path and one server runtime claim. */
public final class ClientCompatibilityReport {
    private final ClientPath path;
    private final String serverRuntime;
    private final Map<ClientFeature, ClientFeatureOutcome> outcomes;

    /**
     * Creates a complete report.
     *
     * @param path client translation path
     * @param serverRuntime privacy-safe exact server runtime label
     * @param outcomes exactly one outcome per required client feature
     */
    public ClientCompatibilityReport(
            final ClientPath path, final String serverRuntime,
            final Collection<ClientFeatureOutcome> outcomes) {
        this.path = Objects.requireNonNull(path, "path");
        if (serverRuntime == null
                || !serverRuntime.matches("[A-Za-z0-9_.@/-]{3,160}")) {
            throw new IllegalArgumentException("serverRuntime must be safe");
        }
        this.serverRuntime = serverRuntime;
        final Map<ClientFeature, ClientFeatureOutcome> indexed =
                new EnumMap<ClientFeature, ClientFeatureOutcome>(ClientFeature.class);
        for (ClientFeatureOutcome outcome : Objects.requireNonNull(
                outcomes, "outcomes")) {
            final ClientFeatureOutcome safe =
                    Objects.requireNonNull(outcome, "outcome");
            if (indexed.put(safe.feature(), safe) != null) {
                throw new IllegalArgumentException("duplicate client feature outcome");
            }
        }
        if (indexed.size() != ClientFeature.values().length) {
            throw new IllegalArgumentException(
                    "every client feature requires an outcome");
        }
        this.outcomes = Collections.unmodifiableMap(indexed);
    }

    /** @return selected client path */ public ClientPath path() { return path; }
    /** @return exact server runtime label, proving the adapter was not replaced */
    public String serverRuntime() { return serverRuntime; }
    /** @return deterministic feature outcome */
    public ClientFeatureOutcome outcome(final ClientFeature feature) {
        return outcomes.get(Objects.requireNonNull(feature, "feature"));
    }
    /** @return immutable outcomes in enum order */
    public List<ClientFeatureOutcome> outcomes() {
        return Collections.unmodifiableList(
                new ArrayList<ClientFeatureOutcome>(outcomes.values()));
    }
    /** @return true only when every gameplay information surface is preserved */
    public boolean activationSafe() {
        for (ClientFeatureOutcome outcome : outcomes.values()) {
            if (!outcome.informationPreserved()) {
                return false;
            }
        }
        return true;
    }

    @Override public int hashCode() {
        return Objects.hash(path, serverRuntime, outcomes);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ClientCompatibilityReport)) { return false; }
        final ClientCompatibilityReport that = (ClientCompatibilityReport) other;
        return path == that.path && serverRuntime.equals(that.serverRuntime)
                && outcomes.equals(that.outcomes);
    }
}
