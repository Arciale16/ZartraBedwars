package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.shop.api.ShopCatalog;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original deterministic starter definitions for the M11 Phase 4 addon mechanics. */
public final class AddonMechanics {
    private AddonMechanics() { }
    /** @return complete local Phase 4 action catalogue */
    public static UtilityItemCatalog starterCatalog() {
        return new UtilityItemCatalog(Arrays.asList(
                action("popup-tower", UtilityItemDefinition.Kind.POPUP_TOWER,
                        UtilityItemDefinition.TargetRule.LOCATION, "iron", 24, 5, params("blocks", 48, "steps", 12)),
                action("rush-bridge", UtilityItemDefinition.Kind.RUSH,
                        UtilityItemDefinition.TargetRule.LOCATION, "iron", 16, 4, params("blocks", 16, "speed", 4)),
                action("ultimate-ability", UtilityItemDefinition.Kind.ULTIMATE,
                        UtilityItemDefinition.TargetRule.ENEMY, "diamond", 2, 12, params("duration", 6, "radius", 5)),
                action("bedsteal-token", UtilityItemDefinition.Kind.BEDSTEAL,
                        UtilityItemDefinition.TargetRule.OWN_BED, "redstone", 1, 2, params("heart-increase", 2, "bed-level", 1)),
                action("voidless-recovery", UtilityItemDefinition.Kind.VOIDLESS,
                        UtilityItemDefinition.TargetRule.SELF, "iron", 8, 8, params("height", 4, "grace-ticks", 20)),
                action("sponge-effect", UtilityItemDefinition.Kind.SPONGE,
                        UtilityItemDefinition.TargetRule.LOCATION, "gold", 2, 3, params("radius", 5, "duration", 40)),
                action("generator-boost", UtilityItemDefinition.Kind.GENERATOR,
                        UtilityItemDefinition.TargetRule.GENERATOR, "diamond", 4, 10, params("multiplier", 2, "duration", 30)),
                action("rotation-item", UtilityItemDefinition.Kind.ITEM_ROTATION,
                        UtilityItemDefinition.TargetRule.NONE, "emerald", 2, 10,
                        Collections.singletonMap("rotation-revision", 1L))));
    }
    private static UtilityItemDefinition action(final String name, final UtilityItemDefinition.Kind kind,
                                                final UtilityItemDefinition.TargetRule target,
                                                final String resource, final long cost,
                                                final long cooldown, final Map<String, Long> parameters) {
        return new UtilityItemDefinition(DefinitionId.of("zartra", "utility/" + name),
                DefinitionId.of("zartra", "item/" + name), kind,
                PermissionNode.of("zartrabedwars.item." + name.replace('-', '.')),
                new ShopCatalog.Price(Collections.singletonList(new ShopCatalog.ResourceAmount(
                        ResourceId.of("zartra", resource), cost))), Duration.ofSeconds(cooldown),
                target, 64, parameters);
    }
    private static Map<String, Long> params(final String first, final long firstValue,
                                            final String second, final long secondValue) {
        final Map<String, Long> values = new LinkedHashMap<String, Long>();
        values.put(first, firstValue);
        values.put(second, secondValue);
        return values;
    }
}
