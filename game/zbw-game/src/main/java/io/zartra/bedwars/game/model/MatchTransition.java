package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable successful state-machine transition and ordered domain facts. */
public final class MatchTransition {
    private final MatchSnapshot before;
    private final MatchSnapshot after;
    private final List<Fact> facts;
    private final boolean duplicate;
    private final VictoryEvaluation.CompletionIntent completionIntent;

    /** Creates a transition result. */
    public MatchTransition(final MatchSnapshot before, final MatchSnapshot after,
                           final List<Fact> facts, final boolean duplicate) {
        this(before, after, facts, duplicate, null);
    }

    /** Creates a transition result with an optional generic victory intent. */
    public MatchTransition(final MatchSnapshot before, final MatchSnapshot after,
                           final List<Fact> facts, final boolean duplicate,
                           final VictoryEvaluation.CompletionIntent completionIntent) {
        this.before = Objects.requireNonNull(before, "before");
        this.after = Objects.requireNonNull(after, "after");
        final List<Fact> copy = new ArrayList<Fact>(Objects.requireNonNull(facts, "facts"));
        if (copy.contains(null)) { throw new IllegalArgumentException("facts cannot contain null"); }
        this.facts = Collections.unmodifiableList(copy);
        this.duplicate = duplicate;
        this.completionIntent = completionIntent;
    }
    /** @return state before command */ public MatchSnapshot before() { return before; }
    /** @return state after command */ public MatchSnapshot after() { return after; }
    /** @return ordered immutable domain facts */ public List<Fact> facts() { return facts; }
    /** @return whether an idempotent retry made no mutation */ public boolean duplicate() { return duplicate; }
    /** @return generic victory completion intent, when one was detected */
    public Optional<VictoryEvaluation.CompletionIntent> completionIntent() {
        return Optional.ofNullable(completionIntent);
    }

    /** One immutable fact emitted by the aggregate. */
    public static final class Fact {
        private final DefinitionId type;
        private final PlayerId playerId;
        private final DefinitionId teamId;
        /** Creates a typed fact whose optional identities are immutable. */
        public Fact(final DefinitionId type, final PlayerId playerId, final DefinitionId teamId) {
            this.type = Objects.requireNonNull(type, "type");
            this.playerId = playerId;
            this.teamId = teamId;
        }
        /** @return semantic fact type */ public DefinitionId type() { return type; }
        /** @return involved player when applicable */ public Optional<PlayerId> playerId() { return Optional.ofNullable(playerId); }
        /** @return involved team when applicable */ public Optional<DefinitionId> teamId() { return Optional.ofNullable(teamId); }
    }
}
