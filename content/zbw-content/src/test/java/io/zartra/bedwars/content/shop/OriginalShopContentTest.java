package io.zartra.bedwars.content.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.TenderRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;

final class OriginalShopContentTest {
    @Test void originalCatalogMatchesNormativeStarterBaseline() {
        final ShopCatalog catalog = OriginalShopContent.standardCatalog();
        assertEquals(ShopIds.CatalogId.of("zbw", "standard"), catalog.id());
        assertEquals(OriginalShopContent.STANDARD_FOUNDRY, catalog.balanceProfile());
        assertEquals(7, catalog.categories().size());
        assertEquals(24, catalog.items().size());
        assertEquals(4, price(catalog, "wool", TenderRegistry.IRON));
        assertEquals(5, price(catalog, "diamond_sword", TenderRegistry.EMERALD));
        assertEquals(7, price(catalog, "permanent_diamond_armor", TenderRegistry.EMERALD));
        assertEquals(22, price(catalog, "pop_up_tower", TenderRegistry.IRON));
        assertEquals(Duration.ofMillis(1250), item(catalog, "fire_charge").rules().cooldown());
        assertEquals(Duration.ofSeconds(90), item(catalog, "rescue_capsule").rules().cooldown());
        assertEquals(1, item(catalog, "permanent_chain_armor").rules().playerLimit());
        assertTrue(catalog.category(ShopIds.CategoryId.of("zbw", "potions")).isPresent());
        TenderRegistry.nativeMatchResources().validate(catalog);
    }

    @Test void fourOriginalProfilesUseDeterministicCeilingRounding() {
        assertEquals(4, OriginalShopContent.balanceProfiles().size());
        final ShopBalanceProfile lean = OriginalShopContent.balanceProfiles().get(1);
        assertEquals(DefinitionId.of("zbw", "shop/lean_kit"), lean.id());
        assertEquals(new BigDecimal("0.85"), lean.priceMultiplier());
        final ShopCatalog.Price base = new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 3)));
        assertEquals(3, lean.applyPrice(base).amounts().get(0).amount());
        assertEquals(Duration.ofMillis(1100), lean.applyCooldown(Duration.ofSeconds(1)));
        assertEquals(2, lean.applyPersonalLimit(2));
        assertEquals(0, lean.applyPersonalLimit(0));
        final ShopBalanceProfile reserve = OriginalShopContent.balanceProfiles().get(2);
        assertEquals(5, reserve.applyTeamStock(4));
    }

    @Test void balanceProfilesRejectUnsafeValues() {
        assertThrows(IllegalArgumentException.class, () -> new ShopBalanceProfile(
                DefinitionId.of("test", "shop/bad"), BigDecimal.ZERO, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE));
        final ShopBalanceProfile standard = OriginalShopContent.balanceProfiles().get(0);
        assertThrows(IllegalArgumentException.class, () -> standard.applyPersonalLimit(-1));
    }

    private static ShopCatalog.ItemDefinition item(final ShopCatalog catalog, final String path) {
        return catalog.item(ShopIds.ItemId.of("zbw", path)).get();
    }

    private static long price(final ShopCatalog catalog, final String path,
                              final io.zartra.bedwars.api.identity.ResourceId resource) {
        return item(catalog, path).price().amounts().stream()
                .filter(value -> value.resourceId().equals(resource)).findFirst().get().amount();
    }
}
