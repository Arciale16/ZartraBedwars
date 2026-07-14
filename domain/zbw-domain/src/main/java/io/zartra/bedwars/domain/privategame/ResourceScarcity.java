package io.zartra.bedwars.domain.privategame;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PrivateGameModifierId;
import io.zartra.bedwars.domain.generator.ResourceGenerationProfile;
import java.util.Objects;
import java.util.Optional;

/**
 * Original eleventh private-game modifier contract for independent resource multipliers.
 *
 * <p>Settings accept any namespaced resource ID, so iron, gold, diamond, emerald and custom
 * generator outputs follow the same contract. Preset IDs are stable; numeric balancing belongs to
 * the versioned M20 content pack.</p>
 */
public final class ResourceScarcity implements PrivateGameModifierDefinition {
    /** Stable modifier ID. */
    public static final PrivateGameModifierId ID = PrivateGameModifierId.of("zartra", "resource_scarcity");
    private static final ResourceScarcity INSTANCE = new ResourceScarcity();
    private ResourceScarcity() { }
    /** @return immutable definition singleton */ public static ResourceScarcity definition() { return INSTANCE; }
    @Override public PrivateGameModifierId id() { return ID; }
    @Override public int schemaVersion() { return 1; }
    @Override public boolean supportsCustomResources() { return true; }

    /** Immutable host-selected Resource Scarcity settings. */
    public static final class Settings {
        private final Preset preset;
        private final ResourceGenerationProfile multipliers;
        private Settings(final Preset preset, final ResourceGenerationProfile multipliers) {
            this.preset = preset;
            this.multipliers = Objects.requireNonNull(multipliers, "multipliers");
        }
        /** @return settings linked to a built-in preset */
        public static Settings preset(final Preset preset, final ResourceGenerationProfile multipliers) { return new Settings(Objects.requireNonNull(preset, "preset"), multipliers); }
        /** @return custom settings without claiming a built-in balancing preset */
        public static Settings custom(final ResourceGenerationProfile multipliers) { return new Settings(null, multipliers); }
        /** @return selected preset, empty for custom settings */ public Optional<Preset> preset() { return Optional.ofNullable(preset); }
        /** @return independent native/custom resource multipliers */ public ResourceGenerationProfile multipliers() { return multipliers; }
    }

    /** Stable preset identities; their versioned values are supplied by content definitions. */
    public enum Preset {
        /** Original scarce preset identity. */ SCARCE("scarce"),
        /** Original reduced preset identity. */ REDUCED("reduced"),
        /** Neutral normal preset identity. */ NORMAL("normal"),
        /** Original abundant preset identity. */ ABUNDANT("abundant"),
        /** Original extreme preset identity. */ EXTREME("extreme");
        private final DefinitionId id;
        Preset(final String path) { id = DefinitionId.of("zartra", "resource_scarcity/" + path); }
        /** @return stable namespaced preset ID */ public DefinitionId id() { return id; }
    }
}
