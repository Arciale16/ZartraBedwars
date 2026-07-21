package io.zartra.bedwars.progression.achievement;

import io.zartra.bedwars.progression.objective.ObjectiveEvent;
import io.zartra.bedwars.progression.objective.ObjectiveExecutionEngine;
import io.zartra.bedwars.progression.runtime.M13RewardIntent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stateless tier-unlock policy over shared objective evaluation. */
public final class AchievementRuntime {
    /** Projects the latest objective value into one-time achievement tiers. */
    public Outcome apply(final AchievementDefinition definition,
                         final AchievementProgress current,
                         final ObjectiveExecutionEngine.Evaluation evaluation,
                         final ObjectiveEvent event, final Instant now) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(evaluation, "evaluation");
        Objects.requireNonNull(event, "event");
        if (!definition.id().equals(current.achievementId())
                || definition.version() != current.definitionVersion()) {
            throw new IllegalArgumentException("definition does not match progress");
        }
        if (current.lastEvent().isPresent() && current.lastEvent().get().equals(event.idempotencyKey())) {
            return new Outcome(current, Collections.<M13RewardIntent>emptyList());
        }
        int tier = current.tier();
        final List<M13RewardIntent> rewards = new ArrayList<M13RewardIntent>();
        for (AchievementDefinition.Tier candidate : definition.tiers()) {
            if (candidate.number() > tier && evaluation.state().value() >= candidate.target()) {
                tier = candidate.number();
                for (int index = 0; index < candidate.rewards().size(); index++) {
                    rewards.add(new M13RewardIntent(candidate.rewards().get(index), current.playerId(),
                            io.zartra.bedwars.api.identity.IdempotencyKey.of("m13", "achievement/"
                                    + definition.id().path() + "/" + tier + "/" + index)));
                }
            }
        }
        final boolean discovered = current.discovered() || tier > 0 || !definition.hidden();
        final AchievementProgress next = new AchievementProgress(current.achievementId(),
                current.playerId(), current.definitionVersion(), tier, evaluation.state().value(),
                discovered, Math.addExact(current.revision(), 1),
                java.util.Optional.of(event.idempotencyKey()), now);
        return new Outcome(next, rewards);
    }

    /** Immutable achievement projection result. */
    public static final class Outcome {
        private final AchievementProgress progress;
        private final List<M13RewardIntent> rewards;
        private Outcome(final AchievementProgress progress, final List<M13RewardIntent> rewards) {
            this.progress = progress;
            this.rewards = Collections.unmodifiableList(new ArrayList<M13RewardIntent>(rewards));
        }
        /** @return resulting progress */ public AchievementProgress progress() { return progress; }
        /** @return newly unlocked tier rewards */ public List<M13RewardIntent> rewards() { return rewards; }
    }
}
