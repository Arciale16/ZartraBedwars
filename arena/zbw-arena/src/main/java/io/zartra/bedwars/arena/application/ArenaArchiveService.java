package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.archive.ArenaArchive;
import io.zartra.bedwars.arena.archive.ArenaArchiveCodec;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.spi.ArenaArchiveStore;
import io.zartra.bedwars.arena.spi.ArenaAuditSink;
import io.zartra.bedwars.arena.spi.ArenaEventSink;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Authorized bounded import, export, backup and restore application use cases.
 *
 * <p>Codec work and repository/store calls execute on an M05 worker. Archives contain no paths,
 * code or world files; M06 world cloning/reset handles world data separately. Import and restore
 * preserve stable arena/map identities and reject malformed, oversized or checksum-invalid data.</p>
 */
public final class ArenaArchiveService {
    private final ArenaRepository arenas;
    private final ArenaArchiveStore archives;
    private final ArenaArchiveCodec codec;
    private final ArenaValidation.Validator validator;
    private final ArenaServiceSupport support;

    /** Creates an archive service with explicit persistence, security and event dependencies. */
    public ArenaArchiveService(final ArenaRepository arenas, final ArenaArchiveStore archives,
                               final ArenaArchiveCodec codec,
                               final ArenaValidation.Validator validator,
                               final AuthorizationService authorization,
                               final ArenaAuditSink audit, final ArenaEventSink events,
                               final TimeSource timeSource) {
        this.arenas = Objects.requireNonNull(arenas, "arenas");
        this.archives = Objects.requireNonNull(archives, "archives");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.support = new ArenaServiceSupport(authorization, audit, events, timeSource);
    }

    /** @return deterministic export envelope without mutating durable state */
    public Result<ArenaArchive> exportArena(final ArenaId arenaId,
                                            final AuthorizationSubject actor,
                                            final CorrelationId correlationId) {
        final Result<ArenaRepository.Record> loaded = load(ArenaOperation.EXPORT, arenaId,
                actor, correlationId, false);
        if (loaded.isFailure()) { return Result.failure(loaded.error().get()); }
        final Result<ArenaArchive> encoded = codec.encode(newArchiveId("export"),
                loaded.requireValue().bundle(), support.timeSource().now());
        if (encoded.isSuccess()) {
            support.after(ArenaOperation.EXPORT, arenaId, actor, correlationId,
                    loaded.requireValue().revision());
        }
        return encoded;
    }

    /** @return durably stored integrity-checked backup */
    public Result<ArenaArchive> backup(final ArenaId arenaId,
                                       final AuthorizationSubject actor,
                                       final CorrelationId correlationId) {
        final Result<ArenaRepository.Record> loaded = load(ArenaOperation.BACKUP, arenaId,
                actor, correlationId, true);
        if (loaded.isFailure()) { return Result.failure(loaded.error().get()); }
        final Result<ArenaArchive> encoded = codec.encode(newArchiveId("backup"),
                loaded.requireValue().bundle(), support.timeSource().now());
        if (encoded.isFailure()) { return encoded; }
        final Result<ArenaArchive> stored = archives.save(encoded.requireValue());
        if (stored.isSuccess()) {
            support.after(ArenaOperation.BACKUP, arenaId, actor, correlationId,
                    loaded.requireValue().revision());
        } else {
            support.failure(ArenaOperation.BACKUP, arenaId, actor, correlationId,
                    stored.error().get().code());
        }
        return stored;
    }

    /** @return newly imported durable record preserving archive identities */
    public Result<ArenaRepository.Record> importArena(final ArenaArchive archive,
                                                      final AuthorizationSubject actor,
                                                      final CorrelationId correlationId) {
        final ArenaArchive value = Objects.requireNonNull(archive, "archive");
        if (!authorize(ArenaOperation.IMPORT, value.arenaId(), actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<ArenaBundle> decoded = codec.decode(value);
        if (decoded.isFailure()) { return Result.failure(decoded.error().get()); }
        if (!transitionValid(decoded.requireValue())) { return Result.failure(ArenaFailures.INVALID); }
        return finish(ArenaOperation.IMPORT, value.arenaId(), actor, correlationId,
                arenas.save(new ArenaRepository.SaveRequest(decoded.requireValue(), 0L,
                        validator.validate(decoded.requireValue()).mayEnable())));
    }

    /** @return exact-revision record restored from an explicit archive */
    public Result<ArenaRepository.Record> restore(final ArenaArchive archive,
                                                  final long expectedRevision,
                                                  final AuthorizationSubject actor,
                                                  final CorrelationId correlationId) {
        final ArenaArchive value = Objects.requireNonNull(archive, "archive");
        if (expectedRevision < 1L) { throw new IllegalArgumentException("expectedRevision must be positive"); }
        if (!authorize(ArenaOperation.RESTORE, value.arenaId(), actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<ArenaBundle> decoded = codec.decode(value);
        if (decoded.isFailure()) { return Result.failure(decoded.error().get()); }
        if (!transitionValid(decoded.requireValue())) { return Result.failure(ArenaFailures.INVALID); }
        return finish(ArenaOperation.RESTORE, value.arenaId(), actor, correlationId,
                arenas.save(new ArenaRepository.SaveRequest(decoded.requireValue(), expectedRevision,
                        validator.validate(decoded.requireValue()).mayEnable())));
    }

    /** @return exact-revision record restored from a durable backup identity */
    public Result<ArenaRepository.Record> restoreBackup(final DefinitionId archiveId,
                                                        final ArenaId arenaId,
                                                        final long expectedRevision,
                                                        final AuthorizationSubject actor,
                                                        final CorrelationId correlationId) {
        final Result<Optional<ArenaArchive>> found = archives.find(
                Objects.requireNonNull(archiveId, "archiveId"));
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()
                || !found.requireValue().get().arenaId().equals(arenaId)) {
            return Result.failure(ArenaFailures.NOT_FOUND);
        }
        return restore(found.requireValue().get(), expectedRevision, actor, correlationId);
    }

    private Result<ArenaRepository.Record> load(
            final ArenaOperation operation, final ArenaId arenaId,
            final AuthorizationSubject actor, final CorrelationId correlationId,
            final boolean preChange) {
        if (!support.authorize(operation, arenaId, actor, correlationId)
                || preChange && !support.before(operation, arenaId, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<Optional<ArenaRepository.Record>> found = arenas.find(arenaId);
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        return found.requireValue().isPresent() ? Result.success(found.requireValue().get())
                : Result.<ArenaRepository.Record>failure(ArenaFailures.NOT_FOUND);
    }

    private boolean authorize(final ArenaOperation operation, final ArenaId arenaId,
                              final AuthorizationSubject actor,
                              final CorrelationId correlationId) {
        return support.authorize(operation, arenaId, actor, correlationId)
                && support.before(operation, arenaId, actor, correlationId);
    }

    private boolean transitionValid(final ArenaBundle bundle) {
        return bundle.arena().status() != ArenaDefinition.Status.ENABLED
                || validator.validate(bundle).mayEnable();
    }

    private Result<ArenaRepository.Record> finish(
            final ArenaOperation operation, final ArenaId arenaId,
            final AuthorizationSubject actor, final CorrelationId correlationId,
            final Result<ArenaRepository.Record> result) {
        if (result.isSuccess()) {
            support.after(operation, arenaId, actor, correlationId,
                    result.requireValue().revision());
        } else {
            support.failure(operation, arenaId, actor, correlationId,
                    result.error().get().code());
        }
        return result;
    }

    private static DefinitionId newArchiveId(final String kind) {
        return DefinitionId.of("zartra", "arena/archive/" + kind + "/" + UUID.randomUUID());
    }
}
