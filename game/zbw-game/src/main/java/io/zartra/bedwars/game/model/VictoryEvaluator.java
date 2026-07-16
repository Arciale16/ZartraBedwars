package io.zartra.bedwars.game.model;

/**
 * Platform-neutral extension point for deciding whether a match has a winner.
 *
 * <p>Implementations must be deterministic, thread-safe and free of I/O. M10 mode policies may
 * replace this evaluator without changing the M08 aggregate or Paper projections.</p>
 */
public interface VictoryEvaluator {
    /** @return typed evaluation for the supplied immutable snapshot */
    VictoryEvaluation evaluate(MatchSnapshot snapshot);
}
