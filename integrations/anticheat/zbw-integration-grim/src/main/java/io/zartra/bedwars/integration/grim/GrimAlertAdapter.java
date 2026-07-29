package io.zartra.bedwars.integration.grim;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.anticheat.AntiCheatProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Event-push Grim adapter that normalizes alerts without performing anticheat checks. */
public final class GrimAlertAdapter implements AntiCheatProvider {
    private static final int MAX_DEDUPLICATION = 4096;
    private final Source source;
    private final OptionalProviderLifecycle lifecycle;
    private final Map<String, Boolean> delivered =
            new LinkedHashMap<String, Boolean>(64, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                @Override protected boolean removeEldestEntry(
                        final Map.Entry<String, Boolean> row) {
                    return size() > MAX_DEDUPLICATION;
                }
            };
    private Subscription subscription;

    /** @param source operator runtime event source @param probe availability
     * @param timeSource health clock */
    public GrimAlertAdapter(final Source source,
                            final OptionalProviderLifecycle.Probe probe,
                            final TimeSource timeSource) {
        this.source = Objects.requireNonNull(source, "source");
        lifecycle = new OptionalProviderLifecycle(ProviderId.of("zartra", "grim"),
                SemanticVersion.parse("2.3.72"),
                CapabilitySet.of(Collections.singletonList(
                        CapabilityId.of("zartra", "anticheat-alert"))),
                timeSource, "provider.grim", probe);
    }

    @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
    @Override public Health health() { return lifecycle.health(); }
    @Override public CompletionStage<Result<LifecycleState>> start() { return lifecycle.start(); }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        closeSubscription();
        return lifecycle.drain(deadline);
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() {
        closeSubscription();
        return lifecycle.stop();
    }

    @Override
    public synchronized Result<Subscription> subscribe(final AlertSink sink) {
        Objects.requireNonNull(sink, "sink");
        if (!lifecycle.available()) { return failure("unavailable"); }
        if (subscription != null) { return failure("duplicate-subscription"); }
        try {
            final RawSubscription raw = source.subscribe(alert -> deliver(alert, sink));
            subscription = () -> {
                synchronized (GrimAlertAdapter.this) {
                    raw.close();
                    subscription = null;
                    delivered.clear();
                }
            };
            return Result.success(subscription);
        } catch (RuntimeException failure) {
            return failure("binding-failed");
        }
    }

    private void deliver(final RawAlert raw, final AlertSink sink) {
        Objects.requireNonNull(raw, "raw");
        synchronized (this) {
            if (!lifecycle.available() || delivered.put(raw.alertId(), Boolean.TRUE) != null) {
                return;
            }
        }
        sink.onAlert(Alert.of(raw.metadata(), raw.playerId(),
                DefinitionId.of("grim", normalize(raw.checkName())),
                severity(raw.violationLevel()), raw.violationLevel()));
    }

    private synchronized void closeSubscription() {
        if (subscription != null) { subscription.close(); }
    }

    private static Severity severity(final BigDecimal level) {
        if (level.compareTo(BigDecimal.ONE) <= 0) { return Severity.INFORMATIONAL; }
        if (level.compareTo(BigDecimal.valueOf(5)) < 0) { return Severity.LOW; }
        if (level.compareTo(BigDecimal.TEN) < 0) { return Severity.MEDIUM; }
        if (level.compareTo(BigDecimal.valueOf(20)) < 0) { return Severity.HIGH; }
        return Severity.CRITICAL;
    }

    private static String normalize(final String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]+", "-").replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "unknown" : normalized.substring(
                0, Math.min(normalized.length(), 120));
    }

    private static <T> Result<T> failure(final String reason) {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "provider/grim-" + reason),
                "provider.grim_" + reason.replace('-', '_'),
                ApiError.RetryDisposition.RETRYABLE));
    }

    /** Operator runtime event source; implementations must push and never poll. */
    public interface Source {
        /** Registers one bounded callback. */ RawSubscription subscribe(RawSink sink);
    }
    /** Raw vendor callback. */
    public interface RawSink {
        /** @param alert validated raw alert */ void onAlert(RawAlert alert);
    }
    /** Idempotent vendor subscription. */
    public interface RawSubscription {
        /** Stops callbacks. */ void close();
    }

    /** Immutable minimal Grim signal before normalization. */
    public static final class RawAlert {
        private final String alertId;
        private final EventMetadata metadata;
        private final PlayerId playerId;
        private final String checkName;
        private final BigDecimal violationLevel;

        /** @param alertId stable deduplication ID @param metadata event metadata
         * @param playerId player @param checkName vendor check name
         * @param violationLevel non-negative violation level */
        public RawAlert(final String alertId, final EventMetadata metadata,
                        final PlayerId playerId, final String checkName,
                        final BigDecimal violationLevel) {
            if (alertId == null || !alertId.matches("[A-Za-z0-9_.:-]{1,128}")) {
                throw new IllegalArgumentException("alertId must be safe");
            }
            this.alertId = alertId;
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            if (checkName == null || checkName.isEmpty() || checkName.length() > 128) {
                throw new IllegalArgumentException("checkName must be bounded");
            }
            this.checkName = checkName;
            this.violationLevel = Objects.requireNonNull(violationLevel, "violationLevel");
            if (violationLevel.signum() < 0 || violationLevel.precision() > 34) {
                throw new IllegalArgumentException("violationLevel must be bounded");
            }
        }
        /** @return alert ID */ public String alertId() { return alertId; }
        /** @return metadata */ public EventMetadata metadata() { return metadata; }
        /** @return player */ public PlayerId playerId() { return playerId; }
        /** @return vendor check */ public String checkName() { return checkName; }
        /** @return violation level */ public BigDecimal violationLevel() { return violationLevel; }
    }
}
