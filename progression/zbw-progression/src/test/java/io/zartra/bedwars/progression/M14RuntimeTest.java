package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
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
import io.zartra.bedwars.progression.profile.ProfileSettings;
import io.zartra.bedwars.progression.repository.CosmeticStateRepository;
import io.zartra.bedwars.progression.repository.EntitlementRepository;
import io.zartra.bedwars.progression.repository.ProfileSettingsRepository;
import io.zartra.bedwars.progression.runtime.M14CampaignRuntime;
import io.zartra.bedwars.progression.runtime.M14ProfileRuntime;
import io.zartra.bedwars.progression.runtime.M14Runtime;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** M14 runtime branch coverage and deterministic error-path tests. */
class M14RuntimeTest {
    private static final CosmeticCategoryId CATEGORY = CosmeticCategoryId.of("zbw", "kill_effect");
    private static final CosmeticRarityId RARITY = CosmeticRarityId.of("zbw", "common");
    private static final CosmeticRarity RARITY_VALUE = new CosmeticRarity(RARITY, 0, "rarity.common");
    private static final CosmeticId ENABLED_NO_ENTITLEMENT = CosmeticId.of("zbw", "enabled-no-ent");
    private static final CosmeticId ENABLED_WITH_ENTITLEMENT = CosmeticId.of("zbw", "enabled-with-ent");
    private static final CosmeticId DISABLED = CosmeticId.of("zbw", "disabled");
    private static final EntitlementId REQUIRED_ENTITLEMENT = EntitlementId.of("zbw", "unlock/m14");
    private static final PlayerProgressionId PLAYER = PlayerProgressionId.of(
            PlayerId.of(UUID.fromString("00000000-0000-0000-0000-0000000000f1")));
    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final AuditMetadata AUDIT = new AuditMetadata("m14-runtime-test",
            CorrelationId.of(UUID.fromString("00000000-0000-0000-0000-0000000000f2")),
            NOW, NOW);
    private static final Set<EntitlementId> OWNED_ENTITLEMENTS = Collections.singleton(REQUIRED_ENTITLEMENT);
    private static final ApiError TRANSIENT = ApiError.of(DefinitionId.of("zbw", "retry"), "transient.failure",
            ApiError.RetryDisposition.RETRYABLE);

    @Test
    void validatesSelectionBranchStatesAndDefinitionLookup() {
        final M14Runtime emergency = new M14Runtime(catalog(), new M14Configuration(1, 1, 1, 1, true),
                loadoutRepositoryNoInteraction(), ownedEntitlementRepository());
        assertEquals(M14Runtime.Status.DISABLED,
                emergency.validateSelection(unitOfWork(), PLAYER, ENABLED_NO_ENTITLEMENT, NOW).status());

        final M14Runtime runtime = new M14Runtime(catalog(), enabledConfiguration(),
                loadoutRepositoryNoInteraction(), ownedEntitlementRepository());
        assertEquals(M14Runtime.Status.NOT_FOUND,
                runtime.validateSelection(unitOfWork(), PLAYER, CosmeticId.of("zbw", "missing"), NOW).status());

        final CosmeticId disabledId = DISABLED;
        assertEquals(M14Runtime.Status.UNAVAILABLE,
                runtime.validateSelection(unitOfWork(), PLAYER, disabledId, NOW).status());
        assertEquals(Optional.empty(), runtime.definition(disabledId));

        assertEquals(M14Runtime.Status.RETRY,
                new M14Runtime(catalog(), enabledConfiguration(), loadoutRepositoryNoInteraction(),
                        failureEntitlementRepository()).validateSelection(unitOfWork(),
                        PLAYER, ENABLED_NO_ENTITLEMENT, NOW).status());
        assertEquals(M14Runtime.Status.LOCKED,
                new M14Runtime(catalog(), enabledConfiguration(), loadoutRepositoryNoInteraction(),
                        emptyEntitlementRepository()).validateSelection(unitOfWork(),
                        PLAYER, ENABLED_WITH_ENTITLEMENT, NOW).status());
        assertEquals(M14Runtime.Status.ACCEPTED,
                runtime.validateSelection(unitOfWork(), PLAYER, ENABLED_NO_ENTITLEMENT, NOW).status());
        assertTrue(runtime.validateSelection(unitOfWork(), PLAYER, ENABLED_NO_ENTITLEMENT, NOW).accepted());
        assertTrue(runtime.definition(ENABLED_NO_ENTITLEMENT).isPresent());
        assertFalse(runtime.definition(CosmeticId.of("zbw", "missing")).isPresent());
    }

    @Test
    void equipsCosmeticsAcrossPersistenceBranches() {
        final MemoryLoadoutRepository loadouts = new MemoryLoadoutRepository(false, false);
        final CosmeticId replacement = ENABLED_NO_ENTITLEMENT;
        final RecordRevision expected = loadouts.startupRevision();
        final M14Runtime runtime = new M14Runtime(catalog(), enabledConfiguration(),
                loadouts, entitlementRepository(OWNED_ENTITLEMENTS));

        final CosmeticLoadout replaced = runtime.equip(unitOfWork(), PLAYER, replacement, expected,
                idempotency(), AUDIT, NOW).requireValue();
        assertEquals(1, replaced.equipped().size());
        assertEquals(replacement, replaced.equipped().get(CATEGORY));
        assertEquals(expected.next(), replaced.revision());

        final CosmeticId unknown = CosmeticId.of("zbw", "missing");
        assertThrows(IllegalArgumentException.class, () -> runtime.equip(unitOfWork(), PLAYER, unknown,
                RecordRevision.initial(), idempotency(), AUDIT, NOW));

        final RecordRevision stale = expected.next();
        assertThrows(IllegalArgumentException.class, () -> runtime.equip(unitOfWork(), PLAYER, replacement,
                stale, idempotency(), AUDIT, NOW));

        final M14Runtime absentLoadout = new M14Runtime(catalog(), enabledConfiguration(),
                loadoutAbsent(), entitlementRepository(OWNED_ENTITLEMENTS));
        assertThrows(IllegalArgumentException.class, () -> absentLoadout.equip(unitOfWork(), PLAYER,
                ENABLED_NO_ENTITLEMENT, expected, idempotency(), AUDIT, NOW));

        final M14Runtime failingLoadout = new M14Runtime(catalog(), enabledConfiguration(),
                loadoutSaveFailure(), entitlementRepository(OWNED_ENTITLEMENTS));
        assertFalse(failingLoadout.equip(unitOfWork(), PLAYER, replacement, expected,
                idempotency(), AUDIT, NOW).isSuccess());
        assertEquals(M14Runtime.Status.RETRY,
                runtime.validateSelection(unitOfWork(), PLAYER, replacement, NOW).status());
    }

    @Test
    void campaignRuntimeEvaluatesLifecycleBranches() {
        final CalendarCampaign active = campaign("active", NOW.minusSeconds(10), NOW.plusSeconds(10));
        final CalendarCampaign future = campaign("future", NOW.plusSeconds(10), NOW.plusSeconds(20));
        final CalendarCampaign expired = campaign("expired", NOW.minusSeconds(20), NOW.minusSeconds(10));
        final M14CampaignRuntime runtime = new M14CampaignRuntime(new M14Catalog(
                Collections.singletonList(CATEGORY), Collections.singletonList(RARITY_VALUE),
                Collections.<CosmeticDefinition>emptyList(),
                Arrays.asList(active, future, expired)));
        assertEquals(M14CampaignRuntime.Status.ACTIVE, runtime.evaluate(active.id(), NOW).status());
        assertEquals(M14CampaignRuntime.Status.NOT_STARTED, runtime.evaluate(future.id(), NOW).status());
        assertEquals(M14CampaignRuntime.Status.EXPIRED, runtime.evaluate(expired.id(), NOW).status());
        assertEquals(M14CampaignRuntime.Status.NOT_FOUND,
                runtime.evaluate(CalendarCampaignId.of("zbw", "missing"), NOW).status());
    }

    @Test
    void profileRuntimeHonoursVisibilityAndReplacement() {
        final MemoryProfileRepository settings = new MemoryProfileRepository();
        final M14ProfileRuntime runtime = new M14ProfileRuntime(settings);
        final ProfileSettings profile = new ProfileSettings(PLAYER, ProfileSettings.Visibility.PRIVATE,
                true, false, RecordRevision.initial(), AUDIT);
        assertFalse(runtime.find(unitOfWork(), PLAYER).requireValue().isPresent());
        assertEquals(profile, runtime.update(unitOfWork(), profile,
                RecordRevision.initial(), idempotency()).requireValue());
        assertEquals(RecordRevision.initial().next(),
                runtime.replacement(profile, ProfileSettings.Visibility.PUBLIC, true, true, AUDIT).revision());
        assertTrue(runtime.mayView(profile, true, false, false, false));
        assertFalse(runtime.mayView(profile, false, false, false, false));
        assertTrue(runtime.mayView(new ProfileSettings(PLAYER, ProfileSettings.Visibility.FRIENDS,
                true, false, RecordRevision.initial(), AUDIT), false, true, false, false));
        assertTrue(runtime.mayView(new ProfileSettings(PLAYER, ProfileSettings.Visibility.PARTY,
                true, false, RecordRevision.initial(), AUDIT), false, false, true, false));
        assertTrue(runtime.mayView(new ProfileSettings(PLAYER, ProfileSettings.Visibility.PUBLIC,
                false, false, RecordRevision.initial(), AUDIT), false, false, false, false));
        assertTrue(runtime.mayView(profile, false, false, false, true));
        assertThrows(IllegalArgumentException.class, () -> runtime.mayView(null, true, true, true, false));
    }

    private static M14Catalog catalog() {
        return new M14Catalog(Collections.singletonList(CATEGORY), Collections.singletonList(RARITY_VALUE),
                Arrays.asList(
                        new CosmeticDefinition(ENABLED_NO_ENTITLEMENT, 1, CATEGORY, RARITY,
                                "cosmetic.enabled.no.ent",
                                Collections.singletonList(CosmeticDefinition.Trigger.KILL),
                                Optional.empty(), Optional.empty(), Optional.empty(), true),
                        new CosmeticDefinition(ENABLED_WITH_ENTITLEMENT, 1, CATEGORY, RARITY,
                                "cosmetic.enabled.with.ent",
                                Collections.singletonList(CosmeticDefinition.Trigger.WIN),
                                Optional.of(REQUIRED_ENTITLEMENT), Optional.empty(), Optional.empty(), true),
                        new CosmeticDefinition(DISABLED, 1, CATEGORY, RARITY,
                                "cosmetic.disabled",
                                Collections.singletonList(CosmeticDefinition.Trigger.LOBBY),
                                Optional.empty(), Optional.empty(), Optional.empty(), false)),
                Collections.<CalendarCampaign>emptyList());
    }

    private static M14Configuration enabledConfiguration() {
        return new M14Configuration(1, 1, 32, 128, false);
    }

    private static UnitOfWork unitOfWork() {
        return new MemoryUnit();
    }

    private static IdempotencyKey idempotency() {
        return IdempotencyKey.of("zbw", "equip/m14");
    }

    private static CalendarCampaign campaign(final String id, final Instant start, final Instant end) {
        return new CalendarCampaign(CalendarCampaignId.of("zbw", id), 1, start, end,
                Collections.singletonList(io.zartra.bedwars.progression.model.RewardId.of("zbw", "reward/" + id)),
                Optional.empty());
    }

    private static CosmeticStateRepository loadoutRepositoryNoInteraction() {
        return new MemoryLoadoutRepository(false, false);
    }

    private static EntitlementRepository entitlementRepository(final Set<EntitlementId> owned) {
        return new EntitlementRepository() {
            @Override
            public Result<io.zartra.bedwars.progression.model.EntitlementGrant> grant(
                    final UnitOfWork unitOfWork, final io.zartra.bedwars.progression.model.EntitlementGrant grant) {
                return Result.success(new io.zartra.bedwars.progression.model.EntitlementGrant(
                        grant.recipient(), grant.entitlementId(), grant.idempotencyKey(), grant.audit()));
            }

            @Override
            public Result<Optional<io.zartra.bedwars.progression.model.EntitlementGrant>> findByIdempotencyKey(
                    final UnitOfWork unitOfWork, final IdempotencyKey key) {
                return Result.success(Optional.empty());
            }

            @Override
            public Result<Set<EntitlementId>> findAll(final UnitOfWork unit, final PlayerProgressionId owner) {
                return Result.success(owned);
            }
        };
    }

    private static EntitlementRepository ownedEntitlementRepository() {
        return new EntitlementRepository() {
            @Override
            public Result<io.zartra.bedwars.progression.model.EntitlementGrant> grant(
                    final UnitOfWork unitOfWork, final io.zartra.bedwars.progression.model.EntitlementGrant grant) {
                return Result.success(new io.zartra.bedwars.progression.model.EntitlementGrant(
                        grant.recipient(), grant.entitlementId(), grant.idempotencyKey(), grant.audit()));
            }

            @Override
            public Result<Optional<io.zartra.bedwars.progression.model.EntitlementGrant>> findByIdempotencyKey(
                    final UnitOfWork unitOfWork, final IdempotencyKey key) {
                return Result.success(Optional.empty());
            }

            @Override
            public Result<Set<EntitlementId>> findAll(final UnitOfWork unit, final PlayerProgressionId owner) {
                return Result.success(OWNED_ENTITLEMENTS);
            }
        };
    }

    private static EntitlementRepository failureEntitlementRepository() {
        return new EntitlementRepository() {
            @Override
            public Result<io.zartra.bedwars.progression.model.EntitlementGrant> grant(
                    final UnitOfWork unitOfWork, final io.zartra.bedwars.progression.model.EntitlementGrant grant) {
                return Result.success(new io.zartra.bedwars.progression.model.EntitlementGrant(
                        grant.recipient(), grant.entitlementId(), grant.idempotencyKey(), grant.audit()));
            }

            @Override
            public Result<Optional<io.zartra.bedwars.progression.model.EntitlementGrant>> findByIdempotencyKey(
                    final UnitOfWork unitOfWork, final IdempotencyKey key) {
                return Result.success(Optional.empty());
            }

            @Override
            public Result<Set<EntitlementId>> findAll(final UnitOfWork unit, final PlayerProgressionId owner) {
                return Result.failure(TRANSIENT);
            }
        };
    }

    private static EntitlementRepository emptyEntitlementRepository() {
        return new EntitlementRepository() {
            @Override
            public Result<io.zartra.bedwars.progression.model.EntitlementGrant> grant(
                    final UnitOfWork unitOfWork, final io.zartra.bedwars.progression.model.EntitlementGrant grant) {
                return Result.success(new io.zartra.bedwars.progression.model.EntitlementGrant(
                        grant.recipient(), grant.entitlementId(), grant.idempotencyKey(), grant.audit()));
            }

            @Override
            public Result<Optional<io.zartra.bedwars.progression.model.EntitlementGrant>> findByIdempotencyKey(
                    final UnitOfWork unitOfWork, final IdempotencyKey key) {
                return Result.success(Optional.empty());
            }

            @Override
            public Result<Set<EntitlementId>> findAll(final UnitOfWork unit, final PlayerProgressionId owner) {
                return Result.success(Collections.<EntitlementId>emptySet());
            }
        };
    }

    private static CosmeticStateRepository loadoutAbsent() {
        return new MemoryLoadoutRepository(true, false);
    }

    private static CosmeticStateRepository loadoutSaveFailure() {
        return new MemoryLoadoutRepository(false, true);
    }

    private static final class MemoryLoadoutRepository implements CosmeticStateRepository {
        private CosmeticLoadout loadout;
        private final boolean absent;
        private final boolean failSave;

        private MemoryLoadoutRepository(final boolean absent, final boolean failSave) {
            this.absent = absent;
            this.failSave = failSave;
            if (!absent) {
                final Map<CosmeticCategoryId, CosmeticId> equipped = new HashMap<CosmeticCategoryId, CosmeticId>();
                equipped.put(CATEGORY, CosmeticId.of("zbw", "initial"));
                loadout = new CosmeticLoadout(PLAYER, equipped, new HashSet<CosmeticId>(),
                        Collections.<String>singletonList("default"), RecordRevision.initial(), AUDIT);
            }
        }

        @Override
        public Result<Optional<CosmeticLoadout>> find(final UnitOfWork unitOfWork, final PlayerProgressionId owner) {
            return Result.success(Optional.ofNullable(loadout));
        }

        @Override
        public Result<CosmeticLoadout> save(final UnitOfWork unitOfWork, final CosmeticLoadout replacement,
                                           final RecordRevision expectedRevision, final IdempotencyKey idempotencyKey) {
            if (failSave) { return Result.failure(TRANSIENT); }
            if (!replacement.owner().equals(PLAYER)) { return Result.failure(TRANSIENT); }
            loadout = replacement;
            return Result.success(replacement);
        }

        private RecordRevision startupRevision() {
            return RecordRevision.initial();
        }
    }

    private static final class MemoryProfileRepository implements ProfileSettingsRepository {
        private ProfileSettings settings;

        @Override
        public Result<Optional<ProfileSettings>> find(final UnitOfWork unitOfWork, final PlayerProgressionId owner) {
            return Result.success(Optional.ofNullable(settings));
        }

        @Override
        public Result<ProfileSettings> save(final UnitOfWork unitOfWork, final ProfileSettings replacement,
                                           final RecordRevision expectedRevision, final IdempotencyKey idempotencyKey) {
            settings = replacement;
            return Result.success(replacement);
        }
    }

    private static final class MemoryUnit implements UnitOfWork {
        @Override
        public State state() {
            return State.ACTIVE;
        }

        @Override
        public Result<State> commit() {
            return Result.success(State.COMMITTED);
        }

        @Override
        public Result<State> rollback() {
            return Result.success(State.ROLLED_BACK);
        }

        @Override
        public void close() {
        }
    }
}
