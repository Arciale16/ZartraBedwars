package io.zartra.bedwars.atlas.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Asynchronous immutable review persistence boundary with duplicate protection. */
public interface AtlasReviewRepository {
    /** Appends a review exactly once by review identity. */
    CompletionStage<Boolean> append(AtlasReview review);
    /** Loads one review by identity. */
    CompletionStage<Optional<AtlasReview>> find(AtlasReviewId reviewId);
    /** Loads reviews in deterministic submission-time then review-identity order. */
    CompletionStage<List<AtlasReview>> findByCase(AtlasCaseId caseId);
}
