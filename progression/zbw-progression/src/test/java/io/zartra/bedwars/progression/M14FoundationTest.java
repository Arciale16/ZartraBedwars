package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.calendar.CalendarCampaign;
import io.zartra.bedwars.progression.calendar.CalendarCampaignId;
import io.zartra.bedwars.progression.catalog.M14Catalog;
import io.zartra.bedwars.progression.catalog.M14Configuration;
import io.zartra.bedwars.progression.cosmetic.CosmeticCategoryId;
import io.zartra.bedwars.progression.cosmetic.CosmeticDefinition;
import io.zartra.bedwars.progression.cosmetic.CosmeticId;
import io.zartra.bedwars.progression.cosmetic.CosmeticLoadout;
import io.zartra.bedwars.progression.cosmetic.CosmeticRarity;
import io.zartra.bedwars.progression.cosmetic.CosmeticRarityId;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.profile.ProfileSettings;
import io.zartra.bedwars.progression.quest.QuestId;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** M14 Phase 1 immutable-model, validation and compatibility contracts. */
class M14FoundationTest {
    private static final CosmeticCategoryId CATEGORY = CosmeticCategoryId.of("zbw", "kill_effect");
    private static final CosmeticRarityId RARITY = CosmeticRarityId.of("zbw", "common");

    @Test
    void typedIdsAndDefinitionReuseM12AndM13References() {
        assertEquals("zbw:spark", CosmeticId.parse("zbw:spark").toString());
        assertThrows(IllegalArgumentException.class, () -> CosmeticId.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> CosmeticId.parse(null));
        assertThrows(IllegalArgumentException.class, () -> CosmeticId.parse("zbw:"));
        final CosmeticDefinition definition = definition(CosmeticId.of("zbw", "spark"));
        assertEquals(1, definition.version());
        assertEquals(CATEGORY, definition.categoryId());
        assertEquals(RARITY, definition.rarityId());
        assertEquals("cosmetic.spark", definition.displayKey());
        assertEquals(CosmeticDefinition.Trigger.KILL, definition.triggers().get(0));
        assertTrue(definition.entitlement().isPresent());
        assertTrue(definition.quest().isPresent());
        assertTrue(definition.achievement().isPresent());
        assertTrue(definition.enabled());
        assertThrows(UnsupportedOperationException.class,
                () -> definition.triggers().add(CosmeticDefinition.Trigger.WIN));
    }

    @Test
    void invalidDefinitionsAndRaritiesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CosmeticRarity(RARITY, -1, "rarity.common"));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticRarity(RARITY, 0, "BAD KEY"));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticDefinition(
                CosmeticId.of("zbw", "bad"), 0, CATEGORY, RARITY, "cosmetic.bad",
                Collections.singletonList(CosmeticDefinition.Trigger.KILL), Optional.empty(),
                Optional.empty(), Optional.empty(), true));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticDefinition(
                CosmeticId.of("zbw", "bad"), 1, CATEGORY, RARITY, "BAD KEY",
                Collections.singletonList(CosmeticDefinition.Trigger.KILL), Optional.empty(),
                Optional.empty(), Optional.empty(), true));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticDefinition(
                CosmeticId.of("zbw", "bad"), 1, CATEGORY, RARITY, "cosmetic.bad",
                Collections.<CosmeticDefinition.Trigger>emptyList(), Optional.empty(),
                Optional.empty(), Optional.empty(), true));
    }

    @Test
    void catalogueValidatesReferencesUniquenessAndProductionCount() {
        final CosmeticRarity rarity = new CosmeticRarity(RARITY, 0, "rarity.common");
        final M14Catalog catalog = new M14Catalog(Collections.singletonList(CATEGORY),
                Collections.singletonList(rarity), Collections.singletonList(definition(
                CosmeticId.of("zbw", "spark"))), Collections.singletonList(campaign()));
        assertEquals(1, catalog.categories().size());
        assertEquals(1, catalog.rarities().size());
        assertEquals(1, catalog.cosmetics().size());
        assertEquals(1, catalog.campaigns().size());
        final M14Configuration configuration = new M14Configuration(1, 300, 16, 64, false);
        assertThrows(IllegalArgumentException.class, () -> catalog.validateProduction(configuration));
        assertThrows(IllegalArgumentException.class, () -> new M14Catalog(
                Arrays.asList(CATEGORY, CATEGORY), Collections.singletonList(rarity),
                Collections.<CosmeticDefinition>emptyList(), Collections.<CalendarCampaign>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new M14Catalog(
                Collections.singletonList(CATEGORY), Arrays.asList(rarity, rarity),
                Collections.<CosmeticDefinition>emptyList(), Collections.<CalendarCampaign>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new M14Catalog(
                Collections.<CosmeticCategoryId>emptyList(), Collections.singletonList(rarity),
                Collections.singletonList(definition(CosmeticId.of("zbw", "spark"))),
                Collections.<CalendarCampaign>emptyList()));
    }

    @Test
    void configurationEnforcesProductionAndRuntimeBudgets() {
        final M14Configuration configuration = new M14Configuration(2, 300, 32, 128, true);
        assertEquals(2, configuration.schemaVersion());
        assertEquals(300, configuration.minimumProductionCosmetics());
        assertEquals(32, configuration.maxEffectsPerPlayerPerTick());
        assertEquals(128, configuration.maxEntitiesPerArena());
        assertTrue(configuration.emergencyDisable());
        assertThrows(IllegalArgumentException.class, () -> new M14Configuration(0, 300, 1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new M14Configuration(1, 299, 1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new M14Configuration(1, 300, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new M14Configuration(1, 300, 1, 4097, false));
    }

    @Test
    void loadoutAndProfileSnapshotsAreImmutableRevisionedAndAudited() {
        final HashMap<CosmeticCategoryId, CosmeticId> equipped = new HashMap<CosmeticCategoryId, CosmeticId>();
        equipped.put(CATEGORY, CosmeticId.of("zbw", "spark"));
        final HashSet<CosmeticId> favourites = new HashSet<CosmeticId>(equipped.values());
        final ArrayList<String> presets = new ArrayList<String>(Collections.singletonList("default"));
        final CosmeticLoadout loadout = new CosmeticLoadout(owner(), equipped, favourites, presets,
                RecordRevision.of(4), audit());
        equipped.clear();
        favourites.clear();
        presets.clear();
        assertEquals(1, loadout.equipped().size());
        assertEquals(1, loadout.favourites().size());
        assertEquals(1, loadout.presets().size());
        assertEquals(4, loadout.revision().value());
        assertEquals("test", loadout.audit().actor());
        assertThrows(UnsupportedOperationException.class, () -> loadout.equipped().clear());
        final ProfileSettings settings = new ProfileSettings(owner(), ProfileSettings.Visibility.PRIVATE,
                true, false, RecordRevision.initial(), audit());
        assertEquals(ProfileSettings.Visibility.PRIVATE, settings.visibility());
        assertTrue(settings.cosmeticsVisible());
        assertFalse(settings.lowPerformanceMode());
        assertEquals(0, settings.revision().value());
        assertEquals(owner(), settings.owner());
    }

    @Test
    void loadoutRejectsNullAndOversizedCollections() {
        final HashMap<CosmeticCategoryId, CosmeticId> invalid = new HashMap<CosmeticCategoryId, CosmeticId>();
        invalid.put(CATEGORY, null);
        assertThrows(IllegalArgumentException.class, () -> new CosmeticLoadout(owner(), invalid,
                Collections.<CosmeticId>emptySet(), Collections.<String>emptyList(),
                RecordRevision.initial(), audit()));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticLoadout(owner(),
                Collections.<CosmeticCategoryId, CosmeticId>emptyMap(),
                Collections.<CosmeticId>singleton(null), Collections.<String>emptyList(),
                RecordRevision.initial(), audit()));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticLoadout(owner(),
                Collections.<CosmeticCategoryId, CosmeticId>emptyMap(),
                Collections.<CosmeticId>emptySet(), Collections.nCopies(33, "preset"),
                RecordRevision.initial(), audit()));
    }

    @Test
    void calendarCampaignValidatesWindowRewardsAndPolicy() {
        final CalendarCampaign campaign = campaign();
        assertEquals("zbw:winter", campaign.id().toString());
        assertEquals(1, campaign.version());
        assertTrue(campaign.endsAt().isAfter(campaign.startsAt()));
        assertEquals("eligibility.winter", campaign.eligibilityPolicy().get());
        assertEquals(RewardId.of("zbw", "winter_reward"), campaign.rewards().get(0));
        assertThrows(IllegalArgumentException.class, () -> new CalendarCampaign(
                CalendarCampaignId.of("zbw", "bad"), 1, Instant.parse("2026-12-31T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"),
                Collections.singletonList(RewardId.of("zbw", "reward")), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CalendarCampaign(
                CalendarCampaignId.of("zbw", "bad"), 1, Instant.parse("2026-12-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"), Collections.<RewardId>emptyList(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CalendarCampaign(
                CalendarCampaignId.of("zbw", "bad"), 1, Instant.parse("2026-12-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                Collections.singletonList(RewardId.of("zbw", "reward")), Optional.of("BAD POLICY")));
    }

    private static CosmeticDefinition definition(final CosmeticId id) {
        return new CosmeticDefinition(id, 1, CATEGORY, RARITY, "cosmetic.spark",
                Collections.singletonList(CosmeticDefinition.Trigger.KILL),
                Optional.of(EntitlementId.of("zbw", "cosmetic_spark")),
                Optional.of(QuestId.of("zbw", "spark_quest")),
                Optional.of(AchievementId.of("zbw", "spark_achievement")), true);
    }

    private static CalendarCampaign campaign() {
        return new CalendarCampaign(CalendarCampaignId.of("zbw", "winter"), 1,
                Instant.parse("2026-12-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"),
                Collections.singletonList(RewardId.of("zbw", "winter_reward")),
                Optional.of("eligibility.winter"));
    }

    private static PlayerProgressionId owner() {
        return PlayerProgressionId.of(PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000014")));
    }

    private static AuditMetadata audit() {
        final Instant time = Instant.parse("2026-07-22T00:00:00Z");
        return new AuditMetadata("test", CorrelationId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000014")), time, time);
    }
}
