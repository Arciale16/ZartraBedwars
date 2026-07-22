package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.progression.catalog.M14Catalog;
import io.zartra.bedwars.progression.catalog.M14Configuration;
import io.zartra.bedwars.progression.calendar.CalendarCampaign;
import io.zartra.bedwars.progression.calendar.CalendarCampaignId;
import io.zartra.bedwars.progression.cosmetic.CosmeticCategoryId;
import io.zartra.bedwars.progression.cosmetic.CosmeticDefinition;
import io.zartra.bedwars.progression.cosmetic.CosmeticId;
import io.zartra.bedwars.progression.cosmetic.CosmeticRarity;
import io.zartra.bedwars.progression.cosmetic.CosmeticRarityId;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.quest.QuestId;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** M14 completion gate: approved 300-definition cosmetic catalogue. */
final class M14CatalogueCompletionTest {
    private static final String RESOURCE = "m14-cosmetic-catalogue.tsv";
    private static final int REQUIRED_DEFINITIONS = 300;
    private static final int REQUIRED_CATEGORIES = 10;
    private static final int REQUIRED_RARITIES = 6;
    private static final int REQUIRED_CAMPAIGNS = 6;

    @Test
    void approvedCatalogueContains300TypedDefinitionsWithDeterministicOrderingAndProvenance() throws Exception {
        final List<String> lines = rawCatalogueLines();
        final CataloguePayload payload = parseCatalogue(lines);
        assertEquals(REQUIRED_DEFINITIONS, payload.cosmetics().size());
        assertEquals(REQUIRED_DEFINITIONS, payload.expectedLineCount());
        assertEquals(REQUIRED_CATEGORIES, payload.categories().size());
        assertEquals(REQUIRED_RARITIES, payload.rarityDefinitions().size());
        assertEquals(REQUIRED_CAMPAIGNS, countUniqueCampaigns(payload.campaignIds()));
        assertEquals(payload.ids(), sortedIds(payload.cosmetics()), "catalogue rows must be deterministic and sorted by definition");
        assertEquals(payload.sortedCosmetics(), payload.cosmetics(), "catalogue rows must preserve sorted source order");
        assertEquals(REQUIRED_CAMPAIGNS, countUniqueCampaigns(payload.campaignIds()),
                "campaign identifiers must cover all configured windows");

        final M14Catalog catalog = new M14Catalog(payload.categories(), payload.rarityDefinitions(),
                payload.cosmetics(), payload.campaigns());
        catalog.validateProduction(new M14Configuration(1, REQUIRED_DEFINITIONS, 32, 128, false));

        assertEquals(REQUIRED_DEFINITIONS, catalog.cosmetics().size());
        assertTrue(catalog.cosmetics().stream().allMatch(value -> value.version() >= 1));
        assertEquals(6, catalog.rarities().size());
    }

    @Test
    void rejectsMalformedProvenanceOrInvalidTypedIdsInCatalogueRows() throws Exception {
        final List<String> lines = rawCatalogueLines();
        final CataloguePayload payload = parseCatalogue(lines);
        assertEquals(REQUIRED_DEFINITIONS, payload.cosmetics().size());
        assertEquals(0, payload.ids().stream()
                .filter(value -> value.equals("zbw:cosmetic_999")).count(),
                "invalid id must not exist");
        assertEquals(6, payload.rarityDefinitions().size());

        final Set<String> expected = new LinkedHashSet<String>(Arrays.asList(
                "common", "uncommon", "rare", "epic", "legendary", "mythic"));
        final Set<String> actual = new LinkedHashSet<String>();
        for (final CosmeticRarity rarity : payload.rarityDefinitions()) {
            actual.add(rarity.id().path());
        }
        assertEquals(expected, actual, "rarity IDs must match expected campaign scope");

        for (final Map.Entry<String, Map<String, String>> row : payload.provenance().entrySet()) {
            final Map<String, String> provenance = row.getValue();
            assertFalse(provenance.get("source").trim().isEmpty(), "source required");
            assertEquals("approved", provenance.get("status"), "status must be approved");
            assertFalse(provenance.get("author").trim().isEmpty(), "author required");
            assertTrue(provenance.get("hash").matches("^h[0-9a-f]{64}$"), "hash must be hex sha-256 with approved prefix");
        }

        assertThrows(IllegalArgumentException.class, () -> parseCatalogue(Arrays.asList(
                "zbw:cosmetic_bad\t1\tlobby\tcommon\tcosmetic.invalid\tKILL\t\t\t\tevent\tzbw-native\tapproved\towner:team\tbad-hash")));
        assertThrows(IllegalArgumentException.class, () -> parseCatalogue(Arrays.asList(
                "zbw:cosmetic_bad\t1\tlobby\tcommon\tcosmetic.invalid\tKILL\tnot-an-id\t\t\tseason\torigin:zbw-native\tapproved\towner:team\th1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")));
        assertThrows(IllegalArgumentException.class, () -> parseCatalogue(Arrays.asList(
                "zbw:cosmetic_bad\t1\tlobby\tcommon\tcosmetic.invalid\tNON_EXISTENT_TRIGGER\t\t\tseason\tevent\torigin:zbw-native\tapproved\towner:team\th1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")));
    }

    private static CataloguePayload parseCatalogue(final List<String> lines) {
        final List<CosmeticDefinition> cosmetics = new ArrayList<CosmeticDefinition>();
        final List<CalendarCampaign> campaigns = new ArrayList<CalendarCampaign>();
        final Map<String, CosmeticCategoryId> categories = new LinkedHashMap<String, CosmeticCategoryId>();
        final Map<String, CosmeticRarity> rarities = new LinkedHashMap<String, CosmeticRarity>();
        final Map<String, Map<String, String>> provenance = new LinkedHashMap<String, Map<String, String>>();

        for (final String line : lines) {
            final String[] values = line.split("\\t", -1);
            if (values.length != 14) {
                throw new IllegalArgumentException("cosmetic row must contain 14 columns");
            }
            final String rawId = values[0];
            final int version = Integer.parseInt(values[1]);
            final String category = values[2];
            final String rarity = values[3];
            final String display = values[4];
            final String trigger = values[5];
            final String entitlement = values[6];
            final String quest = values[7];
            final String achievement = values[8];
            final String campaign = values[9];
            final String source = values[10];
            final String status = values[11];
            final String author = values[12];
            final String hash = values[13];

            final CosmeticId cosmeticId = CosmeticId.parse(rawId);
            final CosmeticCategoryId categoryId = Cosmetics.category(category);
            final CosmeticRarityId rarityId = Cosmetics.rarity(rarity);
            final CosmeticRarity rarityDefinition = rarities.get(rarity);
            if (rarityDefinition == null) {
                final String weight = "rarity." + rarity;
                rarities.put(rarity, new CosmeticRarity(rarityId, 0, weight));
            }
            categories.put(category, categoryId);
            final CosmeticDefinition definition = new CosmeticDefinition(cosmeticId, version, categoryId, rarityId,
                    display, Arrays.asList(CosmeticDefinition.Trigger.valueOf(trigger)),
                    optionalEntitlement(entitlement), optionalQuest(quest),
                    optionalAchievement(achievement), true);
            cosmetics.add(definition);

            campaigns.add(new CalendarCampaign(CalendarCampaignId.of("zbw", campaign),
                    1, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
                    Arrays.<RewardId>asList(RewardId.of("zbw", "reward-" + campaign)),
                    Optional.<String>empty()));

            final Map<String, String> provenanceFields = new LinkedHashMap<String, String>();
            provenanceFields.put("source", stripPrefix(source, "origin:"));
            provenanceFields.put("status", status);
            provenanceFields.put("author", stripPrefix(author, "owner:"));
            provenanceFields.put("hash", hash);
            provenance.put(rawId, provenanceFields);
        }

        final List<CosmeticDefinition> sorted = sortById(cosmetics);
        return new CataloguePayload(lines.size(), cosmetics, campaigns, new ArrayList<CosmeticCategoryId>(categories.values()),
                new ArrayList<CosmeticRarity>(rarities.values()), sorted, provenance);
    }

    private static String stripPrefix(final String value, final String prefix) {
        if (value == null) {
            return "";
        }
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        throw new IllegalArgumentException("missing provenance prefix: " + value);
    }

    private static Optional<EntitlementId> optionalEntitlement(final String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(EntitlementId.parse(value));
    }

    private static Optional<QuestId> optionalQuest(final String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(QuestId.parse(value));
    }

    private static Optional<AchievementId> optionalAchievement(final String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(AchievementId.parse(value));
    }

    private static List<String> sortedIds(final List<CosmeticDefinition> cosmetics) {
        return cosmetics.stream().map(value -> value.id().toString()).sorted().collect(Collectors.toList());
    }

    private static List<CosmeticDefinition> sortById(final List<CosmeticDefinition> values) {
        final List<CosmeticDefinition> copy = new ArrayList<CosmeticDefinition>(values);
        Collections.sort(copy, new Comparator<CosmeticDefinition>() {
            @Override
            public int compare(final CosmeticDefinition first, final CosmeticDefinition second) {
                return first.id().toString().compareTo(second.id().toString());
            }
        });
        return Collections.unmodifiableList(copy);
    }

    private static int countUniqueCampaigns(final List<CalendarCampaignId> campaigns) {
        return new LinkedHashSet<CalendarCampaignId>(campaigns).size();
    }

    private static List<String> rawCatalogueLines() throws Exception {
        final List<String> lines = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE),
                        "catalogue resource missing"),
                StandardCharsets.UTF_8));
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
        return Collections.unmodifiableList(lines);
    }

    private static final class Cosmetics {
        private static final Map<String, CosmeticCategoryId> CATEGORY_CACHE = new LinkedHashMap<String, CosmeticCategoryId>();
        private static final Map<String, CosmeticRarityId> RARITY_CACHE = new LinkedHashMap<String, CosmeticRarityId>();

        static CosmeticCategoryId category(final String value) {
            CosmeticCategoryId cached = CATEGORY_CACHE.get(value);
            if (cached == null) {
                cached = CosmeticCategoryId.of("zbw", value);
                CATEGORY_CACHE.put(value, cached);
            }
            return cached;
        }

        static CosmeticRarityId rarity(final String value) {
            CosmeticRarityId cached = RARITY_CACHE.get(value);
            if (cached == null) {
                cached = CosmeticRarityId.of("zbw", value);
                RARITY_CACHE.put(value, cached);
            }
            return cached;
        }
    }

    private static final class CataloguePayload {
        private final int expectedLineCount;
        private final List<CosmeticDefinition> cosmetics;
        private final List<CalendarCampaign> campaigns;
        private final List<CosmeticCategoryId> categories;
        private final List<CosmeticRarity> rarityDefinitions;
        private final List<CosmeticDefinition> sortedCosmetics;
        private final Map<String, Map<String, String>> provenance;

        private CataloguePayload(final int expectedLineCount, final List<CosmeticDefinition> cosmetics,
                                final List<CalendarCampaign> campaigns, final List<CosmeticCategoryId> categories,
                                final List<CosmeticRarity> rarityDefinitions,
                                final List<CosmeticDefinition> sortedCosmetics,
                                final Map<String, Map<String, String>> provenance) {
            this.expectedLineCount = expectedLineCount;
            this.cosmetics = Collections.unmodifiableList(new ArrayList<CosmeticDefinition>(cosmetics));
            this.campaigns = Collections.unmodifiableList(new ArrayList<CalendarCampaign>(campaigns));
            this.categories = Collections.unmodifiableList(new ArrayList<CosmeticCategoryId>(categories));
            this.rarityDefinitions = Collections.unmodifiableList(new ArrayList<CosmeticRarity>(rarityDefinitions));
            this.sortedCosmetics = Collections.unmodifiableList(new ArrayList<CosmeticDefinition>(sortedCosmetics));
            this.provenance = Collections.unmodifiableMap(new LinkedHashMap<String, Map<String, String>>(provenance));
        }

        List<String> ids() {
            return Collections.unmodifiableList(cosmetics.stream().map(value -> value.id().toString())
                    .collect(Collectors.toList()));
        }

        List<CosmeticDefinition> cosmetics() {
            return cosmetics;
        }

        List<CalendarCampaign> campaigns() {
            return campaigns;
        }

        List<CalendarCampaignId> campaignIds() {
            final List<CalendarCampaignId> ids = new ArrayList<CalendarCampaignId>();
            for (final CalendarCampaign campaign : campaigns) {
                ids.add(campaign.id());
            }
            return ids;
        }

        List<CosmeticCategoryId> categories() {
            return categories;
        }

        List<CosmeticRarity> rarityDefinitions() {
            return rarityDefinitions;
        }

        List<CosmeticDefinition> sortedCosmetics() {
            return sortedCosmetics;
        }

        Map<String, Map<String, String>> provenance() {
            return provenance;
        }

        int expectedLineCount() {
            return expectedLineCount;
        }
    }
}
