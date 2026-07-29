package io.zartra.bedwars.integration.vulcan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.anticheat.AntiCheatProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Proprietary-class-free Vulcan normalization tests. */
final class VulcanAlertAdapterTest {
    @Test void mapsEntitledRuntimeSignalWithoutVendorBinary() {
        CapturingSource source = new CapturingSource();
        VulcanAlertAdapter adapter = new VulcanAlertAdapter(source,
                OptionalProviderLifecycle.Probe.AVAILABLE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        adapter.start().toCompletableFuture().join();
        List<AntiCheatProvider.Alert> alerts = new ArrayList<AntiCheatProvider.Alert>();
        adapter.subscribe(alerts::add);
        source.sink.onAlert(new VulcanAlertAdapter.RawAlert("vulcan-1", metadata(),
                PlayerId.of(new UUID(4, 4)), "Speed Type B", 85,
                BigDecimal.valueOf(8)));
        assertEquals(1, alerts.size());
        assertEquals("vulcan:speed-type-b", alerts.get(0).checkId().toString());
        assertEquals(AntiCheatProvider.Severity.CRITICAL, alerts.get(0).severity());
    }

    private static EventMetadata metadata() {
        return EventMetadata.of(EventId.of(new UUID(5, 5)),
                EventTypeId.of("vulcan", "alert"), CorrelationId.of(new UUID(6, 6)),
                Instant.EPOCH, 1, 1, EventMetadata.ThreadContext.PROVIDER_WORKER);
    }

    private static final class CapturingSource implements VulcanAlertAdapter.Source {
        private VulcanAlertAdapter.RawSink sink;
        @Override public VulcanAlertAdapter.RawSubscription subscribe(
                final VulcanAlertAdapter.RawSink value) {
            sink = value;
            return () -> sink = null;
        }
    }
}
