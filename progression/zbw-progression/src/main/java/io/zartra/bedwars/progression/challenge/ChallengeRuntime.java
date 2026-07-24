package io.zartra.bedwars.progression.challenge;

import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.objective.ObjectiveEvent;
import io.zartra.bedwars.progression.objective.ObjectiveExecutionEngine;
import io.zartra.bedwars.progression.runtime.M13RewardIntent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stateless challenge activation and completion policy. */
public final class ChallengeRuntime {
    /** Activates one challenge and derives its bounded deadline. */
    public ChallengeProgress activate(final ChallengeDefinition definition,
                                      final PlayerProgressionId playerId, final Instant now) {
        return new ChallengeProgress(definition.id(), playerId, definition.version(),
                ChallengeProgress.Status.ACTIVE, now, now.plus(definition.duration()), 0,
                java.util.Optional.empty());
    }

    /** Applies an objective evaluation to an active challenge. */
    public Outcome apply(final ChallengeDefinition definition, final ChallengeProgress current,
                         final ObjectiveExecutionEngine.Evaluation evaluation,
                         final ObjectiveEvent event, final Instant now) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(evaluation, "evaluation");
        Objects.requireNonNull(event, "event");
        if (!definition.id().equals(current.challengeId())
                || definition.version() != current.definitionVersion()) {
            throw new IllegalArgumentException("definition does not match challenge");
        }
        if (current.lastEvent().isPresent() && current.lastEvent().get().equals(event.idempotencyKey())) {
            return new Outcome(current, Collections.<M13RewardIntent>emptyList());
        }
        final ChallengeProgress.Status status;
        if (!now.isBefore(current.expiresAt())) { status = ChallengeProgress.Status.EXPIRED; }
        else if (current.status() == ChallengeProgress.Status.ACTIVE && evaluation.completed()) {
            status = ChallengeProgress.Status.COMPLETED;
        } else { status = current.status(); }
        final ChallengeProgress next = new ChallengeProgress(current.challengeId(), current.playerId(),
                current.definitionVersion(), status, current.activatedAt(), current.expiresAt(),
                Math.addExact(current.revision(), 1), java.util.Optional.of(event.idempotencyKey()));
        if (status != ChallengeProgress.Status.COMPLETED
                || current.status() == ChallengeProgress.Status.COMPLETED) {
            return new Outcome(next, Collections.<M13RewardIntent>emptyList());
        }
        final List<M13RewardIntent> rewards = new ArrayList<M13RewardIntent>();
        for (int index = 0; index < definition.rewards().size(); index++) {
            rewards.add(new M13RewardIntent(definition.rewards().get(index), current.playerId(),
                    io.zartra.bedwars.api.identity.IdempotencyKey.of("m13", "challenge/"
                            + definition.id().path() + "/" + index)));
        }
        return new Outcome(next, rewards);
    }

    /** Immutable challenge projection result. */
    public static final class Outcome {
        private final ChallengeProgress progress;
        private final List<M13RewardIntent> rewards;
        private Outcome(final ChallengeProgress progress, final List<M13RewardIntent> rewards) {
            this.progress = progress;
            this.rewards = Collections.unmodifiableList(new ArrayList<M13RewardIntent>(rewards));
        }
        /** @return resulting challenge */ public ChallengeProgress progress() { return progress; }
        /** @return one-time M12 reward intents */ public List<M13RewardIntent> rewards() { return rewards; }
    }
}
