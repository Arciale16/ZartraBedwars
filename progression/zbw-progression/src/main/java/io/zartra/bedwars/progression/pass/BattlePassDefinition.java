package io.zartra.bedwars.progression.pass;

import io.zartra.bedwars.progression.model.RewardId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable versioned battle-pass season with free and premium reward tracks. */
public final class BattlePassDefinition {
    private final SeasonId id;
    private final int version;
    private final Instant startsAt;
    private final Instant endsAt;
    private final Instant graceEndsAt;
    private final List<Tier> tiers;

    /** Creates a validated season definition. */
    public BattlePassDefinition(final SeasonId id, final int version, final Instant startsAt,
                                final Instant endsAt, final Instant graceEndsAt,
                                final List<Tier> tiers) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt");
        this.graceEndsAt = Objects.requireNonNull(graceEndsAt, "graceEndsAt");
        if (!endsAt.isAfter(startsAt) || graceEndsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("season and grace windows are invalid");
        }
        final List<Tier> copy = new ArrayList<Tier>(Objects.requireNonNull(tiers, "tiers"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("tiers must not be empty or contain null");
        }
        long previousXp = -1;
        int previousNumber = 0;
        for (Tier tier : copy) {
            if (tier.number() != previousNumber + 1 || tier.requiredXp() <= previousXp) {
                throw new IllegalArgumentException("tiers must be sequential with increasing XP");
            }
            previousNumber = tier.number();
            previousXp = tier.requiredXp();
        }
        this.tiers = Collections.unmodifiableList(copy);
    }

    /** @return season identity */ public SeasonId id() { return id; }
    /** @return schema version */ public int version() { return version; }
    /** @return season start */ public Instant startsAt() { return startsAt; }
    /** @return season end */ public Instant endsAt() { return endsAt; }
    /** @return claim grace deadline */ public Instant graceEndsAt() { return graceEndsAt; }
    /** @return ordered immutable tiers */ public List<Tier> tiers() { return tiers; }

    /** One reward-bearing pass tier. */
    public static final class Tier {
        private final int number;
        private final long requiredXp;
        private final List<RewardId> freeRewards;
        private final List<RewardId> premiumRewards;

        /** Creates a validated pass tier. */
        public Tier(final int number, final long requiredXp, final List<RewardId> freeRewards,
                    final List<RewardId> premiumRewards) {
            if (number < 1 || requiredXp < 0) {
                throw new IllegalArgumentException("number must be positive and XP non-negative");
            }
            this.number = number;
            this.requiredXp = requiredXp;
            this.freeRewards = immutableRewards(freeRewards, "freeRewards");
            this.premiumRewards = immutableRewards(premiumRewards, "premiumRewards");
            if (this.freeRewards.isEmpty() && this.premiumRewards.isEmpty()) {
                throw new IllegalArgumentException("at least one reward track must be populated");
            }
        }

        /** @return one-based tier number */ public int number() { return number; }
        /** @return cumulative pass XP */ public long requiredXp() { return requiredXp; }
        /** @return immutable free-track M12 rewards */ public List<RewardId> freeRewards() { return freeRewards; }
        /** @return immutable premium-track M12 rewards */
        public List<RewardId> premiumRewards() { return premiumRewards; }

        private static List<RewardId> immutableRewards(final List<RewardId> rewards, final String name) {
            final List<RewardId> copy = new ArrayList<RewardId>(Objects.requireNonNull(rewards, name));
            if (copy.contains(null)) { throw new IllegalArgumentException(name + " must not contain null"); }
            return Collections.unmodifiableList(copy);
        }
    }
}
