package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.progression.projection.ProgressionProjector;
import io.zartra.bedwars.progression.projection.ProjectionIdempotencyPort;
import io.zartra.bedwars.progression.repository.CurrencyAccountRepository;
import io.zartra.bedwars.progression.repository.EconomicTransactionRepository;
import io.zartra.bedwars.progression.repository.EntitlementRepository;
import io.zartra.bedwars.progression.repository.ExperienceLedgerRepository;
import io.zartra.bedwars.progression.repository.LevelHistoryRepository;
import io.zartra.bedwars.progression.repository.PrestigeHistoryRepository;
import io.zartra.bedwars.progression.repository.ProgressionAccountRepository;
import io.zartra.bedwars.progression.repository.RewardRepository;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Verifies Phase 1 repository and projection boundaries remain contracts only. */
class RepositoryContractTest {
    @Test void allStorageAndProjectionPortsAreInterfaces() {
        for (Class<?> type : Arrays.<Class<?>>asList(ProgressionAccountRepository.class,
                ExperienceLedgerRepository.class, LevelHistoryRepository.class,
                PrestigeHistoryRepository.class, CurrencyAccountRepository.class,
                EconomicTransactionRepository.class, RewardRepository.class,
                EntitlementRepository.class, ProjectionIdempotencyPort.class,
                ProgressionProjector.class)) {
            assertTrue(type.isInterface(), type.getName());
        }
    }
}
