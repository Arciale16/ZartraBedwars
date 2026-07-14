package io.zartra.bedwars.integration.discord.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class DiscordContractTest {
    @Test
    void envelopeCarriesTypedIdempotencySensitivityAndDeadline() {
        final EventMetadata metadata = EventMetadata.of(EventId.of(new UUID(1L, 1L)),
                EventTypeId.of("zartra", "match/end"), CorrelationId.of(new UUID(2L, 2L)),
                Instant.parse("2026-07-14T12:00:00Z"), 1L, 1,
                EventMetadata.ThreadContext.PROVIDER_WORKER);
        final TestPayload payload = new TestPayload();
        final DiscordEventEnvelope<TestPayload> envelope = DiscordEventEnvelope.of(metadata,
                IdempotencyKey.of("match", "123e4567-e89b-12d3-a456-426614174000"),
                DiscordEventEnvelope.Sensitivity.PUBLIC, DiscordCapabilities.NOTIFICATIONS,
                Instant.parse("2026-07-14T12:01:00Z"), payload);
        assertEquals(payload, envelope.payload());
        assertEquals("match:123e4567-e89b-12d3-a456-426614174000", envelope.idempotencyKey().toString());
        assertEquals(DiscordEventEnvelope.Sensitivity.PUBLIC, envelope.sensitivity());
        assertThrows(IllegalArgumentException.class, () -> DiscordEventEnvelope.of(metadata,
                envelope.idempotencyKey(), DiscordEventEnvelope.Sensitivity.PUBLIC,
                DiscordCapabilities.NOTIFICATIONS, Instant.parse("2026-07-14T11:59:59Z"), payload));
    }

    @Test
    void queryContextIsScopedImmutableAndRequiresAuthorization() {
        final DiscordIntegrationApi.QueryContext context = DiscordIntegrationApi.QueryContext.of(
                ProviderId.of("test", "bot"), CorrelationId.of(new UUID(3L, 3L)),
                EnumSet.of(DiscordIntegrationApi.Scope.PUBLIC_STATISTICS),
                Instant.parse("2026-07-14T12:00:01Z"));
        assertTrue(context.scopes().contains(DiscordIntegrationApi.Scope.PUBLIC_STATISTICS));
        assertThrows(UnsupportedOperationException.class,
                () -> context.scopes().add(DiscordIntegrationApi.Scope.ACCOUNT_LINK));
        assertThrows(IllegalArgumentException.class, () -> DiscordIntegrationApi.QueryContext.of(
                context.caller(), context.correlationId(), EnumSet.noneOf(DiscordIntegrationApi.Scope.class),
                context.deadline()));
        assertFalse(context.scopes().contains(DiscordIntegrationApi.Scope.ACCOUNT_LINK));
    }

    @Test
    void providerSpiIsAsynchronousAndClassifiesRetryWithoutSecrets() throws Exception {
        assertTrue(Provider.class.isAssignableFrom(DiscordProvider.class));
        final Method delivery = DiscordProvider.class.getMethod("deliver", DiscordEventEnvelope.class);
        assertEquals(CompletionStage.class, delivery.getReturnType());
        final DiscordProvider.DeliveryResult retry = DiscordProvider.DeliveryResult.of(
                DiscordProvider.Classification.RETRYABLE_FAILURE, "discord.timeout");
        assertEquals(DiscordProvider.Classification.RETRYABLE_FAILURE, retry.classification());
        assertEquals("discord.timeout", retry.diagnosticCode());
        assertThrows(IllegalArgumentException.class, () -> DiscordProvider.DeliveryResult.of(
                DiscordProvider.Classification.PERMANENT_FAILURE, "token=secret"));
        assertEquals("zartra:discord/account_linking", DiscordCapabilities.ACCOUNT_LINKING.toString());
    }

    private static final class TestPayload implements DiscordEventEnvelope.Payload {
        @Override public DefinitionId schema() { return DefinitionId.of("test", "payload"); }
        @Override public int schemaVersion() { return 1; }
    }
}
