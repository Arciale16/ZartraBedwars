package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Generic winner policy requiring exactly one surviving team among participating teams. */
public final class StandardVictoryEvaluator implements VictoryEvaluator {
    private static final DefinitionId TEAM_VICTORY =
            DefinitionId.of("zartra", "outcome/team_victory");

    @Override public VictoryEvaluation evaluate(final MatchSnapshot snapshot) {
        final MatchSnapshot value = Objects.requireNonNull(snapshot, "snapshot");
        if (value.state() != MatchSnapshot.State.PLAYING) {
            return VictoryEvaluation.none();
        }
        int eligible = 0;
        int surviving = 0;
        DefinitionId winner = null;
        for (TeamSnapshot team : value.teams()) {
            if (team.members().isEmpty()) { continue; }
            eligible++;
            if (!team.eliminated()) {
                surviving++;
                winner = team.teamId();
            }
        }
        return eligible >= 2 && surviving == 1
                ? VictoryEvaluation.complete(winner, TEAM_VICTORY)
                : VictoryEvaluation.none();
    }
}
