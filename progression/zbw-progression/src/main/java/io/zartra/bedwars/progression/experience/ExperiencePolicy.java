package io.zartra.bedwars.progression.experience;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Versioned, immutable XP source and anti-farming policy. */
public final class ExperiencePolicy {
    private final int version;
    private final Map<DefinitionId, Source> sources;

    /** Creates a validated policy whose source identifiers are unique. */
    public ExperiencePolicy(final int version, final Map<DefinitionId, Source> sources) {
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<DefinitionId, Source>(
                Objects.requireNonNull(sources, "sources")));
        if (this.sources.isEmpty() || this.sources.containsKey(null)
                || this.sources.containsValue(null)) {
            throw new IllegalArgumentException("sources must be non-empty and contain no nulls");
        }
    }

    /** Calculates a deterministic award using integral basis points. */
    public Award calculate(final DefinitionId sourceId, final long requested,
                           final int multiplierBasisPoints, final long bonus,
                           final int recentAwards, final Instant occurredAt) {
        final Source source = sources.get(Objects.requireNonNull(sourceId, "sourceId"));
        if (source == null || !source.enabled()) { throw new IllegalArgumentException("unknown or disabled XP source"); }
        if (requested <= 0 || multiplierBasisPoints < 0 || bonus < 0 || recentAwards < 0) {
            throw new IllegalArgumentException("invalid XP award input");
        }
        final long bounded = Math.min(requested, source.maximumBaseAward());
        final long multiplied = Math.multiplyExact(bounded, multiplierBasisPoints) / 10_000L;
        long total = Math.addExact(multiplied, bonus);
        if (recentAwards >= source.fullAwardsPerWindow()) {
            total = Math.multiplyExact(total, source.farmingBasisPoints()) / 10_000L;
        }
        total = Math.min(total, source.maximumFinalAward());
        return new Award(sourceId, requested, total, version, Objects.requireNonNull(occurredAt, "occurredAt"));
    }

    /** @return schema version */ public int version() { return version; }
    /** @return immutable source map */ public Map<DefinitionId, Source> sources() { return sources; }

    /** One configurable XP source. */
    public static final class Source {
        private final long maximumBaseAward;
        private final long maximumFinalAward;
        private final int fullAwardsPerWindow;
        private final int farmingBasisPoints;
        private final Duration farmingWindow;
        private final boolean enabled;

        /** Creates a bounded source rule. */
        public Source(final long maximumBaseAward, final long maximumFinalAward,
                      final int fullAwardsPerWindow, final int farmingBasisPoints,
                      final Duration farmingWindow, final boolean enabled) {
            if (maximumBaseAward < 1 || maximumFinalAward < 1 || fullAwardsPerWindow < 1
                    || farmingBasisPoints < 0 || farmingBasisPoints > 10_000
                    || farmingWindow == null || farmingWindow.isZero() || farmingWindow.isNegative()) {
                throw new IllegalArgumentException("invalid XP source policy");
            }
            this.maximumBaseAward = maximumBaseAward;
            this.maximumFinalAward = maximumFinalAward;
            this.fullAwardsPerWindow = fullAwardsPerWindow;
            this.farmingBasisPoints = farmingBasisPoints;
            this.farmingWindow = farmingWindow;
            this.enabled = enabled;
        }
        /** @return base cap */ public long maximumBaseAward() { return maximumBaseAward; }
        /** @return final cap */ public long maximumFinalAward() { return maximumFinalAward; }
        /** @return full awards permitted per window */ public int fullAwardsPerWindow() { return fullAwardsPerWindow; }
        /** @return repeated-award multiplier */ public int farmingBasisPoints() { return farmingBasisPoints; }
        /** @return farming observation window */ public Duration farmingWindow() { return farmingWindow; }
        /** @return whether this source accepts awards */ public boolean enabled() { return enabled; }
    }

    /** Auditable deterministic calculation result. */
    public static final class Award {
        private final DefinitionId sourceId;
        private final long requested;
        private final long awarded;
        private final int policyVersion;
        private final Instant occurredAt;

        private Award(final DefinitionId sourceId, final long requested, final long awarded,
                      final int policyVersion, final Instant occurredAt) {
            this.sourceId = sourceId;
            this.requested = requested;
            this.awarded = awarded;
            this.policyVersion = policyVersion;
            this.occurredAt = occurredAt;
        }
        /** @return XP source */ public DefinitionId sourceId() { return sourceId; }
        /** @return requested XP */ public long requested() { return requested; }
        /** @return awarded XP */ public long awarded() { return awarded; }
        /** @return policy version */ public int policyVersion() { return policyVersion; }
        /** @return occurrence time */ public Instant occurredAt() { return occurredAt; }
    }
}
