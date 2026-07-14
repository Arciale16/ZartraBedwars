package io.zartra.bedwars.integration.discord.api;

import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Public Discord provider SPI used by embedded webhook, external-bot and custom providers.
 * Delivery runs asynchronously and must never block or mutate gameplay.
 */
public interface DiscordProvider extends Provider {
    /**
     * Accepts an already scoped and redacted envelope.
     *
     * @return asynchronous typed delivery classification; a provider exception is an
     * implementation defect and is isolated by the gateway
     */
    CompletionStage<Result<DeliveryResult>> deliver(
            DiscordEventEnvelope<? extends DiscordEventEnvelope.Payload> envelope);

    /** @return validated extension metadata for a custom provider, or built-in metadata */
    ExtensionMetadata extensionMetadata();

    /** Immutable delivery classification used by bounded retry policy. */
    final class DeliveryResult {
        private final Classification classification;
        private final String diagnosticCode;
        private DeliveryResult(final Classification classification, final String diagnosticCode) {
            this.classification = Objects.requireNonNull(classification, "classification");
            if (diagnosticCode == null || !diagnosticCode.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
                throw new IllegalArgumentException("diagnosticCode must be a safe stable key");
            }
            this.diagnosticCode = diagnosticCode;
        }
        /** @return delivery classification */ public static DeliveryResult of(final Classification classification, final String diagnosticCode) { return new DeliveryResult(classification, diagnosticCode); }
        /** @return retry/policy classification */ public Classification classification() { return classification; }
        /** @return sanitized diagnostic key */ public String diagnosticCode() { return diagnosticCode; }
    }

    /** Delivery result and retry classification. */
    enum Classification {
        /** Provider durably accepted the event. */ DELIVERED,
        /** Provider already accepted the same idempotency key. */ DUPLICATE,
        /** Bounded retry may succeed before the envelope deadline. */ RETRYABLE_FAILURE,
        /** Retrying the unchanged envelope cannot succeed. */ PERMANENT_FAILURE,
        /** Gateway/provider policy intentionally rejected the envelope. */ REJECTED_BY_POLICY
    }
}
