package io.zartra.bedwars.integration.placeholderapi;

import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Lifecycle entrypoint for PlaceholderAPI integration.
 *
 * Runtime adapter is intentionally isolated in a separate module to keep core builds offline-friendly.
 */
public final class PlaceholderApiIntegration {

    private static final String RUNTIME_ADAPTER =
            "io.zartra.bedwars.integration.placeholderapi.runtime.PlaceholderApiRuntimeIntegration";

    private final PlaceholderApiLifecycle lifecycle;
    private Object runtimeHandle;
    private boolean registered;

    public PlaceholderApiIntegration(final PlaceholderApiLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public boolean initialize(final Object plugin) {
        if (plugin == null) {
            return false;
        }
        final PlaceholderRegistry registry = lifecycle.registry();
        final Object handle = invokeStart(plugin, lifecycle);
        if (handle == null) {
            return false;
        }
        runtimeHandle = handle;
        registered = true;
        return true;
    }

    public void close() {
        if (runtimeHandle != null) {
            invokeClose(runtimeHandle);
            runtimeHandle = null;
        }
        registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }

    private static Object invokeStart(final Object plugin, final PlaceholderApiLifecycle lifecycle) {
        try {
            final Class<?> runtimeClass = Class.forName(RUNTIME_ADAPTER);
            final Method start = runtimeClass.getMethod("initialize", Object.class, PlaceholderApiLifecycle.class);
            return start.invoke(null, plugin, lifecycle);
        } catch (final ClassNotFoundException | NoSuchMethodException e) {
            return null;
        } catch (final IllegalAccessException | InvocationTargetException e) {
            final Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException("Runtime PlaceholderAPI adapter failed", cause);
        }
    }

    private static void invokeClose(final Object handle) {
        try {
            final Class<?> runtimeClass = Class.forName(RUNTIME_ADAPTER);
            final Method close = runtimeClass.getMethod("close", Object.class);
            close.invoke(null, handle);
        } catch (final Exception ignored) {
            // Keep plugin shutdown tolerant when runtime adapter is absent.
        }
    }
}
