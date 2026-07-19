package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.shop.api.RotationContracts;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.ShopUserData;
import io.zartra.bedwars.shop.api.TenderRegistry;
import io.zartra.bedwars.storage.api.StorageEngine;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JdbcShopStateStoreTest {
    private static final PlayerId PLAYER = PlayerId.of(new UUID(0, 91));
    @TempDir Path temporary;

    @Test void preferencesHistoryAndRotationSurviveRestart() {
        final Path database = temporary.resolve("m11-state.db");
        final ShopIds.ItemId wool = ShopIds.ItemId.of("test", "wool");
        final Map<Integer, ShopIds.ItemId> slots = new LinkedHashMap<>();
        slots.put(0, wool);
        final ShopUserData.QuickBuyLayout quickBuy = new ShopUserData.QuickBuyLayout(slots);
        final ShopUserData.Favourites favourites = new ShopUserData.Favourites(
                Collections.singleton(wool));
        final ShopUserData.HistoryEntry history = new ShopUserData.HistoryEntry(
                IdempotencyKey.of("test", "purchase-one"), wool, 1,
                new ShopCatalog.Price(Collections.singletonList(
                        new ShopCatalog.ResourceAmount(TenderRegistry.IRON, 4))),
                Instant.parse("2026-07-18T10:00:00Z"));
        final RotationContracts.Snapshot rotation = new RotationContracts.Snapshot(
                ShopIds.RotationId.of("test", "weekly"), 1,
                Instant.parse("2026-07-14T00:00:00Z"),
                Instant.parse("2026-07-21T00:00:00Z"), Collections.singletonList(wool));
        try (JdbcStorageEngine engine = open(database)) {
            final JdbcShopStateStore store = new JdbcShopStateStore(engine, Runnable::run);
            assertEquals(1, store.replace(PLAYER, 0, quickBuy, favourites)
                    .toCompletableFuture().join().revision());
            assertTrue(store.append(PLAYER, history).toCompletableFuture().join());
            assertFalse(store.append(PLAYER, history).toCompletableFuture().join());
            assertEquals(1, store.replace(rotation, 0).toCompletableFuture().join().revision());
        }
        try (JdbcStorageEngine engine = open(database)) {
            final JdbcShopStateStore store = new JdbcShopStateStore(engine, Runnable::run);
            assertEquals(wool, store.load(PLAYER).toCompletableFuture().join()
                    .quickBuy().assignments().get(0));
            assertEquals(history.key(), store.recent(PLAYER, 10).toCompletableFuture().join()
                    .get(0).key());
            assertEquals(wool, store.load(rotation.id()).toCompletableFuture().join()
                    .get().activeItems().get(0));
        }
    }

    @Test void optimisticConflictsAndBoundsFailClosed() {
        try (JdbcStorageEngine engine = open(temporary.resolve("m11-conflict.db"))) {
            final JdbcShopStateStore store = new JdbcShopStateStore(engine, Runnable::run);
            final ShopUserData.PreferenceSnapshot empty = store.load(PLAYER)
                    .toCompletableFuture().join();
            assertEquals(0, empty.revision());
            store.replace(PLAYER, 0, empty.quickBuy(), empty.favourites())
                    .toCompletableFuture().join();
            assertThrows(CompletionException.class, () -> store.replace(PLAYER, 0,
                    empty.quickBuy(), empty.favourites()).toCompletableFuture().join());
            assertThrows(IllegalArgumentException.class, () -> store.recent(PLAYER, 0));
            assertThrows(IllegalArgumentException.class, () -> store.replace(PLAYER, -1,
                    empty.quickBuy(), empty.favourites()));
        }
    }

    private static JdbcStorageEngine open(final Path database) {
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(StorageEngine.EngineKind.SQLITE,
                "jdbc:sqlite:" + database.toAbsolutePath(), "", new char[0], 1,
                Duration.ofSeconds(5), Duration.ofSeconds(5))).requireValue();
    }
}
