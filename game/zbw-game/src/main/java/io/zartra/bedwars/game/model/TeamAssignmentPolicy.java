package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Deterministic M08 assignment input policy without matchmaking or an M10 selector. */
public final class TeamAssignmentPolicy {
    private static final ApiError NO_CAPACITY = ApiError.of(
            DefinitionId.of("zartra", "game/assignment/no_capacity"),
            "game.assignment.no_capacity", ApiError.RetryDisposition.RETRYABLE);

    /** Selects an eligible team using preference then smallest-size/stable-ID ordering. */
    public Result<DefinitionId> assign(final PlayerId playerId, final MatchSnapshot snapshot,
                                       final Optional<DefinitionId> preferredTeam) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(preferredTeam, "preferredTeam");
        if (snapshot.session(playerId).isPresent()) {
            return Result.success(snapshot.session(playerId).get().teamId());
        }
        if (preferredTeam.isPresent()) {
            final Optional<TeamSnapshot> preferred = snapshot.team(preferredTeam.get());
            if (preferred.isPresent() && preferred.get().hasCapacity()
                    && !preferred.get().eliminated()) {
                return Result.success(preferred.get().teamId());
            }
        }
        final List<TeamSnapshot> eligible = new ArrayList<TeamSnapshot>();
        for (TeamSnapshot team : snapshot.teams()) {
            if (team.hasCapacity() && !team.eliminated()) { eligible.add(team); }
        }
        if (eligible.isEmpty()) { return Result.failure(NO_CAPACITY); }
        Collections.sort(eligible, new Comparator<TeamSnapshot>() {
            @Override public int compare(final TeamSnapshot left, final TeamSnapshot right) {
                final int sizeOrder = Integer.compare(left.members().size(), right.members().size());
                return sizeOrder != 0 ? sizeOrder : left.teamId().compareTo(right.teamId());
            }
        });
        return Result.success(eligible.get(0).teamId());
    }
}
