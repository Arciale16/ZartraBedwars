package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.shop.api.RotationContracts;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.ShopUserData;
import io.zartra.bedwars.shop.api.TenderRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ShopUserDataAndRotationTest {
    @Test void quickBuyAndFavouritesAreBoundedAndImmutable() {
        final ShopIds.ItemId wool = ShopIds.ItemId.of("test", "wool");
        final Map<Integer, ShopIds.ItemId> slots = new LinkedHashMap<Integer, ShopIds.ItemId>();
        slots.put(0, wool);
        final ShopUserData.QuickBuyLayout layout = new ShopUserData.QuickBuyLayout(slots);
        slots.clear();
        assertEquals(wool, layout.assignments().get(0));
        assertThrows(UnsupportedOperationException.class,
                () -> layout.assignments().put(1, wool));
        assertThrows(IllegalArgumentException.class, () -> new ShopUserData.QuickBuyLayout(
                Collections.singletonMap(45, wool)));
        final Map<Integer, ShopIds.ItemId> duplicate = new LinkedHashMap<Integer, ShopIds.ItemId>();
        duplicate.put(0, wool);
        duplicate.put(1, wool);
        assertThrows(IllegalArgumentException.class,
                () -> new ShopUserData.QuickBuyLayout(duplicate));
        final ShopUserData.Favourites favourites = new ShopUserData.Favourites(
                Collections.singletonList(wool));
        assertTrue(favourites.items().contains(wool));
        assertThrows(IllegalArgumentException.class, () -> new ShopUserData.Favourites(
                Arrays.asList(wool, wool)));
        assertEquals(4, new ShopUserData.PreferenceSnapshot(4, layout, favourites).revision());
    }

    @Test void historyOrderingIsDeterministicAndNewestFirst() {
        final ShopCatalog.Price price = new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 4)));
        final ShopUserData.HistoryEntry older = new ShopUserData.HistoryEntry(
                IdempotencyKey.of("test", "older"), ShopIds.ItemId.of("test", "wool"),
                1, price, Instant.parse("2026-01-01T00:00:00Z"));
        final ShopUserData.HistoryEntry newer = new ShopUserData.HistoryEntry(
                IdempotencyKey.of("test", "newer"), ShopIds.ItemId.of("test", "wool"),
                2, price, Instant.parse("2026-01-02T00:00:00Z"));
        assertEquals(newer, ShopUserData.newestFirst(Arrays.asList(older, newer)).get(0));
        assertEquals(2, newer.batches());
        assertEquals(price, newer.charged());
        assertEquals(ShopIds.ItemId.of("test", "wool"), newer.itemId());
        assertEquals(IdempotencyKey.of("test", "newer"), newer.key());
        assertThrows(IllegalArgumentException.class, () -> new ShopUserData.HistoryEntry(
                older.key(), older.itemId(), 0, price, older.committedAt()));
    }

    @Test void rotationDefinitionPreservesScheduleAndPolicy() {
        final Instant start = Instant.parse("2026-01-01T00:00:00Z");
        final RotationContracts.PoolEntry wool = new RotationContracts.PoolEntry(
                ShopIds.ItemId.of("test", "wool"), 10);
        final RotationContracts.PoolEntry tnt = new RotationContracts.PoolEntry(
                ShopIds.ItemId.of("test", "tnt"), 5);
        final RotationContracts.Definition definition = new RotationContracts.Definition(
                ShopIds.RotationId.of("test", "weekly"), ZoneId.of("Europe/London"),
                Duration.ofDays(7), start, Optional.of(start.plus(Duration.ofDays(28))),
                1, 2, Duration.ofSeconds(3), Optional.empty(), Arrays.asList(wool, tnt));
        assertEquals(ZoneId.of("Europe/London"), definition.zoneId());
        assertEquals(Duration.ofDays(7), definition.period());
        assertEquals(1, definition.slots());
        assertEquals(2, definition.noRepeatPeriods());
        assertEquals(10, definition.pool().get(0).weight());
        assertFalse(definition.permission().isPresent());
        assertTrue(definition.endsAt().isPresent());
        final RotationContracts.Snapshot snapshot = new RotationContracts.Snapshot(
                definition.id(), 2, start, start.plus(Duration.ofDays(7)),
                Collections.singletonList(wool.itemId()));
        assertEquals(2, snapshot.revision());
        assertEquals(wool.itemId(), snapshot.activeItems().get(0));
        assertThrows(IllegalArgumentException.class, () -> new RotationContracts.Definition(
                definition.id(), definition.zoneId(), Duration.ofSeconds(1), start,
                Optional.empty(), 1, 0, Duration.ZERO, Optional.empty(),
                Collections.singletonList(wool)));
        assertThrows(IllegalArgumentException.class, () -> new RotationContracts.Definition(
                definition.id(), definition.zoneId(), definition.period(), start,
                Optional.empty(), 2, 0, Duration.ZERO, Optional.empty(),
                Collections.singletonList(wool)));
        assertThrows(IllegalArgumentException.class, () -> new RotationContracts.Snapshot(
                definition.id(), 1, start, start, Collections.singletonList(wool.itemId())));
    }
}
