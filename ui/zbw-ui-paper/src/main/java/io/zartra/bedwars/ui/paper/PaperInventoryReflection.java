package io.zartra.bedwars.ui.paper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/** Exact Paper 1.21.1 reflective inventory port for the approved non-transitive API mirror. */
public final class PaperInventoryReflection implements PaperGuiAdapter.InventoryPort {
    private static final Class<?> BUKKIT = type("org.bukkit.Bukkit");
    private static final Class<?> HOLDER = type("org.bukkit.inventory.InventoryHolder");
    private static final Class<?> INVENTORY = type("org.bukkit.inventory.Inventory");
    private static final Class<?> MATERIAL = type("org.bukkit.Material");
    private static final Class<?> ITEM_STACK = type("org.bukkit.inventory.ItemStack");
    private static final Class<?> ITEM_META = type("org.bukkit.inventory.meta.ItemMeta");

    @Override public Object create(final int size, final String title) {
        return invokeStatic(BUKKIT, "createInventory", new Class<?>[] {HOLDER, int.class, String.class},
                null, Integer.valueOf(size), title);
    }
    @Override public void clear(final Object inventory) {
        invoke(inventory, method(INVENTORY, "clear", new Class<?>[0]), new Object[0]);
    }
    @Override public void set(final Object inventory, final int slot, final PaperGuiAdapter.RenderedItem item) {
        final Object material = enumValue(MATERIAL, item.material());
        final Object stack = construct(ITEM_STACK, new Class<?>[] {MATERIAL}, material);
        final Object meta = invoke(stack, "getItemMeta", new Class<?>[0]);
        invoke(meta, method(ITEM_META, "setDisplayName", new Class<?>[] {String.class}),
                new Object[] {item.name()});
        invoke(meta, method(ITEM_META, "setLore", new Class<?>[] {List.class}),
                new Object[] {item.lore()});
        invoke(stack, "setItemMeta", new Class<?>[] {ITEM_META}, meta);
        invoke(inventory, method(INVENTORY, "setItem", new Class<?>[] {int.class, ITEM_STACK}),
                new Object[] {Integer.valueOf(slot), stack});
    }
    @Override public void open(final Object viewer, final Object inventory) { invoke(viewer, "openInventory", new Class<?>[] {INVENTORY}, inventory); }
    @Override public void close(final Object viewer) { invoke(viewer, "closeInventory", new Class<?>[0]); }
    @Override public Object inventory(final Object event) { return invoke(event, "getInventory", new Class<?>[0]); }
    @Override public int rawSlot(final Object event) { return ((Integer) invoke(event, "getRawSlot", new Class<?>[0])).intValue(); }
    @Override public void cancel(final Object event) { invoke(event, "setCancelled", new Class<?>[] {boolean.class}, Boolean.TRUE); }

    private static Object enumValue(final Class<?> type, final String value) {
        @SuppressWarnings({"rawtypes", "unchecked"}) final Object result = Enum.valueOf((Class) type, value);
        return result;
    }
    private static Object construct(final Class<?> type, final Class<?>[] parameterTypes, final Object... arguments) {
        try { final Constructor<?> constructor = type.getConstructor(parameterTypes);
         return constructor.newInstance(arguments);
        }
        catch (ReflectiveOperationException failure) { throw platformFailure("constructor", failure); }
    }
    private static Object invokeStatic(final Class<?> type, final String name,
                                       final Class<?>[] parameterTypes, final Object... arguments) {
        return invoke(null, method(type, name, parameterTypes), arguments);
    }
    private static Object invoke(final Object target, final String name,
                                 final Class<?>[] parameterTypes, final Object... arguments) {
        return invoke(target, method(target.getClass(), name, parameterTypes), arguments);
    }
    private static Object invoke(final Object target, final Method method, final Object[] arguments) {
        try { return method.invoke(target, arguments); }
        catch (IllegalAccessException failure) { throw platformFailure("inaccessible method", failure); }
        catch (InvocationTargetException failure) {
            final Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) { throw runtime; }
            if (cause instanceof Error error) { throw error; }
            throw platformFailure("platform method", cause);
        }
    }
    private static Method method(final Class<?> type, final String name, final Class<?>[] parameters) {
        try { return type.getMethod(name, parameters); }
        catch (NoSuchMethodException failure) { throw platformFailure("method " + name, failure); }
    }
    private static Class<?> type(final String name) {
        try { return Class.forName(name, false, PaperInventoryReflection.class.getClassLoader()); }
        catch (ClassNotFoundException failure) { throw platformFailure("type " + name, failure); }
    }
    private static IllegalStateException platformFailure(final String operation, final Throwable failure) {
        return new IllegalStateException("Paper 1.21.1 inventory adapter missing " + operation, failure);
    }
}
