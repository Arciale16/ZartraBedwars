package io.zartra.bedwars.command.paper;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/** Exact Paper 1.21.1 calls isolated from non-transitive platform interfaces. */
final class PaperCommandReflection {
    private static final Class<?> PLAYER = type("org.bukkit.entity.Player");
    private PaperCommandReflection() { }
    static boolean isPlayer(final Object sender) { return PLAYER.isInstance(sender); }
    static UUID uniqueId(final Object sender) { return (UUID) invoke(sender, "getUniqueId"); }
    static String locale(final Object sender) { return invoke(sender, "locale").toString(); }
    static void send(final Object sender, final String message) { invoke(sender, "sendMessage", String.class, message); }
    private static Object invoke(final Object target, final String name) {
        return invoke(target, name, new Class<?>[0], new Object[0]);
    }
    private static Object invoke(final Object target, final String name,
                                 final Class<?> parameter, final Object argument) {
        return invoke(target, name, new Class<?>[] {parameter}, new Object[] {argument});
    }
    private static Object invoke(final Object target, final String name,
                                 final Class<?>[] parameters, final Object[] arguments) {
        try { return target.getClass().getMethod(name, parameters).invoke(target, arguments); }
        catch (IllegalAccessException | NoSuchMethodException failure) { throw new IllegalStateException("Paper command method missing: " + name, failure); }
        catch (InvocationTargetException failure) {
            final Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) { throw runtime; }
            if (cause instanceof Error error) { throw error; }
            throw new IllegalStateException("Paper command method failed: " + name, cause);
        }
    }
    private static Class<?> type(final String name) {
        try { return Class.forName(name, false, PaperCommandReflection.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw new IllegalStateException("Paper command type missing: " + name, failure); }
    }
}
