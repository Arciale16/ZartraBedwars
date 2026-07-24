package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.achievement.AchievementDefinition;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.catalog.M13Catalog;
import io.zartra.bedwars.progression.challenge.ChallengeDefinition;
import io.zartra.bedwars.progression.challenge.ChallengeId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.objective.ObjectiveDefinition;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import io.zartra.bedwars.progression.objective.ObjectiveEventType;
import io.zartra.bedwars.progression.objective.ObjectiveFilter;
import io.zartra.bedwars.progression.objective.ObjectiveProgress;
import io.zartra.bedwars.progression.pass.BattlePassDefinition;
import io.zartra.bedwars.progression.pass.SeasonId;
import io.zartra.bedwars.progression.quest.QuestAssignment;
import io.zartra.bedwars.progression.quest.QuestDefinition;
import io.zartra.bedwars.progression.quest.QuestId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies the M13 Phase 1 immutable definition and catalogue boundaries. */
class M13FoundationTest {
    private static final Instant NOW = Instant.parse("2026-07-21T12:00:00Z");
    private static final ObjectiveId WINS = ObjectiveId.of("zbw", "objective/wins");
    private static final ObjectiveId BEDS = ObjectiveId.of("zbw", "objective/beds");
    private static final RewardId REWARD = RewardId.of("zbw", "reward/coins");

    @Test void typedIdentitiesParseAndCompare() {
        assertEquals(WINS, ObjectiveId.parse("zbw:objective/wins"));
        assertEquals(QuestId.of("zbw", "quest/win"), QuestId.parse("zbw:quest/win"));
        assertEquals(AchievementId.of("zbw", "achievement/win"),
                AchievementId.parse("zbw:achievement/win"));
        assertEquals(SeasonId.of("zbw", "season/one"), SeasonId.parse("zbw:season/one"));
        assertEquals(ChallengeId.of("zbw", "challenge/one"), ChallengeId.parse("zbw:challenge/one"));
        assertEquals(ObjectiveEventType.of("zbw", "game/complete"),
                ObjectiveEventType.parse("zbw:game/complete"));
        assertThrows(RuntimeException.class, () -> ObjectiveId.parse("invalid"));
        assertThrows(RuntimeException.class, () -> QuestId.parse("invalid"));
        assertThrows(RuntimeException.class, () -> AchievementId.parse("invalid"));
        assertThrows(RuntimeException.class, () -> SeasonId.parse("invalid"));
        assertThrows(RuntimeException.class, () -> ChallengeId.parse("invalid"));
        assertThrows(RuntimeException.class, () -> ObjectiveEventType.parse("invalid"));
    }

    @Test void objectiveDefinitionsEnforceCompositionAndDefensiveCopies() {
        final ArrayList<ObjectiveId> children = new ArrayList<ObjectiveId>(Arrays.asList(WINS, BEDS));
        final ObjectiveDefinition composite = objective(ObjectiveId.of("zbw", "objective/meta"),
                ObjectiveDefinition.Composition.ALL, children);
        children.clear();
        assertEquals(2, composite.children().size());
        assertEquals(1, composite.version());
        assertEquals(ObjectiveEventType.of("zbw", "game/complete"), composite.eventType());
        assertEquals(10L, composite.target());
        assertEquals(ObjectiveDefinition.Scope.PLAYER, composite.scope());
        assertEquals(ObjectiveFilter.Dimension.MATCH_VALIDITY,
                composite.filters().get(0).dimension());
        assertEquals("public", composite.filters().get(0).expectedValue());
        assertEquals(ObjectiveDefinition.Composition.ALL, composite.composition());
        assertThrows(UnsupportedOperationException.class, () -> composite.children().clear());
        assertThrows(IllegalArgumentException.class, () -> objective(WINS,
                ObjectiveDefinition.Composition.SINGLE, Arrays.asList(WINS, BEDS)));
        assertThrows(IllegalArgumentException.class, () -> objective(WINS,
                ObjectiveDefinition.Composition.ANY, Collections.singletonList(BEDS)));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveDefinition(WINS, 0,
                ObjectiveEventType.of("zbw", "game/complete"), 1,
                ObjectiveDefinition.Composition.SINGLE, ObjectiveDefinition.Scope.PLAYER,
                Collections.<ObjectiveFilter>emptyList(), Collections.<ObjectiveId>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveDefinition(WINS, 1,
                ObjectiveEventType.of("zbw", "game/complete"), 0,
                ObjectiveDefinition.Composition.SINGLE, ObjectiveDefinition.Scope.PLAYER,
                Collections.<ObjectiveFilter>emptyList(), Collections.<ObjectiveId>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveDefinition(WINS, 1,
                ObjectiveEventType.of("zbw", "game/complete"), 1,
                ObjectiveDefinition.Composition.SINGLE, ObjectiveDefinition.Scope.PLAYER,
                Collections.<ObjectiveFilter>emptyList(), Collections.singletonList((ObjectiveId) null)));
        assertThrows(IllegalArgumentException.class, () -> new ObjectiveDefinition(WINS, 1,
                ObjectiveEventType.of("zbw", "game/complete"), 1,
                ObjectiveDefinition.Composition.SINGLE, ObjectiveDefinition.Scope.PLAYER,
                Collections.singletonList((ObjectiveFilter) null), Collections.<ObjectiveId>emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> new ObjectiveFilter(ObjectiveFilter.Dimension.MODE, ""));
    }

    @Test void progressIsMonotonicBoundedAndImmediatelyIdempotent() {
        final IdempotencyKey first = IdempotencyKey.of("zbw", "event/one");
        final ObjectiveProgress initial = new ObjectiveProgress(WINS, "player:one", 0, 0, first, NOW);
        assertEquals(WINS, initial.objectiveId());
        assertEquals("player:one", initial.ownerId());
        assertEquals(0, initial.value());
        assertEquals(0, initial.revision());
        assertEquals(NOW, initial.updatedAt());
        assertSame(initial, initial.apply(2, first, NOW.plusSeconds(1), 5));
        final ObjectiveProgress progressed = initial.apply(10, IdempotencyKey.of("zbw", "event/two"),
                NOW.plusSeconds(2), 5);
        assertEquals(5, progressed.value());
        assertEquals(1, progressed.revision());
        assertEquals("zbw:event/two", progressed.lastEvent().toString());
        assertThrows(IllegalArgumentException.class, () -> initial.apply(0, first, NOW, 1));
        assertThrows(IllegalArgumentException.class, () -> initial.apply(1, first, NOW, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ObjectiveProgress(WINS, "", 0, 0, first, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new ObjectiveProgress(WINS, "player", -1, 0, first, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new ObjectiveProgress(WINS, "player", 0, -1, first, NOW));
    }

    @Test void questDefinitionsAndAssignmentsAreValidated() {
        final ArrayList<RewardId> rewards = new ArrayList<RewardId>(Collections.singletonList(REWARD));
        final QuestDefinition quest = quest(QuestId.of("zbw", "quest/win"), WINS, rewards);
        rewards.clear();
        assertEquals(1, quest.rewards().size());
        assertEquals(QuestDefinition.Schedule.DAILY, quest.schedule());
        assertEquals(QuestDefinition.ClaimPolicy.MANUAL, quest.claimPolicy());
        assertEquals(Duration.ofHours(24), quest.cooldown().get());
        assertTrue(quest.repeatable());
        assertFalse(quest.hidden());
        assertThrows(IllegalArgumentException.class, () -> new QuestDefinition(quest.id(), 0,
                quest.schedule(), WINS, quest.rewards(), quest.claimPolicy(), quest.cooldown(), true, false));
        assertThrows(IllegalArgumentException.class, () -> new QuestDefinition(quest.id(), 1,
                quest.schedule(), WINS, Collections.<RewardId>emptyList(), quest.claimPolicy(),
                Optional.<Duration>empty(), true, false));
        assertThrows(IllegalArgumentException.class, () -> new QuestDefinition(quest.id(), 1,
                quest.schedule(), WINS, quest.rewards(), quest.claimPolicy(), Optional.of(Duration.ZERO),
                true, false));

        final PlayerProgressionId player = PlayerProgressionId.of(PlayerId.of(new UUID(1, 2)));
        final QuestAssignment assignment = new QuestAssignment(quest.id(), player,
                QuestAssignment.Status.ACTIVE, NOW, NOW.plusSeconds(60), 2);
        assertEquals(quest.id(), assignment.questId());
        assertEquals(player, assignment.playerId());
        assertEquals(QuestAssignment.Status.ACTIVE, assignment.status());
        assertEquals(NOW, assignment.assignedAt());
        assertEquals(NOW.plusSeconds(60), assignment.expiresAt());
        assertEquals(2, assignment.revision());
        assertThrows(IllegalArgumentException.class, () -> new QuestAssignment(quest.id(), player,
                QuestAssignment.Status.ACTIVE, NOW, NOW, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuestAssignment(quest.id(), player,
                QuestAssignment.Status.ACTIVE, NOW, NOW.plusSeconds(1), -1));
    }

    @Test void achievementTiersAreMonotonicAndDefensive() {
        final AchievementDefinition.Tier first = tier(1, 1, 5);
        final AchievementDefinition.Tier second = tier(2, 10, 20);
        final ArrayList<AchievementDefinition.Tier> tiers =
                new ArrayList<AchievementDefinition.Tier>(Arrays.asList(first, second));
        final AchievementDefinition achievement = new AchievementDefinition(
                AchievementId.of("zbw", "achievement/wins"), 1, "combat", WINS, tiers, true, false);
        tiers.clear();
        assertEquals(2, achievement.tiers().size());
        assertEquals("combat", achievement.category());
        assertEquals(WINS, achievement.objectiveId());
        assertTrue(achievement.hidden());
        assertFalse(achievement.repeatable());
        assertEquals(1, first.number());
        assertEquals(1, first.target());
        assertEquals(5, first.points());
        assertEquals(REWARD, first.rewards().get(0));
        assertThrows(IllegalArgumentException.class, () -> new AchievementDefinition(achievement.id(), 0,
                "combat", WINS, Arrays.asList(first, second), false, false));
        assertThrows(IllegalArgumentException.class, () -> new AchievementDefinition(achievement.id(), 1,
                "", WINS, Arrays.asList(first, second), false, false));
        assertThrows(IllegalArgumentException.class, () -> new AchievementDefinition(achievement.id(), 1,
                "combat", WINS, Collections.<AchievementDefinition.Tier>emptyList(), false, false));
        assertThrows(IllegalArgumentException.class, () -> new AchievementDefinition(achievement.id(), 1,
                "combat", WINS, Arrays.asList(second, first), false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new AchievementDefinition.Tier(0, 1, 0, Collections.singletonList(REWARD)));
        assertThrows(IllegalArgumentException.class,
                () -> new AchievementDefinition.Tier(1, 1, -1, Collections.singletonList(REWARD)));
        assertThrows(IllegalArgumentException.class,
                () -> new AchievementDefinition.Tier(1, 1, 0, Collections.singletonList((RewardId) null)));
    }

    @Test void battlePassEnforcesVersionedSeasonAndTracks() {
        final ArrayList<RewardId> free = new ArrayList<RewardId>(Collections.singletonList(REWARD));
        final BattlePassDefinition.Tier tier = new BattlePassDefinition.Tier(1, 0, free,
                Collections.singletonList(RewardId.of("zbw", "reward/title")));
        free.clear();
        final BattlePassDefinition pass = new BattlePassDefinition(SeasonId.of("zbw", "season/one"), 1,
                NOW, NOW.plusSeconds(100), NOW.plusSeconds(120), Collections.singletonList(tier));
        assertEquals(1, pass.version());
        assertEquals(NOW, pass.startsAt());
        assertEquals(NOW.plusSeconds(100), pass.endsAt());
        assertEquals(NOW.plusSeconds(120), pass.graceEndsAt());
        assertEquals(1, pass.tiers().size());
        assertEquals(1, tier.number());
        assertEquals(0, tier.requiredXp());
        assertEquals(1, tier.freeRewards().size());
        assertEquals(1, tier.premiumRewards().size());
        assertThrows(IllegalArgumentException.class, () -> new BattlePassDefinition(pass.id(), 0,
                NOW, NOW.plusSeconds(1), NOW.plusSeconds(2), pass.tiers()));
        assertThrows(IllegalArgumentException.class, () -> new BattlePassDefinition(pass.id(), 1,
                NOW, NOW, NOW, pass.tiers()));
        assertThrows(IllegalArgumentException.class, () -> new BattlePassDefinition(pass.id(), 1,
                NOW, NOW.plusSeconds(2), NOW.plusSeconds(1), pass.tiers()));
        assertThrows(IllegalArgumentException.class, () -> new BattlePassDefinition(pass.id(), 1,
                NOW, NOW.plusSeconds(1), NOW.plusSeconds(1), Collections.<BattlePassDefinition.Tier>emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> new BattlePassDefinition.Tier(0, 0, Collections.singletonList(REWARD),
                        Collections.<RewardId>emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> new BattlePassDefinition.Tier(1, 0, Collections.<RewardId>emptyList(),
                        Collections.<RewardId>emptyList()));
        final BattlePassDefinition.Tier second = new BattlePassDefinition.Tier(2, 10,
                Collections.singletonList(REWARD), Collections.<RewardId>emptyList());
        assertThrows(IllegalArgumentException.class, () -> new BattlePassDefinition(pass.id(), 1,
                NOW, NOW.plusSeconds(20), NOW.plusSeconds(20), Arrays.asList(second, tier)));
    }

    @Test void challengeDefinitionsAreTypedAndBounded() {
        final ArrayList<RewardId> rewards = new ArrayList<RewardId>(Collections.singletonList(REWARD));
        final ChallengeDefinition challenge = new ChallengeDefinition(
                ChallengeId.of("zbw", "challenge/sprint"), 1, ChallengeDefinition.Variant.TIMED,
                WINS, Duration.ofMinutes(10), rewards);
        rewards.clear();
        assertEquals(1, challenge.version());
        assertEquals(ChallengeDefinition.Variant.TIMED, challenge.variant());
        assertEquals(WINS, challenge.objectiveId());
        assertEquals(Duration.ofMinutes(10), challenge.duration());
        assertEquals(1, challenge.rewards().size());
        assertThrows(IllegalArgumentException.class, () -> new ChallengeDefinition(challenge.id(), 0,
                challenge.variant(), WINS, challenge.duration(), challenge.rewards()));
        assertThrows(IllegalArgumentException.class, () -> new ChallengeDefinition(challenge.id(), 1,
                challenge.variant(), WINS, Duration.ZERO, challenge.rewards()));
        assertThrows(IllegalArgumentException.class, () -> new ChallengeDefinition(challenge.id(), 1,
                challenge.variant(), WINS, challenge.duration(), Collections.<RewardId>emptyList()));
    }

    @Test void catalogueRejectsDuplicatesAndUnknownReferences() {
        final ObjectiveDefinition wins = objective(WINS, ObjectiveDefinition.Composition.SINGLE,
                Collections.<ObjectiveId>emptyList());
        final QuestDefinition quest = quest(QuestId.of("zbw", "quest/win"), WINS,
                Collections.singletonList(REWARD));
        final AchievementDefinition achievement = new AchievementDefinition(
                AchievementId.of("zbw", "achievement/wins"), 1, "combat", WINS,
                Collections.singletonList(tier(1, 1, 5)), false, false);
        final BattlePassDefinition season = new BattlePassDefinition(SeasonId.of("zbw", "season/one"), 1,
                NOW, NOW.plusSeconds(10), NOW.plusSeconds(10), Collections.singletonList(
                        new BattlePassDefinition.Tier(1, 0, Collections.singletonList(REWARD),
                                Collections.<RewardId>emptyList())));
        final ChallengeDefinition challenge = new ChallengeDefinition(
                ChallengeId.of("zbw", "challenge/win"), 1, ChallengeDefinition.Variant.DAILY,
                WINS, Duration.ofHours(1), Collections.singletonList(REWARD));
        final M13Catalog catalog = new M13Catalog(Collections.singletonList(wins),
                Collections.singletonList(quest), Collections.singletonList(achievement),
                Collections.singletonList(challenge), Collections.singletonList(season));
        assertEquals(1, catalog.objectives().size());
        assertEquals(1, catalog.quests().size());
        assertEquals(1, catalog.achievements().size());
        assertEquals(1, catalog.challenges().size());
        assertEquals(1, catalog.seasons().size());
        assertThrows(UnsupportedOperationException.class, () -> catalog.quests().clear());
        assertThrows(IllegalArgumentException.class, () -> new M13Catalog(Arrays.asList(wins, wins),
                Collections.singletonList(quest), Collections.singletonList(achievement),
                Collections.singletonList(challenge), Collections.singletonList(season)));
        assertThrows(IllegalArgumentException.class, () -> new M13Catalog(Collections.singletonList(wins),
                Arrays.asList(quest, quest), Collections.singletonList(achievement),
                Collections.singletonList(challenge), Collections.singletonList(season)));
        final QuestDefinition unknown = quest(QuestId.of("zbw", "quest/unknown"), BEDS,
                Collections.singletonList(REWARD));
        assertThrows(IllegalArgumentException.class, () -> new M13Catalog(Collections.singletonList(wins),
                Collections.singletonList(unknown), Collections.singletonList(achievement),
                Collections.singletonList(challenge), Collections.singletonList(season)));
        final ObjectiveDefinition composite = objective(ObjectiveId.of("zbw", "objective/composite"),
                ObjectiveDefinition.Composition.ALL, Arrays.asList(WINS, BEDS));
        assertThrows(IllegalArgumentException.class, () -> new M13Catalog(Arrays.asList(wins, composite),
                Collections.singletonList(quest), Collections.singletonList(achievement),
                Collections.singletonList(challenge), Collections.singletonList(season)));
        assertThrows(IllegalArgumentException.class, () -> new M13Catalog(
                Collections.singletonList((ObjectiveDefinition) null), Collections.<QuestDefinition>emptyList(),
                Collections.<AchievementDefinition>emptyList(), Collections.<ChallengeDefinition>emptyList(),
                Collections.<BattlePassDefinition>emptyList()));
    }

    private ObjectiveDefinition objective(final ObjectiveId id,
                                           final ObjectiveDefinition.Composition composition,
                                           final java.util.List<ObjectiveId> children) {
        return new ObjectiveDefinition(id, 1, ObjectiveEventType.of("zbw", "game/complete"), 10,
                composition, ObjectiveDefinition.Scope.PLAYER,
                Collections.singletonList(new ObjectiveFilter(
                        ObjectiveFilter.Dimension.MATCH_VALIDITY, "public")), children);
    }

    private QuestDefinition quest(final QuestId id, final ObjectiveId objective,
                                  final java.util.List<RewardId> rewards) {
        return new QuestDefinition(id, 1, QuestDefinition.Schedule.DAILY, objective, rewards,
                QuestDefinition.ClaimPolicy.MANUAL, Optional.of(Duration.ofHours(24)), true, false);
    }

    private AchievementDefinition.Tier tier(final int number, final long target, final int points) {
        return new AchievementDefinition.Tier(number, target, points, Collections.singletonList(REWARD));
    }
}
