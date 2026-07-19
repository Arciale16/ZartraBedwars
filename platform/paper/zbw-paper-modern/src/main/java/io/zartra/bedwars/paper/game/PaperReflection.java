package io.zartra.bedwars.paper.game;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Exact-version reflective access used because the approved Paper mirror is non-transitive. */
final class PaperReflection {
    static final Class<?> PLAYER = type("org.bukkit.entity.Player");
    static final Class<?> ITEM_STACK = type("org.bukkit.inventory.ItemStack");
    static final Class<?> LOCATION = type("org.bukkit.Location");
    static final Class<?> WORLD = type("org.bukkit.World");
    static final Class<?> GAME_MODE = type("org.bukkit.GameMode");
    static final Class<?> BAR_COLOR = type("org.bukkit.boss.BarColor");
    static final Class<?> BAR_STYLE = type("org.bukkit.boss.BarStyle");
    static final Class<?> BAR_FLAG = type("org.bukkit.boss.BarFlag");
    static final Class<?> MATERIAL = type("org.bukkit.Material");
    static final Class<?> INVENTORY = type("org.bukkit.inventory.Inventory");
    static final Class<?> BLOCK = type("org.bukkit.block.Block");
    static final Class<?> BLOCK_DATA = type("org.bukkit.block.data.BlockData");
    static final Class<?> ENTITY = type("org.bukkit.entity.Entity");
    static final Class<?> POTION_EFFECT = type("org.bukkit.potion.PotionEffect");
    static final Class<?> POTION_EFFECT_TYPE = type("org.bukkit.potion.PotionEffectType");
    private PaperReflection() { }

    static Object invoke(final Object target, final String method,
                         final Class<?>[] parameterTypes, final Object... arguments) {
        if (target == null) { throw new IllegalArgumentException("reflection target cannot be null"); }
        return call(method(target.getClass(), method, parameterTypes), target, arguments);
    }

    static Object invokeStatic(final Class<?> type, final String method,
                               final Class<?>[] parameterTypes, final Object... arguments) {
        return call(method(type, method, parameterTypes), null, arguments);
    }

    static Object construct(final Class<?> type, final Class<?>[] parameterTypes,
                            final Object... arguments) {
        try {
            final Constructor<?> constructor = type.getConstructor(parameterTypes);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException failure) {
            throw failure("constructor", failure);
        }
    }

    static Object constant(final Class<?> type, final String name) {
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException failure) {
            throw failure("constant " + name, failure);
        }
    }

    static Object emptyArray(final Class<?> component) { return Array.newInstance(component, 0); }

    static Class<?> type(final String name) {
        try {
            return Class.forName(name, false, PaperReflection.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw failure("type " + name, failure);
        }
    }
    private static Method method(final Class<?> type, final String name,
                                 final Class<?>[] parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException failure) {
            throw failure("method " + type.getName() + '#' + name, failure);
        }
    }
    private static Object call(final Method method, final Object target, final Object[] arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException failure) {
            throw failure("inaccessible method", failure);
        }
        catch (InvocationTargetException failure) {
            final Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) { throw runtime; }
            if (cause instanceof Error error) { throw error; }
            throw failure("platform invocation", failure);
        }
    }
    private static IllegalStateException failure(final String operation, final Throwable cause) {
        return new IllegalStateException("Paper 1.21.1 adapter missing " + operation, cause);
    }
}
