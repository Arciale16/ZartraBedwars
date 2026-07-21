package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.progression.application.ProgressionService;
import io.zartra.bedwars.progression.entitlement.UnlockPolicy;
import io.zartra.bedwars.progression.experience.ExperiencePolicy;
import io.zartra.bedwars.progression.level.LevelFormula;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.ExperienceAmount;
import io.zartra.bedwars.progression.model.LevelDefinition;
import io.zartra.bedwars.progression.model.LevelState;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.PrestigeDefinition;
import io.zartra.bedwars.progression.model.PrestigeState;
import io.zartra.bedwars.progression.model.ProgressionAccount;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.prestige.PrestigePolicy;
import io.zartra.bedwars.progression.reward.RewardEngine;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Acceptance tests for M12 Phase 3 deterministic progression policies. */
class ProgressionEngineTest {
    private static final Instant NOW = Instant.parse("2026-07-20T10:00:00Z");
    private static final DefinitionId WIN = DefinitionId.of("zartra", "match/win");

    @Test void experienceAppliesMultiplierBonusCapAndFarmingProtection() {
        final ExperiencePolicy policy = experiencePolicy(true);
        final ExperiencePolicy.Award normal = policy.calculate(WIN, 80, 15_000, 10, 0, NOW);
        assertEquals(100, normal.awarded());
        assertEquals(80, normal.requested());
        assertEquals(3, normal.policyVersion());
        assertEquals(WIN, normal.sourceId());
        assertEquals(NOW, normal.occurredAt());
        assertEquals(50, policy.calculate(WIN, 100, 10_000, 0, 2, NOW).awarded());
        assertThrows(IllegalArgumentException.class, () -> policy.calculate(WIN, 0, 1, 0, 0, NOW));
        assertThrows(IllegalArgumentException.class, () -> policy.calculate(WIN, 1, -1, 0, 0, NOW));
        assertThrows(IllegalArgumentException.class, () -> policy.calculate(DefinitionId.of("x", "missing"), 1, 1, 0, 0, NOW));
        assertThrows(IllegalArgumentException.class, () -> experiencePolicy(false).calculate(WIN, 1, 1, 0, 0, NOW));
        assertThrows(IllegalArgumentException.class, () -> new ExperiencePolicy(0, Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> new ExperiencePolicy.Source(0, 1, 1, 0, Duration.ofSeconds(1), true));
        assertThrows(IllegalArgumentException.class, () -> new ExperiencePolicy.Source(1, 1, 1, 0, Duration.ZERO, true));
        assertEquals(Duration.ofMinutes(5), policy.sources().get(WIN).farmingWindow());
        assertThrows(UnsupportedOperationException.class, () -> policy.sources().clear());
    }

    @Test void levelFormulaPreviewsTransitionsRecalculationAndMigration() {
        final LevelFormula formula = formula(2);
        assertEquals(1, formula.levelFor(ExperienceAmount.of(0)));
        assertEquals(2, formula.levelFor(ExperienceAmount.of(100)));
        assertEquals(3, formula.levelFor(ExperienceAmount.of(250)));
        assertEquals(250, formula.requiredExperience(3).value());
        assertEquals(2, formula.preview(ExperienceAmount.of(120)).level());
        assertEquals(250, formula.preview(ExperienceAmount.of(120)).nextThreshold().value());
        assertEquals(2, formula.preview(ExperienceAmount.of(120)).version());
        assertEquals(120, formula.preview(ExperienceAmount.of(120)).lifetimeExperience().value());
        assertEquals(300, formula.preview(ExperienceAmount.of(300)).nextThreshold().value());
        assertThrows(IllegalArgumentException.class, () -> formula.requiredExperience(0));
        assertThrows(IllegalArgumentException.class, () -> new LevelFormula(1,
                Arrays.asList(new LevelDefinition(2, ExperienceAmount.of(0)))));
        assertThrows(IllegalArgumentException.class, () -> new LevelFormula(1,
                Arrays.asList(new LevelDefinition(1, ExperienceAmount.of(0)),
                        new LevelDefinition(2, ExperienceAmount.of(0)))));
    }

    @Test void prestigeCreatesAtomicIntentAndRejectsInvalidOrDuplicateTransition() {
        final Map<Integer, PrestigePolicy.Tier> tiers = new LinkedHashMap<Integer, PrestigePolicy.Tier>();
        tiers.put(1, new PrestigePolicy.Tier(new PrestigeDefinition(1, 3, "Bronze"), RewardId.of("zartra", "prestige/1")));
        final PrestigePolicy policy = new PrestigePolicy(4, tiers);
        final PrestigePolicy.Transition transition = policy.transition(new PrestigeState(0, NOW), 3, 1, NOW.plusSeconds(1));
        assertEquals(0, transition.before().prestige());
        assertEquals(1, transition.after().prestige());
        assertEquals(RewardId.of("zartra", "prestige/1"), transition.rewardId());
        assertEquals(4, transition.version());
        assertThrows(IllegalStateException.class, () -> policy.transition(transition.after(), 3, 1, NOW));
        assertThrows(IllegalStateException.class, () -> policy.transition(new PrestigeState(0, NOW), 2, 1, NOW));
        assertThrows(IllegalArgumentException.class, () -> new PrestigePolicy(0, tiers));
        assertThrows(IllegalArgumentException.class, () -> new PrestigePolicy(1, Collections.emptyMap()));
    }

    @Test void unlockPolicyProducesOnlyNewGenericOutputs() {
        final EntitlementId level = EntitlementId.of("zartra", "level/2");
        final EntitlementId prestige = EntitlementId.of("zartra", "prestige/1");
        final UnlockPolicy policy = new UnlockPolicy(5, Arrays.asList(
                new UnlockPolicy.Definition(level, 2, 0),
                new UnlockPolicy.Definition(prestige, 1, 1)));
        assertEquals(Collections.singleton(level), policy.newlyUnlocked(2, 0, Collections.emptySet()));
        final Set<EntitlementId> existing = Collections.singleton(level);
        assertEquals(Collections.singleton(prestige), policy.newlyUnlocked(2, 1, existing));
        assertEquals(5, policy.version());
        assertThrows(IllegalArgumentException.class, () -> policy.newlyUnlocked(0, 0, existing));
        assertThrows(IllegalArgumentException.class, () -> new UnlockPolicy.Definition(level, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new UnlockPolicy(0, Collections.emptyList()));
    }

    @Test void rewardEngineHandlesDeliveryDuplicateOfflineRetryFailureExpirationAndCompensation() {
        final MemoryRewardStore store = new MemoryRewardStore();
        final RecordingDelivery delivery = new RecordingDelivery();
        final RewardEngine engine = new RewardEngine(store, delivery, new RewardEngine.RetryPolicy(2));
        final RewardEngine.Plan plan = plan("one", Optional.of(Duration.ofHours(1)));
        assertEquals(RewardEngine.Status.DELIVERED, engine.grant(plan, NOW, true).status());
        assertEquals(1, engine.grant(plan, NOW.plusSeconds(1), true).attempts());
        assertEquals(1, delivery.deliveries);

        final RewardEngine.Plan offline = plan("offline", Optional.empty());
        assertEquals(RewardEngine.Status.PENDING, engine.grant(offline, NOW, false).status());
        assertEquals(RewardEngine.Status.DELIVERED, engine.retry(offline, 0, NOW, true).status());

        final RewardEngine.Plan failing = plan("failure", Optional.empty());
        delivery.fail = true;
        assertEquals("IllegalStateException", engine.grant(failing, NOW, true).failureCode());
        assertEquals(1, store.failures.size());
        delivery.fail = false;
        assertEquals(RewardEngine.Status.DELIVERED, engine.retry(failing, 1, NOW, true).status());

        final RewardEngine.Plan compensated = plan("compensate", Optional.empty());
        assertEquals(RewardEngine.Status.COMPENSATED, engine.retry(compensated, 2, NOW, true).status());
        assertEquals(1, delivery.compensations);
        final RewardEngine.Plan expired = plan("expired", Optional.of(Duration.ofSeconds(1)));
        assertEquals(RewardEngine.Status.EXPIRED, engine.grant(expired, NOW.plusSeconds(2), true).status());
        assertThrows(IllegalArgumentException.class, () -> new RewardEngine.RetryPolicy(0));
        assertThrows(IllegalArgumentException.class, () -> new RewardEngine.RetryPolicy(101));
        assertThrows(IllegalArgumentException.class, () -> new RewardEngine.Output(RewardEngine.Output.Kind.CURRENCY, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new RewardEngine.Definition(
                RewardId.of("zartra", "empty"), 1, Collections.emptyList(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new RewardEngine.Definition(
                RewardId.of("zartra", "expired"), 1,
                Collections.singletonList(new RewardEngine.Output(RewardEngine.Output.Kind.CURRENCY, "zartra:coins", 1)),
                Optional.of(Duration.ZERO)));
    }

    @Test void administrationIsCentralizedAuthorizedAndDelegatesAtomicIdempotentMutations() {
        final RecordingMutationPort port = new RecordingMutationPort();
        final AuthorizationService allow = request -> AuthorizationDecision.allow(DefinitionId.of("auth", "allowed"));
        final ProgressionService service = new ProgressionService(allow, port, experiencePolicy(true), formula(2));
        final AuthorizationSubject actor = AuthorizationSubject.of(AuthorizationSubject.Kind.CONSOLE, DefinitionId.of("actor", "console"));
        final IdempotencyKey key = IdempotencyKey.of("test", "admin/1");
        assertFalse(service.grantExperience(actor, player(), WIN, 10, 10_000, 0, 0, key, NOW).duplicate());
        assertEquals(10, port.delta);
        service.removeExperience(actor, player(), 2, key, NOW);
        assertEquals(-2, port.delta);
        service.recalculate(actor, player(), key, NOW);
        assertTrue(port.recalculated);
        final Map<Integer, PrestigePolicy.Tier> tiers = new LinkedHashMap<Integer, PrestigePolicy.Tier>();
        tiers.put(1, new PrestigePolicy.Tier(new PrestigeDefinition(1, 1, "One"), RewardId.of("zartra", "prestige/one")));
        service.prestige(actor, player(), new PrestigePolicy(1, tiers), 1, key, NOW);
        assertTrue(port.prestiged);
        assertFalse(service.inspect(actor, player()).isPresent());
        assertThrows(IllegalArgumentException.class, () -> service.removeExperience(actor, player(), 0, key, NOW));
        final AuthorizationService deny = request -> AuthorizationDecision.deny(DefinitionId.of("auth", "denied"));
        final ProgressionService denied = new ProgressionService(deny, port, experiencePolicy(true), formula(2));
        assertThrows(SecurityException.class, () -> denied.inspect(actor, player()));
    }

    private ExperiencePolicy experiencePolicy(final boolean enabled) {
        final Map<DefinitionId, ExperiencePolicy.Source> sources = new LinkedHashMap<DefinitionId, ExperiencePolicy.Source>();
        sources.put(WIN, new ExperiencePolicy.Source(100, 100, 2, 5_000, Duration.ofMinutes(5), enabled));
        return new ExperiencePolicy(3, sources);
    }

    private LevelFormula formula(final int version) {
        return new LevelFormula(version, Arrays.asList(new LevelDefinition(1, ExperienceAmount.of(0)),
                new LevelDefinition(2, ExperienceAmount.of(100)), new LevelDefinition(3, ExperienceAmount.of(250))));
    }

    private PlayerProgressionId player() { return PlayerProgressionId.of(PlayerId.of(new UUID(1, 2))); }

    private RewardEngine.Plan plan(final String id, final Optional<Duration> lifetime) {
        final RewardEngine.Output output = new RewardEngine.Output(RewardEngine.Output.Kind.ENTITLEMENT, "zartra:unlock/" + id, 1);
        final RewardEngine.Definition definition = new RewardEngine.Definition(RewardId.of("zartra", "reward/" + id), 1,
                Collections.singletonList(output), lifetime);
        return new RewardEngine.Plan(definition, player(), IdempotencyKey.of("test", "reward/" + id), NOW);
    }

    private static final class MemoryRewardStore implements RewardEngine.Store {
        private final Map<IdempotencyKey, RewardEngine.Outcome> outcomes = new HashMap<IdempotencyKey, RewardEngine.Outcome>();
        private final List<RewardEngine.Outcome> failures = new ArrayList<RewardEngine.Outcome>();
        @Override public RewardEngine.Claim claim(final RewardEngine.Plan plan) {
            final RewardEngine.Outcome prior = outcomes.get(plan.idempotencyKey());
            return prior == null ? RewardEngine.Claim.newClaim() : RewardEngine.Claim.duplicate(prior);
        }
        @Override public Optional<RewardEngine.Outcome> outcome(final IdempotencyKey key) { return Optional.ofNullable(outcomes.get(key)); }
        @Override public RewardEngine.Outcome record(final RewardEngine.Plan plan,
                final RewardEngine.Outcome outcome) {
            outcomes.put(plan.idempotencyKey(), outcome);
            return outcome;
        }
        @Override public void recordFailure(final RewardEngine.Plan plan, final RewardEngine.Outcome outcome) { failures.add(outcome); }
    }

    private static final class RecordingDelivery implements RewardEngine.Delivery {
        private int deliveries;
        private int compensations;
        private boolean fail;
        @Override public void deliver(final RewardEngine.Plan plan) {
            if (fail) { throw new IllegalStateException("offline"); }
            deliveries++;
        }
        @Override public void compensate(final RewardEngine.Plan plan) { compensations++; }
    }

    private static final class RecordingMutationPort implements ProgressionService.MutationPort {
        private long delta;
        private boolean recalculated;
        private boolean prestiged;
        @Override public ProgressionService.MutationResult changeExperience(final PlayerProgressionId player, final long value,
                final IdempotencyKey key, final AuthorizationSubject actor, final Instant now, final int formulaVersion) {
            delta = value;
            return new ProgressionService.MutationResult(account(), false);
        }
        @Override public Optional<ProgressionAccount> inspect(final PlayerProgressionId player) { return Optional.empty(); }
        @Override public ProgressionService.MutationResult recalculate(final PlayerProgressionId player, final LevelFormula formula,
                final IdempotencyKey key, final AuthorizationSubject actor, final Instant now) {
            recalculated = true;
            return new ProgressionService.MutationResult(account(), false);
        }
        @Override public ProgressionService.MutationResult prestige(final PlayerProgressionId player,
                final PrestigePolicy policy, final int requestedTier, final IdempotencyKey key,
                final AuthorizationSubject actor, final Instant now) {
            prestiged = true;
            return new ProgressionService.MutationResult(account(), false);
        }
        private ProgressionAccount account() {
            final PlayerProgressionId id = PlayerProgressionId.of(PlayerId.of(new UUID(1, 2)));
            final AuditMetadata audit = new AuditMetadata("test", CorrelationId.of(new UUID(3, 4)), NOW, NOW);
            return new ProgressionAccount(id, ExperienceAmount.zero(),
                    new LevelState(1, ExperienceAmount.zero(), NOW), new PrestigeState(0, NOW),
                    Collections.emptySet(), RecordRevision.initial(), audit);
        }
    }
}
