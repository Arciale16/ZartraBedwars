package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Namespace for strongly typed shop identities. */
public final class ShopIds {
    private ShopIds() { }

    private static DefinitionId parse(final String serialized, final String family) {
        final DefinitionId value = DefinitionId.parse(serialized);
        if (!value.path().startsWith(family + "/")
                || value.path().length() == family.length() + 1) {
            throw new IllegalArgumentException("identity must use the " + family + "/ path");
        }
        return value;
    }

    /** Immutable shop catalog identity. */
    public static final class CatalogId implements Comparable<CatalogId> {
        private final DefinitionId value;
        private CatalogId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return identity in {@code namespace:shop-catalog/path} form */
        public static CatalogId of(final String namespace, final String path) {
            return new CatalogId(DefinitionId.of(namespace, "shop-catalog/" + path));
        }
        /** @return parsed catalog identity */
        public static CatalogId parse(final String value) {
            return new CatalogId(ShopIds.parse(value, "shop-catalog"));
        }
        /** @return underlying definition ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final CatalogId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof CatalogId && value.equals(((CatalogId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Immutable shop category identity. */
    public static final class CategoryId implements Comparable<CategoryId> {
        private final DefinitionId value;
        private CategoryId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return identity in {@code namespace:shop-category/path} form */
        public static CategoryId of(final String namespace, final String path) {
            return new CategoryId(DefinitionId.of(namespace, "shop-category/" + path));
        }
        /** @return parsed category identity */
        public static CategoryId parse(final String value) {
            return new CategoryId(ShopIds.parse(value, "shop-category"));
        }
        /** @return underlying definition ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final CategoryId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof CategoryId && value.equals(((CategoryId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Immutable purchasable item identity. */
    public static final class ItemId implements Comparable<ItemId> {
        private final DefinitionId value;
        private ItemId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return identity in {@code namespace:shop-item/path} form */
        public static ItemId of(final String namespace, final String path) {
            return new ItemId(DefinitionId.of(namespace, "shop-item/" + path));
        }
        /** @return parsed item identity */
        public static ItemId parse(final String value) {
            return new ItemId(ShopIds.parse(value, "shop-item"));
        }
        /** @return underlying definition ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final ItemId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ItemId && value.equals(((ItemId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Immutable item-rotation identity. */
    public static final class RotationId implements Comparable<RotationId> {
        private final DefinitionId value;
        private RotationId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return identity in {@code namespace:shop-rotation/path} form */
        public static RotationId of(final String namespace, final String path) {
            return new RotationId(DefinitionId.of(namespace, "shop-rotation/" + path));
        }
        /** @return parsed rotation identity */
        public static RotationId parse(final String value) {
            return new RotationId(ShopIds.parse(value, "shop-rotation"));
        }
        /** @return underlying definition ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final RotationId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof RotationId && value.equals(((RotationId) other).value); }
        @Override public String toString() { return value.toString(); }
    }
}
