package io.zartra.bedwars.paper.replay.visual;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Primary Paper representation adapter with an isolated reflection boundary. */
public final class BukkitReplayVisualRenderer implements ReplayVisualRenderer {
    private final ReflectionOperations reflection;

    /** Creates the primary Bukkit reflection adapter. */
    public BukkitReplayVisualRenderer() {
        this(new JvmReflectionOperations());
    }

    BukkitReplayVisualRenderer(final ReflectionOperations reflection) {
        this.reflection = Objects.requireNonNull(reflection, "reflection");
    }

    @Override public Object spawn(final UUID viewerId, final VisualEntityState state) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(state, "state");
        final Object world = world(state.position().world());
        final Object location = location(world, state.position());
        final Object stand = reflection.invoke(world, "spawn",
                new Class<?>[]{reflection.type("org.bukkit.Location"), Class.class},
                new Object[]{location, reflection.type("org.bukkit.entity.ArmorStand")});
        invokeBoolean(stand, "setPersistent", false);
        invokeBoolean(stand, "setGravity", false);
        invokeBoolean(stand, "setBasePlate", false);
        invokeBoolean(stand, "setArms", true);
        reflection.invoke(stand, "setCustomName", new Class<?>[]{String.class},
                new Object[]{state.displayName()});
        invokeBoolean(stand, "setCustomNameVisible", true);
        applyEquipment(stand, state.equipment());
        return stand;
    }

    @Override public void update(final UUID viewerId, final Object handle,
                                 final VisualEntityState state) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(state, "state");
        final Object target = location(world(state.position().world()), state.position());
        reflection.invoke(handle, "teleport",
                new Class<?>[]{reflection.type("org.bukkit.Location")}, new Object[]{target});
        reflection.invoke(handle, "setCustomName", new Class<?>[]{String.class},
                new Object[]{state.displayName()});
        applyEquipment(handle, state.equipment());
    }

    @Override public void remove(final UUID viewerId, final Object handle) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(handle, "handle");
        reflection.invoke(handle, "remove", new Class<?>[0], new Object[0]);
    }

    private void applyEquipment(final Object stand, final VisualEquipmentState state) {
        final Object equipment = reflection.invoke(
                stand, "getEquipment", new Class<?>[0], new Object[0]);
        if (equipment == null) { return; }
        reflection.invoke(equipment, "clear", new Class<?>[0], new Object[0]);
        for (Map.Entry<String, String> entry : state.items().entrySet()) {
            final String method = equipmentMethod(entry.getKey());
            if (method != null) {
                reflection.invoke(equipment, method,
                        new Class<?>[]{reflection.type("org.bukkit.inventory.ItemStack")},
                        new Object[]{item(entry.getValue())});
            }
        }
    }

    private static String equipmentMethod(final String slot) {
        switch (slot.toLowerCase(Locale.ROOT)) {
            case "main_hand":
            case "hand":
                return "setItemInMainHand";
            case "off_hand":
                return "setItemInOffHand";
            case "head":
            case "helmet":
                return "setHelmet";
            case "chest":
            case "chestplate":
                return "setChestplate";
            case "legs":
            case "leggings":
                return "setLeggings";
            case "feet":
            case "boots":
                return "setBoots";
            default:
                return null;
        }
    }

    private Object item(final String materialName) {
        final Class<?> materialType = reflection.type("org.bukkit.Material");
        final Object material = reflection.invokeStatic(materialType, "matchMaterial",
                new Class<?>[]{String.class}, new Object[]{materialName});
        if (material == null) {
            throw new IllegalArgumentException("unknown replay equipment material");
        }
        return reflection.construct("org.bukkit.inventory.ItemStack",
                new Class<?>[]{materialType}, new Object[]{material});
    }

    private Object world(final String name) {
        final Object value = reflection.invokeStatic(reflection.type("org.bukkit.Bukkit"),
                "getWorld", new Class<?>[]{String.class}, new Object[]{name});
        if (value == null) { throw new IllegalStateException("replay world is unavailable"); }
        return value;
    }

    private Object location(final Object world, final VisualPosition position) {
        return reflection.construct("org.bukkit.Location",
                new Class<?>[]{reflection.type("org.bukkit.World"), double.class, double.class,
                    double.class, float.class, float.class},
                new Object[]{world, position.x(), position.y(), position.z(),
                    position.yaw(), position.pitch()});
    }

    private void invokeBoolean(final Object target, final String method, final boolean value) {
        reflection.invoke(target, method, new Class<?>[]{boolean.class}, new Object[]{value});
    }

    interface ReflectionOperations {
        Class<?> type(String name);
        Object construct(String type, Class<?>[] parameterTypes, Object[] arguments);
        Object invoke(Object target, String method, Class<?>[] parameterTypes, Object[] arguments);
        Object invokeStatic(Class<?> owner, String method,
                            Class<?>[] parameterTypes, Object[] arguments);
    }

    private static final class JvmReflectionOperations implements ReflectionOperations {
        @Override public Class<?> type(final String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException failure) {
                throw platformFailure(failure);
            }
        }

        @Override public Object construct(final String name, final Class<?>[] parameterTypes,
                                          final Object[] arguments) {
            try {
                return type(name).getConstructor(parameterTypes).newInstance(arguments);
            } catch (ReflectiveOperationException failure) {
                throw platformFailure(failure);
            }
        }

        @Override public Object invoke(final Object target, final String method,
                                       final Class<?>[] parameterTypes,
                                       final Object[] arguments) {
            try {
                return target.getClass().getMethod(method, parameterTypes)
                        .invoke(target, arguments);
            } catch (ReflectiveOperationException failure) {
                throw platformFailure(failure);
            }
        }

        @Override public Object invokeStatic(final Class<?> owner, final String method,
                                             final Class<?>[] parameterTypes,
                                             final Object[] arguments) {
            try {
                final Method reflected = owner.getMethod(method, parameterTypes);
                return reflected.invoke(null, arguments);
            } catch (ReflectiveOperationException failure) {
                throw platformFailure(failure);
            }
        }

        private static IllegalStateException platformFailure(
                final ReflectiveOperationException failure) {
            final Throwable cause = failure instanceof InvocationTargetException
                    && failure.getCause() != null ? failure.getCause() : failure;
            return new IllegalStateException("Paper replay visual operation failed", cause);
        }
    }
}
