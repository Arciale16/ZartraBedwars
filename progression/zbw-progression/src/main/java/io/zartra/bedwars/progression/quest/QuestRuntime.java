package io.zartra.bedwars.progression.quest;

import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.objective.ObjectiveExecutionEngine;
import io.zartra.bedwars.progression.runtime.M13RewardIntent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stateless quest assignment, expiration, completion and chain policy. */
public final class QuestRuntime {
    /** Assigns a quest with an explicit deterministic validity window. */
    public QuestAssignment assign(final QuestDefinition definition,
                                  final PlayerProgressionId playerId,
                                  final Instant assignedAt, final Instant expiresAt,
                                  final boolean prerequisitesMet) {
        Objects.requireNonNull(definition, "definition");
        return new QuestAssignment(definition.id(), playerId,
                prerequisitesMet ? QuestAssignment.Status.ACTIVE : QuestAssignment.Status.LOCKED,
                assignedAt, expiresAt, 0);
    }

    /** Activates a locked assignment when its prerequisite chain becomes available. */
    public QuestAssignment activate(final QuestAssignment current) {
        Objects.requireNonNull(current, "current");
        if (current.status() != QuestAssignment.Status.LOCKED) { return current; }
        return transition(current, QuestAssignment.Status.ACTIVE);
    }

    /** Applies objective completion or expiration without delivering rewards directly. */
    public Outcome apply(final QuestDefinition definition, final QuestAssignment current,
                         final ObjectiveExecutionEngine.Evaluation evaluation,
                         final Instant now) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(evaluation, "evaluation");
        Objects.requireNonNull(now, "now");
        if (!definition.id().equals(current.questId())) {
            throw new IllegalArgumentException("definition does not match assignment");
        }
        if (!now.isBefore(current.expiresAt()) || evaluation.expired()) {
            return new Outcome(transition(current, QuestAssignment.Status.EXPIRED),
                    Collections.<M13RewardIntent>emptyList());
        }
        if (current.status() != QuestAssignment.Status.ACTIVE || !evaluation.completed()) {
            return new Outcome(current, Collections.<M13RewardIntent>emptyList());
        }
        final QuestAssignment completed = transition(current, QuestAssignment.Status.COMPLETED);
        final List<M13RewardIntent> rewards = new ArrayList<M13RewardIntent>();
        for (int index = 0; index < definition.rewards().size(); index++) {
            rewards.add(new M13RewardIntent(definition.rewards().get(index), current.playerId(),
                    io.zartra.bedwars.api.identity.IdempotencyKey.of("m13", "quest/"
                            + current.questId().path() + "/" + current.revision() + "/" + index)));
        }
        return new Outcome(completed, rewards);
    }

    /** Resets a repeatable quest after its configured cooldown. */
    public QuestAssignment reset(final QuestDefinition definition, final QuestAssignment current,
                                 final Instant now, final Instant expiresAt) {
        if (!definition.repeatable() || current.status() != QuestAssignment.Status.COMPLETED) {
            throw new IllegalStateException("quest is not resettable");
        }
        if (definition.cooldown().isPresent()
                && now.isBefore(current.assignedAt().plus(definition.cooldown().get()))) {
            throw new IllegalStateException("quest cooldown is active");
        }
        return new QuestAssignment(current.questId(), current.playerId(), QuestAssignment.Status.ACTIVE,
                now, expiresAt, Math.addExact(current.revision(), 1));
    }

    /** Resolves the next configured quest in a chain after completion. */
    public QuestId next(final QuestAssignment current, final List<QuestId> chain) {
        if (current.status() != QuestAssignment.Status.COMPLETED) {
            throw new IllegalStateException("chain cannot advance before completion");
        }
        final int index = Objects.requireNonNull(chain, "chain").indexOf(current.questId());
        return index >= 0 && index + 1 < chain.size() ? chain.get(index + 1) : null;
    }

    private static QuestAssignment transition(final QuestAssignment current,
                                              final QuestAssignment.Status status) {
        return new QuestAssignment(current.questId(), current.playerId(), status,
                current.assignedAt(), current.expiresAt(), Math.addExact(current.revision(), 1));
    }

    /** Immutable quest projection result. */
    public static final class Outcome {
        private final QuestAssignment assignment;
        private final List<M13RewardIntent> rewards;
        private Outcome(final QuestAssignment assignment, final List<M13RewardIntent> rewards) {
            this.assignment = assignment;
            this.rewards = Collections.unmodifiableList(new ArrayList<M13RewardIntent>(rewards));
        }
        /** @return resulting assignment */ public QuestAssignment assignment() { return assignment; }
        /** @return immutable M12 reward intents */ public List<M13RewardIntent> rewards() { return rewards; }
    }
}
