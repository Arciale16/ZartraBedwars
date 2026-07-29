package io.zartra.bedwars.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.anticheat.AntiCheatProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.integration.grim.GrimAlertAdapter;
import io.zartra.bedwars.integration.vulcan.VulcanAlertAdapter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-INT-009 cross-provider certification without Atlas decision ownership. */
final class M21AntiCheatCertificationTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test void grimOnlyProducesOneNormalizedSignal() {
        assertEquals(1, certify(true, false).size());
    }

    @Test void vulcanOnlyProducesOneNormalizedSignal() {
        assertEquals(1, certify(false, true).size());
    }

    @Test void bothProvidersRemainIndependentSignalSources() {
        final List<AntiCheatProvider.Alert> alerts = certify(true, true);
        assertEquals(2, alerts.size());
        assertEquals("grim:bad-packet", alerts.get(0).checkId().toString());
        assertEquals("vulcan:speed", alerts.get(1).checkId().toString());
    }

    private static List<AntiCheatProvider.Alert> certify(
            final boolean grimEnabled, final boolean vulcanEnabled) {
        final List<AntiCheatProvider.Alert> alerts = new ArrayList<AntiCheatProvider.Alert>();
        if (grimEnabled) {
            final GrimSource source = new GrimSource();
            final GrimAlertAdapter adapter = new GrimAlertAdapter(
                    source, OptionalProviderLifecycle.Probe.AVAILABLE, clock());
            adapter.start().toCompletableFuture().join();
            adapter.subscribe(alerts::add);
            source.sink.onAlert(new GrimAlertAdapter.RawAlert(
                    "grim-1", metadata("grim"), player(), "Bad Packet",
                    BigDecimal.TEN));
        }
        if (vulcanEnabled) {
            final VulcanSource source = new VulcanSource();
            final VulcanAlertAdapter adapter = new VulcanAlertAdapter(
                    source, OptionalProviderLifecycle.Probe.AVAILABLE, clock());
            adapter.start().toCompletableFuture().join();
            adapter.subscribe(alerts::add);
            source.sink.onAlert(new VulcanAlertAdapter.RawAlert(
                    "vulcan-1", metadata("vulcan"), player(), "Speed", 60,
                    BigDecimal.ONE));
        }
        return alerts;
    }

    private static EventMetadata metadata(final String provider) {
        return EventMetadata.of(
                EventId.of(UUID.nameUUIDFromBytes((provider + ":event").getBytes(StandardCharsets.UTF_8))),
                EventTypeId.of(provider, "alert"),
                CorrelationId.of(UUID.nameUUIDFromBytes((provider + ":correlation").getBytes(StandardCharsets.UTF_8))),
                NOW, 1, 1, EventMetadata.ThreadContext.PROVIDER_WORKER);
    }

    private static PlayerId player() {
        return PlayerId.of(new UUID(0, 21));
    }

    private static TimeSource clock() {
        return TimeSource.FixedTimeSource.at(NOW);
    }

    private static final class GrimSource implements GrimAlertAdapter.Source {
        private GrimAlertAdapter.RawSink sink;
        @Override public GrimAlertAdapter.RawSubscription subscribe(
                final GrimAlertAdapter.RawSink value) {
            sink = value;
            return () -> sink = null;
        }
    }

    private static final class VulcanSource implements VulcanAlertAdapter.Source {
        private VulcanAlertAdapter.RawSink sink;
        @Override public VulcanAlertAdapter.RawSubscription subscribe(
                final VulcanAlertAdapter.RawSink value) {
            sink = value;
            return () -> sink = null;
        }
    }
}
