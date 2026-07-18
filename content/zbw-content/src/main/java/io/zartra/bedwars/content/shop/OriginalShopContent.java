package io.zartra.bedwars.content.shop;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.TenderRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Factory for the original {@code zbw:shop/standard_foundry} content baseline. */
public final class OriginalShopContent {
    /** Stable default profile identity. */
    public static final DefinitionId STANDARD_FOUNDRY = DefinitionId.of("zbw", "shop/standard_foundry");

    private OriginalShopContent() { }

    /** @return all four original starter balance profiles in stable order */
    public static List<ShopBalanceProfile> balanceProfiles() {
        final List<ShopBalanceProfile> result = new ArrayList<ShopBalanceProfile>();
        result.add(profile("standard_foundry", "1.00", "1.00", "1.00", "1.00"));
        result.add(profile("lean_kit", "0.85", "1.10", "0.85", "0.85"));
        result.add(profile("tactical_reserve", "1.15", "0.90", "1.25", "1.00"));
        result.add(profile("rapid_exchange", "0.95", "0.65", "1.00", "1.25"));
        return Collections.unmodifiableList(result);
    }

    private static ShopBalanceProfile profile(final String path, final String price,
                                              final String cooldown, final String stock,
                                              final String personal) {
        return new ShopBalanceProfile(DefinitionId.of("zbw", "shop/" + path),
                new BigDecimal(price), new BigDecimal(cooldown),
                new BigDecimal(stock), new BigDecimal(personal));
    }

    /**
     * Returns the version-one original catalog from {@code docs/BALANCING_BASELINE.md}.
     *
     * <p>Utility semantic identities are content declarations, not claims that later M11 item
     * mechanics are already implemented.</p>
     */
    public static ShopCatalog standardCatalog() {
        final List<ShopCatalog.Category> categories = Arrays.asList(
                category("blocks", 0), category("melee", 1), category("armor", 2),
                category("tools", 3), category("ranged", 4), category("potions", 5),
                category("utility", 6));
        final List<ShopCatalog.ItemDefinition> items = new ArrayList<ShopCatalog.ItemDefinition>();
        items.add(item("blocks", "wool", 16, TenderRegistry.IRON, 4, 64, 0, 0));
        items.add(item("blocks", "hardened_clay", 12, TenderRegistry.IRON, 12, 48, 0, 0));
        items.add(item("blocks", "blast_resistant_glass", 4, TenderRegistry.IRON, 12, 32, 0, 0));
        items.add(item("blocks", "end_stone", 16, TenderRegistry.IRON, 28, 64, 0, 0));
        items.add(item("blocks", "timber", 4, TenderRegistry.GOLD, 4, 32, 0, 0));
        items.add(item("melee", "stone_sword", 1, TenderRegistry.IRON, 12, 1, 0, 0));
        items.add(item("melee", "iron_sword", 1, TenderRegistry.GOLD, 8, 1, 0, 0));
        items.add(item("melee", "diamond_sword", 1, TenderRegistry.EMERALD, 5, 1, 0, 0));
        items.add(item("armor", "permanent_chain_armor", 1, TenderRegistry.IRON, 36, 1, 1, 0));
        items.add(item("armor", "permanent_iron_armor", 1, TenderRegistry.GOLD, 10, 1, 1, 0));
        items.add(item("armor", "permanent_diamond_armor", 1, TenderRegistry.EMERALD, 7, 1, 1, 0));
        items.add(item("tools", "shears", 1, TenderRegistry.IRON, 24, 1, 0, 0));
        items.add(item("tools", "pickaxe_tier_1", 1, TenderRegistry.IRON, 12, 1, 0, 0));
        items.add(item("tools", "axe_tier_1", 1, TenderRegistry.IRON, 12, 1, 0, 0));
        items.add(item("ranged", "bow", 1, TenderRegistry.GOLD, 14, 1, 0, 0));
        items.add(item("ranged", "arrows", 8, TenderRegistry.GOLD, 3, 64, 0, 0));
        items.add(item("utility", "fire_charge", 1, TenderRegistry.IRON, 48, 0, 0, 1250));
        items.add(item("utility", "tnt", 1, TenderRegistry.GOLD, 5, 0, 0, 2500));
        items.add(item("utility", "water_bucket", 1, TenderRegistry.GOLD, 4, 2, 0, 0));
        items.add(item("utility", "bridge_egg", 1, TenderRegistry.EMERALD, 2, 0, 0, 5000));
        items.add(item("utility", "warp_pearl", 1, TenderRegistry.EMERALD, 4, 0, 0, 3000));
        items.add(item("utility", "rescue_capsule", 1, TenderRegistry.GOLD, 5, 1, 0, 90000));
        items.add(item("utility", "pop_up_tower", 1, TenderRegistry.IRON, 22, 1, 0, 8000));
        items.add(item("utility", "sponge_burst", 1, TenderRegistry.GOLD, 4, 0, 0, 3000));
        return new ShopCatalog(ShopIds.CatalogId.of("zbw", "standard"), 1,
                ShopCatalog.Scope.global(), STANDARD_FOUNDRY, categories, items);
    }

    private static ShopCatalog.Category category(final String path, final int order) {
        return new ShopCatalog.Category(ShopIds.CategoryId.of("zbw", path),
                MessageKey.of("shop.category." + path + ".name"),
                MessageKey.of("shop.category." + path + ".lore"),
                DefinitionId.of("zbw", "material/category_" + path), order, order,
                ShopCatalog.Visibility.VISIBLE, Optional.empty(), Optional.empty());
    }

    private static ShopCatalog.ItemDefinition item(final String category, final String path,
                                                    final int quantity, final ResourceId tender,
                                                    final long price, final int inventoryLimit,
                                                    final int playerLimit, final long cooldownMillis) {
        final int maximumBulk = inventoryLimit == 0 ? 64 : Math.max(1, inventoryLimit / quantity);
        final ShopCatalog.PurchaseRules rules = new ShopCatalog.PurchaseRules(false, maximumBulk,
                Duration.ofMillis(cooldownMillis), inventoryLimit, playerLimit, 0, 0,
                Optional.empty(), Optional.empty());
        return new ShopCatalog.ItemDefinition(ShopIds.ItemId.of("zbw", path),
                ShopIds.CategoryId.of("zbw", category), DefinitionId.of("zbw", "item/" + path),
                MessageKey.of("shop.item." + path + ".name"),
                MessageKey.of("shop.item." + path + ".lore"), quantity,
                new ShopCatalog.Price(Collections.singletonList(
                        new ShopCatalog.ResourceAmount(tender, price))),
                ShopCatalog.Availability.STANDARD, rules, Collections.emptyList());
    }
}
