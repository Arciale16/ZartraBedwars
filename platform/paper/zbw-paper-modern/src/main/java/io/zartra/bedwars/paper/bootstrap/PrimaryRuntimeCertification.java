package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Opt-in deterministic Paper runtime certification sequence used only by M06 verification. */
final class PrimaryRuntimeCertification {
    private final JavaPlugin plugin;
    private final PaperFoundationRuntime runtime;
    private final Duration timeout;
    private final List<WorldOperationResult> results = new ArrayList<WorldOperationResult>();

    PrimaryRuntimeCertification(final JavaPlugin plugin, final PaperFoundationRuntime runtime,
                                final Duration timeout) {
        this.plugin = plugin;
        this.runtime = runtime;
        this.timeout = timeout;
    }

    void start() {
        final WorldKey template = WorldKey.of("m06_template");
        final WorldKey clone = WorldKey.of("m06_clone");
        run(WorldOperation.create(WorldOperation.Type.LOAD, template, null, timeout))
                .thenCompose(result -> run(WorldOperation.create(
                        WorldOperation.Type.UNLOAD, template, null, timeout)))
                .thenCompose(result -> run(WorldOperation.create(
                        WorldOperation.Type.CLONE, clone, template, timeout)))
                .thenCompose(result -> run(WorldOperation.create(
                        WorldOperation.Type.RESET, clone, template, timeout)))
                .thenCompose(result -> run(WorldOperation.create(
                        WorldOperation.Type.UNLOAD, clone, null, timeout)))
                .whenComplete((result, failure) -> finish(failure));
    }

    private CompletionStage<WorldOperationResult> run(final WorldOperation operation) {
        return runtime.submit(operation).completion().thenApply(result -> {
            synchronized (results) { results.add(result); }
            if (result.status() != WorldOperationResult.Status.SUCCEEDED) {
                plugin.getLogger().severe("M06 certification operation failed: type="
                        + operation.type() + " status=" + result.status()
                        + " reason=" + result.reason());
                throw new IllegalStateException("certification operation failed: " + result.status());
            }
            return result;
        });
    }

    private void finish(final Throwable failure) {
        final Thread writer = new Thread(() -> {
            final boolean workerThread = !Bukkit.isPrimaryThread();
            final boolean operationsSucceeded = failure == null && results.size() == 5
                    && results.get(4).resources().leakFreeAfterUnload();
            boolean workerShutdown = false;
            try {
                workerShutdown = runtime.stop().toCompletableFuture().get(10, TimeUnit.SECONDS)
                        .booleanValue();
            } catch (Exception exception) {
                plugin.getLogger().severe("M06 certification worker shutdown failed");
            }
            final boolean success = operationsSucceeded && workerShutdown;
            final String evidence = "{\n"
                    + "  \"schema_version\": 1,\n"
                    + "  \"runtime\": \"Paper 1.21.1 build 133\",\n"
                    + "  \"server_sha256\": \""
                    + io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter.SERVER_SHA256
                    + "\",\n  \"certified_at\": \"" + Instant.now() + "\",\n"
                    + "  \"operations\": " + results.size() + ",\n"
                    + "  \"results\": " + resultsJson() + ",\n"
                    + "  \"filesystem_evidence_off_owner\": " + workerThread + ",\n"
                    + "  \"leak_free_after_unload\": "
                    + (results.isEmpty() ? false
                            : results.get(results.size() - 1).resources().leakFreeAfterUnload())
                    + ",\n  \"worker_shutdown\": " + workerShutdown
                    + ",\n  \"success\": " + success + "\n}\n";
            try {
                final Path evidencePath = plugin.getDataFolder().toPath()
                        .resolve("m06-primary-certification.json");
                Files.createDirectories(evidencePath.getParent());
                Files.write(evidencePath, evidence.getBytes(StandardCharsets.UTF_8));
            } catch (IOException exception) {
                plugin.getLogger().severe("M06 certification evidence write failed");
            }
            Bukkit.getScheduler().runTask(plugin, Bukkit::shutdown);
        }, "zbw-m06-certification-evidence");
        writer.setDaemon(false);
        writer.start();
    }

    private String resultsJson() {
        final StringBuilder value = new StringBuilder("[");
        synchronized (results) {
            for (int index = 0; index < results.size(); index++) {
                if (index > 0) { value.append(','); }
                final WorldOperationResult result = results.get(index);
                value.append("{\"type\":\"").append(result.operation().type())
                        .append("\",\"status\":\"").append(result.status())
                        .append("\",\"reason\":\"").append(result.reason()).append("\"}");
            }
        }
        return value.append(']').toString();
    }
}
