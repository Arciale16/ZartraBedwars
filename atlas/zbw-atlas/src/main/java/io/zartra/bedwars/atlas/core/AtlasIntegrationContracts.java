package io.zartra.bedwars.atlas.core;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasReviewerId;
import io.zartra.bedwars.replay.api.ReplayId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Neutral query and intent ports that preserve M12-M17 ownership. */
public final class AtlasIntegrationContracts {
    private AtlasIntegrationContracts() { }

    /** M17-owned replay access query; no replay payload crosses this port. */
    public interface ReplayAccessQuery {
        CompletionStage<Boolean> authorize(
                ReplayId replayId, UUID viewerId, boolean protectedEvidence);
    }

    /** M15-owned aggregate query used only for reviewer eligibility. */
    public interface StatisticsEligibilityQuery {
        CompletionStage<Long> completedMatches(UUID playerId);
    }

    /** M12-owned reward intent; implementations own ledger/idempotency. */
    public interface RewardRequestPort {
        CompletionStage<Boolean> request(AtlasReviewerId reviewerId, String policyId);
    }

    /** M13 consumer for qualified outcomes; Atlas does not mutate objectives. */
    public interface QualifiedReviewOutcomeSink {
        CompletionStage<Void> publish(AtlasCaseId caseId, String outcomeId, double confidence);
    }

    /** M16 provider boundary; it is not a placeholder resolver or engine. */
    public interface AtlasQueryProvider {
        CompletionStage<Optional<String>> reviewerStatus(UUID playerId);
        CompletionStage<Long> availableCases();
        CompletionStage<Optional<String>> reputationSummary(UUID playerId);
    }
}
