package io.zartra.bedwars.api.integration.anticheat;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.math.BigDecimal;
import java.util.Objects;

/** Event-driven, vendor-neutral anticheat provider SPI. */
public interface AntiCheatProvider extends Provider {
    /**
     * Registers one non-blocking sink. Implementations push events and must not poll gameplay.
     *
     * @param sink callback that must return quickly and perform persistence asynchronously
     * @return typed subscription result
     */
    Result<Subscription> subscribe(AlertSink sink);

    /** Normalized alert callback. */
    interface AlertSink {
        /** @param alert immutable normalized alert */
        void onAlert(Alert alert);
    }

    /** Idempotent subscription handle. */
    interface Subscription {
        /** Stops future callbacks; repeated calls have no additional effect. */
        void close();
    }

    /** Immutable normalized anticheat alert. */
    final class Alert {
        private final EventMetadata metadata;
        private final PlayerId playerId;
        private final DefinitionId checkId;
        private final Severity severity;
        private final BigDecimal violationLevel;
        private Alert(final EventMetadata metadata, final PlayerId playerId, final DefinitionId checkId,
                      final Severity severity, final BigDecimal violationLevel) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            this.checkId = Objects.requireNonNull(checkId, "checkId");
            this.severity = Objects.requireNonNull(severity, "severity");
            this.violationLevel = Objects.requireNonNull(violationLevel, "violationLevel");
            if (violationLevel.signum() < 0 || violationLevel.precision() > 34) {
                throw new IllegalArgumentException("violationLevel must be non-negative and bounded");
            }
        }
        /** @return normalized alert */ public static Alert of(final EventMetadata metadata, final PlayerId playerId, final DefinitionId checkId, final Severity severity, final BigDecimal violationLevel) { return new Alert(metadata, playerId, checkId, severity, violationLevel); }
        /** @return event identity, ordering and thread context */ public EventMetadata metadata() { return metadata; }
        /** @return affected player */ public PlayerId playerId() { return playerId; }
        /** @return vendor-neutral namespaced check ID */ public DefinitionId checkId() { return checkId; }
        /** @return normalized severity */ public Severity severity() { return severity; }
        /** @return normalized non-negative violation level */ public BigDecimal violationLevel() { return violationLevel; }
    }

    /** Normalized alert severity. */
    enum Severity { INFORMATIONAL, LOW, MEDIUM, HIGH, CRITICAL }
}
