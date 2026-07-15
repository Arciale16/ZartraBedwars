package io.zartra.bedwars.paper.world;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldProvider;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Primary native Paper provider with bounded state and explicit thread-affinity steps. */
public final class PaperNativeWorldProvider implements WorldProvider {
    private static final ProviderId ID = ProviderId.of("zartra", "paper_native_world");
    private static final DefinitionId IO_FAILURE = DefinitionId.of("zartra", "world/io_failure");
    private static final DefinitionId PLATFORM_FAILURE = DefinitionId.of("zartra", "world/platform_failure");
    private static final DefinitionId WRONG_THREAD = DefinitionId.of("zartra", "world/wrong_thread");
    private static final DefinitionId CANCELLED = DefinitionId.of("zartra", "world/cancelled");
    private final Path worldRoot;
    private final PaperPlatform platform;
    private final int maximumTrackedWorlds;
    private final Map<WorldKey, ResourceSnapshot> resources =
            new LinkedHashMap<WorldKey, ResourceSnapshot>();

    /** Creates the production provider rooted in Paper's validated world container. */
    public PaperNativeWorldProvider(final Path worldRoot, final int maximumTrackedWorlds) {
        this(worldRoot, maximumTrackedWorlds, new BukkitPaperPlatform());
    }

    PaperNativeWorldProvider(final Path worldRoot, final int maximumTrackedWorlds,
                             final PaperPlatform platform) {
        this.worldRoot = Objects.requireNonNull(worldRoot, "worldRoot").toAbsolutePath().normalize();
        this.platform = Objects.requireNonNull(platform, "platform");
        if (maximumTrackedWorlds < 1 || maximumTrackedWorlds > 256) {
            throw new IllegalArgumentException("maximumTrackedWorlds must be between 1 and 256");
        }
        this.maximumTrackedWorlds = maximumTrackedWorlds;
    }

    @Override public ProviderId id() { return ID; }

    @Override public Plan plan(final WorldOperation operation) {
        Objects.requireNonNull(operation, "operation");
        reserve(operation.target());
        switch (operation.type()) {
            case LOAD:
                return new Plan(operation, Arrays.<Step>asList(
                        new EnsureDirectoryStep(operation.target()),
                        new PlatformStep(operation.target(), true)));
            case CLONE:
                return new Plan(operation, Arrays.<Step>asList(
                        new CopyStep(operation.source().get(), operation.target()),
                        new PlatformStep(operation.target(), true)));
            case RESET:
                return new Plan(operation, Arrays.<Step>asList(
                        new PlatformStep(operation.target(), false),
                        new ReplaceStep(operation.source().get(), operation.target(), operation),
                        new PlatformStep(operation.target(), true),
                        new DiscardBackupStep(operation.target(), operation)));
            case UNLOAD:
                return new Plan(operation, Collections.<Step>singletonList(
                        new PlatformStep(operation.target(), false)));
            default:
                throw new IllegalArgumentException("unsupported world operation");
        }
    }

    @Override public synchronized ResourceSnapshot snapshot(final WorldKey world) {
        final ResourceSnapshot snapshot = resources.get(Objects.requireNonNull(world, "world"));
        return snapshot == null ? new ResourceSnapshot(false, 0, 0, 0) : snapshot;
    }

    private synchronized void reserve(final WorldKey world) {
        if (!resources.containsKey(world) && resources.size() >= maximumTrackedWorlds) {
            throw new IllegalStateException("world provider tracking capacity exhausted");
        }
        if (!resources.containsKey(world)) {
            resources.put(world, new ResourceSnapshot(false, 0, 0, 0));
        }
    }

    private synchronized void update(final WorldKey world, final ResourceSnapshot snapshot) {
        resources.put(world, snapshot);
    }

    private Path path(final WorldKey key) {
        final Path resolved = worldRoot.resolve(key.value()).normalize();
        if (!resolved.startsWith(worldRoot)) {
            throw new IllegalArgumentException("world path escapes the configured root");
        }
        return resolved;
    }

    private Path backup(final WorldKey key, final WorldOperation operation) {
        return worldRoot.resolve(".zbw-" + key.value() + "-" + operation.operationId()).normalize();
    }

    private abstract class AbstractStep implements Step {
        private final DefinitionId id;
        private final Affinity affinity;
        AbstractStep(final String id, final Affinity affinity) {
            this.id = DefinitionId.of("zartra", "world/" + id);
            this.affinity = affinity;
        }
        @Override public DefinitionId id() { return id; }
        @Override public Affinity affinity() { return affinity; }
        final StepResult cancelled(final CancellationToken token) {
            return token.isCancellationRequested() ? StepResult.failure(CANCELLED) : null;
        }
        final StepResult thread(final boolean ownerRequired) {
            return platform.isOwnerThread() == ownerRequired ? null : StepResult.failure(WRONG_THREAD);
        }
    }

    private final class EnsureDirectoryStep extends AbstractStep {
        private final WorldKey target;
        private boolean created;
        EnsureDirectoryStep(final WorldKey target) {
            super("ensure_directory", Affinity.WORKER);
            this.target = target;
        }
        @Override public StepResult execute(final CancellationToken token) {
            final StepResult invalid = first(cancelled(token), thread(false));
            if (invalid != null) { return invalid; }
            try {
                final Path targetPath = path(target);
                created = Files.notExists(targetPath);
                Files.createDirectories(targetPath);
                return StepResult.success();
            } catch (IOException exception) { return StepResult.failure(IO_FAILURE); }
        }
        @Override public StepResult rollback(final CancellationToken token) {
            try {
                if (created) { delete(path(target)); }
                return StepResult.success();
            }
            catch (IOException exception) { return StepResult.failure(IO_FAILURE); }
        }
    }

    private final class CopyStep extends AbstractStep {
        private final WorldKey source;
        private final WorldKey target;
        CopyStep(final WorldKey source, final WorldKey target) {
            super("copy_template", Affinity.WORKER);
            this.source = source;
            this.target = target;
        }
        @Override public StepResult execute(final CancellationToken token) {
            final StepResult invalid = first(cancelled(token), thread(false));
            if (invalid != null) { return invalid; }
            try {
                copy(path(source), path(target), token);
                return StepResult.success();
            }
            catch (IOException exception) { return StepResult.failure(IO_FAILURE); }
        }
        @Override public StepResult rollback(final CancellationToken token) {
            try {
                delete(path(target));
                return StepResult.success();
            }
            catch (IOException exception) { return StepResult.failure(IO_FAILURE); }
        }
    }

    private final class ReplaceStep extends AbstractStep {
        private final WorldKey source;
        private final WorldKey target;
        private final Path backup;
        ReplaceStep(final WorldKey source, final WorldKey target, final WorldOperation operation) {
            super("replace_world", Affinity.WORKER);
            this.source = source;
            this.target = target;
            this.backup = backup(target, operation);
        }
        @Override public StepResult execute(final CancellationToken token) {
            final StepResult invalid = first(cancelled(token), thread(false));
            if (invalid != null) { return invalid; }
            try {
                final Path targetPath = path(target);
                delete(backup);
                if (Files.exists(targetPath)) {
                    move(targetPath, backup);
                }
                copy(path(source), targetPath, token);
                return StepResult.success();
            } catch (IOException exception) {
                try {
                    delete(path(target));
                    if (Files.exists(backup)) { move(backup, path(target)); }
                } catch (IOException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                return StepResult.failure(IO_FAILURE);
            }
        }
        @Override public StepResult rollback(final CancellationToken token) {
            try {
                delete(path(target));
                if (Files.exists(backup)) {
                    move(backup, path(target));
                }
                return StepResult.success();
            } catch (IOException exception) { return StepResult.failure(IO_FAILURE); }
        }
    }

    private final class DiscardBackupStep extends AbstractStep {
        private final Path backup;
        DiscardBackupStep(final WorldKey target, final WorldOperation operation) {
            super("discard_backup", Affinity.WORKER);
            this.backup = backup(target, operation);
        }
        @Override public StepResult execute(final CancellationToken token) {
            final StepResult invalid = first(cancelled(token), thread(false));
            if (invalid != null) { return invalid; }
            try {
                delete(backup);
                return StepResult.success();
            }
            catch (IOException exception) { return StepResult.failure(IO_FAILURE); }
        }
        @Override public StepResult rollback(final CancellationToken token) { return StepResult.success(); }
    }

    private final class PlatformStep extends AbstractStep {
        private final WorldKey target;
        private final boolean load;
        PlatformStep(final WorldKey target, final boolean load) {
            super(load ? "load_world" : "unload_world", Affinity.OWNER);
            this.target = target;
            this.load = load;
        }
        @Override public StepResult execute(final CancellationToken token) {
            final StepResult invalid = first(cancelled(token), thread(true));
            if (invalid != null) { return invalid; }
            return apply(load);
        }
        @Override public StepResult rollback(final CancellationToken token) { return apply(!load); }
        private StepResult apply(final boolean desiredLoaded) {
            if (!platform.isOwnerThread()) { return StepResult.failure(WRONG_THREAD); }
            final boolean success = desiredLoaded ? platform.load(target) : platform.unload(target);
            if (!success) { return StepResult.failure(PLATFORM_FAILURE); }
            final ResourceSnapshot snapshot = platform.resources(target);
            update(target, snapshot);
            return desiredLoaded == snapshot.loaded()
                    ? StepResult.success() : StepResult.failure(PLATFORM_FAILURE);
        }
    }

    private static StepResult first(final StepResult first, final StepResult second) {
        return first == null ? second : first;
    }

    private static void copy(final Path source, final Path target,
                             final CancellationToken cancellation) throws IOException {
        if (!Files.isDirectory(source) || Files.exists(target)) {
            throw new IOException("source must exist and target must not exist");
        }
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult preVisitDirectory(final Path directory,
                                                                   final BasicFileAttributes attributes)
                        throws IOException {
                    requireActive(cancellation);
                    final Path destination = target.resolve(source.relativize(directory));
                    Files.createDirectories(destination);
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(final Path file,
                                                           final BasicFileAttributes attributes)
                        throws IOException {
                    requireActive(cancellation);
                    final String name = file.getFileName().toString();
                    if (!"uid.dat".equals(name) && !"session.lock".equals(name)) {
                        Files.copy(file, target.resolve(source.relativize(file)),
                                StandardCopyOption.COPY_ATTRIBUTES);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            delete(target);
            throw exception;
        }
    }

    private static void requireActive(final CancellationToken token) throws IOException {
        if (token.isCancellationRequested()) { throw new IOException("world operation cancelled"); }
    }

    private static void move(final Path source, final Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(source, target); }
    }

    private static void delete(final Path root) throws IOException {
        if (Files.notExists(root)) { return; }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(final Path file,
                                                       final BasicFileAttributes attributes)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(final Path directory,
                                                                 final IOException exception)
                    throws IOException {
                if (exception != null) { throw exception; }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
