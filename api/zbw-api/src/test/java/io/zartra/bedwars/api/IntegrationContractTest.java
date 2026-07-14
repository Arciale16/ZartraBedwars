package io.zartra.bedwars.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.anticheat.AntiCheatProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationContractTest {
    @Test
    void anticheatAlertIsNormalizedImmutableAndBounded() {
        final EventMetadata metadata = EventMetadata.of(
                EventId.of(new UUID(1L, 1L)), EventTypeId.of("anticheat", "alert"),
                CorrelationId.of(new UUID(2L, 2L)), Instant.parse("2026-07-14T12:00:00Z"),
                9L, 1, EventMetadata.ThreadContext.PROVIDER_WORKER);
        final AntiCheatProvider.Alert alert = AntiCheatProvider.Alert.of(
                metadata, PlayerId.of(new UUID(3L, 3L)), DefinitionId.of("vendor", "speed"),
                AntiCheatProvider.Severity.HIGH, new BigDecimal("4.25"));
        assertEquals(metadata, alert.metadata());
        assertEquals("vendor:speed", alert.checkId().toString());
        assertEquals(AntiCheatProvider.Severity.HIGH, alert.severity());
        assertEquals(new BigDecimal("4.25"), alert.violationLevel());
        assertThrows(IllegalArgumentException.class, () -> AntiCheatProvider.Alert.of(
                metadata, alert.playerId(), alert.checkId(), alert.severity(), BigDecimal.ONE.negate()));
        assertThrows(NullPointerException.class, () -> AntiCheatProvider.Alert.of(
                null, alert.playerId(), alert.checkId(), alert.severity(), BigDecimal.ZERO));
    }
}
