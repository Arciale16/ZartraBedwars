package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.achievement.AchievementProgress;
import io.zartra.bedwars.progression.challenge.ChallengeId;
import io.zartra.bedwars.progression.challenge.ChallengeProgress;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import io.zartra.bedwars.progression.objective.ObjectiveRuntimeState;
import io.zartra.bedwars.progression.pass.SeasonId;
import io.zartra.bedwars.progression.pass.SeasonProgress;
import io.zartra.bedwars.progression.quest.QuestAssignment;
import io.zartra.bedwars.progression.quest.QuestId;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** SQLite persistence, recovery, rollback and optimistic-lock evidence for M13 state. */
final class JdbcM13StateRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final PlayerProgressionId PLAYER = PlayerProgressionId.of(PlayerId.of(new UUID(0, 130)));
    private static final ObjectiveId OBJECTIVE = ObjectiveId.of("zbw", "objective/wins");
    private static final QuestId QUEST = QuestId.of("zbw", "quest/win");
    private static final AchievementId ACHIEVEMENT = AchievementId.of("zbw", "achievement/win");
    private static final ChallengeId CHALLENGE = ChallengeId.of("zbw", "challenge/win");
    private static final SeasonId SEASON = SeasonId.of("zbw", "season/one");
    @TempDir Path temporary;

    @Test void migrationAndEveryStateSurviveRestart() {
        final Path database = temporary.resolve("m13.db");
        final String checksum;
        try (JdbcStorageEngine engine = open(database)) {
            checksum = migrate(engine);
            final JdbcM13StateRepository repository = new JdbcM13StateRepository(5);
            try (UnitOfWork unit = write(engine)) {
                assertTrue(repository.claimEvent(unit, key("event"), NOW).requireValue());
                assertFalse(repository.claimEvent(unit, key("event"), NOW).requireValue());
                repository.saveObjective(unit, objective(), 0).requireValue();
                repository.saveQuest(unit, quest(), 0).requireValue();
                repository.saveAchievement(unit, achievement(), 0).requireValue();
                repository.saveChallenge(unit, challenge(), 0).requireValue();
                repository.saveSeason(unit, season(), 0).requireValue();
                unit.commit().requireValue();
            }
        }
        try (JdbcStorageEngine engine = open(database)) {
            assertEquals(checksum, migrate(engine));
            final JdbcM13StateRepository repository = new JdbcM13StateRepository(5);
            try (UnitOfWork unit = read(engine)) {
                assertEquals(3, repository.findObjective(unit, PLAYER, OBJECTIVE)
                        .requireValue().get().value());
                assertEquals(QuestAssignment.Status.ACTIVE, repository.findQuest(unit, PLAYER, QUEST)
                        .requireValue().get().status());
                assertEquals(1, repository.findAchievement(unit, PLAYER, ACHIEVEMENT)
                        .requireValue().get().tier());
                assertEquals(ChallengeProgress.Status.ACTIVE,
                        repository.findChallenge(unit, PLAYER, CHALLENGE).requireValue().get().status());
                assertEquals(Collections.singleton(1), repository.findSeason(unit, PLAYER, SEASON)
                        .requireValue().get().claimedFreeTiers());
                unit.commit().requireValue();
            }
        }
    }

    @Test void rollbackAndOptimisticConflictPreserveLastCommittedState() {
        try (JdbcStorageEngine engine = open(temporary.resolve("conflict.db"))) {
            migrate(engine);
            final JdbcM13StateRepository repository = new JdbcM13StateRepository(5);
            try (UnitOfWork unit = write(engine)) {
                repository.saveObjective(unit, objective(), 0).requireValue();
                unit.rollback().requireValue();
            }
            try (UnitOfWork unit = read(engine)) {
                assertFalse(repository.findObjective(unit, PLAYER, OBJECTIVE).requireValue().isPresent());
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                repository.saveObjective(unit, objective(), 0).requireValue();
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                assertTrue(repository.saveObjective(unit, objectiveRevisionTwo(), 99).isFailure());
                unit.rollback().requireValue();
            }
        }
    }

    private static ObjectiveRuntimeState objective() {
        return new ObjectiveRuntimeState(OBJECTIVE, PLAYER, 1, 3, 0,
                ObjectiveRuntimeState.Status.ACTIVE, 0, Optional.of(key("objective")),
                Optional.of(NOW.plusSeconds(60)), audit());
    }
    private static ObjectiveRuntimeState objectiveRevisionTwo() {
        return new ObjectiveRuntimeState(OBJECTIVE, PLAYER, 1, 5, 0,
                ObjectiveRuntimeState.Status.ACTIVE, 2, Optional.of(key("objective-two")),
                Optional.empty(), audit());
    }
    private static QuestAssignment quest() {
        return new QuestAssignment(QUEST, PLAYER, QuestAssignment.Status.ACTIVE, NOW,
                NOW.plusSeconds(60), 0);
    }
    private static AchievementProgress achievement() {
        return new AchievementProgress(ACHIEVEMENT, PLAYER, 1, 1, 5, true, 0,
                Optional.of(key("achievement")), NOW);
    }
    private static ChallengeProgress challenge() {
        return new ChallengeProgress(CHALLENGE, PLAYER, 1, ChallengeProgress.Status.ACTIVE,
                NOW, NOW.plusSeconds(60), 0, Optional.of(key("challenge")));
    }
    private static SeasonProgress season() {
        return new SeasonProgress(SEASON, PLAYER, 1, 15, 2, Collections.singleton(1), 0,
                Optional.of(key("season")), NOW);
    }
    private static AuditMetadata audit() {
        return new AuditMetadata("test", CorrelationId.of(new UUID(0, 131)), NOW, NOW);
    }
    private static IdempotencyKey key(final String value) { return IdempotencyKey.of("test", value); }
    private static String migrate(final JdbcStorageEngine engine) {
        try (UnitOfWork unit = write(engine)) {
            final JdbcUnitOfWork jdbc = (JdbcUnitOfWork) unit;
            new ProgressionSchemaMigrator(5).migrate(jdbc.connection()).requireValue();
            final M13SchemaMigrator.Report report = new M13SchemaMigrator(5)
                    .migrate(jdbc.connection()).requireValue();
            unit.commit().requireValue();
            return report.checksum();
        }
    }
    private static UnitOfWork write(final JdbcStorageEngine engine) {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_WRITE,
                Duration.ofSeconds(5), 2)).requireValue();
    }
    private static UnitOfWork read(final JdbcStorageEngine engine) {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_ONLY,
                Duration.ofSeconds(5), 2)).requireValue();
    }
    private static JdbcStorageEngine open(final Path database) {
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(StorageEngine.EngineKind.SQLITE,
                "jdbc:sqlite:" + database.toAbsolutePath(), "", new char[0], 1,
                Duration.ofSeconds(5), Duration.ofSeconds(5))).requireValue();
    }
}
