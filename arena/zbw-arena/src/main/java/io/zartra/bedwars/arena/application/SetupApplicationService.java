package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.setup.MarkerProposal;
import io.zartra.bedwars.arena.setup.SetupMutation;
import io.zartra.bedwars.arena.setup.SetupPreview;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import io.zartra.bedwars.arena.spi.ArenaAuditSink;
import io.zartra.bedwars.arena.spi.ArenaEventSink;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.spi.MarkerDiscoveryPort;
import io.zartra.bedwars.arena.spi.SetupCommitPort;
import io.zartra.bedwars.arena.spi.SetupSessionRepository;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Isolated setup-session, edit, validation, preview/apply, history and atomic-save use cases.
 *
 * <p>Repository and marker discovery calls may block and must run on an M05 bounded worker. This
 * service contains no command, GUI, inventory, item or platform policy. Every mutation is revision
 * fenced; final M09 presentation adapters invoke these same methods.</p>
 */
public final class SetupApplicationService {
    private final ArenaRepository arenas;
    private final SetupSessionRepository sessions;
    private final SetupCommitPort commits;
    private final MarkerDiscoveryPort markers;
    private final ArenaValidation.Validator validator;
    private final ArenaPolicy policy;
    private final ArenaServiceSupport support;

    /** Creates a setup service with explicit storage, security and event boundaries. */
    public SetupApplicationService(final ArenaRepository arenas,
                                   final SetupSessionRepository sessions,
                                   final SetupCommitPort commits,
                                   final MarkerDiscoveryPort markers,
                                   final ArenaValidation.Validator validator,
                                   final ArenaPolicy policy,
                                   final AuthorizationService authorization,
                                   final ArenaAuditSink audit, final ArenaEventSink events,
                                   final TimeSource timeSource) {
        this.arenas = Objects.requireNonNull(arenas, "arenas");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.commits = Objects.requireNonNull(commits, "commits");
        this.markers = Objects.requireNonNull(markers, "markers");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.support = new ArenaServiceSupport(authorization, audit, events, timeSource);
    }

    /** @return a durable isolated active session for an existing arena */
    public Result<SetupSession> begin(final ArenaId arenaId, final long expectedArenaRevision,
                                      final AuthorizationSubject actor,
                                      final CorrelationId correlationId) {
        if (expectedArenaRevision < 1L) {
            throw new IllegalArgumentException("expectedArenaRevision must be positive");
        }
        if (!authorizeAndBegin(ArenaOperation.SETUP_ENTER, arenaId, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<List<SetupSession>> inventory = sessions.listSessions();
        if (inventory.isFailure()) { return Result.failure(inventory.error().get()); }
        int active = 0;
        for (SetupSession session : inventory.requireValue()) {
            if (session.state() == SetupSession.State.ACTIVE) { active++; }
            if (session.state() == SetupSession.State.ACTIVE
                    && session.arenaId().equals(arenaId)) {
                return Result.failure(ArenaFailures.CONFLICT);
            }
        }
        if (active >= policy.maximumConcurrentSetupSessions()) {
            return Result.failure(ArenaFailures.CAPACITY);
        }
        final Result<Optional<ArenaRepository.Record>> found = arenas.find(arenaId);
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()) { return Result.failure(ArenaFailures.NOT_FOUND); }
        final ArenaRepository.Record record = found.requireValue().get();
        if (record.revision() != expectedArenaRevision) { return Result.failure(ArenaFailures.CONFLICT); }
        final SetupSession session = SetupSession.begin(SetupSessionId.random(), actor,
                expectedArenaRevision, record.bundle());
        final Result<SetupSession> saved = sessions.save(session, 0L);
        return finish(ArenaOperation.SETUP_ENTER, arenaId, actor, correlationId, saved);
    }

    /** @return revision-fenced session after one typed mutation */
    public Result<SetupSession> mutate(final SetupSessionId sessionId,
                                       final long expectedDraftRevision,
                                       final SetupMutation mutation,
                                       final AuthorizationSubject actor,
                                       final CorrelationId correlationId) {
        final Result<SetupSession> loaded = loadActive(sessionId, expectedDraftRevision,
                ArenaOperation.SETUP_EDIT, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        final SetupSession changed = loaded.requireValue().mutate(
                Objects.requireNonNull(mutation, "mutation"), support.timeSource().now());
        return finish(ArenaOperation.SETUP_EDIT, changed.arenaId(), actor, correlationId,
                sessions.save(changed, expectedDraftRevision));
    }

    /** @return revision-fenced session one edit earlier */
    public Result<SetupSession> undo(final SetupSessionId sessionId,
                                     final long expectedDraftRevision,
                                     final AuthorizationSubject actor,
                                     final CorrelationId correlationId) {
        return history(sessionId, expectedDraftRevision, true, actor, correlationId);
    }

    /** @return revision-fenced session one edit later */
    public Result<SetupSession> redo(final SetupSessionId sessionId,
                                     final long expectedDraftRevision,
                                     final AuthorizationSubject actor,
                                     final CorrelationId correlationId) {
        return history(sessionId, expectedDraftRevision, false, actor, correlationId);
    }

    /** @return current draft validation without mutation */
    public Result<ArenaValidation.Report> validate(final SetupSessionId sessionId,
                                                   final long expectedDraftRevision,
                                                   final AuthorizationSubject actor,
                                                   final CorrelationId correlationId) {
        final Result<SetupSession> loaded = loadActive(sessionId, expectedDraftRevision,
                ArenaOperation.SETUP_VALIDATE, actor, correlationId);
        if (loaded.isFailure()) { return Result.failure(loaded.error().get()); }
        final ArenaValidation.Report report = validator.validate(loaded.requireValue().draft());
        support.after(ArenaOperation.SETUP_VALIDATE, loaded.requireValue().arenaId(), actor,
                correlationId, Math.max(1L, expectedDraftRevision));
        return Result.success(report);
    }

    /**
     * Builds an immutable candidate by applying ordered changes without persisting them.
     *
     * @return preview bound to the unchanged base revision
     */
    public Result<SetupPreview> preview(final SetupSessionId sessionId,
                                        final long expectedDraftRevision,
                                        final List<SetupMutation> changes,
                                        final AuthorizationSubject actor,
                                        final CorrelationId correlationId) {
        final Result<SetupSession> loaded = loadActive(sessionId, expectedDraftRevision,
                ArenaOperation.SETUP_VALIDATE, actor, correlationId);
        if (loaded.isFailure()) { return Result.failure(loaded.error().get()); }
        SetupSession candidate = loaded.requireValue();
        final Instant now = support.timeSource().now();
        final List<SetupMutation> requested = Objects.requireNonNull(changes, "changes");
        if (requested.isEmpty() || requested.size() > 256 || requested.contains(null)) {
            throw new IllegalArgumentException("preview requires one to 256 changes");
        }
        for (SetupMutation change : requested) { candidate = candidate.mutate(change, now); }
        return Result.success(SetupPreview.candidate(sessionId, expectedDraftRevision,
                loaded.requireValue().draft(), candidate.draft(),
                validator.validate(candidate.draft())));
    }

    /** @return persisted draft after explicit apply of an unmodified, current preview */
    public Result<SetupSession> applyPreview(final SetupPreview preview,
                                             final AuthorizationSubject actor,
                                             final CorrelationId correlationId) {
        final SetupPreview value = Objects.requireNonNull(preview, "preview");
        final Result<SetupSession> loaded = loadActive(value.sessionId(), value.draftRevision(),
                ArenaOperation.SETUP_APPLY, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        if (!value.matches(loaded.requireValue())) {
            return Result.failure(ArenaFailures.STALE_PREVIEW);
        }
        final SetupSession changed = loaded.requireValue().applyPreview(value.bundle());
        return finish(ArenaOperation.SETUP_APPLY, changed.arenaId(), actor, correlationId,
                sessions.save(changed, value.draftRevision()));
    }

    /** @return a discovery proposal that cannot mutate state by itself */
    public Result<MarkerProposal> discoverMarkers(final SetupSessionId sessionId,
                                                  final long expectedDraftRevision,
                                                  final AuthorizationSubject actor,
                                                  final CorrelationId correlationId) {
        final Result<SetupSession> loaded = loadActive(sessionId, expectedDraftRevision,
                ArenaOperation.SETUP_VALIDATE, actor, correlationId);
        return loaded.isFailure() ? Result.<MarkerProposal>failure(loaded.error().get())
                : markers.discover(loaded.requireValue());
    }

    /** @return persisted session after explicit apply of a current marker proposal */
    public Result<SetupSession> applyMarkers(final MarkerProposal proposal,
                                             final AuthorizationSubject actor,
                                             final CorrelationId correlationId) {
        final MarkerProposal value = Objects.requireNonNull(proposal, "proposal");
        final Result<SetupSession> loaded = loadActive(value.sessionId(), value.draftRevision(),
                ArenaOperation.SETUP_APPLY, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        if (!value.matches(loaded.requireValue())) { return Result.failure(ArenaFailures.STALE_PREVIEW); }
        SetupSession changed = loaded.requireValue();
        final Instant now = support.timeSource().now();
        for (SetupMutation mutation : value.mutations()) { changed = changed.mutate(mutation, now); }
        return finish(ArenaOperation.SETUP_APPLY, changed.arenaId(), actor, correlationId,
                sessions.save(changed, value.draftRevision()));
    }

    /** @return atomic durable arena/session commit evidence */
    public Result<SetupCommitPort.CommitResult> commit(final SetupPreview preview,
                                                       final boolean enable,
                                                       final AuthorizationSubject actor,
                                                       final CorrelationId correlationId) {
        final SetupPreview value = Objects.requireNonNull(preview, "preview");
        final Result<SetupSession> loaded = loadActive(value.sessionId(), value.draftRevision(),
                ArenaOperation.SETUP_SAVE, actor, correlationId);
        if (loaded.isFailure()) { return Result.failure(loaded.error().get()); }
        if (!value.matches(loaded.requireValue())) { return Result.failure(ArenaFailures.STALE_PREVIEW); }
        final ArenaValidation.Report report = validator.validate(value.bundle());
        if (enable && !report.mayEnable()) { return Result.failure(ArenaFailures.INVALID); }
        ArenaBundle bundle = value.bundle();
        if (enable && bundle.arena().status() != ArenaDefinition.Status.ENABLED) {
            bundle = new ArenaBundle(bundle.arena().toBuilder()
                    .status(ArenaDefinition.Status.ENABLED)
                    .revision(bundle.arena().version() + 1L, support.timeSource().now()).build(),
                    bundle.map());
        }
        final SetupPreview finalPreview = value.withBundle(bundle, validator.validate(bundle));
        final Result<SetupCommitPort.CommitResult> result = commits.commit(
                loaded.requireValue(), finalPreview, report.mayEnable());
        if (result.isSuccess()) {
            support.after(ArenaOperation.SETUP_SAVE, loaded.requireValue().arenaId(), actor,
                    correlationId, result.requireValue().arena().revision());
        } else {
            support.failure(ArenaOperation.SETUP_SAVE, loaded.requireValue().arenaId(), actor,
                    correlationId, result.error().get().code());
        }
        return result;
    }

    /** @return terminal abandoned session retaining the durable arena unchanged */
    public Result<SetupSession> abandon(final SetupSessionId sessionId,
                                        final long expectedDraftRevision,
                                        final AuthorizationSubject actor,
                                        final CorrelationId correlationId) {
        final Result<SetupSession> loaded = loadActive(sessionId, expectedDraftRevision,
                ArenaOperation.SETUP_EXIT, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        final SetupSession abandoned = loaded.requireValue().abandoned();
        return finish(ArenaOperation.SETUP_EXIT, abandoned.arenaId(), actor, correlationId,
                sessions.save(abandoned, expectedDraftRevision));
    }

    private Result<SetupSession> history(final SetupSessionId sessionId,
                                         final long expectedDraftRevision,
                                         final boolean undo,
                                         final AuthorizationSubject actor,
                                         final CorrelationId correlationId) {
        final Result<SetupSession> loaded = loadActive(sessionId, expectedDraftRevision,
                ArenaOperation.SETUP_EDIT, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        final SetupSession changed;
        try { changed = undo ? loaded.requireValue().undo() : loaded.requireValue().redo(); }
        catch (IllegalStateException exception) { return Result.failure(ArenaFailures.CONFLICT); }
        return finish(ArenaOperation.SETUP_EDIT, changed.arenaId(), actor, correlationId,
                sessions.save(changed, expectedDraftRevision));
    }

    private Result<SetupSession> loadActive(final SetupSessionId sessionId,
                                            final long expectedDraftRevision,
                                            final ArenaOperation operation,
                                            final AuthorizationSubject actor,
                                            final CorrelationId correlationId) {
        if (expectedDraftRevision < 0L) { throw new IllegalArgumentException("expectedDraftRevision is negative"); }
        final Result<Optional<SetupSession>> found = sessions.find(
                Objects.requireNonNull(sessionId, "sessionId"));
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()) { return Result.failure(ArenaFailures.NOT_FOUND); }
        final SetupSession session = found.requireValue().get();
        if (session.state() != SetupSession.State.ACTIVE
                || session.draftRevision() != expectedDraftRevision) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        if (!authorizeAndBegin(operation, session.arenaId(), actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        return Result.success(session);
    }

    private boolean authorizeAndBegin(final ArenaOperation operation, final ArenaId arenaId,
                                      final AuthorizationSubject actor,
                                      final CorrelationId correlationId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(correlationId, "correlationId");
        return support.authorize(operation, arenaId, actor, correlationId)
                && support.before(operation, arenaId, actor, correlationId);
    }

    private Result<SetupSession> finish(final ArenaOperation operation, final ArenaId arenaId,
                                        final AuthorizationSubject actor,
                                        final CorrelationId correlationId,
                                        final Result<SetupSession> result) {
        if (result.isSuccess()) {
            support.after(operation, arenaId, actor, correlationId,
                    Math.max(1L, result.requireValue().draftRevision()));
        } else {
            support.failure(operation, arenaId, actor, correlationId, result.error().get().code());
        }
        return result;
    }
}
