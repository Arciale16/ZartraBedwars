package io.zartra.bedwars.paper.replay.visual;

import java.util.Objects;

/** Immutable player/entity representation reconstructed from replay facts. */
public final class VisualEntityState {
    private final String entityId;
    private final String displayName;
    private final VisualPosition position;
    private final VisualEquipmentState equipment;
    private final double health;
    private final boolean alive;

    /** Creates a validated visual entity state. */
    public VisualEntityState(final String entityId, final String displayName,
                             final VisualPosition position,
                             final VisualEquipmentState equipment,
                             final double health, final boolean alive) {
        if (entityId == null || entityId.trim().isEmpty() || entityId.length() > 160) {
            throw new IllegalArgumentException("entityId is malformed");
        }
        if (displayName == null || displayName.trim().isEmpty() || displayName.length() > 80) {
            throw new IllegalArgumentException("displayName is malformed");
        }
        if (!Double.isFinite(health) || health < 0.0D || health > 2048.0D) {
            throw new IllegalArgumentException("health is outside the visual range");
        }
        this.entityId = entityId;
        this.displayName = displayName;
        this.position = Objects.requireNonNull(position, "position");
        this.equipment = Objects.requireNonNull(equipment, "equipment");
        this.health = health;
        this.alive = alive;
    }

    /** @return stable replay entity identity */ public String entityId() { return entityId; }
    /** @return captured player/entity display name */ public String displayName() { return displayName; }
    /** @return immutable position */ public VisualPosition position() { return position; }
    /** @return immutable equipment */ public VisualEquipmentState equipment() { return equipment; }
    /** @return captured health */ public double health() { return health; }
    /** @return whether the representation is alive */ public boolean alive() { return alive; }

    /** Returns a state with a new position. */
    public VisualEntityState move(final VisualPosition next) {
        return new VisualEntityState(entityId, displayName, next, equipment, health, alive);
    }

    /** Returns a state with changed equipment. */
    public VisualEntityState equip(final String slot, final String item) {
        return new VisualEntityState(entityId, displayName, position,
                equipment.with(slot, item), health, alive);
    }

    /** Returns a state with captured health/alive flags. */
    public VisualEntityState health(final double nextHealth, final boolean nextAlive) {
        return new VisualEntityState(entityId, displayName, position, equipment,
                nextHealth, nextAlive);
    }

    @Override public boolean equals(final Object other) {
        if (!(other instanceof VisualEntityState)) { return false; }
        final VisualEntityState value = (VisualEntityState) other;
        return entityId.equals(value.entityId) && displayName.equals(value.displayName)
                && position.equals(value.position) && equipment.equals(value.equipment)
                && Double.compare(health, value.health) == 0 && alive == value.alive;
    }

    @Override public int hashCode() {
        return Objects.hash(entityId, displayName, position, equipment, health, alive);
    }
}
