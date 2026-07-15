package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.arena.spi.ArenaAuditSink;
import io.zartra.bedwars.arena.spi.ArenaEventSink;
import io.zartra.bedwars.arena.spi.ArenaIdentityFactory;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Authorized CRUD, rename, duplicate, validation, enable and recovery use cases.
 *
 * <p>Repository calls may block. Call every method from an M05 bounded application worker, never
 * from a Minecraft owner/tick thread. Inputs and outputs contain no Bukkit, Paper, filesystem or
 * SQL types. Expected failures use {@link Result}; programming errors reject null immediately.</p>
 */
public final class ArenaApplicationService {
    private static final ArenaId CATALOG_TARGET = ArenaId.of(new UUID(0L, 0L));
    private final ArenaRepository repository;
    private final ArenaIdentityFactory identities;
    private final ArenaValidation.Validator validator;
    private final ArenaPolicy policy;
    private final ArenaServiceSupport support;

    /** Creates an application service with explicit dependencies and no service locator. */
    public ArenaApplicationService(final ArenaRepository repository,
                                   final ArenaIdentityFactory identities,
                                   final ArenaValidation.Validator validator,
                                   final ArenaPolicy policy,
                                   final AuthorizationService authorization,
                                   final ArenaAuditSink audit, final ArenaEventSink events,
                                   final TimeSource timeSource) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.identities = Objects.requireNonNull(identities, "identities");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.support = new ArenaServiceSupport(authorization, audit, events, timeSource);
    }

    /** @return atomically created draft record */
    public Result<ArenaRepository.Record> create(final ArenaBundle bundle,
                                                 final AuthorizationSubject actor,
                                                 final CorrelationId correlationId) {
        final ArenaBundle value = Objects.requireNonNull(bundle, "bundle");
        if (!authorizeAndBegin(ArenaOperation.CREATE, value.arenaId(), actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<List<ArenaRepository.Record>> inventory = repository.listRecords();
        if (inventory.isFailure()) { return failure(ArenaOperation.CREATE, value.arenaId(), actor, correlationId, inventory.error().get()); }
        if (inventory.requireValue().size() >= policy.maximumArenas()) {
            return failure(ArenaOperation.CREATE, value.arenaId(), actor, correlationId, ArenaFailures.CAPACITY);
        }
        final Result<ArenaRepository.Record> saved = repository.save(
                new ArenaRepository.SaveRequest(value, 0L, false));
        return finish(ArenaOperation.CREATE, value.arenaId(), actor, correlationId, saved);
    }

    /** @return authorized record when present */
    public Result<Optional<ArenaRepository.Record>> find(final ArenaId id,
                                                         final AuthorizationSubject actor,
                                                         final CorrelationId correlationId) {
        final ArenaId target = Objects.requireNonNull(id, "id");
        if (!support.authorize(ArenaOperation.READ, target, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<Optional<ArenaRepository.Record>> result = repository.find(target);
        if (result.isSuccess()) {
            support.after(ArenaOperation.READ, target, actor, correlationId,
                    result.requireValue().isPresent() ? result.requireValue().get().revision() : 1L);
        } else {
            support.failure(ArenaOperation.READ, target, actor, correlationId,
                    result.error().get().code());
        }
        return result;
    }

    /** @return authorized stable inventory snapshot */
    public Result<List<ArenaRepository.Record>> list(final AuthorizationSubject actor,
                                                     final CorrelationId correlationId) {
        if (!support.authorize(ArenaOperation.READ, CATALOG_TARGET, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<List<ArenaRepository.Record>> result = repository.listRecords();
        if (result.isSuccess()) {
            support.after(ArenaOperation.READ, CATALOG_TARGET, actor, correlationId, 1L);
        } else {
            support.failure(ArenaOperation.READ, CATALOG_TARGET, actor, correlationId,
                    result.error().get().code());
        }
        return result;
    }

    /** @return renamed record retaining arena/map identities and references */
    public Result<ArenaRepository.Record> rename(final ArenaId id, final long expectedRevision,
                                                 final String displayName,
                                                 final AuthorizationSubject actor,
                                                 final CorrelationId correlationId) {
        final Result<ArenaRepository.Record> loaded = loadForMutation(
                ArenaOperation.RENAME, id, expectedRevision, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        final ArenaRepository.Record source = loaded.requireValue();
        final Instant now = support.timeSource().now();
        final ArenaDefinition arena = source.bundle().arena().toBuilder().displayName(displayName)
                .revision(source.bundle().arena().version() + 1L, now).build();
        final MapDefinition map = source.bundle().map().rename(displayName, now);
        return save(ArenaOperation.RENAME, new ArenaBundle(arena, map), expectedRevision,
                false, actor, correlationId);
    }

    /** @return independent deep copy with newly generated arena/map identities */
    public Result<ArenaRepository.Record> duplicate(final ArenaId sourceId,
                                                    final long expectedRevision,
                                                    final String displayName,
                                                    final WorldKey targetWorld,
                                                    final AuthorizationSubject actor,
                                                    final CorrelationId correlationId) {
        final Result<ArenaRepository.Record> loaded = loadForMutation(
                ArenaOperation.DUPLICATE, sourceId, expectedRevision, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        final ArenaRepository.Record source = loaded.requireValue();
        final Result<List<ArenaRepository.Record>> inventory = repository.listRecords();
        if (inventory.isFailure()) {
            return failure(ArenaOperation.DUPLICATE, sourceId, actor, correlationId,
                    inventory.error().get());
        }
        if (inventory.requireValue().size() >= policy.maximumArenas()) {
            return failure(ArenaOperation.DUPLICATE, sourceId, actor, correlationId,
                    ArenaFailures.CAPACITY);
        }
        final ArenaId newArenaId = identities.newArenaId();
        final MapId newMapId = identities.newMapId();
        if (!support.authorize(ArenaOperation.CREATE, newArenaId, actor, correlationId)
                || !support.before(ArenaOperation.CREATE, newArenaId, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Instant now = support.timeSource().now();
        final ArenaBundle copy = deepCopy(source.bundle(), newArenaId, newMapId, displayName,
                Objects.requireNonNull(targetWorld, "targetWorld"), now);
        final Result<ArenaRepository.Record> saved = repository.save(
                new ArenaRepository.SaveRequest(copy, 0L, false));
        if (saved.isSuccess()) {
            support.after(ArenaOperation.DUPLICATE, sourceId, actor, correlationId, source.revision());
            support.after(ArenaOperation.CREATE, newArenaId, actor, correlationId,
                    saved.requireValue().revision());
            return saved;
        }
        support.failure(ArenaOperation.DUPLICATE, sourceId, actor, correlationId,
                saved.error().get().code());
        return saved;
    }

    /** @return enabled record only when every validation error is absent */
    public Result<ArenaRepository.Record> enable(final ArenaId id, final long expectedRevision,
                                                 final AuthorizationSubject actor,
                                                 final CorrelationId correlationId) {
        return changeStatus(ArenaOperation.ENABLE, id, expectedRevision,
                ArenaDefinition.Status.ENABLED, actor, correlationId);
    }

    /** @return disabled record */
    public Result<ArenaRepository.Record> disable(final ArenaId id, final long expectedRevision,
                                                  final AuthorizationSubject actor,
                                                  final CorrelationId correlationId) {
        return changeStatus(ArenaOperation.DISABLE, id, expectedRevision,
                ArenaDefinition.Status.DISABLED, actor, correlationId);
    }

    /** @return complete validation report without mutating state */
    public Result<ArenaValidation.Report> validate(final ArenaId id,
                                                   final AuthorizationSubject actor,
                                                   final CorrelationId correlationId) {
        final Result<Optional<ArenaRepository.Record>> found = find(id, actor, correlationId);
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()) { return Result.failure(ArenaFailures.NOT_FOUND); }
        return Result.success(validator.validate(found.requireValue().get().bundle()));
    }

    /** @return whether an exact-revision record was deleted */
    public Result<Boolean> delete(final ArenaId id, final long expectedRevision,
                                  final AuthorizationSubject actor,
                                  final CorrelationId correlationId) {
        if (!authorizeAndBegin(ArenaOperation.DELETE, id, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<Boolean> deleted = repository.delete(id, expectedRevision);
        if (deleted.isSuccess() && deleted.requireValue().booleanValue()) {
            support.after(ArenaOperation.DELETE, id, actor, correlationId, expectedRevision);
        } else {
            support.failure(ArenaOperation.DELETE, id, actor, correlationId,
                    deleted.isFailure() ? deleted.error().get().code() : ArenaFailures.CONFLICT.code());
        }
        return deleted;
    }

    /** @return record restored from its durable last-known-good image */
    public Result<ArenaRepository.Record> restoreLastKnownGood(
            final ArenaId id, final long expectedRevision, final AuthorizationSubject actor,
            final CorrelationId correlationId) {
        if (!authorizeAndBegin(ArenaOperation.RESTORE, id, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<Optional<ArenaRepository.Record>> found = repository.find(id);
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()) { return Result.failure(ArenaFailures.NOT_FOUND); }
        if (found.requireValue().get().revision() != expectedRevision) {
            return Result.failure(ArenaFailures.CONFLICT);
        }
        return finish(ArenaOperation.RESTORE, id, actor, correlationId,
                repository.restoreLastKnownGood(id, expectedRevision));
    }

    private Result<ArenaRepository.Record> changeStatus(
            final ArenaOperation operation, final ArenaId id, final long expectedRevision,
            final ArenaDefinition.Status status, final AuthorizationSubject actor,
            final CorrelationId correlationId) {
        final Result<ArenaRepository.Record> loaded = loadForMutation(
                operation, id, expectedRevision, actor, correlationId);
        if (loaded.isFailure()) { return loaded; }
        final ArenaBundle source = loaded.requireValue().bundle();
        final ArenaValidation.Report report = validator.validate(source);
        if (status == ArenaDefinition.Status.ENABLED && !report.mayEnable()) {
            support.failure(operation, id, actor, correlationId, ArenaFailures.INVALID.code());
            return Result.failure(ArenaFailures.INVALID);
        }
        final ArenaDefinition changed = source.arena().toBuilder().status(status)
                .revision(source.arena().version() + 1L, support.timeSource().now()).build();
        return save(operation, new ArenaBundle(changed, source.map()), expectedRevision,
                report.mayEnable(), actor, correlationId);
    }

    private Result<ArenaRepository.Record> loadForMutation(
            final ArenaOperation operation, final ArenaId id, final long expectedRevision,
            final AuthorizationSubject actor, final CorrelationId correlationId) {
        if (expectedRevision < 1L) { throw new IllegalArgumentException("expectedRevision must be positive"); }
        if (!authorizeAndBegin(operation, id, actor, correlationId)) {
            return Result.failure(ArenaFailures.FORBIDDEN);
        }
        final Result<Optional<ArenaRepository.Record>> found = repository.find(id);
        if (found.isFailure()) { return Result.failure(found.error().get()); }
        if (!found.requireValue().isPresent()) { return Result.failure(ArenaFailures.NOT_FOUND); }
        final ArenaRepository.Record record = found.requireValue().get();
        if (record.revision() != expectedRevision) { return Result.failure(ArenaFailures.CONFLICT); }
        return Result.success(record);
    }

    private boolean authorizeAndBegin(final ArenaOperation operation, final ArenaId id,
                                      final AuthorizationSubject actor,
                                      final CorrelationId correlationId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(correlationId, "correlationId");
        return support.authorize(operation, Objects.requireNonNull(id, "id"), actor, correlationId)
                && support.before(operation, id, actor, correlationId);
    }

    private Result<ArenaRepository.Record> save(
            final ArenaOperation operation, final ArenaBundle bundle, final long expectedRevision,
            final boolean lastKnownGood, final AuthorizationSubject actor,
            final CorrelationId correlationId) {
        return finish(operation, bundle.arenaId(), actor, correlationId,
                repository.save(new ArenaRepository.SaveRequest(bundle, expectedRevision,
                        lastKnownGood)));
    }

    private Result<ArenaRepository.Record> finish(
            final ArenaOperation operation, final ArenaId id, final AuthorizationSubject actor,
            final CorrelationId correlationId, final Result<ArenaRepository.Record> result) {
        if (result.isSuccess()) {
            support.after(operation, id, actor, correlationId, result.requireValue().revision());
        } else {
            support.failure(operation, id, actor, correlationId, result.error().get().code());
        }
        return result;
    }

    private <T> Result<T> failure(final ArenaOperation operation, final ArenaId id,
                                  final AuthorizationSubject actor,
                                  final CorrelationId correlationId, final ApiError error) {
        support.failure(operation, id, actor, correlationId, error.code());
        return Result.failure(error);
    }

    private static ArenaBundle deepCopy(final ArenaBundle source, final ArenaId arenaId,
                                        final MapId mapId, final String displayName,
                                        final WorldKey targetWorld, final Instant now) {
        final ArenaDefinition original = source.arena();
        final ArenaDefinition copy = ArenaDefinition.builder(arenaId, mapId, displayName, now)
                .worlds(targetWorld, original.templateWorld().orElseThrow(
                        () -> new IllegalArgumentException("source template world is absent")))
                .worldAdapter(original.worldAdapter()).group(original.group())
                .modes(original.modes()).playerLimits(original.minimumPlayers(),
                        original.maximumPlayers(), original.teamSize())
                .selection(original.priority(), original.rotationWeight())
                .waitingSpawn(original.waitingSpawn().orElse(null))
                .spectatorSpawn(original.spectatorSpawn().orElse(null))
                .bounds(original.bounds().orElse(null))
                .limits(original.voidY(), original.buildMinimumY(), original.buildMaximumY())
                .teams(new ArrayList<>(original.teams()))
                .generators(new ArrayList<>(original.generators()))
                .npcs(new ArrayList<>(original.npcs()))
                .protectedRegions(new ArrayList<>(original.protectedRegions()))
                .holograms(new ArrayList<>(original.holograms()))
                .speeds(original.speeds()).rules(original.rules()).metadata(original.metadata())
                .status(ArenaDefinition.Status.DISABLED).revision(0L, now).build();
        final MapDefinition map = source.map();
        final MapDefinition mapCopy = new MapDefinition(mapId, displayName, now, now, 0L,
                map.template(), map.group(), map.author(), map.description(), map.supportedModes(),
                map.minimumTeamSize(), map.maximumTeamSize(), map.tags(), map.metadata(),
                DefinitionId.of("zartra", "validation/pending"));
        return new ArenaBundle(copy, mapCopy);
    }
}
