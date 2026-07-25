package io.zartra.bedwars.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.event.EventMetadata.ThreadContext;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.progression.projection.ProjectionResult;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import io.zartra.bedwars.statistics.integration.ConfiguredStatisticsEventAdapter;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.projection.StatisticProjection;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** M15 deterministic conversion coverage for existing M08, M11 and M12 boundaries. */
final class ConfiguredStatisticsEventAdapterTest {
    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(new UUID(0, 181));
    private static final DefinitionId KILL = DefinitionId.of("zartra", "event/kill");
    private static final DefinitionId PURCHASE = DefinitionId.of("zartra", "shop-item/sword");
    private static final DefinitionId XP = DefinitionId.of("zartra", "event/xp");
    private static final StatisticId STATISTIC = StatisticId.of("zartra", "kills");
    private static final StatisticScope SCOPE = StatisticScope.of("zartra", "global");

    @Test
    void convertsM08FactsInStableOrderAndSkipsUnsupportedOrDuplicateTransitions() {
        final ConfiguredStatisticsEventAdapter adapter = adapter();
        final MatchTransition transition = new MatchTransition(snapshot(), snapshot(), Arrays.asList(
                new MatchTransition.Fact(DefinitionId.of("zartra", "event/ignored"), PLAYER, null),
                new MatchTransition.Fact(KILL, PLAYER, null)), false);
        final List<ConfiguredStatisticsEventAdapter.PlayerEvent> events = adapter.fromM08(
                new ConfiguredStatisticsEventAdapter.M08Event(metadata(4), key("m08"), transition));
        assertEquals(1, events.size());
        assertEquals(StatisticProjection.Source.MATCH, events.get(0).event().source());
        assertEquals("m08/statistics-1", events.get(0).event().idempotencyKey().path());
        assertTrue(adapter.fromM08(new ConfiguredStatisticsEventAdapter.M08Event(metadata(4),
                key("duplicate"), new MatchTransition(snapshot(), snapshot(),
                Collections.<MatchTransition.Fact>emptyList(), true))).isEmpty());
    }

    @Test
    void convertsCommittedM11PurchasesAndAppliedM12ProjectionsOnly() {
        final ConfiguredStatisticsEventAdapter adapter = adapter();
        final List<ConfiguredStatisticsEventAdapter.PlayerEvent> purchase = adapter.fromM11(
                new ConfiguredStatisticsEventAdapter.M11Event(metadata(5), key("m11"), purchase()));
        assertEquals(1, purchase.size());
        assertEquals(StatisticProjection.Source.SETTLEMENT, purchase.get(0).event().source());
        final ProgressionEventInput input = new ProgressionEventInput(metadata(6),
                PlayerProgressionId.of(PLAYER), XP, key("m12"), new byte[0]);
        final ProjectionResult applied = new ProjectionResult(ProjectionResult.Status.APPLIED,
                null, RecordRevision.initial(), null);
        assertEquals(1, adapter.fromM12(new ConfiguredStatisticsEventAdapter.M12Event(input,
                applied)).size());
        final ProjectionResult duplicate = new ProjectionResult(ProjectionResult.Status.DUPLICATE,
                null, null, null);
        assertFalse(adapter.fromM12(new ConfiguredStatisticsEventAdapter.M12Event(input,
                duplicate)).iterator().hasNext());
    }

    private static ConfiguredStatisticsEventAdapter adapter() {
        final Map<DefinitionId, ConfiguredStatisticsEventAdapter.Mapping> mappings =
                new LinkedHashMap<DefinitionId, ConfiguredStatisticsEventAdapter.Mapping>();
        final ConfiguredStatisticsEventAdapter.Mapping mapping =
                new ConfiguredStatisticsEventAdapter.Mapping(STATISTIC, SCOPE, 1);
        mappings.put(KILL, mapping);
        mappings.put(PURCHASE, mapping);
        mappings.put(XP, mapping);
        return new ConfiguredStatisticsEventAdapter(mappings);
    }

    private static MatchSnapshot snapshot() {
        return new MatchSnapshot(MatchId.of(new UUID(0, 182)), ArenaId.of(new UUID(0, 183)), 0,
                MatchSnapshot.State.PLAYING, 0, Arrays.asList(
                TeamSnapshot.empty(DefinitionId.of("zartra", "team/red"), 2),
                TeamSnapshot.empty(DefinitionId.of("zartra", "team/blue"), 2)),
                Collections.emptyList(), null, null, false, NOW);
    }

    private static PurchaseOutcome purchase() {
        final PurchaseContext context = new PurchaseContext(AuthorizationSubject.of(
                AuthorizationSubject.Kind.PLAYER, DefinitionId.of("zartra", "player/test")),
                PLAYER, MatchId.of(new UUID(0, 182)), ArenaId.of(new UUID(0, 183)),
                DefinitionId.of("zartra", "mode/standard"),
                DefinitionId.of("zartra", "team/red"), Optional.empty());
        final PurchaseRequest request = new PurchaseRequest(context, ShopIds.CatalogId.of("zartra", "main"),
                ShopIds.ItemId.of("zartra", "sword"), 1, true, key("purchase"));
        final ShopCatalog.Price price = new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(ResourceId.of("zartra", "iron"), 1)));
        final PurchaseQuote quote = new PurchaseQuote(request, 1, 0, price, 1,
                Collections.singletonList(PermissionNode.of("zbw.shop.purchase")), NOW,
                NOW.plusSeconds(10));
        return new PurchaseOutcome(quote, false, NOW);
    }

    private static EventMetadata metadata(final long sequence) {
        return EventMetadata.of(EventId.of(new UUID(0, 190 + sequence)),
                EventTypeId.of("zartra", "statistics/source"), CorrelationId.of(new UUID(0, 191)),
                NOW, sequence, 1, ThreadContext.APPLICATION_WORKER);
    }

    private static IdempotencyKey key(final String value) {
        return IdempotencyKey.of("test", value);
    }
}
