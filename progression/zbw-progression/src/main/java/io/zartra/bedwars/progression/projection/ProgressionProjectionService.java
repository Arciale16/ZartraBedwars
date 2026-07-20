package io.zartra.bedwars.progression.projection;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.integration.ProgressionEventAdapter;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Objects;
import java.util.Optional;

/** Exactly-once coordinator for configured M08 completion and M11 settlement inputs. */
public final class ProgressionProjectionService implements ProgressionProjector {
    private final ProgressionEventAdapter adapter;
    private final Port port;
    /** Creates a coordinator over a caller-owned M04 transaction. */
    public ProgressionProjectionService(final ProgressionEventAdapter adapter, final Port port) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.port = Objects.requireNonNull(port, "port");
    }
    @Override public Result<ProjectionResult> project(final UnitOfWork unitOfWork,
                                                       final ProgressionEventInput input) {
        final Optional<ProgressionEventAdapter.Intent> intent = adapter.adapt(input);
        if (!intent.isPresent()) {
            return Result.success(new ProjectionResult(ProjectionResult.Status.REJECTED,
                    null, null, "unmapped progression event"));
        }
        return port.project(Objects.requireNonNull(unitOfWork, "unitOfWork"), intent.get());
    }
    @Override public Result<ProjectionResult> recover(final UnitOfWork unitOfWork,
                                                       final ProjectionRecoveryState recoveryState) {
        return port.recover(Objects.requireNonNull(unitOfWork, "unitOfWork"),
                Objects.requireNonNull(recoveryState, "recoveryState"));
    }

    /** Transactional inbox/account/ledger/history/reward/outbox adapter boundary. */
    public interface Port {
        /** Claims and applies one event atomically; duplicates return existing evidence. */
        Result<ProjectionResult> project(UnitOfWork unitOfWork, ProgressionEventAdapter.Intent intent);
        /** Replays a bounded pending record after restart without duplicating effects. */
        Result<ProjectionResult> recover(UnitOfWork unitOfWork, ProjectionRecoveryState recoveryState);
    }
}
