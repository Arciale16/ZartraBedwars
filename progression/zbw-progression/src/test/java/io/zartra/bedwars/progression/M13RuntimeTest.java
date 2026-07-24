package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.event.EventMetadata.ThreadContext;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.achievement.AchievementDefinition;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.achievement.AchievementProgress;
import io.zartra.bedwars.progression.achievement.AchievementRuntime;
import io.zartra.bedwars.progression.challenge.ChallengeDefinition;
import io.zartra.bedwars.progression.challenge.ChallengeId;
import io.zartra.bedwars.progression.challenge.ChallengeProgress;
import io.zartra.bedwars.progression.challenge.ChallengeRuntime;
import io.zartra.bedwars.progression.integration.M13EventAdapter;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.objective.ObjectiveDefinition;
import io.zartra.bedwars.progression.objective.ObjectiveEvent;
import io.zartra.bedwars.progression.objective.ObjectiveEventType;
import io.zartra.bedwars.progression.objective.ObjectiveExecutionEngine;
import io.zartra.bedwars.progression.objective.ObjectiveFilter;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import io.zartra.bedwars.progression.objective.ObjectiveRuntimeState;
import io.zartra.bedwars.progression.pass.BattlePassDefinition;
import io.zartra.bedwars.progression.pass.BattlePassRuntime;
import io.zartra.bedwars.progression.pass.SeasonId;
import io.zartra.bedwars.progression.pass.SeasonProgress;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.progression.quest.QuestAssignment;
import io.zartra.bedwars.progression.quest.QuestDefinition;
import io.zartra.bedwars.progression.quest.QuestId;
import io.zartra.bedwars.progression.quest.QuestRuntime;
import io.zartra.bedwars.progression.runtime.M13ProjectionEngine;
import io.zartra.bedwars.progression.runtime.M13StateRepository;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Deterministic unit and integration-contract evidence for M13 Phase 2. */
class M13RuntimeTest {
    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final PlayerProgressionId PLAYER = PlayerProgressionId.of(PlayerId.of(new UUID(0, 13)));
    private static final ObjectiveId OBJECTIVE = ObjectiveId.of("zbw", "objective/wins");
    private static final ObjectiveEventType WIN = ObjectiveEventType.of("zbw", "match/win");
    private static final RewardId REWARD = RewardId.of("zbw", "reward/coins");

    @Test void objectiveEngineFiltersAccumulatesCompletesAndDeduplicates() {
        final ObjectiveExecutionEngine engine = new ObjectiveExecutionEngine();
        final ObjectiveDefinition definition = objective(10);
        final ObjectiveRuntimeState initial = ObjectiveRuntimeState.active(definition, PLAYER,
                Optional.of(NOW.plusSeconds(60)), audit());
        final ObjectiveExecutionEngine.Evaluation ignored = engine.evaluate(definition, initial,
                event(4, key("ignored"), Collections.singletonMap("match_validity", "private")),
                false, NOW);
        assertFalse(ignored.changed());
        final ObjectiveExecutionEngine.Evaluation first = engine.evaluate(definition, initial,
                event(4, key("one"), filters()), false, NOW);
        assertEquals(4, first.state().value());
        assertTrue(first.changed());
        final ObjectiveExecutionEngine.Evaluation completed = engine.evaluate(definition,
                first.state(), event(7, key("two"), filters()), false, NOW.plusSeconds(1));
        assertTrue(completed.completed());
        assertEquals(ObjectiveRuntimeState.Status.COMPLETED, completed.state().status());
        assertEquals(10, completed.state().value());
        assertEquals(1, completed.state().completionCount());
        final ObjectiveExecutionEngine.Evaluation duplicate = engine.evaluate(definition,
                completed.state(), event(1, key("two"), filters()), false, NOW.plusSeconds(2));
        assertTrue(duplicate.duplicate());
        assertSame(completed.state(), duplicate.state());
        assertFalse(engine.evaluate(definition, completed.state(),
                event(1, key("three"), filters()), false, NOW.plusSeconds(2)).changed());
    }

    @Test void objectiveEngineSupportsRepeatExpirationOverflowAndValidation() {
        final ObjectiveExecutionEngine engine = new ObjectiveExecutionEngine();
        final ObjectiveDefinition definition = objective(3);
        final ObjectiveRuntimeState initial = ObjectiveRuntimeState.active(definition, PLAYER,
                Optional.empty(), audit());
        final ObjectiveExecutionEngine.Evaluation repeated = engine.evaluate(definition, initial,
                event(8, key("repeat"), filters()), true, NOW);
        assertEquals(2, repeated.state().value());
        assertEquals(2, repeated.state().completionCount());
        final ObjectiveRuntimeState expiring = ObjectiveRuntimeState.active(definition, PLAYER,
                Optional.of(NOW), audit());
        assertTrue(engine.evaluate(definition, expiring, event(1, key("expire"), filters()),
                false, NOW).expired());
        final ObjectiveRuntimeState huge = new ObjectiveRuntimeState(OBJECTIVE, PLAYER, 1,
                Long.MAX_VALUE - 1, 0, ObjectiveRuntimeState.Status.ACTIVE, 1, Optional.empty(),
                Optional.empty(), audit());
        assertEquals(3, engine.evaluate(definition, huge,
                event(10, key("overflow"), filters()), false, NOW).state().value());
        final ObjectiveDefinition incompatible = new ObjectiveDefinition(OBJECTIVE, 2, WIN, 4,
                ObjectiveDefinition.Composition.SINGLE, ObjectiveDefinition.Scope.PLAYER,
                Collections.singletonList(new ObjectiveFilter(
                        ObjectiveFilter.Dimension.MATCH_VALIDITY, "public")),
                Collections.<ObjectiveId>emptyList());
        assertThrows(IllegalArgumentException.class, () -> engine.evaluate(incompatible, initial,
                event(1, key("bad"), filters()), false, NOW));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveEvent(
                ObjectiveEvent.Source.M08_GAME, WIN, PLAYER, 0, filters(), key("bad"), audit()));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveRuntimeState(OBJECTIVE,
                PLAYER, 0, 0, 0, ObjectiveRuntimeState.Status.ACTIVE, 0,
                Optional.empty(), Optional.empty(), audit()));
    }

    @Test void questRuntimeAssignsActivatesCompletesResetsAndChains() {
        final QuestRuntime runtime = new QuestRuntime();
        final QuestDefinition definition = quest(true);
        final QuestAssignment locked = runtime.assign(definition, PLAYER, NOW, NOW.plusSeconds(100), false);
        assertEquals(QuestAssignment.Status.LOCKED, locked.status());
        final QuestAssignment active = runtime.activate(locked);
        assertEquals(QuestAssignment.Status.ACTIVE, active.status());
        final ObjectiveExecutionEngine.Evaluation completion = new ObjectiveExecutionEngine().evaluate(
                objective(1), ObjectiveRuntimeState.active(objective(1), PLAYER,
                        Optional.empty(), audit()), event(1, key("quest"), filters()), false, NOW);
        final QuestRuntime.Outcome outcome = runtime.apply(definition, active, completion, NOW);
        assertEquals(QuestAssignment.Status.COMPLETED, outcome.assignment().status());
        assertEquals(REWARD, outcome.rewards().get(0).rewardId());
        assertThrows(IllegalStateException.class, () -> runtime.reset(definition,
                outcome.assignment(), NOW.plusSeconds(1), NOW.plusSeconds(100)));
        final QuestAssignment reset = runtime.reset(definition, outcome.assignment(),
                NOW.plusSeconds(11), NOW.plusSeconds(100));
        assertEquals(QuestAssignment.Status.ACTIVE, reset.status());
        final QuestId next = QuestId.of("zbw", "quest/next");
        assertEquals(next, runtime.next(outcome.assignment(), Arrays.asList(definition.id(), next)));
        assertNull(runtime.next(outcome.assignment(), Collections.singletonList(definition.id())));
        assertEquals(QuestAssignment.Status.EXPIRED, runtime.apply(definition, active, completion,
                active.expiresAt()).assignment().status());
    }

    @Test void achievementChallengeAndBattlePassRuntimesRemainIdempotent() {
        final ObjectiveDefinition objective = objective(10);
        final ObjectiveExecutionEngine.Evaluation completion = new ObjectiveExecutionEngine().evaluate(
                objective, ObjectiveRuntimeState.active(objective, PLAYER, Optional.empty(), audit()),
                event(10, key("shared"), filters()), false, NOW);
        final AchievementDefinition achievement = new AchievementDefinition(
                AchievementId.of("zbw", "achievement/winner"), 1, "game", OBJECTIVE,
                Arrays.asList(new AchievementDefinition.Tier(1, 5, 5, Collections.singletonList(REWARD)),
                        new AchievementDefinition.Tier(2, 10, 10, Collections.singletonList(REWARD))),
                true, false);
        final AchievementProgress start = new AchievementProgress(achievement.id(), PLAYER, 1,
                0, 0, false, 0, Optional.empty(), NOW);
        final AchievementRuntime.Outcome achieved = new AchievementRuntime().apply(achievement,
                start, completion, event(10, key("shared"), filters()), NOW);
        assertEquals(2, achieved.progress().tier());
        assertTrue(achieved.progress().discovered());
        assertEquals(2, achieved.rewards().size());
        assertTrue(new AchievementRuntime().apply(achievement, achieved.progress(), completion,
                event(10, key("shared"), filters()), NOW).rewards().isEmpty());

        final ChallengeDefinition challenge = new ChallengeDefinition(
                ChallengeId.of("zbw", "challenge/win"), 1, ChallengeDefinition.Variant.DAILY,
                OBJECTIVE, Duration.ofMinutes(5), Collections.singletonList(REWARD));
        final ChallengeRuntime challengeRuntime = new ChallengeRuntime();
        final ChallengeProgress challengeStart = challengeRuntime.activate(challenge, PLAYER, NOW);
        final ChallengeRuntime.Outcome challengeDone = challengeRuntime.apply(challenge,
                challengeStart, completion, event(10, key("challenge"), filters()), NOW);
        assertEquals(ChallengeProgress.Status.COMPLETED, challengeDone.progress().status());
        assertEquals(1, challengeDone.rewards().size());
        assertEquals(ChallengeProgress.Status.EXPIRED, challengeRuntime.apply(challenge,
                challengeStart, completion, event(10, key("late"), filters()),
                challengeStart.expiresAt()).progress().status());

        final BattlePassDefinition pass = pass();
        final BattlePassRuntime passRuntime = new BattlePassRuntime();
        final SeasonProgress season = passRuntime.start(pass, PLAYER, NOW);
        final SeasonProgress tierTwo = passRuntime.addExperience(pass, season, 15,
                key("pass"), NOW);
        assertEquals(2, tierTwo.tier());
        assertSame(tierTwo, passRuntime.addExperience(pass, tierTwo, 2, key("pass"), NOW));
        final SeasonProgress claimed = passRuntime.claimFreeTier(pass, tierTwo, 1,
                key("claim"), NOW);
        assertTrue(claimed.claimedFreeTiers().contains(1));
        assertSame(claimed, passRuntime.claimFreeTier(pass, claimed, 1, key("claim"), NOW));
    }

    @Test void existingEventPipelinesAdaptThroughExplicitAllowlist() {
        final DefinitionId kind = DefinitionId.of("zbw", "match/completed");
        final Map<DefinitionId, M13EventAdapter.Rule> rules = new LinkedHashMap<DefinitionId, M13EventAdapter.Rule>();
        rules.put(kind, new M13EventAdapter.Rule(ObjectiveEvent.Source.M08_GAME, WIN, 2, "m08"));
        final M13EventAdapter adapter = new M13EventAdapter(rules);
        final ProgressionEventInput input = new ProgressionEventInput(metadata(), PLAYER, kind,
                key("source"), new byte[] {1});
        final ObjectiveEvent event = adapter.adapt(input, filters()).get();
        assertEquals(ObjectiveEvent.Source.M08_GAME, event.source());
        assertEquals(2, event.amount());
        assertFalse(adapter.adapt(new ProgressionEventInput(metadata(), PLAYER,
                DefinitionId.of("zbw", "ignored"), key("ignored"), new byte[0]), filters()).isPresent());
        assertThrows(IllegalArgumentException.class, () -> new M13EventAdapter.Rule(
                ObjectiveEvent.Source.M12_PROGRESSION, WIN, 0, "m12"));
    }

    @Test void projectionClaimsPersistsAndCreatesStableRewardIntent() {
        final MemoryRepository repository = new MemoryRepository();
        final M13ProjectionEngine projection = new M13ProjectionEngine(repository,
                new ObjectiveExecutionEngine(), ignored -> Collections.singletonList(REWARD));
        final Result<M13ProjectionEngine.Projection> first = projection.project(new MemoryUnit(),
                objective(1), event(1, key("projection"), filters()), false,
                Optional.empty(), NOW);
        assertTrue(first.isSuccess());
        assertFalse(first.requireValue().duplicate());
        assertTrue(first.requireValue().evaluation().get().completed());
        assertEquals(1, first.requireValue().rewardIntents().size());
        assertTrue(projection.project(new MemoryUnit(), objective(1),
                event(1, key("projection"), filters()), false, Optional.empty(), NOW)
                .requireValue().duplicate());
    }

    @Test void runtimeRejectsMalformedStateAndPreservesNonQualifyingLifecycles() {
        final Map<String, String> oversized = new LinkedHashMap<String, String>();
        for (int index = 0; index < 33; index++) { oversized.put("key" + index, "value"); }
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveEvent(
                ObjectiveEvent.Source.M08_GAME, WIN, PLAYER, 1, oversized, key("large"), audit()));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveEvent(
                ObjectiveEvent.Source.M08_GAME, WIN, PLAYER, 1,
                Collections.singletonMap("", "value"), key("empty"), audit()));
        assertThrows(IllegalArgumentException.class, () -> new AchievementProgress(
                AchievementId.of("zbw", "achievement/invalid"), PLAYER, 1, -1, 0,
                false, 0, Optional.empty(), NOW));
        assertThrows(IllegalArgumentException.class, () -> new ChallengeProgress(
                ChallengeId.of("zbw", "challenge/invalid"), PLAYER, 1,
                ChallengeProgress.Status.ACTIVE, NOW, NOW, 0, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new SeasonProgress(
                SeasonId.of("zbw", "season/invalid"), PLAYER, 1, 0, 0,
                Collections.singleton(1), 0, Optional.empty(), NOW));
        assertThrows(IllegalArgumentException.class, () -> new SeasonProgress(
                SeasonId.of("zbw", "season/null-claim"), PLAYER, 1, 0, 0,
                Collections.singleton((Integer) null), 0, Optional.empty(), NOW));

        final QuestRuntime quests = new QuestRuntime();
        final QuestDefinition quest = quest(false);
        final QuestAssignment active = quests.assign(quest, PLAYER, NOW, NOW.plusSeconds(60), true);
        assertSame(active, quests.activate(active));
        final ObjectiveDefinition objective = objective(10);
        final ObjectiveExecutionEngine.Evaluation incomplete = new ObjectiveExecutionEngine().evaluate(
                objective, ObjectiveRuntimeState.active(objective, PLAYER, Optional.empty(), audit()),
                event(1, key("incomplete"), filters()), false, NOW);
        assertSame(active, quests.apply(quest, active, incomplete, NOW).assignment());
        assertThrows(IllegalStateException.class, () -> quests.reset(quest, active,
                NOW.plusSeconds(20), NOW.plusSeconds(100)));
        assertThrows(IllegalStateException.class, () -> quests.next(active,
                Collections.singletonList(quest.id())));

        final BattlePassRuntime pass = new BattlePassRuntime();
        final BattlePassDefinition definition = pass();
        assertThrows(IllegalStateException.class, () -> pass.start(definition, PLAYER,
                definition.endsAt()));
        final SeasonProgress season = pass.start(definition, PLAYER, NOW);
        assertThrows(IllegalArgumentException.class, () -> pass.addExperience(definition,
                season, 0, key("zero"), NOW));
        assertThrows(IllegalArgumentException.class, () -> pass.claimFreeTier(definition,
                season, 1, key("locked"), NOW));
        final SeasonProgress unlocked = pass.addExperience(definition, season, 1,
                key("unlock"), NOW);
        assertThrows(IllegalStateException.class, () -> pass.claimFreeTier(definition,
                unlocked, 1, key("late-claim"), definition.graceEndsAt()));

        final ChallengeDefinition challenge = new ChallengeDefinition(
                ChallengeId.of("zbw", "challenge/incomplete"), 1,
                ChallengeDefinition.Variant.WEEKLY, OBJECTIVE, Duration.ofMinutes(1),
                Collections.singletonList(REWARD));
        final ChallengeRuntime challengeRuntime = new ChallengeRuntime();
        final ChallengeProgress challengeProgress = challengeRuntime.activate(challenge, PLAYER, NOW);
        assertEquals(ChallengeProgress.Status.ACTIVE, challengeRuntime.apply(challenge,
                challengeProgress, incomplete, event(1, key("challenge-incomplete"), filters()), NOW)
                .progress().status());

        final Map<DefinitionId, M13EventAdapter.Rule> nullKey =
                new LinkedHashMap<DefinitionId, M13EventAdapter.Rule>();
        nullKey.put(null, new M13EventAdapter.Rule(ObjectiveEvent.Source.M08_GAME, WIN, 1, "m08"));
        assertThrows(IllegalArgumentException.class, () -> new M13EventAdapter(nullKey));
        final Map<DefinitionId, M13EventAdapter.Rule> nullValue =
                new LinkedHashMap<DefinitionId, M13EventAdapter.Rule>();
        nullValue.put(DefinitionId.of("zbw", "event"), null);
        assertThrows(IllegalArgumentException.class, () -> new M13EventAdapter(nullValue));

        final AchievementDefinition achievement = new AchievementDefinition(
                AchievementId.of("zbw", "achievement/other"), 1, "game", OBJECTIVE,
                Collections.singletonList(new AchievementDefinition.Tier(1, 10, 1,
                        Collections.<RewardId>emptyList())), true, false);
        final AchievementProgress wrong = new AchievementProgress(
                AchievementId.of("zbw", "achievement/wrong"), PLAYER, 1, 0, 0,
                false, 0, Optional.empty(), NOW);
        assertThrows(IllegalArgumentException.class, () -> new AchievementRuntime().apply(
                achievement, wrong, incomplete, event(1, key("wrong-achievement"), filters()), NOW));
    }

    private static ObjectiveDefinition objective(final long target) {
        return new ObjectiveDefinition(OBJECTIVE, 1, WIN, target,
                ObjectiveDefinition.Composition.SINGLE, ObjectiveDefinition.Scope.PLAYER,
                Collections.singletonList(new ObjectiveFilter(
                        ObjectiveFilter.Dimension.MATCH_VALIDITY, "public")),
                Collections.<ObjectiveId>emptyList());
    }
    private static QuestDefinition quest(final boolean repeatable) {
        return new QuestDefinition(QuestId.of("zbw", "quest/win"), 1,
                QuestDefinition.Schedule.DAILY, OBJECTIVE, Collections.singletonList(REWARD),
                QuestDefinition.ClaimPolicy.MANUAL, Optional.of(Duration.ofSeconds(10)),
                repeatable, false);
    }
    private static BattlePassDefinition pass() {
        return new BattlePassDefinition(SeasonId.of("zbw", "season/one"), 1,
                NOW.minusSeconds(10), NOW.plusSeconds(100), NOW.plusSeconds(200), Arrays.asList(
                new BattlePassDefinition.Tier(1, 0, Collections.singletonList(REWARD), Collections.<RewardId>emptyList()),
                new BattlePassDefinition.Tier(2, 10, Collections.singletonList(REWARD), Collections.<RewardId>emptyList())));
    }
    private static ObjectiveEvent event(final long amount, final IdempotencyKey key,
                                        final Map<String, String> attributes) {
        return new ObjectiveEvent(ObjectiveEvent.Source.M08_GAME, WIN, PLAYER, amount,
                attributes, key, audit());
    }
    private static Map<String, String> filters() { return Collections.singletonMap("match_validity", "public"); }
    private static IdempotencyKey key(final String path) { return IdempotencyKey.of("test", path); }
    private static AuditMetadata audit() { return new AuditMetadata("test", CorrelationId.of(new UUID(0, 14)), NOW, NOW); }
    private static EventMetadata metadata() { return EventMetadata.of(EventId.of(new UUID(0, 15)),
            EventTypeId.of("test", "source"), CorrelationId.of(new UUID(0, 16)), NOW, 1, 1,
            ThreadContext.APPLICATION_WORKER); }

    private static final class MemoryRepository implements M13StateRepository {
        private final java.util.Set<IdempotencyKey> claims = new java.util.HashSet<IdempotencyKey>();
        private ObjectiveRuntimeState objective;
        @Override public Result<Boolean> claimEvent(final UnitOfWork unit, final IdempotencyKey key,
                                                    final Instant at) { return Result.success(claims.add(key)); }
        @Override public Result<Optional<ObjectiveRuntimeState>> findObjective(final UnitOfWork unit,
                final PlayerProgressionId player, final ObjectiveId id) { return Result.success(Optional.ofNullable(objective)); }
        @Override public Result<ObjectiveRuntimeState> saveObjective(final UnitOfWork unit,
                final ObjectiveRuntimeState state, final long expected) {
            objective = state;
            return Result.success(state);
        }
        @Override public Result<Optional<QuestAssignment>> findQuest(final UnitOfWork u, final PlayerProgressionId p, final QuestId i) { return Result.success(Optional.empty()); }
        @Override public Result<QuestAssignment> saveQuest(final UnitOfWork u, final QuestAssignment s, final long e) { return Result.success(s); }
        @Override public Result<Optional<AchievementProgress>> findAchievement(final UnitOfWork u, final PlayerProgressionId p, final AchievementId i) { return Result.success(Optional.empty()); }
        @Override public Result<AchievementProgress> saveAchievement(final UnitOfWork u, final AchievementProgress s, final long e) { return Result.success(s); }
        @Override public Result<Optional<ChallengeProgress>> findChallenge(final UnitOfWork u, final PlayerProgressionId p, final ChallengeId i) { return Result.success(Optional.empty()); }
        @Override public Result<ChallengeProgress> saveChallenge(final UnitOfWork u, final ChallengeProgress s, final long e) { return Result.success(s); }
        @Override public Result<Optional<SeasonProgress>> findSeason(final UnitOfWork u, final PlayerProgressionId p, final SeasonId i) { return Result.success(Optional.empty()); }
        @Override public Result<SeasonProgress> saveSeason(final UnitOfWork u, final SeasonProgress s, final long e) { return Result.success(s); }
    }

    private static final class MemoryUnit implements UnitOfWork {
        @Override public State state() { return State.ACTIVE; }
        @Override public Result<State> commit() { return Result.success(State.COMMITTED); }
        @Override public Result<State> rollback() { return Result.success(State.ROLLED_BACK); }
        @Override public void close() { }
    }
}
