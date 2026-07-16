package io.zartra.bedwars.game.application;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.game.model.GameRules;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.StandardVictoryEvaluator;
import io.zartra.bedwars.game.model.TeamDefinition;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.game.model.VictoryEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic Java 8 factory from durable arena definitions to match aggregates.
 *
 * <p>The assembler performs no I/O, reads no runtime configuration and contains no platform
 * types. Callers must supply one atomically loaded {@link ArenaBundle} and its expected versions.
 * Normal runtime composition should use this class instead of manually creating team snapshots.</p>
 */
public final class ArenaMatchAssembler {
    private static final ApiError STALE = error("assembly/stale_definition", true);
    private static final ApiError UNAVAILABLE = error("assembly/arena_unavailable", false);
    private static final ApiError INVALID = error("assembly/invalid_definition", false);

    private final ArenaValidation.Validator validator;
    private final VictoryEvaluator victoryEvaluator;

    /** Creates an assembler with original starter validation and generic victory policies. */
    public ArenaMatchAssembler() {
        this(new ArenaValidation.DefaultValidator(), new StandardVictoryEvaluator());
    }

    /** Creates an assembler with explicit validation and victory policies. */
    public ArenaMatchAssembler(final ArenaValidation.Validator validator,
                               final VictoryEvaluator victoryEvaluator) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.victoryEvaluator = Objects.requireNonNull(victoryEvaluator, "victoryEvaluator");
    }

    /**
     * Validates and assembles an enabled, current arena definition.
     *
     * @return a waiting match aggregate or a typed fail-closed error
     */
    public Result<MatchStateMachine> assemble(final MatchAssemblyRequest request) {
        final MatchAssemblyRequest value = Objects.requireNonNull(request, "request");
        final ArenaBundle bundle = value.arenaBundle();
        if (bundle.arena().version() != value.expectedArenaVersion()
                || bundle.map().version() != value.expectedMapVersion()) {
            return Result.failure(STALE);
        }
        if (bundle.arena().status() != ArenaDefinition.Status.ENABLED) {
            return Result.failure(UNAVAILABLE);
        }
        if (!validator.validate(bundle).mayEnable()) {
            return Result.failure(INVALID);
        }
        final List<TeamSnapshot> teams = new ArrayList<TeamSnapshot>();
        for (ArenaTeam team : bundle.arena().teams()) {
            teams.add(TeamSnapshot.empty(TeamDefinition.of(team.id(), team.displayName(),
                    team.color(), bundle.arena().teamSize())));
        }
        try {
            final GameRules rules = new GameRules(bundle.arena().minimumPlayers(),
                    bundle.arena().maximumPlayers(), value.timingPolicy());
            return Result.success(new MatchStateMachine(value.matchId(), bundle.arenaId(),
                    rules, teams, value.createdAt(), victoryEvaluator));
        } catch (IllegalArgumentException inconsistent) {
            return Result.failure(INVALID);
        }
    }

    private static ApiError error(final String path, final boolean retryable) {
        return ApiError.of(DefinitionId.of("zartra", "game/" + path),
                "game." + path.replace('/', '.'), retryable
                        ? ApiError.RetryDisposition.RETRYABLE
                        : ApiError.RetryDisposition.PERMANENT);
    }
}
