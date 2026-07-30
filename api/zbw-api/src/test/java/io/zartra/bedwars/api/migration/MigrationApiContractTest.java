package io.zartra.bedwars.api.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MigrationApiContractTest {
    @Test
    void recordsRequestsPlansAndReportsAreImmutableAndDeterministic() {
        final Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("z", "last");
        attributes.put("a", "first");
        final MigrationApi.Record record =
                new MigrationApi.Record("record/one", "source-kind", attributes);
        attributes.clear();
        final MigrationApi.Request request = new MigrationApi.Request(
                "migration/one", "operator-export", "lawful-input",
                MigrationApi.Mode.DRY_RUN, MigrationApi.ConflictPolicy.FAIL,
                Collections.singletonList(record));
        final MigrationApi.Plan plan = new MigrationApi.Plan(
                request.migrationId(), request.mode(), request.records(),
                Arrays.asList("z-finding", "a-finding"), true);
        final MigrationApi.Report report = new MigrationApi.Report(
                request.migrationId(), MigrationApi.Status.PLANNED, 1, 1, plan.findings());

        assertEquals(Arrays.asList("a", "z"),
                new java.util.ArrayList<String>(record.attributes().keySet()));
        assertEquals(Arrays.asList("a-finding", "z-finding"), plan.findings());
        assertEquals(1, report.sourceCount());
        assertEquals(record, new MigrationApi.Record(
                "record/one", "source-kind", record.attributes()));
        assertNotEquals(record, new MigrationApi.Record(
                "record/two", "source-kind", record.attributes()));
        assertThrows(UnsupportedOperationException.class,
                () -> record.attributes().put("x", "y"));
    }

    @Test
    void invalidAndUnsafeBoundariesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MigrationApi.Record(
                "bad id", "kind", Collections.<String, String>emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> new MigrationApi.Request(
                "migration/one", "source", "contains\nnewline", MigrationApi.Mode.APPLY,
                MigrationApi.ConflictPolicy.FAIL, Collections.<MigrationApi.Record>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new MigrationApi.Conversion(
                MigrationApi.ConversionState.UNSUPPORTED,
                Collections.singletonList(new MigrationApi.Record(
                        "record/one", "kind", Collections.<String, String>emptyMap())),
                "unsupported"));
        assertThrows(IllegalArgumentException.class, () -> new MigrationApi.Report(
                "migration/one", MigrationApi.Status.FAILED, -1, 0,
                Collections.<String>emptyList()));
    }
}
