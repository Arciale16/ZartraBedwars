package io.zartra.bedwars.integration.grim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** Grim normalization, duplicate suppression and subscription tests. */
final class GrimAlertAdapterTest {
    @Test void normalizesAndDeduplicatesPushedAlerts() {
        CapturingSource source = new CapturingSource();
        GrimAlertAdapter adapter = new GrimAlertAdapter(source,
                OptionalProviderLifecycle.Probe.AVAILABLE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        adapter.start().toCompletableFuture().join();
        List<AntiCheatProvider.Alert> alerts = new ArrayList<AntiCheatProvider.Alert>();
        assertTrue(adapter.subscribe(alerts::add).isSuccess());
        GrimAlertAdapter.RawAlert raw = new GrimAlertAdapter.RawAlert("alert-1",
                metadata(), PlayerId.of(new UUID(1, 1)), "Bad Packet A",
                BigDecimal.valueOf(12));
        source.sink.onAlert(raw);
        source.sink.onAlert(raw);
        assertEquals(1, alerts.size());
        assertEquals("grim:bad-packet-a", alerts.get(0).checkId().toString());
        assertEquals(AntiCheatProvider.Severity.HIGH, alerts.get(0).severity());
        assertTrue(adapter.subscribe(alerts::add).isFailure());
    }

    private static EventMetadata metadata() {
        return EventMetadata.of(EventId.of(new UUID(2, 2)),
                EventTypeId.of("grim", "alert"), CorrelationId.of(new UUID(3, 3)),
                Instant.EPOCH, 1, 1, EventMetadata.ThreadContext.PROVIDER_WORKER);
    }

    private static final class CapturingSource implements GrimAlertAdapter.Source {
        private GrimAlertAdapter.RawSink sink;
        @Override public GrimAlertAdapter.RawSubscription subscribe(
                final GrimAlertAdapter.RawSink value) {
            sink = value;
            return () -> sink = null;
        }
    }
}
