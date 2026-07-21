package io.zartra.bedwars.progression.repository;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.PrestigeState;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.List;

/** Append-only prestige history port. */
public interface PrestigeHistoryRepository {
    /** Appends a prestige transition. */ Result<PrestigeState> append(UnitOfWork unitOfWork, PlayerProgressionId owner, PrestigeState state);
    /** Reads bounded prestige history. */ Result<List<PrestigeState>> history(UnitOfWork unitOfWork, PlayerProgressionId owner, int limit);
}
