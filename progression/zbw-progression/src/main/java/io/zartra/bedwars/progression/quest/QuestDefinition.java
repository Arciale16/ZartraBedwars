package io.zartra.bedwars.progression.quest;

import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable quest definition built on shared objectives and M12 rewards. */
public final class QuestDefinition {
    /** Assignment cadence. */
    public enum Schedule {
        /** Every day. */ DAILY, /** Every week. */ WEEKLY, /** Every month. */ MONTHLY,
        /** During a season. */ SEASONAL, /** During an event. */ EVENT,
        /** Assigned once. */ ONE_TIME, /** Administrator controlled. */ CUSTOM
    }

    /** Reward claim behavior. */
    public enum ClaimPolicy {
        /** Player explicitly claims. */ MANUAL, /** Completion claims automatically. */ AUTOMATIC
    }

    private final QuestId id;
    private final int version;
    private final Schedule schedule;
    private final ObjectiveId objectiveId;
    private final List<RewardId> rewards;
    private final ClaimPolicy claimPolicy;
    private final Optional<Duration> cooldown;
    private final boolean repeatable;
    private final boolean hidden;

    /** Creates a validated quest definition. */
    public QuestDefinition(final QuestId id, final int version, final Schedule schedule,
                           final ObjectiveId objectiveId, final List<RewardId> rewards,
                           final ClaimPolicy claimPolicy, final Optional<Duration> cooldown,
                           final boolean repeatable, final boolean hidden) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.schedule = Objects.requireNonNull(schedule, "schedule");
        this.objectiveId = Objects.requireNonNull(objectiveId, "objectiveId");
        final List<RewardId> copy = new ArrayList<RewardId>(Objects.requireNonNull(rewards, "rewards"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("rewards must not be empty or contain null");
        }
        this.rewards = Collections.unmodifiableList(copy);
        this.claimPolicy = Objects.requireNonNull(claimPolicy, "claimPolicy");
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        if (cooldown.isPresent() && (cooldown.get().isZero() || cooldown.get().isNegative())) {
            throw new IllegalArgumentException("cooldown must be positive");
        }
        this.repeatable = repeatable;
        this.hidden = hidden;
    }

    /** @return quest identity */ public QuestId id() { return id; }
    /** @return schema version */ public int version() { return version; }
    /** @return assignment schedule */ public Schedule schedule() { return schedule; }
    /** @return shared objective identity */ public ObjectiveId objectiveId() { return objectiveId; }
    /** @return immutable M12 reward identities */ public List<RewardId> rewards() { return rewards; }
    /** @return claim policy */ public ClaimPolicy claimPolicy() { return claimPolicy; }
    /** @return optional repeat cooldown */ public Optional<Duration> cooldown() { return cooldown; }
    /** @return whether assignment may repeat */ public boolean repeatable() { return repeatable; }
    /** @return whether undiscovered assignments are hidden */ public boolean hidden() { return hidden; }
}
