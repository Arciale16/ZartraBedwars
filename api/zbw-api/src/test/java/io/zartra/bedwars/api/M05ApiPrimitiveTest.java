package io.zartra.bedwars.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.api.failure.FailureKind;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.lifecycle.Lifecycle;
import io.zartra.bedwars.api.recovery.Recovery;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskContext;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class M05ApiPrimitiveTest {
    private static final DefinitionId ID = DefinitionId.of("zartra", "test");
    private static final CorrelationId CORRELATION =
            CorrelationId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test void taskAndFailurePrimitivesValidateBoundaries() {
        final TaskId taskId = TaskId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertEquals(taskId, TaskId.parse(taskId.toString()));
        final TaskDescriptor descriptor = TaskDescriptor.of(taskId, ID, ID, CORRELATION,
                Duration.ofSeconds(1), true);
        assertEquals(taskId, descriptor.taskId());
        assertEquals(ID, descriptor.operationId());
        assertEquals(ID, descriptor.ownerId());
        assertEquals(CORRELATION, descriptor.correlationId());
        assertEquals(Duration.ofSeconds(1), descriptor.timeout());
        assertTrue(descriptor.idempotent());
        assertThrows(IllegalArgumentException.class, () -> TaskDescriptor.of(
                taskId, ID, ID, CORRELATION, Duration.ZERO, true));

        final FailureReport report = FailureReport.of(ID, FailureKind.TIMEOUT, CORRELATION,
                "scheduler.timeout", true, NOW);
        assertEquals(report, FailureReport.of(ID, FailureKind.TIMEOUT, CORRELATION,
                "scheduler.timeout", true, NOW));
        assertEquals(FailureKind.TIMEOUT, report.kind());
        assertTrue(report.retryable());
        assertThrows(IllegalArgumentException.class, () -> FailureReport.of(
                ID, FailureKind.INVALID, CORRELATION, "Not valid", false, NOW));

        assertTrue(SchedulerPort.Outcome.success("ok").isSuccess());
        assertEquals("ok", SchedulerPort.Outcome.success("ok").value().get());
        assertFalse(SchedulerPort.Outcome.failure(report).isSuccess());
        assertEquals(report, SchedulerPort.Outcome.failure(report).failure().get());
    }

    @Test void schedulerHealthDiagnosticAndDoctorValuesAreDeterministic() {
        final SchedulerPort.Snapshot snapshot =
                new SchedulerPort.Snapshot(1, 2, 3, 4, 5, 6, 7, false);
        assertEquals(1, snapshot.active());
        assertEquals(2, snapshot.queued());
        assertEquals(3, snapshot.accepted());
        assertEquals(4, snapshot.completed());
        assertEquals(5, snapshot.failed());
        assertEquals(6, snapshot.rejected());
        assertEquals(7, snapshot.cancelled());
        assertFalse(snapshot.accepting());
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerPort.Snapshot(-1, 0, 0, 0, 0, 0, 0, true));

        final HashMap<String, String> dimensions = new HashMap<String, String>();
        dimensions.put("state", "ready");
        final Health.Metric metric = new Health.Metric(ID, 4L, dimensions);
        assertEquals(4L, metric.value());
        assertEquals("ready", metric.dimensions().get("state"));
        assertThrows(IllegalArgumentException.class,
                () -> new Health.Metric(ID, 0L, Collections.singletonMap("bad label", "x")));

        final Diagnostics.Field second = new Diagnostics.Field(
                DefinitionId.of("zartra", "z"), "2", Diagnostics.Classification.PUBLIC);
        final Diagnostics.Field first = new Diagnostics.Field(
                DefinitionId.of("zartra", "a"), "1", Diagnostics.Classification.PUBLIC);
        final Diagnostics.Export export = new Diagnostics.Export(NOW, Arrays.asList(second, first));
        assertEquals(first.id(), export.fields().get(0).id());
        assertThrows(IllegalArgumentException.class, () -> new Diagnostics.Field(
                ID, null, Diagnostics.Classification.PUBLIC));

        final PluginDoctor.Result z = new PluginDoctor.Result(
                DefinitionId.of("zartra", "z"), Health.Status.HEALTHY, ID,
                Collections.<Diagnostics.Field>emptyList());
        final PluginDoctor.Result a = new PluginDoctor.Result(
                DefinitionId.of("zartra", "a"), Health.Status.DEGRADED, ID,
                Collections.<Diagnostics.Field>emptyList());
        final PluginDoctor.Report doctor = new PluginDoctor.Report(NOW, Arrays.asList(z, a));
        assertEquals(a.checkId(), doctor.results().get(0).checkId());
        assertEquals(NOW, doctor.createdAt());
    }

    @Test void lifecycleAndThreadContractsRejectMalformedValues() {
        final FailureReport failure = FailureReport.of(ID, FailureKind.INTERNAL, CORRELATION,
                "lifecycle.failed", false, NOW);
        final Lifecycle.Report report = new Lifecycle.Report(Lifecycle.State.FAILED,
                Collections.singletonList(ID), Collections.singletonList(failure), true);
        assertEquals(Lifecycle.State.FAILED, report.state());
        assertEquals(ID, report.completed().get(0));
        assertEquals(failure, report.failures().get(0));
        assertTrue(report.forced());
        assertThrows(IllegalArgumentException.class, () -> new Lifecycle.Report(
                Lifecycle.State.NEW, Arrays.asList(ID, null),
                Collections.<FailureReport>emptyList(), false));

        final SchedulerPort.ThreadAccessException exception =
                new SchedulerPort.ThreadAccessException(ID, "owner required");
        assertEquals(ID, exception.ownerId());
        assertEquals("owner required", exception.getMessage());
        final TaskContext context = new TaskContext(TaskDescriptor.of(TaskId.random(), ID, ID,
                CORRELATION, Duration.ofSeconds(1), false), () -> false);
        assertFalse(context.cancellationToken().isCancellationRequested());
    }

    @Test void recoveryMarkersAdvanceMonotonicallyAndFenceTerminals() {
        final Recovery.Marker detected = new Recovery.Marker(
                MatchId.of(UUID.fromString("00000000-0000-0000-0000-000000000003")),
                IdempotencyKey.of("zartra", "match/completion"), Recovery.State.DETECTED, 0L, NOW);
        final Recovery.Marker quiesced = detected.advance(
                Recovery.State.QUIESCED, NOW.plusSeconds(1L));
        assertEquals(1L, quiesced.revision());
        assertEquals(Recovery.State.QUIESCED, quiesced.state());
        assertEquals(detected.matchId(), quiesced.matchId());
        final Recovery.Marker terminal = quiesced.advance(
                Recovery.State.MANUAL_REQUIRED, NOW.plusSeconds(2L));
        assertThrows(IllegalStateException.class,
                () -> terminal.advance(Recovery.State.RECOVERED, NOW));
        assertThrows(IllegalArgumentException.class, () -> new Recovery.Marker(
                detected.matchId(), detected.idempotencyKey(), Recovery.State.DETECTED, -1L, NOW));
        final Recovery.Report report = new Recovery.Report(terminal,
                Collections.<FailureReport>emptyList());
        assertFalse(report.recovered());
        assertEquals(terminal, report.marker());
    }
}
