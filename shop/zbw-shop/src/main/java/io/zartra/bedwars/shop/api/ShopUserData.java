package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/** Immutable Quick Buy, favourite and bounded purchase-history contracts. */
public final class ShopUserData {
    private ShopUserData() { }

    /** Immutable Quick Buy page with unique item and slot assignments. */
    public static final class QuickBuyLayout {
        private final Map<Integer, ShopIds.ItemId> assignments;
        /** Creates a layout for the 45 player-managed slots. */
        public QuickBuyLayout(final Map<Integer, ShopIds.ItemId> assignments) {
            final Map<Integer, ShopIds.ItemId> copy = new LinkedHashMap<Integer, ShopIds.ItemId>();
            final Set<ShopIds.ItemId> unique = new LinkedHashSet<ShopIds.ItemId>();
            for (Map.Entry<Integer, ShopIds.ItemId> entry
                    : Objects.requireNonNull(assignments, "assignments").entrySet()) {
                final Integer slot = Objects.requireNonNull(entry.getKey(), "slot");
                final ShopIds.ItemId item = Objects.requireNonNull(entry.getValue(), "item");
                if (slot < 0 || slot > 44) { throw new IllegalArgumentException("Quick Buy slot is out of range"); }
                if (!unique.add(item)) { throw new IllegalArgumentException("duplicate Quick Buy item"); }
                copy.put(slot, item);
            }
            this.assignments = Collections.unmodifiableMap(copy);
        }
        /** @return immutable slot-to-item assignments */ public Map<Integer, ShopIds.ItemId> assignments() { return assignments; }
    }

    /** Immutable favourite item set. */
    public static final class Favourites {
        private final Set<ShopIds.ItemId> items;
        /** Creates a bounded set of at most 128 favourites. */
        public Favourites(final Collection<ShopIds.ItemId> items) {
            final Set<ShopIds.ItemId> copy = new LinkedHashSet<ShopIds.ItemId>();
            for (ShopIds.ItemId item : Objects.requireNonNull(items, "items")) {
                if (!copy.add(Objects.requireNonNull(item, "item"))) {
                    throw new IllegalArgumentException("duplicate favourite");
                }
            }
            if (copy.size() > 128) { throw new IllegalArgumentException("too many favourites"); }
            this.items = Collections.unmodifiableSet(copy);
        }
        /** @return immutable favourites */ public Set<ShopIds.ItemId> items() { return items; }
    }

    /** Immutable successful or duplicate-safe purchase-history record. */
    public static final class HistoryEntry {
        private final IdempotencyKey key;
        private final ShopIds.ItemId itemId;
        private final int batches;
        private final ShopCatalog.Price charged;
        private final Instant committedAt;
        /** Creates a committed history entry. */
        public HistoryEntry(final IdempotencyKey key, final ShopIds.ItemId itemId,
                            final int batches, final ShopCatalog.Price charged,
                            final Instant committedAt) {
            this.key = Objects.requireNonNull(key, "key");
            this.itemId = Objects.requireNonNull(itemId, "itemId");
            if (batches < 1 || batches > 64) { throw new IllegalArgumentException("batches is out of range"); }
            this.batches = batches;
            this.charged = Objects.requireNonNull(charged, "charged");
            this.committedAt = Objects.requireNonNull(committedAt, "committedAt");
        }
        /** @return purchase idempotency key */ public IdempotencyKey key() { return key; }
        /** @return purchased item */ public ShopIds.ItemId itemId() { return itemId; }
        /** @return purchased batches */ public int batches() { return batches; }
        /** @return exact charged price */ public ShopCatalog.Price charged() { return charged; }
        /** @return successful commit instant */ public Instant committedAt() { return committedAt; }
    }

    /** Asynchronous persistence port for Quick Buy and favourite snapshots. */
    public interface PreferencePort {
        /** Loads immutable user preferences without blocking an owner thread. */
        CompletionStage<PreferenceSnapshot> load(PlayerId playerId);
        /** Atomically replaces preferences using an optimistic expected revision. */
        CompletionStage<PreferenceSnapshot> replace(PlayerId playerId, long expectedRevision,
                                                    QuickBuyLayout quickBuy, Favourites favourites);
    }

    /** Asynchronous, bounded and newest-first history query port. */
    public interface HistoryPort {
        /** Loads at most {@code limit} records; implementations must reject values outside 1..100. */
        CompletionStage<List<HistoryEntry>> recent(PlayerId playerId, int limit);
    }

    /** Immutable optimistic preference snapshot. */
    public static final class PreferenceSnapshot {
        private final long revision;
        private final QuickBuyLayout quickBuy;
        private final Favourites favourites;
        /** Creates a non-negative revision snapshot. */
        public PreferenceSnapshot(final long revision, final QuickBuyLayout quickBuy,
                                  final Favourites favourites) {
            if (revision < 0) { throw new IllegalArgumentException("revision must not be negative"); }
            this.revision = revision;
            this.quickBuy = Objects.requireNonNull(quickBuy, "quickBuy");
            this.favourites = Objects.requireNonNull(favourites, "favourites");
        }
        /** @return optimistic revision */ public long revision() { return revision; }
        /** @return Quick Buy layout */ public QuickBuyLayout quickBuy() { return quickBuy; }
        /** @return favourites */ public Favourites favourites() { return favourites; }
    }

    /** Returns a deterministic immutable newest-first history copy. */
    public static List<HistoryEntry> newestFirst(final Collection<HistoryEntry> entries) {
        final List<HistoryEntry> copy = new ArrayList<HistoryEntry>();
        for (HistoryEntry entry : Objects.requireNonNull(entries, "entries")) {
            copy.add(Objects.requireNonNull(entry, "entry"));
        }
        copy.sort(Comparator.comparing(HistoryEntry::committedAt).reversed()
                .thenComparing(value -> value.key().toString()));
        return Collections.unmodifiableList(copy);
    }
}
