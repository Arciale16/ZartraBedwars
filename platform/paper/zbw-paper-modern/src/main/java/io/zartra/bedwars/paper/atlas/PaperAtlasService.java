package io.zartra.bedwars.paper.atlas;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** Thin Paper lifecycle/permission adapter; Atlas policy remains behind {@link AtlasPaperPort}. */
public final class PaperAtlasService implements AutoCloseable {
    public static final String VIEW = "zartrabedwars.atlas.review";
    public static final String STAFF = "zartrabedwars.atlas.staff";
    public static final String ADMIN = "zartrabedwars.atlas.admin";
    private final AtlasPaperPort port;
    private final Executor ownerExecutor;
    private final AtomicBoolean open = new AtomicBoolean();

    public PaperAtlasService(final AtlasPaperPort port, final Executor ownerExecutor) {
        this.port = Objects.requireNonNull(port, "port");
        this.ownerExecutor = Objects.requireNonNull(ownerExecutor, "ownerExecutor");
    }

    public void start() {
        if (!open.compareAndSet(false, true)) { throw new IllegalStateException("Atlas already started"); }
    }

    public CompletionStage<List<AtlasCaseSummary>> list(final AtlasAudience audience) {
        require(VIEW, audience);
        return ensureOpen(port.list(audience.playerId()));
    }

    public CompletionStage<AtlasView> open(final AtlasAudience audience, final AtlasCaseId caseId) {
        require(VIEW, audience);
        return ensureOpen(port.open(audience.playerId(), caseId));
    }

    public CompletionStage<Boolean> review(final AtlasAudience audience, final AtlasCaseId caseId) {
        require(VIEW, audience);
        return ensureOpen(port.beginReview(audience.playerId(), caseId));
    }

    public CompletionStage<Boolean> verdict(final AtlasAudience audience, final AtlasCaseId caseId,
                                            final String verdict, final String reason) {
        require(VIEW, audience);
        return ensureOpen(port.submitVerdict(
                audience.playerId(), caseId, verdict, reason));
    }

    public CompletionStage<Boolean> finalReview(final AtlasAudience audience,
                                                final AtlasCaseId caseId,
                                                final String disposition) {
        require(STAFF, audience);
        return ensureOpen(port.finalReview(audience.playerId(), caseId, disposition));
    }

    public CompletionStage<String> diagnostics(final AtlasAudience audience) {
        require(ADMIN, audience);
        return ensureOpen(port.diagnostics());
    }

    /** Presents completion only on the injected Paper owner-thread executor. */
    public <T> CompletionStage<T> present(final CompletionStage<T> source,
                                          final AtlasAudience audience,
                                          final java.util.function.Function<T, String> message) {
        return source.thenApplyAsync(value -> {
            audience.present(message.apply(value));
            return value;
        }, ownerExecutor);
    }

    private <T> CompletionStage<T> ensureOpen(final CompletionStage<T> stage) {
        if (!open.get()) {
            CompletableFuture<T> failure = new CompletableFuture<>();
            failure.completeExceptionally(new IllegalStateException("Atlas runtime closed"));
            return failure;
        }
        return stage;
    }

    private static void require(final String permission, final AtlasAudience audience) {
        Objects.requireNonNull(audience, "audience");
        if (!audience.hasPermission(permission)) { throw new SecurityException("permission denied"); }
    }

    @Override public void close() { open.set(false); }
}
