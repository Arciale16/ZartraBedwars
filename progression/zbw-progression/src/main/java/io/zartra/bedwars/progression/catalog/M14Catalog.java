package io.zartra.bedwars.progression.catalog;

import io.zartra.bedwars.progression.calendar.CalendarCampaign;
import io.zartra.bedwars.progression.cosmetic.CosmeticCategoryId;
import io.zartra.bedwars.progression.cosmetic.CosmeticDefinition;
import io.zartra.bedwars.progression.cosmetic.CosmeticRarity;
import io.zartra.bedwars.progression.cosmetic.CosmeticRarityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable M14 definition catalogue with deterministic cross-reference validation. */
public final class M14Catalog {
    private final List<CosmeticCategoryId> categories;
    private final List<CosmeticRarity> rarities;
    private final List<CosmeticDefinition> cosmetics;
    private final List<CalendarCampaign> campaigns;

    /** Creates a validated catalogue snapshot. */
    public M14Catalog(final List<CosmeticCategoryId> categories,
                      final List<CosmeticRarity> rarities,
                      final List<CosmeticDefinition> cosmetics,
                      final List<CalendarCampaign> campaigns) {
        this.categories = immutable(categories, "categories");
        this.rarities = immutable(rarities, "rarities");
        this.cosmetics = immutable(cosmetics, "cosmetics");
        this.campaigns = immutable(campaigns, "campaigns");
        validate();
    }

    /** Validates the production definition-count gate. */
    public void validateProduction(final M14Configuration configuration) {
        if (cosmetics.size() < Objects.requireNonNull(configuration, "configuration")
                .minimumProductionCosmetics()) {
            throw new IllegalArgumentException("cosmetic catalogue is below production minimum");
        }
    }
    /** @return categories */ public List<CosmeticCategoryId> categories() { return categories; }
    /** @return rarities */ public List<CosmeticRarity> rarities() { return rarities; }
    /** @return cosmetics */ public List<CosmeticDefinition> cosmetics() { return cosmetics; }
    /** @return campaigns */ public List<CalendarCampaign> campaigns() { return campaigns; }

    private void validate() {
        final Set<CosmeticCategoryId> categoryIds = unique(categories, "category");
        final Set<CosmeticRarityId> rarityIds = new HashSet<CosmeticRarityId>();
        for (CosmeticRarity rarity : rarities) {
            if (!rarityIds.add(rarity.id())) { throw new IllegalArgumentException("duplicate rarity ID"); }
        }
        final Set<Object> definitionIds = new HashSet<Object>();
        for (CosmeticDefinition cosmetic : cosmetics) {
            if (!definitionIds.add(cosmetic.id())) { throw new IllegalArgumentException("duplicate cosmetic ID"); }
            if (!categoryIds.contains(cosmetic.categoryId())) { throw new IllegalArgumentException("unknown category"); }
            if (!rarityIds.contains(cosmetic.rarityId())) { throw new IllegalArgumentException("unknown rarity"); }
        }
        for (CalendarCampaign campaign : campaigns) {
            if (!definitionIds.add(campaign.id())) { throw new IllegalArgumentException("duplicate campaign ID"); }
        }
    }

    private static <T> Set<T> unique(final List<T> values, final String type) {
        final Set<T> result = new HashSet<T>(values);
        if (result.size() != values.size()) { throw new IllegalArgumentException("duplicate " + type + " ID"); }
        return result;
    }

    private static <T> List<T> immutable(final List<T> values, final String name) {
        final List<T> copy = new ArrayList<T>(Objects.requireNonNull(values, name));
        if (copy.contains(null)) { throw new IllegalArgumentException(name + " must not contain null"); }
        return Collections.unmodifiableList(copy);
    }
}
