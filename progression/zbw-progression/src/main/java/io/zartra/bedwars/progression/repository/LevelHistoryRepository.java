package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.LevelState;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.List;

/** Append-only level history port. */
public interface LevelHistoryRepository {
    /** Appends a level transition. */ Result<LevelState> append(UnitOfWork unitOfWork, PlayerProgressionId owner, LevelState state);
    /** Reads bounded level history. */ Result<List<LevelState>> history(UnitOfWork unitOfWork, PlayerProgressionId owner, int limit);
}
