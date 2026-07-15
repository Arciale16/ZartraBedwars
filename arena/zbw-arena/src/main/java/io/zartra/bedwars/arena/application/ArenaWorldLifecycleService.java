package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.TaskId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.spi.ArenaAuditSink;
import io.zartra.bedwars.arena.spi.ArenaEventSink;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import io.zartra.bedwars.world.orchestration.WorldOrchestrator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Authorized bridge from arena definitions to the bounded M06 world orchestrator.
 *
 * <p>The initial repository lookup may block and therefore begins on an M05 worker. Submitted
 * filesystem steps remain on bounded workers and platform mutations retain M06 owner affinity.
 * No method waits for a world operation or exposes a platform object.</p>
 */
public final class ArenaWorldLifecycleService {
    private final ArenaRepository repository;
    private final WorldOrchestrator worlds;
    private final ArenaPolicy policy;
    private final ArenaServiceSupport support;

    /** Creates a world lifecycle bridge with explicit authorization and audit boundaries. */
    public ArenaWorldLifecycleService(final ArenaRepository repository,
                                      final WorldOrchestrator worlds,
                                      final ArenaPolicy policy,
                                      final AuthorizationService authorization,
                                      final ArenaAuditSink audit, final ArenaEventSink events,
                                      final TimeSource timeSource) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.support = new ArenaServiceSupport(authorization, audit, events, timeSource);
    }

    /** @return cancellable asynchronous reset from the configured immutable template */
    public Result<Handle> reset(final ArenaId arenaId, final AuthorizationSubject actor,
                                final CorrelationId correlationId) {
        return submit(arenaId, ArenaOperation.RESET, WorldOperation.Type.RESET, null,
                actor, correlationId);
    }

    /** @return cancellable asynchronous recovery using the same deterministic reset path */
    public Result<Handle> recover(final ArenaId arenaId, final AuthorizationSubject actor,
                                  final CorrelationId correlationId) {
        return submit(arenaId, ArenaOperation.RESTORE, WorldOperation.Type.RESET, null,
                actor, correlationId);
    }

    /**
     * Clones the source template into the deep-copy target world.
     *
     * @return cancellable asynchronous clone; metadata persistence remains revision fenced by
     *         {@link ArenaApplicationService#duplicate}
     */
    public Result<Handle> duplicateWorld(final ArenaId sourceArenaId, final WorldKey targetWorld,
                                         final AuthorizationSubject actor,
                                         final CorrelationId correlationId) {
        return submit(sourceArenaId, ArenaOperation.DUPLICATE, WorldOperation.Type.CLONE,
                Objects.requireNonNull(targetWorld, "targetWorld"), actor, correlationId);
    }

    private Result<Handle> submit(final ArenaId arenaId, final ArenaOperation operation,
                                  final WorldOperation.Type type, final WorldKey explicitTarget,
                                  final AuthorizationSubject actor,
                                  final CorrelationId correlationId) {
        final ArenaId id = Objects.requireNonNull(arenaId, "arenaId");
        if (!support.authorize(operation, id, actor, correlationId)
                || !support.before(operation, id, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<Optional<ArenaRepository.Record>> found = repository.find(id);
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()) { return Result.failure(ArenaFailures.NOT_FOUND); }
        final ArenaBundle bundle = found.requireValue().get().bundle();
        if (!bundle.arena().templateWorld().isPresent()) {
            return Result.failure(ArenaFailures.INVALID);
        }
        final WorldKey target;
        if (explicitTarget != null) { target = explicitTarget; }
        else if (bundle.arena().world().isPresent()) { target = bundle.arena().world().get(); }
        else { return Result.failure(ArenaFailures.INVALID); }
        final WorldOperation request = WorldOperation.of(TaskId.random(), correlationId, type,
                target, bundle.arena().templateWorld().get(), policy.worldOperationTimeout());
        final WorldOrchestrator.OperationHandle submitted = worlds.submit(request);
        final CompletableFuture<Result<WorldOperationResult>> completion =
                new CompletableFuture<Result<WorldOperationResult>>();
        submitted.completion().whenComplete((result, failure) -> {
            if (failure != null || result.status() != WorldOperationResult.Status.SUCCEEDED) {
                support.failure(operation, id, actor, correlationId,
                        failure == null ? result.reason() : ArenaFailures.WORLD.code());
                completion.complete(Result.<WorldOperationResult>failure(ArenaFailures.WORLD));
            } else {
                support.after(operation, id, actor, correlationId,
                        found.requireValue().get().revision());
                completion.complete(Result.success(result));
            }
        });
        return Result.success(new Handle(submitted, completion));
    }

    /** Cancellable non-blocking arena world-operation handle. */
    public static final class Handle {
        private final WorldOrchestrator.OperationHandle delegate;
        private final CompletionStage<Result<WorldOperationResult>> completion;
        private Handle(final WorldOrchestrator.OperationHandle delegate,
                       final CompletionStage<Result<WorldOperationResult>> completion) {
            this.delegate = delegate;
            this.completion = completion;
        }
        /** @return eventual typed terminal result */ public CompletionStage<Result<WorldOperationResult>> completion() { return completion; }
        /** @return true only when this call newly requested cancellation */ public boolean cancel() { return delegate.cancel(); }
    }
}
