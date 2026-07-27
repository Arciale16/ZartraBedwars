package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.paper.replay.ReplayRuntimeBootstrap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns viewer disconnect registration and presentation cleanup. */
public final class ReplayViewerBootstrap {
    private final ReplayViewerAdapter viewer;
    private final ReplayRuntimeBootstrap.DisconnectRegistration registration;
    private final AtomicBoolean started = new AtomicBoolean();
    private AutoCloseable disconnectListener;

    /** Creates a stopped viewer bootstrap. */
    public ReplayViewerBootstrap(final ReplayViewerAdapter viewer,
                                 final ReplayRuntimeBootstrap.DisconnectRegistration registration) {
        this.viewer = Objects.requireNonNull(viewer, "viewer");
        this.registration = Objects.requireNonNull(registration, "registration");
    }

    /** Registers disconnect cleanup exactly once. */
    public synchronized void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("replay viewer bootstrap already started");
        }
        try {
            disconnectListener = Objects.requireNonNull(
                    registration.register(viewer::disconnect), "disconnect registration");
        } catch (RuntimeException failure) {
            started.set(false);
            throw failure;
        }
    }

    /** Unregisters the listener and clears every owned viewer presentation. */
    public synchronized void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        if (disconnectListener != null) {
            try {
                disconnectListener.close();
            } catch (Exception ignored) {
                // Viewer cleanup below remains authoritative.
            } finally {
                disconnectListener = null;
            }
        }
        viewer.close();
    }
}
