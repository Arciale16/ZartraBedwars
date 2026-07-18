package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.shop.api.RotationContracts;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.shop.api.ShopUserData;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Asynchronous M11 persistence adapter backed by the M04 versioned record schema.
 *
 * <p>Every blocking transaction runs on the supplied bounded worker executor. Preference and
 * rotation writes use optimistic revisions. Purchase history is capped at 100 entries and a
 * repeated idempotency key cannot be appended twice. This adapter owns no match currency or M12
 * progression state.</p>
 */
public final class JdbcShopStateStore implements ShopUserData.PreferencePort,
        ShopUserData.HistoryPort, RotationContracts.StatePort {
    private static final int SCHEMA_VERSION = 1;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final DefinitionId PREFERENCES = DefinitionId.of("zartra", "shop-preferences");
    private static final DefinitionId HISTORY = DefinitionId.of("zartra", "shop-history");
    private static final DefinitionId ROTATION = DefinitionId.of("zartra", "shop-rotation");
    private final StorageEngine engine;
    private final Executor executor;

    /** Creates a non-blocking adapter using one bounded persistence executor. */
    public JdbcShopStateStore(final StorageEngine engine, final Executor executor) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override public CompletionStage<ShopUserData.PreferenceSnapshot> load(
            final PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return async(() -> loadPreferences(playerId));
    }

    @Override public CompletionStage<ShopUserData.PreferenceSnapshot> replace(
            final PlayerId playerId, final long expectedRevision,
            final ShopUserData.QuickBuyLayout quickBuy,
            final ShopUserData.Favourites favourites) {
        Objects.requireNonNull(playerId, "playerId");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expected revision must not be negative");
        }
        Objects.requireNonNull(quickBuy, "quickBuy");
        Objects.requireNonNull(favourites, "favourites");
        return async(() -> {
            final RecordKey key = playerKey(PREFERENCES, playerId);
            final byte[] payload = Codec.preferences(quickBuy, favourites);
            final StoredRecord stored = save(key, expectedRevision, payload);
            return new ShopUserData.PreferenceSnapshot(stored.revision().value(), quickBuy,
                    favourites);
        });
    }

    @Override public CompletionStage<List<ShopUserData.HistoryEntry>> recent(
            final PlayerId playerId, final int limit) {
        Objects.requireNonNull(playerId, "playerId");
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("history limit must be between 1 and 100");
        }
        return async(() -> {
            final Optional<StoredRecord> record = find(playerKey(HISTORY, playerId));
            if (!record.isPresent()) { return Collections.emptyList(); }
            final List<ShopUserData.HistoryEntry> entries = Codec.history(record.get().payload());
            return Collections.unmodifiableList(new ArrayList<ShopUserData.HistoryEntry>(
                    entries.subList(0, Math.min(limit, entries.size()))));
        });
    }

    /** Atomically appends a successful purchase unless its idempotency key already exists. */
    public CompletionStage<Boolean> append(final PlayerId playerId,
                                            final ShopUserData.HistoryEntry entry) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(entry, "entry");
        return async(() -> appendHistory(playerId, entry));
    }

    @Override public CompletionStage<Optional<RotationContracts.Snapshot>> load(
            final ShopIds.RotationId id) {
        Objects.requireNonNull(id, "id");
        return async(() -> {
            final Optional<StoredRecord> record = find(rotationKey(id));
            return record.isPresent() ? Optional.of(Codec.rotation(
                    record.get().revision().value(), record.get().payload()))
                    : Optional.empty();
        });
    }

    @Override public CompletionStage<RotationContracts.Snapshot> replace(
            final RotationContracts.Snapshot snapshot, final long expectedRevision) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expected revision must not be negative");
        }
        return async(() -> {
            final StoredRecord stored = save(rotationKey(snapshot.id()), expectedRevision,
                    Codec.rotation(snapshot));
            return new RotationContracts.Snapshot(snapshot.id(), stored.revision().value(),
                    snapshot.periodStart(), snapshot.periodEnd(), snapshot.activeItems());
        });
    }

    private ShopUserData.PreferenceSnapshot loadPreferences(final PlayerId playerId) {
        final Optional<StoredRecord> record = find(playerKey(PREFERENCES, playerId));
        if (!record.isPresent()) {
            return new ShopUserData.PreferenceSnapshot(0,
                    new ShopUserData.QuickBuyLayout(Collections.emptyMap()),
                    new ShopUserData.Favourites(Collections.emptyList()));
        }
        return Codec.preferences(record.get().revision().value(), record.get().payload());
    }

    private boolean appendHistory(final PlayerId playerId,
                                  final ShopUserData.HistoryEntry entry) {
        final RecordKey key = playerKey(HISTORY, playerId);
        try (UnitOfWork unit = begin(TransactionOptions.AccessMode.READ_WRITE)) {
            final Optional<StoredRecord> current = engine.records().find(unit, key).requireValue();
            final List<ShopUserData.HistoryEntry> entries = current.isPresent()
                    ? new ArrayList<ShopUserData.HistoryEntry>(Codec.history(
                            current.get().payload())) : new ArrayList<ShopUserData.HistoryEntry>();
            for (ShopUserData.HistoryEntry existing : entries) {
                if (existing.key().equals(entry.key())) {
                    unit.rollback().requireValue();
                    return false;
                }
            }
            entries.add(entry);
            final List<ShopUserData.HistoryEntry> ordered = new ArrayList<ShopUserData.HistoryEntry>(
                    ShopUserData.newestFirst(entries));
            if (ordered.size() > 100) { ordered.subList(100, ordered.size()).clear(); }
            final RecordRevision expected = current.isPresent() ? current.get().revision()
                    : RecordRevision.initial();
            engine.records().save(unit, StoredRecord.of(key, expected, SCHEMA_VERSION,
                    Codec.history(ordered), Instant.now()), expected).requireValue();
            unit.commit().requireValue();
            return true;
        }
    }

    private Optional<StoredRecord> find(final RecordKey key) {
        try (UnitOfWork unit = begin(TransactionOptions.AccessMode.READ_ONLY)) {
            final Optional<StoredRecord> result = engine.records().find(unit, key).requireValue();
            unit.commit().requireValue();
            return result;
        }
    }

    private StoredRecord save(final RecordKey key, final long expectedRevision,
                              final byte[] payload) {
        try (UnitOfWork unit = begin(TransactionOptions.AccessMode.READ_WRITE)) {
            final RecordRevision expected = RecordRevision.of(expectedRevision);
            final StoredRecord result = engine.records().save(unit, StoredRecord.of(key, expected,
                    SCHEMA_VERSION, payload, Instant.now()), expected).requireValue();
            unit.commit().requireValue();
            return result;
        }
    }

    private UnitOfWork begin(final TransactionOptions.AccessMode mode) {
        return engine.begin(TransactionOptions.of(mode, TIMEOUT, 2)).requireValue();
    }

    private <T> CompletionStage<T> async(final Work<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try { return work.run(); }
            catch (RuntimeException failure) { throw new CompletionException(failure); }
        }, executor);
    }

    private static RecordKey playerKey(final DefinitionId type, final PlayerId playerId) {
        return RecordKey.of(type, DefinitionId.of("zartra", type.path() + "/" + playerId));
    }
    private static RecordKey rotationKey(final ShopIds.RotationId id) {
        return RecordKey.of(ROTATION, id.value());
    }
    private interface Work<T> { T run(); }

    private static final class Codec {
        private Codec() { }
        private static byte[] preferences(final ShopUserData.QuickBuyLayout quickBuy,
                                          final ShopUserData.Favourites favourites) {
            return write(output -> {
                output.writeInt(quickBuy.assignments().size());
                for (Map.Entry<Integer, ShopIds.ItemId> entry
                        : quickBuy.assignments().entrySet()) {
                    output.writeInt(entry.getKey()); output.writeUTF(entry.getValue().toString());
                }
                output.writeInt(favourites.items().size());
                for (ShopIds.ItemId item : favourites.items()) { output.writeUTF(item.toString()); }
            });
        }
        private static ShopUserData.PreferenceSnapshot preferences(final long revision,
                                                                    final byte[] payload) {
            return read(payload, input -> {
                final Map<Integer, ShopIds.ItemId> slots = new LinkedHashMap<Integer, ShopIds.ItemId>();
                final int slotCount = bounded(input.readInt(), 0, 45, "slot count");
                for (int index = 0; index < slotCount; index++) {
                    slots.put(input.readInt(), ShopIds.ItemId.parse(input.readUTF()));
                }
                final List<ShopIds.ItemId> favourites = new ArrayList<ShopIds.ItemId>();
                final int favouriteCount = bounded(input.readInt(), 0, 128, "favourite count");
                for (int index = 0; index < favouriteCount; index++) {
                    favourites.add(ShopIds.ItemId.parse(input.readUTF()));
                }
                return new ShopUserData.PreferenceSnapshot(revision,
                        new ShopUserData.QuickBuyLayout(slots),
                        new ShopUserData.Favourites(favourites));
            });
        }
        private static byte[] history(final List<ShopUserData.HistoryEntry> entries) {
            return write(output -> {
                output.writeInt(entries.size());
                for (ShopUserData.HistoryEntry entry : entries) {
                    output.writeUTF(entry.key().toString()); output.writeUTF(entry.itemId().toString());
                    output.writeInt(entry.batches()); output.writeLong(entry.committedAt().toEpochMilli());
                    output.writeInt(entry.charged().amounts().size());
                    for (ShopCatalog.ResourceAmount amount : entry.charged().amounts()) {
                        output.writeUTF(amount.resourceId().toString()); output.writeLong(amount.amount());
                    }
                }
            });
        }
        private static List<ShopUserData.HistoryEntry> history(final byte[] payload) {
            return read(payload, input -> {
                final int count = bounded(input.readInt(), 0, 100, "history count");
                final List<ShopUserData.HistoryEntry> entries = new ArrayList<ShopUserData.HistoryEntry>();
                for (int index = 0; index < count; index++) {
                    final IdempotencyKey key = IdempotencyKey.parse(input.readUTF());
                    final ShopIds.ItemId item = ShopIds.ItemId.parse(input.readUTF());
                    final int batches = input.readInt(); final Instant committed = Instant.ofEpochMilli(input.readLong());
                    final int prices = bounded(input.readInt(), 1, 16, "price count");
                    final List<ShopCatalog.ResourceAmount> amounts = new ArrayList<ShopCatalog.ResourceAmount>();
                    for (int price = 0; price < prices; price++) {
                        amounts.add(new ShopCatalog.ResourceAmount(ResourceId.parse(input.readUTF()), input.readLong()));
                    }
                    entries.add(new ShopUserData.HistoryEntry(key, item, batches,
                            new ShopCatalog.Price(amounts), committed));
                }
                return ShopUserData.newestFirst(entries);
            });
        }
        private static byte[] rotation(final RotationContracts.Snapshot snapshot) {
            return write(output -> {
                output.writeUTF(snapshot.id().toString());
                output.writeLong(snapshot.periodStart().toEpochMilli());
                output.writeLong(snapshot.periodEnd().toEpochMilli());
                output.writeInt(snapshot.activeItems().size());
                for (ShopIds.ItemId item : snapshot.activeItems()) { output.writeUTF(item.toString()); }
            });
        }
        private static RotationContracts.Snapshot rotation(final long revision,
                                                            final byte[] payload) {
            return read(payload, input -> {
                final ShopIds.RotationId id = ShopIds.RotationId.parse(input.readUTF());
                final Instant start = Instant.ofEpochMilli(input.readLong());
                final Instant end = Instant.ofEpochMilli(input.readLong());
                final int count = bounded(input.readInt(), 1, 54, "rotation item count");
                final List<ShopIds.ItemId> items = new ArrayList<ShopIds.ItemId>();
                for (int index = 0; index < count; index++) { items.add(ShopIds.ItemId.parse(input.readUTF())); }
                return new RotationContracts.Snapshot(id, revision, start, end, items);
            });
        }
        private static byte[] write(final Writer writer) {
            try {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (DataOutputStream output = new DataOutputStream(bytes)) { writer.write(output); }
                return bytes.toByteArray();
            } catch (IOException failure) { throw new IllegalStateException("shop state encoding failed", failure); }
        }
        private static <T> T read(final byte[] payload, final Reader<T> reader) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
                final T value = reader.read(input);
                if (input.available() != 0) { throw new IllegalArgumentException("trailing shop state bytes"); }
                return value;
            } catch (IOException | RuntimeException failure) {
                throw new IllegalArgumentException("invalid shop state payload", failure);
            }
        }
        private static int bounded(final int value, final int minimum, final int maximum,
                                   final String field) {
            if (value < minimum || value > maximum) { throw new IllegalArgumentException(field + " is out of range"); }
            return value;
        }
        private interface Writer { void write(DataOutputStream output) throws IOException; }
        private interface Reader<T> { T read(DataInputStream input) throws IOException; }
    }
}
