package io.zartra.bedwars.progression.projection;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.UnitOfWork;

/** Application projection contract; implementations perform no platform-thread blocking. */
public interface ProgressionProjector {
    /** Projects one event inside a caller-owned M04 unit of work. */ Result<ProjectionResult> project(UnitOfWork unitOfWork, ProgressionEventInput input);
    /** Replays an explicitly bounded recovery record. */ Result<ProjectionResult> recover(UnitOfWork unitOfWork, ProjectionRecoveryState recoveryState);
}
