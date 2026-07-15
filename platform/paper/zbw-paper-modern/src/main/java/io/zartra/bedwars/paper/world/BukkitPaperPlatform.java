package io.zartra.bedwars.paper.world;

import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldProvider;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/** Bukkit API implementation of the owner-thread Paper boundary. */
final class BukkitPaperPlatform implements PaperPlatform {
    private final Class<?> bukkit = load("org.bukkit.Bukkit");

    @Override public boolean isOwnerThread() {
        return ((Boolean) invokeStatic("isPrimaryThread", new Class<?>[0])).booleanValue();
    }

    @Override public boolean load(final WorldKey world) {
        requireOwner();
        if (world(world) != null) { return true; }
        final Object creator = construct("org.bukkit.WorldCreator", world.value());
        return invoke(creator, "createWorld", new Class<?>[0]) != null;
    }

    @Override public boolean unload(final WorldKey world) {
        requireOwner();
        return world(world) == null || ((Boolean) invokeStatic("unloadWorld",
                new Class<?>[] {String.class, Boolean.TYPE}, world.value(), Boolean.FALSE)).booleanValue();
    }

    @Override public WorldProvider.ResourceSnapshot resources(final WorldKey world) {
        requireOwner();
        final Object loaded = world(world);
        if (loaded == null) {
            return new WorldProvider.ResourceSnapshot(false, 0, 0, 0);
        }
        final Object chunks = invoke(loaded, "getLoadedChunks", new Class<?>[0]);
        final Collection<?> entities = (Collection<?>) invoke(loaded, "getEntities", new Class<?>[0]);
        return new WorldProvider.ResourceSnapshot(true, Array.getLength(chunks), entities.size(), 0);
    }

    private void requireOwner() {
        if (!isOwnerThread()) {
            throw new IllegalStateException("Paper world mutation requires the primary thread");
        }
    }

    private Object world(final WorldKey key) {
        return invokeStatic("getWorld", new Class<?>[] {String.class}, key.value());
    }

    private Object invokeStatic(final String method, final Class<?>[] parameterTypes,
                                final Object... arguments) {
        return invoke(null, bukkit, method, parameterTypes, arguments);
    }

    private static Object invoke(final Object target, final String method,
                                 final Class<?>[] parameterTypes, final Object... arguments) {
        return invoke(target, target.getClass(), method, parameterTypes, arguments);
    }

    private static Object invoke(final Object target, final Class<?> type, final String method,
                                 final Class<?>[] parameterTypes, final Object... arguments) {
        try {
            final Method selected = type.getMethod(method, parameterTypes);
            return selected.invoke(target, arguments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Paper API contract invocation failed: " + method, exception);
        }
    }

    private static Object construct(final String type, final String argument) {
        try { return load(type).getConstructor(String.class).newInstance(argument); }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Paper API construction failed", exception);
        }
    }

    private static Class<?> load(final String type) {
        try { return Class.forName(type); }
        catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Paper API class is unavailable", exception);
        }
    }
}
