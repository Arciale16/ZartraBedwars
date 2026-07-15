package io.zartra.bedwars.observability.doctor;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.scheduler.SchedulerPort;
import io.zartra.bedwars.api.scheduler.TaskDescriptor;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Bounded, failure-isolating executor for registered Plugin Doctor checks. */
public final class PluginDoctorEngine {
    private static final DefinitionId OWNER = DefinitionId.of("zartra", "plugin-doctor");
    private static final DefinitionId CHECK_FAILED = DefinitionId.of("zartra", "doctor/check-failed");
    private final int maximumChecks;
    private final int maximumEvidenceFields;
    private final SchedulerPort scheduler;
    private final Diagnostics.Sanitizer sanitizer;
    private final TimeSource timeSource;
    private final Map<DefinitionId, PluginDoctor.Check> checks =
            new TreeMap<DefinitionId, PluginDoctor.Check>();
    /** Creates a doctor with hard capacities. */
    public PluginDoctorEngine(final int maximumChecks, final int maximumEvidenceFields,
                              final SchedulerPort scheduler,
                              final Diagnostics.Sanitizer sanitizer,
                              final TimeSource timeSource) {
        if (maximumChecks < 1 || maximumEvidenceFields < 0) {
            throw new IllegalArgumentException("doctor bounds are invalid");
        }
        this.maximumChecks = maximumChecks;
        this.maximumEvidenceFields = maximumEvidenceFields;
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }
    /** Registers one unique check. */
    public synchronized void register(final PluginDoctor.Check check) {
        Objects.requireNonNull(check, "check");
        final DefinitionId id = Objects.requireNonNull(check.id(), "check.id");
        if (checks.containsKey(id)) { throw new IllegalArgumentException("duplicate doctor check ID"); }
        if (checks.size() >= maximumChecks) { throw new IllegalStateException("doctor capacity exhausted"); }
        checks.put(id, check);
    }
    /** Runs every check on the bounded worker and never completes exceptionally for check failure. */
    public synchronized CompletionStage<PluginDoctor.Report> run(final Duration perCheckTimeout) {
        if (perCheckTimeout == null || perCheckTimeout.isZero() || perCheckTimeout.isNegative()) {
            throw new IllegalArgumentException("perCheckTimeout must be positive");
        }
        final CorrelationId correlationId = CorrelationId.random();
        final List<CompletableFuture<PluginDoctor.Result>> futures =
                new ArrayList<CompletableFuture<PluginDoctor.Result>>();
        for (PluginDoctor.Check check : checks.values()) {
            final TaskDescriptor descriptor = TaskDescriptor.of(TaskId.random(), check.id(), OWNER,
                    correlationId, perCheckTimeout, true);
            final SchedulerPort.TaskHandle<PluginDoctor.Result> handle = scheduler.submit(descriptor,
                    context -> sanitize(check.inspect(context)));
            futures.add(handle.completion().thenApply(outcome -> outcome.isSuccess()
                    ? outcome.value().get() : failed(check.id())).toCompletableFuture());
        }
        final CompletableFuture<?>[] array = futures.toArray(new CompletableFuture<?>[futures.size()]);
        return CompletableFuture.allOf(array).thenApply(ignored -> {
            final List<PluginDoctor.Result> results = new ArrayList<PluginDoctor.Result>(futures.size());
            for (CompletableFuture<PluginDoctor.Result> future : futures) { results.add(future.join()); }
            return new PluginDoctor.Report(timeSource.now(), results);
        });
    }
    private PluginDoctor.Result sanitize(final PluginDoctor.Result result) {
        Objects.requireNonNull(result, "result");
        if (result.evidence().size() > maximumEvidenceFields) {
            throw new IllegalStateException("doctor evidence capacity exceeded");
        }
        final List<Diagnostics.Field> evidence = new ArrayList<Diagnostics.Field>();
        for (Diagnostics.Field field : result.evidence()) {
            if (field.classification() != Diagnostics.Classification.PUBLIC) { continue; }
            final String value = sanitizer.sanitize(field.value());
            if (value == null || sanitizer.containsSensitiveValue(value)) {
                throw new SecurityException("doctor evidence sanitization failed");
            }
            evidence.add(new Diagnostics.Field(field.id(), value, Diagnostics.Classification.PUBLIC));
        }
        return new PluginDoctor.Result(result.checkId(), result.status(), result.reasonCode(), evidence);
    }
    private static PluginDoctor.Result failed(final DefinitionId checkId) {
        return new PluginDoctor.Result(checkId, Health.Status.UNAVAILABLE, CHECK_FAILED,
                Collections.<Diagnostics.Field>emptyList());
    }
}
