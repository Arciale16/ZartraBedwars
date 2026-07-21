package io.zartra.bedwars.progression.achievement;

import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable tiered achievement definition that reuses a shared objective. */
public final class AchievementDefinition {
    private final AchievementId id;
    private final int version;
    private final String category;
    private final ObjectiveId objectiveId;
    private final List<Tier> tiers;
    private final boolean hidden;
    private final boolean repeatable;

    /** Creates a validated achievement definition. */
    public AchievementDefinition(final AchievementId id, final int version, final String category,
                                 final ObjectiveId objectiveId, final List<Tier> tiers,
                                 final boolean hidden, final boolean repeatable) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        if (category == null || category.trim().isEmpty() || category.length() > 64) {
            throw new IllegalArgumentException("category must contain 1..64 characters");
        }
        this.version = version;
        this.category = category;
        this.objectiveId = Objects.requireNonNull(objectiveId, "objectiveId");
        final List<Tier> copy = new ArrayList<Tier>(Objects.requireNonNull(tiers, "tiers"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("tiers must not be empty or contain null");
        }
        long previous = 0;
        int expectedTier = 1;
        for (Tier tier : copy) {
            if (tier.number() != expectedTier || tier.target() <= previous) {
                throw new IllegalArgumentException("tiers must be sequential with increasing targets");
            }
            expectedTier++;
            previous = tier.target();
        }
        this.tiers = Collections.unmodifiableList(copy);
        this.hidden = hidden;
        this.repeatable = repeatable;
    }

    /** @return achievement identity */ public AchievementId id() { return id; }
    /** @return schema version */ public int version() { return version; }
    /** @return category identity */ public String category() { return category; }
    /** @return shared objective identity */ public ObjectiveId objectiveId() { return objectiveId; }
    /** @return ordered immutable tiers */ public List<Tier> tiers() { return tiers; }
    /** @return whether undiscovered progress is hidden */ public boolean hidden() { return hidden; }
    /** @return whether the final tier may reset */ public boolean repeatable() { return repeatable; }

    /** One monotonic achievement tier. */
    public static final class Tier {
        private final int number;
        private final long target;
        private final int points;
        private final List<RewardId> rewards;

        /** Creates a validated tier. */
        public Tier(final int number, final long target, final int points, final List<RewardId> rewards) {
            if (number < 1 || target < 1 || points < 0) {
                throw new IllegalArgumentException("number/target must be positive and points non-negative");
            }
            this.number = number;
            this.target = target;
            this.points = points;
            final List<RewardId> copy = new ArrayList<RewardId>(Objects.requireNonNull(rewards, "rewards"));
            if (copy.contains(null)) { throw new IllegalArgumentException("rewards must not contain null"); }
            this.rewards = Collections.unmodifiableList(copy);
        }

        /** @return one-based tier number */ public int number() { return number; }
        /** @return monotonic target */ public long target() { return target; }
        /** @return achievement points */ public int points() { return points; }
        /** @return immutable M12 reward identities */ public List<RewardId> rewards() { return rewards; }
    }
}
