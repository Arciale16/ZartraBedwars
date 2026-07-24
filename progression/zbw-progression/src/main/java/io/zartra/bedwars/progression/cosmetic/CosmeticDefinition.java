package io.zartra.bedwars.progression.cosmetic;

import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.quest.QuestId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, versioned cosmetic definition with M12/M13 unlock references. */
public final class CosmeticDefinition {
    /** Lifecycle trigger interpreted by a later platform projection. */
    public enum Trigger { KILL, FINAL_KILL, BED_DESTROY, WIN, PROJECTILE, SHOP, ISLAND, LOBBY }

    private final CosmeticId id;
    private final int version;
    private final CosmeticCategoryId categoryId;
    private final CosmeticRarityId rarityId;
    private final String displayKey;
    private final List<Trigger> triggers;
    private final Optional<EntitlementId> entitlement;
    private final Optional<QuestId> quest;
    private final Optional<AchievementId> achievement;
    private final boolean enabled;

    /** Creates a validated definition. */
    public CosmeticDefinition(final CosmeticId id, final int version,
                              final CosmeticCategoryId categoryId,
                              final CosmeticRarityId rarityId, final String displayKey,
                              final List<Trigger> triggers,
                              final Optional<EntitlementId> entitlement,
                              final Optional<QuestId> quest,
                              final Optional<AchievementId> achievement,
                              final boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.rarityId = Objects.requireNonNull(rarityId, "rarityId");
        if (displayKey == null || !displayKey.matches("[a-z0-9_.-]{3,128}")) {
            throw new IllegalArgumentException("displayKey must be a safe localization key");
        }
        this.displayKey = displayKey;
        final List<Trigger> copy = new ArrayList<Trigger>(Objects.requireNonNull(triggers, "triggers"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("triggers must not be empty or contain null");
        }
        this.triggers = Collections.unmodifiableList(copy);
        this.entitlement = Objects.requireNonNull(entitlement, "entitlement");
        this.quest = Objects.requireNonNull(quest, "quest");
        this.achievement = Objects.requireNonNull(achievement, "achievement");
        this.enabled = enabled;
    }

    /** @return identity */ public CosmeticId id() { return id; }
    /** @return definition version */ public int version() { return version; }
    /** @return category identity */ public CosmeticCategoryId categoryId() { return categoryId; }
    /** @return rarity identity */ public CosmeticRarityId rarityId() { return rarityId; }
    /** @return localization key */ public String displayKey() { return displayKey; }
    /** @return immutable triggers */ public List<Trigger> triggers() { return triggers; }
    /** @return optional M12 entitlement */ public Optional<EntitlementId> entitlement() { return entitlement; }
    /** @return optional M13 quest */ public Optional<QuestId> quest() { return quest; }
    /** @return optional M13 achievement */ public Optional<AchievementId> achievement() { return achievement; }
    /** @return enabled state */ public boolean enabled() { return enabled; }
}
