package io.zartra.bedwars.paper.replay.visual;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable, slot-keyed equipment projection. */
public final class VisualEquipmentState {
    private final Map<String, String> items;

    /** Creates a bounded equipment snapshot. */
    public VisualEquipmentState(final Map<String, String> items) {
        Objects.requireNonNull(items, "items");
        if (items.size() > 16) {
            throw new IllegalArgumentException("equipment exceeds 16 slots");
        }
        final Map<String, String> copy = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : items.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null
                    || entry.getKey().trim().isEmpty() || entry.getValue().trim().isEmpty()) {
                throw new IllegalArgumentException("equipment slot and item must not be blank");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        this.items = Collections.unmodifiableMap(copy);
    }

    /** @return an empty equipment state */
    public static VisualEquipmentState empty() {
        return new VisualEquipmentState(Collections.<String, String>emptyMap());
    }

    /** @return immutable equipment in stable slot order */ public Map<String, String> items() {
        return items;
    }

    /** Returns a new snapshot with one slot replaced or removed. */
    public VisualEquipmentState with(final String slot, final String item) {
        final Map<String, String> next = new TreeMap<String, String>(items);
        if (item == null || item.trim().isEmpty() || "AIR".equalsIgnoreCase(item)) {
            next.remove(slot);
        } else {
            next.put(slot, item);
        }
        return new VisualEquipmentState(next);
    }

    @Override public boolean equals(final Object other) {
        return other instanceof VisualEquipmentState
                && items.equals(((VisualEquipmentState) other).items);
    }

    @Override public int hashCode() { return items.hashCode(); }
}
