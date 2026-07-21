package io.zartra.bedwars.progression.challenge;

import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable timed challenge definition backed by a shared objective. */
public final class ChallengeDefinition {
    /** Supported challenge rotation variants. */
    public enum Variant {
        /** Daily rotation. */ DAILY, /** Weekly rotation. */ WEEKLY,
        /** Season duration. */ SEASONAL, /** Rotating pool. */ ROTATING,
        /** Failure-sensitive rules. */ HARDCORE, /** Mode-scoped rules. */ MODE,
        /** Private-game rules. */ PRIVATE, /** Party aggregate. */ PARTY,
        /** Community aggregate. */ COMMUNITY, /** Streak objective. */ STREAK,
        /** Explicit timer. */ TIMED
    }

    private final ChallengeId id;
    private final int version;
    private final Variant variant;
    private final ObjectiveId objectiveId;
    private final Duration duration;
    private final List<RewardId> rewards;

    /** Creates a validated challenge definition. */
    public ChallengeDefinition(final ChallengeId id, final int version, final Variant variant,
                               final ObjectiveId objectiveId, final Duration duration,
                               final List<RewardId> rewards) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.variant = Objects.requireNonNull(variant, "variant");
        this.objectiveId = Objects.requireNonNull(objectiveId, "objectiveId");
        this.duration = Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        final List<RewardId> copy = new ArrayList<RewardId>(Objects.requireNonNull(rewards, "rewards"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("rewards must not be empty or contain null");
        }
        this.rewards = Collections.unmodifiableList(copy);
    }

    /** @return challenge identity */ public ChallengeId id() { return id; }
    /** @return schema version */ public int version() { return version; }
    /** @return rotation variant */ public Variant variant() { return variant; }
    /** @return shared objective identity */ public ObjectiveId objectiveId() { return objectiveId; }
    /** @return active duration */ public Duration duration() { return duration; }
    /** @return immutable M12 rewards */ public List<RewardId> rewards() { return rewards; }
}
