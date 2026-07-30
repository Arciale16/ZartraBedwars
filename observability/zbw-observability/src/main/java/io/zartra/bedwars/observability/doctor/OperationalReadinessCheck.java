package io.zartra.bedwars.observability.doctor;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.scheduler.TaskContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Extensible M23 operational check for compatibility, dependencies, configuration and migrations.
 *
 * <p>Probes return bounded public state only. They do not load providers, mutate configuration or
 * perform migration work.</p>
 */
public final class OperationalReadinessCheck implements PluginDoctor.Check {
    private static final DefinitionId CHECK_ID =
            DefinitionId.of("zartra", "doctor/operational-readiness");
    private static final DefinitionId READY =
            DefinitionId.of("zartra", "doctor/operational-ready");
    private static final DefinitionId WARNINGS =
            DefinitionId.of("zartra", "doctor/operational-warnings");
    private static final DefinitionId BLOCKED =
            DefinitionId.of("zartra", "doctor/operational-blocked");
    private final List<Probe> probes;

    /** Creates a check requiring coverage of every operational category. */
    public OperationalReadinessCheck(final Collection<? extends Probe> probes) {
        final List<Probe> copy = new ArrayList<Probe>(
                Objects.requireNonNull(probes, "probes"));
        if (copy.contains(null)) { throw new IllegalArgumentException("probes contain null"); }
        Collections.sort(copy, Comparator.comparing(probe -> probe.id().toString()));
        final java.util.Set<DefinitionId> ids = new java.util.HashSet<DefinitionId>();
        final EnumSet<Category> categories = EnumSet.noneOf(Category.class);
        for (Probe probe : copy) {
            if (!ids.add(probe.id())) {
                throw new IllegalArgumentException("duplicate probe " + probe.id());
            }
            categories.add(probe.category());
        }
        if (!categories.equals(EnumSet.allOf(Category.class))) {
            throw new IllegalArgumentException("all operational categories require a probe");
        }
        this.probes = Collections.unmodifiableList(copy);
    }

    @Override public DefinitionId id() { return CHECK_ID; }

    @Override public PluginDoctor.Result inspect(final TaskContext context) throws Exception {
        Objects.requireNonNull(context, "context");
        final List<Diagnostics.Field> evidence = new ArrayList<Diagnostics.Field>();
        boolean warning = false;
        boolean blocked = false;
        for (Probe probe : probes) {
            if (context.cancellationToken().isCancellationRequested()) {
                blocked = true;
                break;
            }
            final ProbeResult result = Objects.requireNonNull(probe.inspect(), "probe result");
            warning |= result.state() == ProbeState.WARNING;
            blocked |= result.state() == ProbeState.BLOCKED;
            evidence.add(new Diagnostics.Field(probe.id(),
                    result.state().name().toLowerCase(java.util.Locale.ROOT)
                            + ":" + result.reason(),
                    Diagnostics.Classification.PUBLIC));
        }
        final Health.Status health = blocked ? Health.Status.UNAVAILABLE
                : warning ? Health.Status.DEGRADED : Health.Status.HEALTHY;
        final DefinitionId reason = blocked ? BLOCKED : warning ? WARNINGS : READY;
        return new PluginDoctor.Result(CHECK_ID, health, reason, evidence);
    }

    /** Operational readiness category. */
    public enum Category {
        /** Server/client compatibility and fallback matrix. */ COMPATIBILITY,
        /** Optional and required provider states. */ PROVIDER,
        /** Dependency lock and runtime prerequisites. */ DEPENDENCY,
        /** Configuration validity and warnings. */ CONFIGURATION,
        /** Backup, provenance and migration readiness. */ MIGRATION
    }

    /** Probe state. */
    public enum ProbeState { READY, WARNING, BLOCKED }

    /** One bounded, read-only readiness probe. */
    public interface Probe {
        /** @return unique stable evidence ID */ DefinitionId id();
        /** @return probe category */ Category category();
        /** @return secret-safe probe result */ ProbeResult inspect() throws Exception;
    }

    /** Immutable probe result. */
    public static final class ProbeResult {
        private final ProbeState state;
        private final String reason;
        /** Creates a result with a stable, single-line reason. */
        public ProbeResult(final ProbeState state, final String reason) {
            this.state = Objects.requireNonNull(state, "state");
            final String checked = Objects.requireNonNull(reason, "reason").trim();
            if (!checked.matches("[a-z0-9][a-z0-9_.:/-]{1,127}")) {
                throw new IllegalArgumentException("invalid reason");
            }
            this.reason = checked;
        }
        /** @return readiness state */ public ProbeState state() { return state; }
        /** @return stable public reason */ public String reason() { return reason; }
    }
}
