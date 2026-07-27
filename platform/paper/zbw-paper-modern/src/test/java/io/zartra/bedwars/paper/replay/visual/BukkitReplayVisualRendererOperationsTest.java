package io.zartra.bedwars.paper.replay.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Contract-tests Bukkit rendering behavior without replacing Paper classes. */
final class BukkitReplayVisualRendererOperationsTest {
    private static final UUID VIEWER =
            UUID.fromString("00000000-0000-0000-0000-000000000079");

    @Test
    void spawnsUpdatesAllEquipmentSlotsAndRemoves() {
        final RecordingOperations operations = new RecordingOperations();
        final BukkitReplayVisualRenderer renderer =
                new BukkitReplayVisualRenderer(operations);
        final Map<String, String> equipment = new LinkedHashMap<String, String>();
        equipment.put("hand", "WOODEN_SWORD");
        equipment.put("off_hand", "RED_WOOL");
        equipment.put("helmet", "LEATHER_HELMET");
        equipment.put("chestplate", "LEATHER_CHESTPLATE");
        equipment.put("leggings", "LEATHER_LEGGINGS");
        equipment.put("boots", "LEATHER_BOOTS");
        equipment.put("ignored", "STONE");

        final Object handle = renderer.spawn(VIEWER, state("Alpha", 1.0D, equipment));
        renderer.update(VIEWER, handle, state("Beta", 7.0D,
                Map.of("main_hand", "IRON_SWORD", "head", "IRON_HELMET",
                        "chest", "IRON_CHESTPLATE", "legs", "IRON_LEGGINGS",
                        "feet", "IRON_BOOTS")));
        renderer.remove(VIEWER, handle);

        assertEquals(operations.stand, handle);
        assertTrue(operations.methods.contains("spawn"));
        assertTrue(operations.methods.contains("teleport"));
        assertTrue(operations.methods.contains("setItemInMainHand"));
        assertTrue(operations.methods.contains("setItemInOffHand"));
        assertTrue(operations.methods.contains("setHelmet"));
        assertTrue(operations.methods.contains("setChestplate"));
        assertTrue(operations.methods.contains("setLeggings"));
        assertTrue(operations.methods.contains("setBoots"));
        assertTrue(operations.methods.contains("remove"));
    }

    @Test
    void handlesMissingEquipmentWorldAndMaterial() {
        final RecordingOperations noEquipment = new RecordingOperations();
        noEquipment.equipment = null;
        new BukkitReplayVisualRenderer(noEquipment).spawn(
                VIEWER, state("Alpha", 1.0D, Map.of()));

        final RecordingOperations noWorld = new RecordingOperations();
        noWorld.world = null;
        assertThrows(IllegalStateException.class,
                () -> new BukkitReplayVisualRenderer(noWorld).spawn(
                        VIEWER, state("Alpha", 1.0D, Map.of())));

        final RecordingOperations noMaterial = new RecordingOperations();
        noMaterial.material = null;
        assertThrows(IllegalArgumentException.class,
                () -> new BukkitReplayVisualRenderer(noMaterial).spawn(
                        VIEWER, state("Alpha", 1.0D, Map.of("head", "INVALID"))));
    }

    @Test
    void validatesRequiredArgumentsAndOperations() {
        final BukkitReplayVisualRenderer renderer =
                new BukkitReplayVisualRenderer(new RecordingOperations());
        assertThrows(NullPointerException.class,
                () -> new BukkitReplayVisualRenderer(null));
        assertThrows(NullPointerException.class,
                () -> renderer.spawn(null, state("Alpha", 1.0D, Map.of())));
        assertThrows(NullPointerException.class, () -> renderer.spawn(VIEWER, null));
        assertThrows(NullPointerException.class,
                () -> renderer.update(VIEWER, null, state("Alpha", 1.0D, Map.of())));
        assertThrows(NullPointerException.class, () -> renderer.remove(VIEWER, null));
    }

    private static VisualEntityState state(final String name, final double x,
                                           final Map<String, String> equipment) {
        return new VisualEntityState("alpha", name,
                new VisualPosition("replay", x, 64.0D, 2.0D, 0.0F, 0.0F),
                new VisualEquipmentState(equipment), 20.0D, true);
    }

    private static final class RecordingOperations
            implements BukkitReplayVisualRenderer.ReflectionOperations {
        private final Object stand = new Object();
        private final List<String> methods = new ArrayList<String>();
        private Object world = new Object();
        private Object equipment = new Object();
        private Object material = new Object();

        @Override public Class<?> type(final String name) { return Object.class; }
        @Override public Object construct(final String type,
                                          final Class<?>[] parameterTypes,
                                          final Object[] arguments) {
            return new Object();
        }
        @Override public Object invoke(final Object target, final String method,
                                       final Class<?>[] parameterTypes,
                                       final Object[] arguments) {
            methods.add(method);
            if ("spawn".equals(method)) { return stand; }
            if ("getEquipment".equals(method)) { return equipment; }
            return null;
        }
        @Override public Object invokeStatic(final Class<?> owner, final String method,
                                             final Class<?>[] parameterTypes,
                                             final Object[] arguments) {
            return "getWorld".equals(method) ? world : material;
        }
    }
}
