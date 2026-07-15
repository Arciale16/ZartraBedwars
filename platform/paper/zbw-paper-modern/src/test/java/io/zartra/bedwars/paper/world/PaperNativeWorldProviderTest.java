package io.zartra.bedwars.paper.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.scheduler.CancellationToken;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PaperNativeWorldProviderTest {
    private static final CancellationToken ACTIVE = () -> false;
    @TempDir Path root;

    @Test void nativeLoadCreatesFilesystemOffThreadAndMutatesOnOwner() throws Exception {
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Plan plan = provider.plan(operation(WorldOperation.Type.LOAD,
                "arena", null));
        assertEquals(WorldProvider.Affinity.WORKER, plan.steps().get(0).affinity());
        assertEquals(WorldProvider.Affinity.OWNER, plan.steps().get(1).affinity());
        run(plan, platform);
        assertTrue(Files.isDirectory(root.resolve("arena")));
        assertTrue(provider.snapshot(WorldKey.of("arena")).loaded());
        assertEquals(1, platform.loadCalls);
    }

    @Test void nativeCloneCopiesTemplateWithoutIdentityAndLoadsTarget() throws Exception {
        template("template", "v1");
        Files.write(root.resolve("template/uid.dat"), "identity".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("template/session.lock"), "lock".getBytes(StandardCharsets.UTF_8));
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        run(provider.plan(operation(WorldOperation.Type.CLONE, "clone", "template")), platform);
        assertEquals("v1", Files.readString(root.resolve("clone/data.txt")));
        assertFalse(Files.exists(root.resolve("clone/uid.dat")));
        assertFalse(Files.exists(root.resolve("clone/session.lock")));
        assertTrue(provider.snapshot(WorldKey.of("clone")).loaded());
    }

    @Test void nativeResetUnloadsReplacesReloadsAndDeletesBackup() throws Exception {
        template("template", "new");
        template("arena", "old");
        final FakePlatform platform = new FakePlatform();
        platform.loaded.set(true);
        final PaperNativeWorldProvider provider = provider(platform);
        run(provider.plan(operation(WorldOperation.Type.RESET, "arena", "template")), platform);
        assertEquals("new", Files.readString(root.resolve("arena/data.txt")));
        assertEquals(1, platform.unloadCalls);
        assertEquals(1, platform.loadCalls);
        try (Stream<Path> entries = Files.list(root)) {
            assertEquals(2L, entries.count());
        }
    }

    @Test void nativeUnloadProducesLeakFreeEvidence() {
        final FakePlatform platform = new FakePlatform();
        platform.loaded.set(true);
        final PaperNativeWorldProvider provider = provider(platform);
        run(provider.plan(operation(WorldOperation.Type.UNLOAD, "arena", null)), platform);
        assertTrue(provider.snapshot(WorldKey.of("arena")).leakFreeAfterUnload());
        assertEquals(1, platform.unloadCalls);
    }

    @Test void wrongThreadAndCancellationFailClosed() throws Exception {
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Plan load = provider.plan(operation(WorldOperation.Type.LOAD,
                "arena", null));
        platform.owner.set(true);
        assertFalse(load.steps().get(0).execute(ACTIVE).isSuccess());
        platform.owner.set(false);
        assertFalse(load.steps().get(0).execute(() -> true).isSuccess());
        Files.createDirectories(root.resolve("arena"));
        assertTrue(load.steps().get(0).execute(ACTIVE).isSuccess());
        assertFalse(load.steps().get(1).execute(ACTIVE).isSuccess());
    }

    @Test void copyRollbackRemovesPartialTarget() throws Exception {
        template("template", "value");
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Step copy = provider.plan(operation(
                WorldOperation.Type.CLONE, "clone", "template")).steps().get(0);
        assertTrue(copy.execute(ACTIVE).isSuccess());
        assertTrue(copy.rollback(ACTIVE).isSuccess());
        assertFalse(Files.exists(root.resolve("clone")));
    }

    @Test void trackingAndConfigurationBoundsAreValidated() {
        final FakePlatform platform = new FakePlatform();
        assertThrows(IllegalArgumentException.class,
                () -> new PaperNativeWorldProvider(root, 0, platform));
        assertThrows(IllegalArgumentException.class,
                () -> new PaperNativeWorldProvider(root, 257, platform));
        final PaperNativeWorldProvider provider = new PaperNativeWorldProvider(root, 1, platform);
        assertTrue(provider.snapshot(WorldKey.of("unknown")).leakFreeAfterUnload());
        provider.plan(operation(WorldOperation.Type.LOAD, "one", null));
        provider.plan(operation(WorldOperation.Type.LOAD, "one", null));
        assertThrows(IllegalStateException.class,
                () -> provider.plan(operation(WorldOperation.Type.LOAD, "two", null)));
    }

    @Test void filesystemFailuresAreTypedAndPartialCopiesAreRemoved() throws Exception {
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Step missingSource = provider.plan(operation(
                WorldOperation.Type.CLONE, "clone", "missing")).steps().get(0);
        assertFalse(missingSource.execute(ACTIVE).isSuccess());
        template("template", "value");
        template("existing", "old");
        final WorldProvider.Step existingTarget = provider.plan(operation(
                WorldOperation.Type.CLONE, "existing", "template")).steps().get(0);
        assertFalse(existingTarget.execute(ACTIVE).isSuccess());

        final AtomicBoolean cancel = new AtomicBoolean();
        final CancellationToken changing = () -> cancel.getAndSet(true);
        final WorldProvider.Step cancelled = provider.plan(operation(
                WorldOperation.Type.CLONE, "cancelled", "template")).steps().get(0);
        assertFalse(cancelled.execute(changing).isSuccess());
        assertFalse(Files.exists(root.resolve("cancelled")));
    }

    @Test void resetCompensationRestoresOriginalAndSupportsInitiallyMissingTarget() throws Exception {
        template("template", "new");
        template("arena", "old");
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Plan reset = provider.plan(operation(
                WorldOperation.Type.RESET, "arena", "template"));
        platform.owner.set(true);
        assertTrue(reset.steps().get(0).execute(ACTIVE).isSuccess());
        platform.owner.set(false);
        assertTrue(reset.steps().get(1).execute(ACTIVE).isSuccess());
        assertEquals("new", Files.readString(root.resolve("arena/data.txt")));
        assertTrue(reset.steps().get(1).rollback(ACTIVE).isSuccess());
        assertEquals("old", Files.readString(root.resolve("arena/data.txt")));

        run(provider.plan(operation(WorldOperation.Type.RESET,
                "initially_missing", "template")), platform);
        assertEquals("new", Files.readString(root.resolve("initially_missing/data.txt")));
    }

    @Test void resetFailureRestoresBackupAndPlatformFailureFailsClosed() throws Exception {
        template("arena", "old");
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Plan reset = provider.plan(operation(
                WorldOperation.Type.RESET, "arena", "missing"));
        platform.owner.set(true);
        assertTrue(reset.steps().get(0).execute(ACTIVE).isSuccess());
        platform.owner.set(false);
        assertFalse(reset.steps().get(1).execute(ACTIVE).isSuccess());
        assertEquals("old", Files.readString(root.resolve("arena/data.txt")));

        platform.succeed.set(false);
        final WorldProvider.Step load = provider.plan(operation(
                WorldOperation.Type.LOAD, "platform_failure", null)).steps().get(1);
        platform.owner.set(true);
        assertFalse(load.execute(ACTIVE).isSuccess());
        platform.succeed.set(true);
        platform.invertSnapshot.set(true);
        assertFalse(load.execute(ACTIVE).isSuccess());
        platform.owner.set(false);
        assertFalse(load.rollback(ACTIVE).isSuccess());
    }

    @Test void existingLoadDirectoryRollbackIsIdempotent() throws Exception {
        template("existing", "value");
        final FakePlatform platform = new FakePlatform();
        final PaperNativeWorldProvider provider = provider(platform);
        final WorldProvider.Step ensure = provider.plan(operation(
                WorldOperation.Type.LOAD, "existing", null)).steps().get(0);
        assertTrue(ensure.execute(ACTIVE).isSuccess());
        assertTrue(ensure.rollback(ACTIVE).isSuccess());
        assertTrue(Files.exists(root.resolve("existing/data.txt")));
    }

    private PaperNativeWorldProvider provider(final FakePlatform platform) {
        return new PaperNativeWorldProvider(root, 8, platform);
    }

    private void template(final String name, final String value) throws Exception {
        Files.createDirectories(root.resolve(name));
        Files.write(root.resolve(name + "/data.txt"), value.getBytes(StandardCharsets.UTF_8));
    }

    private static WorldOperation operation(final WorldOperation.Type type, final String target,
                                            final String source) {
        return WorldOperation.create(type, WorldKey.of(target),
                source == null ? null : WorldKey.of(source), Duration.ofSeconds(5));
    }

    private static void run(final WorldProvider.Plan plan, final FakePlatform platform) {
        final List<WorldProvider.Step> completed = new ArrayList<WorldProvider.Step>();
        for (WorldProvider.Step step : plan.steps()) {
            platform.owner.set(step.affinity() == WorldProvider.Affinity.OWNER);
            final WorldProvider.StepResult result = step.execute(ACTIVE);
            if (!result.isSuccess()) {
                for (int index = completed.size() - 1; index >= 0; index--) {
                    final WorldProvider.Step rollback = completed.get(index);
                    platform.owner.set(rollback.affinity() == WorldProvider.Affinity.OWNER);
                    rollback.rollback(ACTIVE);
                }
                throw new AssertionError("step failed: " + result.reason());
            }
            completed.add(step);
        }
        platform.owner.set(false);
    }

    private static final class FakePlatform implements PaperPlatform {
        private final AtomicBoolean owner = new AtomicBoolean();
        private final AtomicBoolean loaded = new AtomicBoolean();
        private final AtomicBoolean succeed = new AtomicBoolean(true);
        private final AtomicBoolean invertSnapshot = new AtomicBoolean();
        private int loadCalls;
        private int unloadCalls;
        @Override public boolean isOwnerThread() { return owner.get(); }
        @Override public boolean load(final WorldKey world) {
            if (!owner.get()) { throw new AssertionError("load off owner"); }
            loadCalls++;
            loaded.set(true);
            return succeed.get();
        }
        @Override public boolean unload(final WorldKey world) {
            if (!owner.get()) { throw new AssertionError("unload off owner"); }
            unloadCalls++;
            loaded.set(false);
            return succeed.get();
        }
        @Override public WorldProvider.ResourceSnapshot resources(final WorldKey world) {
            if (!owner.get()) { throw new AssertionError("snapshot off owner"); }
            final boolean reportedLoaded = invertSnapshot.get() != loaded.get();
            return reportedLoaded ? new WorldProvider.ResourceSnapshot(true, 1, 2, 0)
                    : new WorldProvider.ResourceSnapshot(false, 0, 0, 0);
        }
    }
}
