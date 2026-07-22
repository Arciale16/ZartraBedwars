package io.zartra.bedwars.progression.catalog;

/** Immutable M14 catalogue and runtime-budget configuration contract. */
public final class M14Configuration {
    private final int schemaVersion;
    private final int minimumProductionCosmetics;
    private final int maxEffectsPerPlayerPerTick;
    private final int maxEntitiesPerArena;
    private final boolean emergencyDisable;

    /** Creates validated configuration. */
    public M14Configuration(final int schemaVersion, final int minimumProductionCosmetics,
                            final int maxEffectsPerPlayerPerTick, final int maxEntitiesPerArena,
                            final boolean emergencyDisable) {
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        if (minimumProductionCosmetics < 300) {
            throw new IllegalArgumentException("production catalogue minimum must be at least 300");
        }
        if (maxEffectsPerPlayerPerTick < 1 || maxEffectsPerPlayerPerTick > 256) {
            throw new IllegalArgumentException("effect budget must be in 1..256");
        }
        if (maxEntitiesPerArena < 0 || maxEntitiesPerArena > 4096) {
            throw new IllegalArgumentException("entity budget must be in 0..4096");
        }
        this.schemaVersion = schemaVersion;
        this.minimumProductionCosmetics = minimumProductionCosmetics;
        this.maxEffectsPerPlayerPerTick = maxEffectsPerPlayerPerTick;
        this.maxEntitiesPerArena = maxEntitiesPerArena;
        this.emergencyDisable = emergencyDisable;
    }
    /** @return schema version */ public int schemaVersion() { return schemaVersion; }
    /** @return required production definition count */
    public int minimumProductionCosmetics() { return minimumProductionCosmetics; }
    /** @return per-player effect budget */ public int maxEffectsPerPlayerPerTick() { return maxEffectsPerPlayerPerTick; }
    /** @return per-arena entity budget */ public int maxEntitiesPerArena() { return maxEntitiesPerArena; }
    /** @return global emergency state */ public boolean emergencyDisable() { return emergencyDisable; }
}
