package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.scripting.api.ActionReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, revisioned and scope-aware shop catalog. */
public final class ShopCatalog {
    private final ShopIds.CatalogId id;
    private final long revision;
    private final Scope scope;
    private final DefinitionId balanceProfile;
    private final List<Category> categories;
    private final List<ItemDefinition> items;
    private final Map<ShopIds.CategoryId, Category> categoriesById;
    private final Map<ShopIds.ItemId, ItemDefinition> itemsById;

    /** Creates a validated catalog snapshot and rejects duplicate or dangling definitions. */
    public ShopCatalog(final ShopIds.CatalogId id, final long revision, final Scope scope,
                       final DefinitionId balanceProfile, final Collection<Category> categories,
                       final Collection<ItemDefinition> items) {
        this.id = Objects.requireNonNull(id, "id");
        if (revision < 1) { throw new IllegalArgumentException("revision must be positive"); }
        this.revision = revision;
        this.scope = Objects.requireNonNull(scope, "scope");
        this.balanceProfile = Objects.requireNonNull(balanceProfile, "balanceProfile");
        final Map<ShopIds.CategoryId, Category> categoryMap = new LinkedHashMap<ShopIds.CategoryId, Category>();
        for (Category value : Objects.requireNonNull(categories, "categories")) {
            final Category checked = Objects.requireNonNull(value, "category");
            if (categoryMap.put(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate category " + checked.id());
            }
        }
        if (categoryMap.isEmpty()) { throw new IllegalArgumentException("catalog requires a category"); }
        final Map<ShopIds.ItemId, ItemDefinition> itemMap = new LinkedHashMap<ShopIds.ItemId, ItemDefinition>();
        for (ItemDefinition value : Objects.requireNonNull(items, "items")) {
            final ItemDefinition checked = Objects.requireNonNull(value, "item");
            if (!categoryMap.containsKey(checked.categoryId())) {
                throw new IllegalArgumentException("item references unknown category " + checked.categoryId());
            }
            if (itemMap.put(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate item " + checked.id());
            }
        }
        final List<Category> sortedCategories = new ArrayList<Category>(categoryMap.values());
        sortedCategories.sort(Comparator.comparingInt(Category::order).thenComparing(Category::id));
        final List<ItemDefinition> sortedItems = new ArrayList<ItemDefinition>(itemMap.values());
        sortedItems.sort(Comparator.comparing(ItemDefinition::categoryId).thenComparing(ItemDefinition::id));
        this.categories = Collections.unmodifiableList(sortedCategories);
        this.items = Collections.unmodifiableList(sortedItems);
        this.categoriesById = Collections.unmodifiableMap(categoryMap);
        this.itemsById = Collections.unmodifiableMap(itemMap);
    }

    /** @return stable catalog identity */ public ShopIds.CatalogId id() { return id; }
    /** @return monotonically increasing content revision */ public long revision() { return revision; }
    /** @return applicability scope */ public Scope scope() { return scope; }
    /** @return selected versioned balance profile */ public DefinitionId balanceProfile() { return balanceProfile; }
    /** @return deterministic immutable categories */ public List<Category> categories() { return categories; }
    /** @return deterministic immutable items */ public List<ItemDefinition> items() { return items; }
    /** @return category when registered */ public Optional<Category> category(final ShopIds.CategoryId categoryId) {
        return Optional.ofNullable(categoriesById.get(Objects.requireNonNull(categoryId, "categoryId")));
    }
    /** @return item when registered */ public Optional<ItemDefinition> item(final ShopIds.ItemId itemId) {
        return Optional.ofNullable(itemsById.get(Objects.requireNonNull(itemId, "itemId")));
    }

    /** Catalog visibility states. Hidden definitions remain addressable to administrative APIs. */
    public enum Visibility { /** Visible. */ VISIBLE, /** Hidden from players. */ HIDDEN, /** Disabled. */ DISABLED }

    /** Purchasable item availability classification. */
    public enum Availability {
        /** Normal catalog item. */ STANDARD, /** Active rotating item. */ ROTATING,
        /** Active seasonal item. */ SEASONAL, /** Extension-defined item. */ CUSTOM,
        /** Hidden from direct player purchase. */ HIDDEN, /** Explicitly disabled. */ DISABLED
    }

    /** Immutable mode, arena, group and team applicability selectors. */
    public static final class Scope {
        private final DefinitionId mode;
        private final ArenaId arena;
        private final DefinitionId group;
        private final DefinitionId team;

        private Scope(final DefinitionId mode, final ArenaId arena,
                      final DefinitionId group, final DefinitionId team) {
            this.mode = mode;
            this.arena = arena;
            this.group = group;
            this.team = team;
        }

        /** @return globally applicable catalog scope */ public static Scope global() { return new Scope(null, null, null, null); }
        /** @return scope with any combination of exact typed selectors */
        public static Scope of(final Optional<DefinitionId> mode, final Optional<ArenaId> arena,
                               final Optional<DefinitionId> group, final Optional<DefinitionId> team) {
            return new Scope(Objects.requireNonNull(mode, "mode").orElse(null),
                    Objects.requireNonNull(arena, "arena").orElse(null),
                    Objects.requireNonNull(group, "group").orElse(null),
                    Objects.requireNonNull(team, "team").orElse(null));
        }
        /** @return configured mode selector */ public Optional<DefinitionId> mode() { return Optional.ofNullable(mode); }
        /** @return configured arena selector */ public Optional<ArenaId> arena() { return Optional.ofNullable(arena); }
        /** @return configured group selector */ public Optional<DefinitionId> group() { return Optional.ofNullable(group); }
        /** @return configured team selector */ public Optional<DefinitionId> team() { return Optional.ofNullable(team); }
        /** @return whether a concrete context satisfies every configured selector */
        public boolean matches(final DefinitionId actualMode, final ArenaId actualArena,
                               final Optional<DefinitionId> actualGroup, final DefinitionId actualTeam) {
            Objects.requireNonNull(actualMode, "actualMode");
            Objects.requireNonNull(actualArena, "actualArena");
            Objects.requireNonNull(actualGroup, "actualGroup");
            Objects.requireNonNull(actualTeam, "actualTeam");
            return (mode == null || mode.equals(actualMode)) && (arena == null || arena.equals(actualArena))
                    && (group == null || actualGroup.filter(group::equals).isPresent())
                    && (team == null || team.equals(actualTeam));
        }
    }

    /** Immutable category definition. */
    public static final class Category {
        private final ShopIds.CategoryId id;
        private final MessageKey name;
        private final MessageKey lore;
        private final DefinitionId icon;
        private final int slot;
        private final int order;
        private final Visibility visibility;
        private final PermissionNode permission;
        private final DefinitionId condition;

        /** Creates a configurable category projection. */
        public Category(final ShopIds.CategoryId id, final MessageKey name, final MessageKey lore,
                        final DefinitionId icon, final int slot, final int order,
                        final Visibility visibility, final Optional<PermissionNode> permission,
                        final Optional<DefinitionId> condition) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
            this.lore = Objects.requireNonNull(lore, "lore");
            this.icon = Objects.requireNonNull(icon, "icon");
            if (slot < 0 || slot > 53 || order < 0) {
                throw new IllegalArgumentException("category slot or order is out of range");
            }
            this.slot = slot;
            this.order = order;
            this.visibility = Objects.requireNonNull(visibility, "visibility");
            this.permission = Objects.requireNonNull(permission, "permission").orElse(null);
            this.condition = Objects.requireNonNull(condition, "condition").orElse(null);
        }
        /** @return category ID */ public ShopIds.CategoryId id() { return id; }
        /** @return localized name key */ public MessageKey name() { return name; }
        /** @return localized lore key */ public MessageKey lore() { return lore; }
        /** @return semantic icon identity */ public DefinitionId icon() { return icon; }
        /** @return configured inventory slot */ public int slot() { return slot; }
        /** @return configured sort order */ public int order() { return order; }
        /** @return visibility */ public Visibility visibility() { return visibility; }
        /** @return optional granular permission */ public Optional<PermissionNode> permission() { return Optional.ofNullable(permission); }
        /** @return optional condition definition */ public Optional<DefinitionId> condition() { return Optional.ofNullable(condition); }
    }

    /** Positive amount of one native or custom match resource. */
    public static final class ResourceAmount {
        private static final long MAX_AMOUNT = 1_000_000_000L;
        private final ResourceId resourceId;
        private final long amount;
        /** Creates a bounded positive resource amount. */
        public ResourceAmount(final ResourceId resourceId, final long amount) {
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
            if (amount < 1 || amount > MAX_AMOUNT) { throw new IllegalArgumentException("resource amount is out of range"); }
            this.amount = amount;
        }
        /** @return resource identity */ public ResourceId resourceId() { return resourceId; }
        /** @return positive amount */ public long amount() { return amount; }
        /** @return safely multiplied amount */
        public ResourceAmount multiply(final int factor) {
            if (factor < 1 || amount > MAX_AMOUNT / factor) { throw new IllegalArgumentException("resource multiplication is out of range"); }
            return new ResourceAmount(resourceId, amount * factor);
        }
        @Override public int hashCode() { return Objects.hash(resourceId, amount); }
        @Override public boolean equals(final Object other) {
            return this == other || other instanceof ResourceAmount
                    && amount == ((ResourceAmount) other).amount
                    && resourceId.equals(((ResourceAmount) other).resourceId);
        }
    }

    /** Immutable one-or-more-resource price. */
    public static final class Price {
        private final List<ResourceAmount> amounts;
        /** Creates a deterministic price and rejects duplicate resource rows. */
        public Price(final Collection<ResourceAmount> amounts) {
            final Map<ResourceId, ResourceAmount> unique = new LinkedHashMap<ResourceId, ResourceAmount>();
            for (ResourceAmount value : Objects.requireNonNull(amounts, "amounts")) {
                final ResourceAmount checked = Objects.requireNonNull(value, "amount");
                if (unique.put(checked.resourceId(), checked) != null) {
                    throw new IllegalArgumentException("duplicate price resource " + checked.resourceId());
                }
            }
            if (unique.isEmpty()) { throw new IllegalArgumentException("price requires a resource"); }
            final List<ResourceAmount> sorted = new ArrayList<ResourceAmount>(unique.values());
            sorted.sort(Comparator.comparing(ResourceAmount::resourceId));
            this.amounts = Collections.unmodifiableList(sorted);
        }
        /** @return deterministic immutable resource amounts */ public List<ResourceAmount> amounts() { return amounts; }
        /** @return a bulk price with every component multiplied */
        public Price multiply(final int factor) {
            final List<ResourceAmount> result = new ArrayList<ResourceAmount>();
            for (ResourceAmount amount : amounts) { result.add(amount.multiply(factor)); }
            return new Price(result);
        }
        @Override public int hashCode() { return amounts.hashCode(); }
        @Override public boolean equals(final Object other) {
            return this == other || other instanceof Price && amounts.equals(((Price) other).amounts);
        }
    }

    /** Immutable purchase cooldown, bulk and limit policy. Zero limits mean unlimited. */
    public static final class PurchaseRules {
        private final boolean confirmationRequired;
        private final int maximumBulk;
        private final Duration cooldown;
        private final int inventoryLimit;
        private final int playerLimit;
        private final int teamLimit;
        private final int arenaLimit;
        private final PermissionNode permission;
        private final DefinitionId condition;

        /** Creates a fully bounded purchase policy. */
        public PurchaseRules(final boolean confirmationRequired, final int maximumBulk,
                             final Duration cooldown, final int inventoryLimit,
                             final int playerLimit, final int teamLimit, final int arenaLimit,
                             final Optional<PermissionNode> permission,
                             final Optional<DefinitionId> condition) {
            if (maximumBulk < 1 || maximumBulk > 64) { throw new IllegalArgumentException("maximumBulk is out of range"); }
            this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
            if (cooldown.isNegative() || cooldown.compareTo(Duration.ofDays(7)) > 0) {
                throw new IllegalArgumentException("cooldown is out of range");
            }
            checkLimit(inventoryLimit);
            checkLimit(playerLimit);
            checkLimit(teamLimit);
            checkLimit(arenaLimit);
            this.confirmationRequired = confirmationRequired;
            this.maximumBulk = maximumBulk;
            this.inventoryLimit = inventoryLimit;
            this.playerLimit = playerLimit;
            this.teamLimit = teamLimit;
            this.arenaLimit = arenaLimit;
            this.permission = Objects.requireNonNull(permission, "permission").orElse(null);
            this.condition = Objects.requireNonNull(condition, "condition").orElse(null);
        }
        private static void checkLimit(final int value) {
            if (value < 0 || value > 1_000_000) { throw new IllegalArgumentException("purchase limit is out of range"); }
        }
        /** @return whether explicit confirmation is mandatory */ public boolean confirmationRequired() { return confirmationRequired; }
        /** @return maximum number of batches in one purchase */ public int maximumBulk() { return maximumBulk; }
        /** @return per-item cooldown */ public Duration cooldown() { return cooldown; }
        /** @return maximum currently owned units, or zero */ public int inventoryLimit() { return inventoryLimit; }
        /** @return per-player match purchase limit, or zero */ public int playerLimit() { return playerLimit; }
        /** @return per-team match purchase limit, or zero */ public int teamLimit() { return teamLimit; }
        /** @return per-arena match purchase limit, or zero */ public int arenaLimit() { return arenaLimit; }
        /** @return optional granular permission */ public Optional<PermissionNode> permission() { return Optional.ofNullable(permission); }
        /** @return optional custom condition */ public Optional<DefinitionId> condition() { return Optional.ofNullable(condition); }
    }

    /** Immutable purchasable definition independent of a platform item stack. */
    public static final class ItemDefinition {
        private final ShopIds.ItemId id;
        private final ShopIds.CategoryId categoryId;
        private final DefinitionId semanticItem;
        private final MessageKey name;
        private final MessageKey lore;
        private final int grantQuantity;
        private final Price price;
        private final Availability availability;
        private final PurchaseRules rules;
        private final List<ActionReference> actions;

        /** Creates a fully validated item definition. */
        public ItemDefinition(final ShopIds.ItemId id, final ShopIds.CategoryId categoryId,
                              final DefinitionId semanticItem, final MessageKey name,
                              final MessageKey lore, final int grantQuantity, final Price price,
                              final Availability availability, final PurchaseRules rules,
                              final Collection<ActionReference> actions) {
            this.id = Objects.requireNonNull(id, "id");
            this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
            this.semanticItem = Objects.requireNonNull(semanticItem, "semanticItem");
            this.name = Objects.requireNonNull(name, "name");
            this.lore = Objects.requireNonNull(lore, "lore");
            if (grantQuantity < 1 || grantQuantity > 4096) { throw new IllegalArgumentException("grantQuantity is out of range"); }
            this.grantQuantity = grantQuantity;
            this.price = Objects.requireNonNull(price, "price");
            this.availability = Objects.requireNonNull(availability, "availability");
            this.rules = Objects.requireNonNull(rules, "rules");
            final List<ActionReference> copy = new ArrayList<ActionReference>();
            for (ActionReference action : Objects.requireNonNull(actions, "actions")) {
                copy.add(Objects.requireNonNull(action, "action"));
            }
            this.actions = Collections.unmodifiableList(copy);
        }
        /** @return stable item ID */ public ShopIds.ItemId id() { return id; }
        /** @return owning category */ public ShopIds.CategoryId categoryId() { return categoryId; }
        /** @return platform-neutral item/effect identity */ public DefinitionId semanticItem() { return semanticItem; }
        /** @return localized name key */ public MessageKey name() { return name; }
        /** @return localized lore key */ public MessageKey lore() { return lore; }
        /** @return granted units per purchased batch */ public int grantQuantity() { return grantQuantity; }
        /** @return base price per batch */ public Price price() { return price; }
        /** @return availability class */ public Availability availability() { return availability; }
        /** @return purchase policy */ public PurchaseRules rules() { return rules; }
        /** @return immutable declarative post-grant actions */ public List<ActionReference> actions() { return actions; }
    }
}
