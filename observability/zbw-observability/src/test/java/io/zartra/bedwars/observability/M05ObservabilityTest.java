package io.zartra.bedwars.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.application.scheduler.BoundedTaskScheduler;
import io.zartra.bedwars.observability.diagnostic.SafeDiagnosticExporter;
import io.zartra.bedwars.observability.doctor.PluginDoctorEngine;
import io.zartra.bedwars.observability.health.BoundedHealthRegistry;
import io.zartra.bedwars.observability.metrics.BoundedMetricRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class M05ObservabilityTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeSource TIME = TimeSource.FixedTimeSource.at(NOW);
    private static final DefinitionId COMPONENT = DefinitionId.of("zartra", "component");
    private static final DefinitionId REASON = DefinitionId.of("zartra", "ready");
    private static final DefinitionId FIELD = DefinitionId.of("zartra", "thread-count");

    @Test void healthRegistryBoundsDuplicatesAndIsolatesFailures() {
        final BoundedHealthRegistry registry = new BoundedHealthRegistry(3, TIME);
        registry.register(source(COMPONENT, Health.Status.HEALTHY));
        registry.register(new Health.Source() {
            @Override public DefinitionId id() { return DefinitionId.of("zartra", "failed"); }
            @Override public Health.Snapshot snapshot() { throw new IllegalStateException("failure"); }
        });
        assertEquals(2, registry.size());
        final List<Health.Snapshot> snapshots = registry.snapshots();
        assertEquals(2, snapshots.size());
        assertEquals(Health.Status.UNAVAILABLE, snapshots.get(1).status());
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(source(COMPONENT, Health.Status.DEGRADED)));
        registry.register(source(DefinitionId.of("zartra", "third"), Health.Status.DEGRADED));
        assertThrows(IllegalStateException.class, () -> registry.register(
                source(DefinitionId.of("zartra", "fourth"), Health.Status.HEALTHY)));
        assertTrue(registry.unregister(COMPONENT));
        assertFalse(registry.unregister(COMPONENT));

        final BoundedHealthRegistry wrong = new BoundedHealthRegistry(1, TIME);
        wrong.register(new Health.Source() {
            @Override public DefinitionId id() { return COMPONENT; }
            @Override public Health.Snapshot snapshot() {
                return new Health.Snapshot(DefinitionId.of("zartra", "other"),
                        Health.Status.HEALTHY, REASON, NOW);
            }
        });
        assertThrows(IllegalStateException.class, wrong::snapshots);
        assertThrows(IllegalArgumentException.class, () -> new BoundedHealthRegistry(0, TIME));
    }

    @Test void metricRegistryEnforcesCardinalityAndDimensionLimits() {
        final BoundedMetricRegistry registry = new BoundedMetricRegistry(2, 1);
        final Map<String, String> ready = Collections.singletonMap("state", "ready");
        registry.increment(COMPONENT, ready, 2L);
        registry.increment(COMPONENT, ready, 3L);
        registry.set(DefinitionId.of("zartra", "queue"), Collections.<String, String>emptyMap(), 7L);
        assertEquals(2, registry.size());
        assertEquals(5L, registry.snapshots().get(0).value());
        assertEquals(7L, registry.snapshots().get(1).value());
        assertThrows(IllegalStateException.class, () -> registry.increment(
                DefinitionId.of("zartra", "third"), ready, 1L));
        final Map<String, String> excessive = new HashMap<String, String>();
        excessive.put("a", "x");
        excessive.put("b", "y");
        assertThrows(IllegalArgumentException.class,
                () -> new BoundedMetricRegistry(1, 1).increment(COMPONENT, excessive, 1L));
        assertThrows(IllegalArgumentException.class, () -> new BoundedMetricRegistry(0, 0));
    }

    @Test void diagnosticsAreAllowlistedBoundedAndSeedSecretSafe() {
        final String seed = "M05-seed-secret-9e107d9d";
        try (SafeDiagnosticExporter.SeededSanitizer sanitizer =
                     new SafeDiagnosticExporter.SeededSanitizer(Collections.singleton(seed))) {
            final SafeDiagnosticExporter exporter = new SafeDiagnosticExporter(
                    2, 3, 128, Collections.singleton(FIELD), sanitizer, TIME);
            exporter.register(contributor(DefinitionId.of("zartra", "runtime"), Arrays.asList(
                    new Diagnostics.Field(FIELD, "value=" + seed,
                            Diagnostics.Classification.PUBLIC),
                    new Diagnostics.Field(DefinitionId.of("zartra", "denied"), seed,
                            Diagnostics.Classification.PUBLIC),
                    new Diagnostics.Field(FIELD, seed, Diagnostics.Classification.SECRET))));
            final Diagnostics.Export result = exporter.export();
            assertEquals(1, result.fields().size());
            assertEquals("value=[redacted]", result.fields().get(0).value());
            assertFalse(result.fields().get(0).value().contains(seed));
            assertEquals(NOW, result.createdAt());
            assertThrows(IllegalArgumentException.class, () -> exporter.register(
                    contributor(DefinitionId.of("zartra", "runtime"), Collections.emptyList())));
            exporter.register(contributor(DefinitionId.of("zartra", "second"), Collections.emptyList()));
            assertThrows(IllegalStateException.class, () -> exporter.register(
                    contributor(DefinitionId.of("zartra", "third"), Collections.emptyList())));
            assertEquals("token=[redacted]", sanitizer.sanitize("token=sensitive"));
        }

        final Diagnostics.Sanitizer unsafe = new Diagnostics.Sanitizer() {
            @Override public String sanitize(final String candidate) { return candidate; }
            @Override public boolean containsSensitiveValue(final String candidate) { return true; }
        };
        final SafeDiagnosticExporter rejected = new SafeDiagnosticExporter(
                1, 1, 10, Collections.singleton(FIELD), unsafe, TIME);
        rejected.register(contributor(COMPONENT, Collections.singletonList(
                new Diagnostics.Field(FIELD, "unsafe", Diagnostics.Classification.PUBLIC))));
        assertThrows(SecurityException.class, rejected::export);
        assertThrows(IllegalArgumentException.class, () -> new SafeDiagnosticExporter(
                0, 1, 1, Collections.singleton(FIELD), unsafe, TIME));
    }

    @Test void diagnosticsRejectContributorAndValueLimitViolations() {
        final SafeDiagnosticExporter.SeededSanitizer sanitizer =
                new SafeDiagnosticExporter.SeededSanitizer(Collections.<String>emptyList());
        final SafeDiagnosticExporter tooMany = new SafeDiagnosticExporter(
                1, 1, 8, Collections.singleton(FIELD), sanitizer, TIME);
        tooMany.register(contributor(COMPONENT, Arrays.asList(
                new Diagnostics.Field(FIELD, "a", Diagnostics.Classification.PUBLIC),
                new Diagnostics.Field(FIELD, "b", Diagnostics.Classification.PUBLIC))));
        assertThrows(IllegalStateException.class, tooMany::export);
        final SafeDiagnosticExporter tooLong = new SafeDiagnosticExporter(
                1, 1, 2, Collections.singleton(FIELD), sanitizer, TIME);
        tooLong.register(contributor(COMPONENT, Collections.singletonList(
                new Diagnostics.Field(FIELD, "long", Diagnostics.Classification.PUBLIC))));
        assertThrows(SecurityException.class, tooLong::export);
        sanitizer.close();
    }

    @Test void pluginDoctorIsolatesFailuresSanitizesEvidenceAndBoundsRegistration()
            throws Exception {
        final String seed = "doctor-seed-secret";
        final SafeDiagnosticExporter.SeededSanitizer sanitizer =
                new SafeDiagnosticExporter.SeededSanitizer(Collections.singleton(seed));
        final BoundedTaskScheduler scheduler = new BoundedTaskScheduler(
                1, 4, "zbw-doctor", () -> 0L, TIME, ignored -> { });
        final PluginDoctorEngine engine = new PluginDoctorEngine(2, 2, scheduler, sanitizer, TIME);
        engine.register(new PluginDoctor.Check() {
            @Override public DefinitionId id() { return DefinitionId.of("zartra", "healthy"); }
            @Override public PluginDoctor.Result inspect(
                    final io.zartra.bedwars.api.scheduler.TaskContext context) {
                return new PluginDoctor.Result(id(), Health.Status.HEALTHY, REASON, Arrays.asList(
                        new Diagnostics.Field(FIELD, seed, Diagnostics.Classification.PUBLIC),
                        new Diagnostics.Field(FIELD, "private", Diagnostics.Classification.PRIVATE)));
            }
        });
        engine.register(new PluginDoctor.Check() {
            @Override public DefinitionId id() { return DefinitionId.of("zartra", "throws"); }
            @Override public PluginDoctor.Result inspect(
                    final io.zartra.bedwars.api.scheduler.TaskContext context) {
                throw new IllegalStateException("check failed");
            }
        });
        final PluginDoctor.Report report = engine.run(Duration.ofSeconds(1))
                .toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(2, report.results().size());
        assertEquals(Health.Status.HEALTHY, report.results().get(0).status());
        assertEquals("[redacted]", report.results().get(0).evidence().get(0).value());
        assertEquals(Health.Status.UNAVAILABLE, report.results().get(1).status());
        assertThrows(IllegalArgumentException.class, () -> engine.register(
                check(DefinitionId.of("zartra", "healthy"))));
        assertThrows(IllegalStateException.class, () -> engine.register(
                check(DefinitionId.of("zartra", "third"))));
        assertThrows(IllegalArgumentException.class, () -> engine.run(Duration.ZERO));
        scheduler.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
        sanitizer.close();
    }

    @Test void pluginDoctorRejectsExcessEvidenceThroughStructuredFailure() throws Exception {
        final SafeDiagnosticExporter.SeededSanitizer sanitizer =
                new SafeDiagnosticExporter.SeededSanitizer(Collections.<String>emptyList());
        final BoundedTaskScheduler scheduler = new BoundedTaskScheduler(
                1, 2, "zbw-doctor-bound", () -> 0L, TIME, ignored -> { });
        final PluginDoctorEngine engine = new PluginDoctorEngine(1, 0, scheduler, sanitizer, TIME);
        engine.register(new PluginDoctor.Check() {
            @Override public DefinitionId id() { return COMPONENT; }
            @Override public PluginDoctor.Result inspect(
                    final io.zartra.bedwars.api.scheduler.TaskContext context) {
                return new PluginDoctor.Result(id(), Health.Status.HEALTHY, REASON,
                        Collections.singletonList(new Diagnostics.Field(
                                FIELD, "one", Diagnostics.Classification.PUBLIC)));
            }
        });
        final PluginDoctor.Report report = engine.run(Duration.ofSeconds(1))
                .toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(Health.Status.UNAVAILABLE, report.results().get(0).status());
        scheduler.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
        sanitizer.close();
        assertThrows(IllegalArgumentException.class,
                () -> new PluginDoctorEngine(0, 0, scheduler, sanitizer, TIME));
    }

    private static Health.Source source(final DefinitionId id, final Health.Status status) {
        return new Health.Source() {
            @Override public DefinitionId id() { return id; }
            @Override public Health.Snapshot snapshot() {
                return new Health.Snapshot(id, status, REASON, NOW);
            }
        };
    }

    private static Diagnostics.Contributor contributor(
            final DefinitionId id, final List<Diagnostics.Field> fields) {
        return new Diagnostics.Contributor() {
            @Override public DefinitionId id() { return id; }
            @Override public List<Diagnostics.Field> fields() { return fields; }
        };
    }

    private static PluginDoctor.Check check(final DefinitionId id) {
        return new PluginDoctor.Check() {
            @Override public DefinitionId id() { return id; }
            @Override public PluginDoctor.Result inspect(
                    final io.zartra.bedwars.api.scheduler.TaskContext context) {
                return new PluginDoctor.Result(id, Health.Status.HEALTHY, REASON,
                        Collections.<Diagnostics.Field>emptyList());
            }
        };
    }
}
