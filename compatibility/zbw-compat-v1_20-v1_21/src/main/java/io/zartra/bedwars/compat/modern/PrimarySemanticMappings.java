package io.zartra.bedwars.compat.modern;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.SemanticKey;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Original semantic sound, effect and platform mappings for Paper 1.21.1 build 133. */
public final class PrimarySemanticMappings {
    private static final String NAMESPACE = "zartra";
    private static final List<CompatibilityMapping> MAPPINGS = List.of(
            mapping(SemanticKey.Kind.MATERIAL, "material/team_block", "material", "WHITE_WOOL"),
            mapping(SemanticKey.Kind.MATERIAL, "material/menu_filler", "material", "GRAY_STAINED_GLASS_PANE"),
            mapping(SemanticKey.Kind.ITEM, "item/confirmation", "item", "LIME_DYE"),
            mapping(SemanticKey.Kind.ITEM, "item/rejection", "item", "RED_DYE"),
            mapping(SemanticKey.Kind.METADATA, "metadata/item_identity", "pdc", "PersistentDataContainer"),
            mapping(SemanticKey.Kind.SOUND, "sound/confirmation", "sound", "minecraft:entity.experience_orb.pickup"),
            mapping(SemanticKey.Kind.SOUND, "sound/rejection", "sound", "minecraft:block.note_block.bass"),
            mapping(SemanticKey.Kind.SOUND, "sound/countdown", "sound", "minecraft:block.note_block.hat"),
            mapping(SemanticKey.Kind.PARTICLE, "effect/confirmation", "particle", "HAPPY_VILLAGER"),
            mapping(SemanticKey.Kind.PARTICLE, "effect/rejection", "particle", "ANGRY_VILLAGER"),
            mapping(SemanticKey.Kind.PARTICLE, "effect/countdown", "particle", "DUST"),
            mapping(SemanticKey.Kind.TEXT, "text/component", "adventure", "Component"),
            mapping(SemanticKey.Kind.TEXT, "text/click_action", "adventure", "ClickEvent"),
            mapping(SemanticKey.Kind.ENTITY, "entity/marker", "entity", "ARMOR_STAND"),
            mapping(SemanticKey.Kind.PACKET, "packet/fake_entity", "paper", "PlayerConnection"),
            mapping(SemanticKey.Kind.USER_INTERFACE, "ui/inventory", "inventory", "CHEST"),
            mapping(SemanticKey.Kind.USER_INTERFACE, "ui/bossbar", "adventure", "BossBar"),
            mapping(SemanticKey.Kind.USER_INTERFACE, "ui/title", "adventure", "Title"),
            mapping(SemanticKey.Kind.SCHEDULER, "scheduler/global_owner", "bukkit", "BukkitScheduler"));

    private PrimarySemanticMappings() { throw new AssertionError("No instances"); }

    private static CompatibilityMapping mapping(final SemanticKey.Kind kind, final String key,
                                                final String renderer, final String nativeValue) {
        return new CompatibilityMapping(SemanticKey.of(kind, DefinitionId.of(NAMESPACE, key)),
                DefinitionId.of(NAMESPACE, "paper121/" + renderer), nativeValue);
    }

    /** @return immutable complete primary mapping list */ public static List<CompatibilityMapping> all() { return MAPPINGS; }
    /** @return immutable required semantic key set */
    public static Set<SemanticKey> required() {
        final Set<SemanticKey> required = new TreeSet<>();
        for (CompatibilityMapping mapping : MAPPINGS) { required.add(mapping.semanticKey()); }
        return Set.copyOf(required);
    }
}
