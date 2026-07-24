package io.zartra.bedwars.progression.cosmetic;

import java.util.Objects;

/** Immutable configurable rarity definition. */
public final class CosmeticRarity {
    private final CosmeticRarityId id;
    private final int sortOrder;
    private final String displayKey;

    /** Creates a rarity. */
    public CosmeticRarity(final CosmeticRarityId id, final int sortOrder, final String displayKey) {
        this.id = Objects.requireNonNull(id, "id");
        if (sortOrder < 0) { throw new IllegalArgumentException("sortOrder must be non-negative"); }
        if (displayKey == null || !displayKey.matches("[a-z0-9_.-]{3,128}")) {
            throw new IllegalArgumentException("displayKey must be a safe localization key");
        }
        this.sortOrder = sortOrder;
        this.displayKey = displayKey;
    }
    /** @return identity */ public CosmeticRarityId id() { return id; }
    /** @return stable sort order */ public int sortOrder() { return sortOrder; }
    /** @return localization key */ public String displayKey() { return displayKey; }
}
