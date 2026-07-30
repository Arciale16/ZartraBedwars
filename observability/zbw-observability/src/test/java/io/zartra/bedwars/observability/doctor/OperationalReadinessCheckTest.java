package io.zartra.bedwars.observability.doctor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.scheduler.TaskContext;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationalReadinessCheckTest {
    @Test
    void aggregatesHealthyWarningsAndBlockedStates() throws Exception {
        assertEquals(Health.Status.HEALTHY,
                check(OperationalReadinessCheck.ProbeState.READY).inspect(context()).status());
        assertEquals(Health.Status.DEGRADED,
                check(OperationalReadinessCheck.ProbeState.WARNING).inspect(context()).status());
        assertEquals(Health.Status.UNAVAILABLE,
                check(OperationalReadinessCheck.ProbeState.BLOCKED).inspect(context()).status());
    }

    @Test
    void rejectsIncompleteAndDuplicateProbeInventories() {
        assertThrows(IllegalArgumentException.class,
                () -> new OperationalReadinessCheck(java.util.Collections.singletonList(
                        probe(OperationalReadinessCheck.Category.PROVIDER,
                                OperationalReadinessCheck.ProbeState.READY))));
        final List<OperationalReadinessCheck.Probe> probes =
                probes(OperationalReadinessCheck.ProbeState.READY);
        probes.add(probes.get(0));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationalReadinessCheck(probes));
        assertThrows(IllegalArgumentException.class,
                () -> new OperationalReadinessCheck.ProbeResult(
                        OperationalReadinessCheck.ProbeState.READY, "contains spaces"));
    }

    private static OperationalReadinessCheck check(
            final OperationalReadinessCheck.ProbeState state) {
        return new OperationalReadinessCheck(probes(state));
    }

    private static List<OperationalReadinessCheck.Probe> probes(
            final OperationalReadinessCheck.ProbeState state) {
        final List<OperationalReadinessCheck.Probe> probes =
                new ArrayList<OperationalReadinessCheck.Probe>();
        for (OperationalReadinessCheck.Category category
                : OperationalReadinessCheck.Category.values()) {
            probes.add(probe(category, state));
        }
        return probes;
    }

    private static OperationalReadinessCheck.Probe probe(
            final OperationalReadinessCheck.Category category,
            final OperationalReadinessCheck.ProbeState state) {
        return new OperationalReadinessCheck.Probe() {
            @Override public DefinitionId id() {
                return DefinitionId.of("zartra",
                        "doctor/test-" + category.name().toLowerCase(java.util.Locale.ROOT));
            }
            @Override public OperationalReadinessCheck.Category category() { return category; }
            @Override public OperationalReadinessCheck.ProbeResult inspect() {
                return new OperationalReadinessCheck.ProbeResult(state, "test-result");
            }
        };
    }

    private static TaskContext context() {
        return new TaskContext(io.zartra.bedwars.api.scheduler.TaskDescriptor.of(
                TaskId.random(), DefinitionId.of("zartra", "doctor/test"),
                DefinitionId.of("zartra", "test-owner"), CorrelationId.random(),
                Duration.ofSeconds(1), true), () -> false);
    }
}
