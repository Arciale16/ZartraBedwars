package io.zartra.bedwars.paper.replay;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Registration and lifecycle owner for the M17 Paper replay runtime. */
public final class ReplayRuntimeBootstrap {
    private final PaperReplayService service;
    private final DisconnectRegistration registration;
    private final AtomicBoolean started = new AtomicBoolean();
    private AutoCloseable disconnectListener;

    /** Creates a stopped bootstrap. */
    public ReplayRuntimeBootstrap(final PaperReplayService service,
                                  final DisconnectRegistration registration) {
        this.service = Objects.requireNonNull(service, "service");
        this.registration = Objects.requireNonNull(registration, "registration");
    }

    /** Starts service admission and registers safe disconnect cleanup. */
    public synchronized void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("replay bootstrap already started");
        }
        service.start();
        try {
            disconnectListener = Objects.requireNonNull(
                    registration.register(service::stop), "disconnect registration");
        } catch (RuntimeException failure) {
            started.set(false);
            service.close();
            throw failure;
        }
    }

    /** Stops registration and schedules controlled cleanup without blocking the owner thread. */
    public synchronized CompletionStage<Void> stop() {
        if (!started.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }
        if (disconnectListener != null) {
            try {
                disconnectListener.close();
            } catch (Exception ignored) {
                // Service cleanup remains authoritative when listener unregistration fails.
            } finally {
                disconnectListener = null;
            }
        }
        return service.close();
    }

    /** @return command/service foundation owned by this runtime */
    public PaperReplayCommands commands() { return new PaperReplayCommands(service); }

    /** Registers one player-disconnect callback. */
    public interface DisconnectRegistration {
        /** @return closeable listener registration */ AutoCloseable register(Consumer<UUID> disconnect);
    }
}
