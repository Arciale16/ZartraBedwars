package io.zartra.bedwars.statistics.integration;

import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.progression.projection.ProjectionResult;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import io.zartra.bedwars.statistics.projection.StatisticProjection;
import java.util.List;

/** Converts existing immutable event boundaries to M15 facts without owning their lifecycle. */
public interface StatisticsEventAdapter {
    /** @return zero or more facts from a persisted M08 match transition */ List<StatisticProjection.Event> fromMatchTransition(MatchTransition transition);
    /** @return zero or more facts from a committed M11 settlement outcome */ List<StatisticProjection.Event> fromPurchaseOutcome(PurchaseOutcome outcome);
    /** @return zero or more facts from an M12 projection outcome */ List<StatisticProjection.Event> fromProgressionProjection(ProjectionResult result);
}
