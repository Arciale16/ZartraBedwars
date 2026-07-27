package io.zartra.bedwars.paper.atlas;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lifecycle owner for Atlas Paper admission and cleanup. */
public final class AtlasRuntimeBootstrap {
    private final PaperAtlasService service;
    private final AtomicBoolean started = new AtomicBoolean();
    public AtlasRuntimeBootstrap(final PaperAtlasService service) {
        this.service = Objects.requireNonNull(service, "service");
    }
    public AtlasCommandRouter start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Atlas bootstrap already started");
        }
        service.start();
        return new AtlasCommandRouter(service);
    }
    public CompletionStage<Void> stop() {
        if (started.compareAndSet(true, false)) { service.close(); }
        return CompletableFuture.completedFuture(null);
    }
}
