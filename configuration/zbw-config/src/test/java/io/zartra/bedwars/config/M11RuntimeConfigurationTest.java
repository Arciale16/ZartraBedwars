package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.config.m11.M11RuntimeConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class M11RuntimeConfigurationTest {
    @Test void migratesAndActivatesAllSectionsDeterministically() {
        final AtomicInteger applied = new AtomicInteger();
        final M11RuntimeConfiguration runtime = runtime(applied, false);
        final List<M11RuntimeConfiguration.Document> candidate = documents(1, "new");
        final M11RuntimeConfiguration.Activation result = runtime.activate(candidate);
        assertTrue(result.success());
        assertEquals(1, result.snapshot().revision());
        assertEquals(8, applied.get());
        assertEquals(2, result.snapshot().document(M11RuntimeConfiguration.Section.SHOPS).version());
        assertEquals("new-migrated", result.snapshot().document(M11RuntimeConfiguration.Section.SHOPS).values().values().iterator().next());
        assertEquals(M11RuntimeConfiguration.SimulationResult.success(8), runtime.simulate(snapshot -> M11RuntimeConfiguration.SimulationResult.success(snapshot.documents().size())));
    }
    @Test void validationAndApplyFailureRetainLastKnownGood() {
        final AtomicInteger applied = new AtomicInteger();
        final M11RuntimeConfiguration runtime = runtime(applied, true);
        final List<M11RuntimeConfiguration.Document> invalid = documents(1, "invalid");
        assertFalse(runtime.activate(invalid).success());
        assertEquals(0, runtime.active().revision());
        final M11RuntimeConfiguration.Activation failed = runtime.activate(documents(1, "new"));
        assertFalse(failed.success());
        assertEquals("apply_failed", failed.code());
        assertEquals(0, runtime.active().revision());
    }
    @Test void rejectsIncompleteDuplicateFutureAndNondeterministicInputs() {
        final AtomicInteger applied = new AtomicInteger();
        final M11RuntimeConfiguration runtime = runtime(applied, false);
        final List<M11RuntimeConfiguration.Document> incomplete = documents(1, "x");
        incomplete.remove(0);
        assertFalse(runtime.activate(incomplete).success());
        final List<M11RuntimeConfiguration.Document> duplicate = documents(1, "x");
        duplicate.add(duplicate.get(0));
        assertThrows(IllegalArgumentException.class, () -> runtime.activate(duplicate));
        assertFalse(runtime.activate(documents(3, "x")).success());
        final AtomicInteger sequence = new AtomicInteger();
        assertEquals("nondeterministic_simulation", runtime.simulate(snapshot -> M11RuntimeConfiguration.SimulationResult.success(sequence.incrementAndGet())).code());
        assertThrows(IllegalArgumentException.class, () -> new M11RuntimeConfiguration.Document(M11RuntimeConfiguration.Section.SHOPS, 0, Collections.emptyMap()));
    }
    private static M11RuntimeConfiguration runtime(AtomicInteger applied, boolean failApply) {
        final List<M11RuntimeConfiguration.Schema> schemas = new ArrayList<>();
        final List<M11RuntimeConfiguration.Participant> participants = new ArrayList<>();
        for (M11RuntimeConfiguration.Section section : M11RuntimeConfiguration.Section.values()) {
            schemas.add(new M11RuntimeConfiguration.Schema(section, 2, Collections.singletonList(new M11RuntimeConfiguration.Migration() {
                public int fromVersion() { return 1;
        } public int toVersion() { return 2;
        }
                public M11RuntimeConfiguration.Document migrate(M11RuntimeConfiguration.Document source) {
                    final Map<DefinitionId,String> values = new java.util.LinkedHashMap<>();
                    source.values().forEach((key,value) -> values.put(key, value + "-migrated"));
                    return new M11RuntimeConfiguration.Document(section, 2, values);
                }
            }), document -> document.values().containsValue("invalid-migrated") ? Optional.of("invalid_value") : Optional.empty()));
            participants.add(new M11RuntimeConfiguration.Participant() {
                public M11RuntimeConfiguration.Section section() { return section;
        }
                public M11RuntimeConfiguration.Prepared prepare(M11RuntimeConfiguration.Snapshot current, M11RuntimeConfiguration.Snapshot candidate) {
                    return new M11RuntimeConfiguration.Prepared() { public void apply() { if (failApply && section == M11RuntimeConfiguration.Section.TRAPS) {
                            throw new IllegalStateException("failure");
                        }
        applied.incrementAndGet();
        } public void rollback() { applied.decrementAndGet();
        } };
                }
            });
        }
        return new M11RuntimeConfiguration(schemas, participants, snapshot(0, documents(2, "old")));
    }
    private static M11RuntimeConfiguration.Snapshot snapshot(long revision, List<M11RuntimeConfiguration.Document> documents) {
        final Map<M11RuntimeConfiguration.Section,M11RuntimeConfiguration.Document> values = new EnumMap<>(M11RuntimeConfiguration.Section.class);
        documents.forEach(document -> values.put(document.section(), document));
        return new M11RuntimeConfiguration.Snapshot(revision, values);
        }
    private static List<M11RuntimeConfiguration.Document> documents(int version, String value) {
        final List<M11RuntimeConfiguration.Document> result = new ArrayList<>();
        for (M11RuntimeConfiguration.Section section : M11RuntimeConfiguration.Section.values()) {
            result.add(new M11RuntimeConfiguration.Document(section, version, Collections.singletonMap(DefinitionId.of("zartra", "config/" + section.name().toLowerCase()), value)));
        }
        return result;
        }
}
