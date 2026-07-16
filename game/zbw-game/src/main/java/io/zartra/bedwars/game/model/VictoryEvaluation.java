package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.util.Objects;
import java.util.Optional;

/** Immutable victory evaluation result with an optional exactly-once completion intent. */
public final class VictoryEvaluation {
    private static final VictoryEvaluation NONE = new VictoryEvaluation(null);
    private final CompletionIntent completionIntent;

    private VictoryEvaluation(final CompletionIntent completionIntent) {
        this.completionIntent = completionIntent;
    }

    /** @return result indicating that gameplay must continue */
    public static VictoryEvaluation none() { return NONE; }

    /** @return result requesting fenced completion for one winning team */
    public static VictoryEvaluation complete(final DefinitionId winningTeamId,
                                              final DefinitionId outcome) {
        return new VictoryEvaluation(new CompletionIntent(winningTeamId, outcome));
    }

    /** @return whether fenced completion should begin */
    public boolean completionRequired() { return completionIntent != null; }
    /** @return typed completion intent when exactly one eligible team survives */
    public Optional<CompletionIntent> completionIntent() {
        return Optional.ofNullable(completionIntent);
    }

    @Override public int hashCode() { return Objects.hash(completionIntent); }
    @Override public boolean equals(final Object other) {
        return other instanceof VictoryEvaluation
                && Objects.equals(completionIntent, ((VictoryEvaluation) other).completionIntent);
    }

    /** Winner and outcome that a caller may fence with its own idempotency key. */
    public static final class CompletionIntent {
        private final DefinitionId winningTeamId;
        private final DefinitionId outcome;

        private CompletionIntent(final DefinitionId winningTeamId,
                                 final DefinitionId outcome) {
            this.winningTeamId = Objects.requireNonNull(winningTeamId, "winningTeamId");
            this.outcome = Objects.requireNonNull(outcome, "outcome");
        }

        /** @return stable winning team identity */
        public DefinitionId winningTeamId() { return winningTeamId; }
        /** @return semantic terminal outcome */
        public DefinitionId outcome() { return outcome; }
        /** @return existing idempotent aggregate command fenced by the supplied key */
        public MatchCommand completionCommand(final IdempotencyKey key) {
            return MatchCommand.complete(outcome, Objects.requireNonNull(key, "key"));
        }

        @Override public int hashCode() { return Objects.hash(winningTeamId, outcome); }
        @Override public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (!(other instanceof CompletionIntent)) { return false; }
            final CompletionIntent that = (CompletionIntent) other;
            return winningTeamId.equals(that.winningTeamId) && outcome.equals(that.outcome);
        }
    }
}
