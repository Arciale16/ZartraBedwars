package io.zartra.bedwars.progression.prestige;

import io.zartra.bedwars.progression.model.PrestigeDefinition;
import io.zartra.bedwars.progression.model.PrestigeState;
import io.zartra.bedwars.progression.model.RewardId;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Versioned deterministic prestige transition policy. */
public final class PrestigePolicy {
    private final int version;
    private final Map<Integer, Tier> tiers;

    /** Creates contiguous prestige tiers. */
    public PrestigePolicy(final int version, final Map<Integer, Tier> tiers) {
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.tiers = Collections.unmodifiableMap(new LinkedHashMap<Integer, Tier>(Objects.requireNonNull(tiers, "tiers")));
        if (this.tiers.isEmpty()) { throw new IllegalArgumentException("tiers must not be empty"); }
        for (int index = 1; index <= this.tiers.size(); index++) {
            final Tier tier = this.tiers.get(index);
            if (tier == null || tier.definition().prestige() != index) {
                throw new IllegalArgumentException("tiers must be contiguous");
            }
        }
        this.version = version;
    }

    /** Prepares an atomic transition intent; persistence decides commit or rollback. */
    public Transition transition(final PrestigeState current, final int currentLevel,
                                 final int requestedPrestige, final Instant now) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(now, "now");
        if (requestedPrestige != current.prestige() + 1) {
            throw new IllegalStateException("prestige must advance exactly one tier");
        }
        final Tier tier = tiers.get(requestedPrestige);
        if (tier == null || currentLevel < tier.definition().requiredLevel()) {
            throw new IllegalStateException("prestige requirements not met");
        }
        return new Transition(version, current, new PrestigeState(requestedPrestige, now), tier.rewardId());
    }

    /** @return policy version */ public int version() { return version; }

    /** Prestige definition and generic reward output. */
    public static final class Tier {
        private final PrestigeDefinition definition;
        private final RewardId rewardId;
        /** Creates a tier. */ public Tier(final PrestigeDefinition definition, final RewardId rewardId) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.rewardId = Objects.requireNonNull(rewardId, "rewardId");
        }
        /** @return definition */ public PrestigeDefinition definition() { return definition; }
        /** @return reward */ public RewardId rewardId() { return rewardId; }
    }

    /** Immutable transaction intent. */
    public static final class Transition {
        private final int version;
        private final PrestigeState before;
        private final PrestigeState after;
        private final RewardId rewardId;
        private Transition(final int version, final PrestigeState before, final PrestigeState after,
                           final RewardId rewardId) {
            this.version = version;
            this.before = before;
            this.after = after;
            this.rewardId = rewardId;
        }
        /** @return policy version */ public int version() { return version; }
        /** @return prior state */ public PrestigeState before() { return before; }
        /** @return proposed state */ public PrestigeState after() { return after; }
        /** @return reward output */ public RewardId rewardId() { return rewardId; }
    }
}
