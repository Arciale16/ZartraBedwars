package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.TenderRegistry;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ShopCatalogContractTest {
    private static final ShopIds.CategoryId BLOCKS = ShopIds.CategoryId.of("test", "blocks");

    @Test void typedIdsRoundTripAndRejectWrongFamilies() {
        assertEquals("test:shop-catalog/main", ShopIds.CatalogId.of("test", "main").toString());
        assertEquals(ShopIds.CatalogId.of("test", "main"),
                ShopIds.CatalogId.parse("test:shop-catalog/main"));
        assertEquals(ShopIds.CategoryId.of("test", "blocks"),
                ShopIds.CategoryId.parse("test:shop-category/blocks"));
        assertEquals(ShopIds.ItemId.of("test", "wool"),
                ShopIds.ItemId.parse("test:shop-item/wool"));
        assertEquals(ShopIds.RotationId.of("test", "weekly"),
                ShopIds.RotationId.parse("test:shop-rotation/weekly"));
        assertThrows(IllegalArgumentException.class,
                () -> ShopIds.ItemId.parse("test:shop-category/wool"));
        assertTrue(ShopIds.ItemId.of("test", "a").compareTo(
                ShopIds.ItemId.of("test", "b")) < 0);
    }

    @Test void catalogIsDeterministicScopedAndImmutable() {
        final ArenaId arena = ArenaId.of(UUID.randomUUID());
        final DefinitionId mode = DefinitionId.of("test", "mode/solo");
        final DefinitionId group = DefinitionId.of("test", "group/ranked");
        final DefinitionId team = DefinitionId.of("test", "team/red");
        final ShopCatalog.Scope scope = ShopCatalog.Scope.of(Optional.of(mode),
                Optional.of(arena), Optional.of(group), Optional.of(team));
        final ShopCatalog catalog = catalog(scope, item("wool", price(TenderRegistry.IRON, 4)));
        assertTrue(scope.matches(mode, arena, Optional.of(group), team));
        assertFalse(scope.matches(DefinitionId.of("test", "mode/doubles"), arena,
                Optional.of(group), team));
        assertEquals(mode, scope.mode().get());
        assertEquals(arena, scope.arena().get());
        assertEquals(group, scope.group().get());
        assertEquals(team, scope.team().get());
        assertEquals(1, catalog.revision());
        assertEquals(BLOCKS, catalog.categories().get(0).id());
        assertEquals("test:shop-item/wool", catalog.items().get(0).id().toString());
        assertTrue(catalog.category(BLOCKS).isPresent());
        assertFalse(catalog.item(ShopIds.ItemId.of("test", "unknown")).isPresent());
        assertThrows(UnsupportedOperationException.class, () -> catalog.items().clear());
    }

    @Test void catalogRejectsDuplicatesDanglingRowsAndInvalidBounds() {
        final ShopCatalog.Category category = category();
        assertThrows(IllegalArgumentException.class, () -> new ShopCatalog(
                ShopIds.CatalogId.of("test", "main"), 1, ShopCatalog.Scope.global(),
                DefinitionId.of("test", "shop/profile"), Arrays.asList(category, category),
                Collections.emptyList()));
        final ShopCatalog.ItemDefinition dangling = new ShopCatalog.ItemDefinition(
                ShopIds.ItemId.of("test", "wool"), ShopIds.CategoryId.of("test", "missing"),
                DefinitionId.of("test", "item/wool"), MessageKey.of("item.wool.name"),
                MessageKey.of("item.wool.lore"), 1, price(TenderRegistry.IRON, 4),
                ShopCatalog.Availability.STANDARD, rules(), Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> new ShopCatalog(
                ShopIds.CatalogId.of("test", "main"), 1, ShopCatalog.Scope.global(),
                DefinitionId.of("test", "shop/profile"), Collections.singletonList(category),
                Collections.singletonList(dangling)));
        assertThrows(IllegalArgumentException.class, () -> new ShopCatalog(
                ShopIds.CatalogId.of("test", "main"), 0, ShopCatalog.Scope.global(),
                DefinitionId.of("test", "shop/profile"), Collections.singletonList(category),
                Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ShopCatalog.Category(BLOCKS,
                MessageKey.of("cat.blocks.name"), MessageKey.of("cat.blocks.lore"),
                DefinitionId.of("test", "material/wool"), 54, 0,
                ShopCatalog.Visibility.VISIBLE, Optional.empty(), Optional.empty()));
    }

    @Test void pricesSupportMultipleAndCustomResourcesWithSafeMultiplication() {
        final ResourceId custom = ResourceId.of("extension", "resource/crystal");
        final ShopCatalog.Price price = new ShopCatalog.Price(Arrays.asList(
                new ShopCatalog.ResourceAmount(custom, 2),
                new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 4)));
        assertEquals(2, price.amounts().size());
        assertEquals(8, price.multiply(2).amounts().stream()
                .filter(value -> value.resourceId().equals(TenderRegistry.IRON))
                .findFirst().get().amount());
        assertThrows(IllegalArgumentException.class, () -> new ShopCatalog.Price(Arrays.asList(
                new ShopCatalog.ResourceAmount(custom, 1),
                new ShopCatalog.ResourceAmount(custom, 2))));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopCatalog.ResourceAmount(custom, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopCatalog.ResourceAmount(custom, 1_000_000_000L).multiply(2));
    }

    @Test void tenderRegistryRequiresExplicitProviders() {
        final ResourceId custom = ResourceId.of("extension", "resource/crystal");
        final TenderRegistry customRegistry = new TenderRegistry(Arrays.asList(
                new TenderRegistry.TenderDefinition(TenderRegistry.IRON,
                        ProviderId.of("zbw", "match"), TenderRegistry.Kind.NATIVE_MATCH_RESOURCE),
                new TenderRegistry.TenderDefinition(custom, ProviderId.of("extension", "crystal"),
                        TenderRegistry.Kind.CUSTOM_MATCH_RESOURCE)));
        assertEquals(TenderRegistry.Kind.CUSTOM_MATCH_RESOURCE,
                customRegistry.find(custom).get().kind());
        assertEquals(ProviderId.of("extension", "crystal"),
                customRegistry.find(custom).get().providerId());
        customRegistry.validate(catalog(ShopCatalog.Scope.global(), item("crystal", price(custom, 2))));
        assertThrows(IllegalArgumentException.class, () -> TenderRegistry.nativeMatchResources()
                .validate(catalog(ShopCatalog.Scope.global(), item("crystal", price(custom, 2)))));
        assertThrows(IllegalArgumentException.class, () -> new TenderRegistry(Arrays.asList(
                new TenderRegistry.TenderDefinition(custom, ProviderId.of("test", "one"),
                        TenderRegistry.Kind.CUSTOM_MATCH_RESOURCE),
                new TenderRegistry.TenderDefinition(custom, ProviderId.of("test", "two"),
                        TenderRegistry.Kind.CUSTOM_MATCH_RESOURCE))));
    }

    private static ShopCatalog catalog(final ShopCatalog.Scope scope,
                                       final ShopCatalog.ItemDefinition item) {
        return new ShopCatalog(ShopIds.CatalogId.of("test", "main"), 1, scope,
                DefinitionId.of("test", "shop/profile"), Collections.singletonList(category()),
                Collections.singletonList(item));
    }

    private static ShopCatalog.Category category() {
        return new ShopCatalog.Category(BLOCKS, MessageKey.of("cat.blocks.name"),
                MessageKey.of("cat.blocks.lore"), DefinitionId.of("test", "material/wool"),
                0, 0, ShopCatalog.Visibility.VISIBLE, Optional.empty(), Optional.empty());
    }

    private static ShopCatalog.ItemDefinition item(final String path, final ShopCatalog.Price price) {
        return new ShopCatalog.ItemDefinition(ShopIds.ItemId.of("test", path), BLOCKS,
                DefinitionId.of("test", "item/" + path), MessageKey.of("item." + path + ".name"),
                MessageKey.of("item." + path + ".lore"), 1, price,
                ShopCatalog.Availability.STANDARD, rules(), Collections.emptyList());
    }

    private static ShopCatalog.PurchaseRules rules() {
        return new ShopCatalog.PurchaseRules(false, 4, Duration.ZERO, 64,
                0, 0, 0, Optional.empty(), Optional.empty());
    }

    private static ShopCatalog.Price price(final ResourceId resourceId, final long amount) {
        return new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(resourceId, amount)));
    }
}
