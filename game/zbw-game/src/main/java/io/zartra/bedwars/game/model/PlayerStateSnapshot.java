package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable pre-session state used for exact, idempotent player restoration. */
public final class PlayerStateSnapshot {
    private final PlayerId playerId;
    private final Inventory inventory;
    private final Location location;
    private final Mode mode;
    private final boolean visible;

    /** Creates a complete state snapshot without retaining a platform object. */
    public PlayerStateSnapshot(final PlayerId playerId, final Inventory inventory,
                               final Location location, final Mode mode, final boolean visible) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.location = Objects.requireNonNull(location, "location");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.visible = visible;
    }

    /** @return player identity */ public PlayerId playerId() { return playerId; }
    /** @return defensive immutable inventory */ public Inventory inventory() { return inventory; }
    /** @return platform-neutral saved location */ public Location location() { return location; }
    /** @return saved game mode */ public Mode mode() { return mode; }
    /** @return saved global visibility preference */ public boolean visible() { return visible; }

    /** Supported platform-neutral game-mode semantics. */
    public enum Mode {
        /** Standard survival play. */ SURVIVAL,
        /** Waiting/protected adventure state. */ ADVENTURE,
        /** Eliminated viewer state. */ SPECTATOR,
        /** Operator creative state, never granted by M08 policy. */ CREATIVE
    }

    /** Immutable location using a semantic world identity. */
    public static final class Location {
        private final DefinitionId worldId;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        /** Creates a finite location. */
        public Location(final DefinitionId worldId, final double x, final double y,
                        final double z, final float yaw, final float pitch) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                throw new IllegalArgumentException("location components must be finite");
            }
            this.worldId = Objects.requireNonNull(worldId, "worldId");
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
        /** @return semantic world identity */ public DefinitionId worldId() { return worldId; }
        /** @return x coordinate */ public double x() { return x; }
        /** @return y coordinate */ public double y() { return y; }
        /** @return z coordinate */ public double z() { return z; }
        /** @return yaw */ public float yaw() { return yaw; }
        /** @return pitch */ public float pitch() { return pitch; }
    }

    /** Immutable semantic item stack. */
    public static final class Item {
        private final DefinitionId itemId;
        private final int amount;
        private final Map<String, String> metadata;

        /** Creates one bounded stack. */
        public Item(final DefinitionId itemId, final int amount,
                    final Map<String, String> metadata) {
            if (amount < 1 || amount > 64) {
                throw new IllegalArgumentException("amount must be between 1 and 64");
            }
            this.itemId = Objects.requireNonNull(itemId, "itemId");
            final Map<String, String> copy = new LinkedHashMap<String, String>(
                    Objects.requireNonNull(metadata, "metadata"));
            if (copy.size() > 32 || copy.containsKey(null) || copy.containsValue(null)) {
                throw new IllegalArgumentException("item metadata must contain at most 32 entries");
            }
            for (Map.Entry<String, String> entry : copy.entrySet()) {
                if (!entry.getKey().matches("[a-z0-9_.-]{1,64}")
                        || entry.getValue().length() > 256) {
                    throw new IllegalArgumentException("item metadata entry is malformed");
                }
            }
            this.amount = amount;
            this.metadata = Collections.unmodifiableMap(copy);
        }
        /** @return semantic item identity */ public DefinitionId itemId() { return itemId; }
        /** @return stack amount */ public int amount() { return amount; }
        /** @return immutable metadata */ public Map<String, String> metadata() { return metadata; }
        /** @return copy with a different positive amount */
        public Item withAmount(final int nextAmount) { return new Item(itemId, nextAmount, metadata); }
        @Override public int hashCode() { return Objects.hash(itemId, amount, metadata); }
        @Override public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (!(other instanceof Item)) { return false; }
            final Item that = (Item) other;
            return amount == that.amount && itemId.equals(that.itemId)
                    && metadata.equals(that.metadata);
        }
    }

    /** Fixed-size immutable inventory whose occupied slots are explicit. */
    public static final class Inventory {
        private final int size;
        private final Map<Integer, Item> occupied;

        /** Creates a bounded inventory with unique in-range slots. */
        public Inventory(final int size, final Map<Integer, Item> occupied) {
            if (size < 1 || size > 128) {
                throw new IllegalArgumentException("inventory size must be between 1 and 128");
            }
            final Map<Integer, Item> copy = new LinkedHashMap<Integer, Item>(
                    Objects.requireNonNull(occupied, "occupied"));
            if (copy.containsKey(null) || copy.containsValue(null)) {
                throw new IllegalArgumentException("inventory entries cannot be null");
            }
            for (Integer slot : copy.keySet()) {
                if (slot.intValue() < 0 || slot.intValue() >= size) {
                    throw new IllegalArgumentException("inventory slot is outside its size");
                }
            }
            this.size = size;
            this.occupied = Collections.unmodifiableMap(copy);
        }
        /** @return empty fixed-size inventory */
        public static Inventory empty(final int size) {
            return new Inventory(size, Collections.<Integer, Item>emptyMap());
        }
        /** @return slot count */ public int size() { return size; }
        /** @return immutable occupied-slot mapping */ public Map<Integer, Item> occupied() { return occupied; }
        /** @return total count of every item */
        public int totalItems() {
            int total = 0;
            for (Item item : occupied.values()) { total += item.amount(); }
            return total;
        }
        /** @return sorted defensive occupied items */
        public List<Item> items() { return Collections.unmodifiableList(new ArrayList<Item>(occupied.values())); }
        @Override public int hashCode() { return Objects.hash(size, occupied); }
        @Override public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (!(other instanceof Inventory)) { return false; }
            final Inventory that = (Inventory) other;
            return size == that.size && occupied.equals(that.occupied);
        }
    }
}
