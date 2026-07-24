package io.zartra.bedwars.progression.runtime;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.objective.ObjectiveDefinition;
import io.zartra.bedwars.progression.objective.ObjectiveEvent;
import io.zartra.bedwars.progression.objective.ObjectiveExecutionEngine;
import io.zartra.bedwars.progression.objective.ObjectiveRuntimeState;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Transactional exactly-once objective projection over the M04/M12 storage boundary. */
public final class M13ProjectionEngine {
    private final M13StateRepository repository;
    private final ObjectiveExecutionEngine evaluator;
    private final RewardResolver rewards;

    /** Creates a projection engine with explicit stateless collaborators. */
    public M13ProjectionEngine(final M13StateRepository repository,
                               final ObjectiveExecutionEngine evaluator,
                               final RewardResolver rewards) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.rewards = Objects.requireNonNull(rewards, "rewards");
    }

    /** Claims, evaluates and persists one objective event in the caller-owned transaction. */
    public Result<Projection> project(final UnitOfWork unitOfWork,
                                      final ObjectiveDefinition definition,
                                      final ObjectiveEvent event, final boolean repeatable,
                                      final Optional<Instant> expiresAt, final Instant now) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        final Result<Boolean> claim = repository.claimEvent(unitOfWork,
                event.idempotencyKey(), event.audit().createdAt());
        if (claim.isFailure()) { return Result.failure(claim.error().get()); }
        if (!claim.requireValue()) { return Result.success(Projection.duplicateResult()); }
        final Result<Optional<ObjectiveRuntimeState>> found = repository.findObjective(unitOfWork,
                event.playerId(), definition.id());
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        final ObjectiveRuntimeState current = found.requireValue().orElseGet(() ->
                ObjectiveRuntimeState.active(definition, event.playerId(), expiresAt, event.audit()));
        final ObjectiveExecutionEngine.Evaluation evaluation = evaluator.evaluate(
                definition, current, event, repeatable, now);
        if (evaluation.changed()) {
            final Result<ObjectiveRuntimeState> saved = repository.saveObjective(unitOfWork,
                    evaluation.state(), current.revision());
            if (saved.isFailure()) { return Result.failure(saved.error().get()); }
        }
        final List<M13RewardIntent> intents = new ArrayList<M13RewardIntent>();
        if (evaluation.completed()) {
            final List<RewardId> definitions = rewards.forCompletion(definition.id());
            for (int index = 0; index < definitions.size(); index++) {
                intents.add(new M13RewardIntent(definitions.get(index), event.playerId(),
                        io.zartra.bedwars.api.identity.IdempotencyKey.of("m13", "objective/"
                                + definition.id().path() + "/" + evaluation.state().completionCount()
                                + "/" + index)));
            }
        }
        return Result.success(new Projection(false, evaluation, intents));
    }

    /** Resolves configured M12 reward references for one completed objective. */
    public interface RewardResolver {
        /** @return bounded immutable or caller-owned reward identities */
        List<RewardId> forCompletion(io.zartra.bedwars.progression.objective.ObjectiveId objectiveId);
    }

    /** Immutable projection result and recovery evidence. */
    public static final class Projection {
        private final boolean duplicate;
        private final ObjectiveExecutionEngine.Evaluation evaluation;
        private final List<M13RewardIntent> rewardIntents;
        private Projection(final boolean duplicate,
                           final ObjectiveExecutionEngine.Evaluation evaluation,
                           final List<M13RewardIntent> rewardIntents) {
            this.duplicate = duplicate;
            this.evaluation = evaluation;
            this.rewardIntents = Collections.unmodifiableList(
                    new ArrayList<M13RewardIntent>(rewardIntents));
        }
        private static Projection duplicateResult() {
            return new Projection(true, null, Collections.<M13RewardIntent>emptyList());
        }
        /** @return whether durable inbox evidence already existed */ public boolean duplicate() { return duplicate; }
        /** @return evaluation when this invocation owned the event */
        public Optional<ObjectiveExecutionEngine.Evaluation> evaluation() { return Optional.ofNullable(evaluation); }
        /** @return exactly-once M12 reward intents */ public List<M13RewardIntent> rewardIntents() { return rewardIntents; }
    }
}
