package io.zartra.bedwars.replay.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.event.EventMetadata;
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
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import io.zartra.bedwars.shop.api.PurchaseQuote;
import io.zartra.bedwars.shop.api.PurchaseRequest;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.api.ShopIds;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-003 one-way M08/M11/M12 ingestion and duplicate regression tests. */
class ReplayEventIngestionTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(
            UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test void progressionInputIsOrderedAndDuplicateSafe() {
        final ReplayEventIngestion ingestion = new ReplayEventIngestion();
        final ReplaySession recording = ReplaySession.create(metadata()).start();
        final ProgressionEventInput input = input(START.plusMillis(12));
        final ReplaySession once = ingestion.ingestProgression(recording, input);
        final ReplaySession duplicate = ingestion.ingestProgression(once, input);
        assertEquals(1, duplicate.timeline().events().size());
        assertEquals(0, duplicate.timeline().events().get(0).sequence());
        assertEquals(12, duplicate.timeline().events().get(0).offsetMillis());
        assertEquals(ReplayEvent.Source.PROGRESSION,
                duplicate.timeline().events().get(0).source());
    }

    @Test void gameFactsKeepSourceOrderAndDuplicateTransitionsAreIgnored() {
        final DefinitionId red = DefinitionId.of("team", "red");
        final MatchSnapshot snapshot = new MatchSnapshot(MatchId.random(), ArenaId.random(), 1,
                MatchSnapshot.State.WAITING, 0,
                Arrays.asList(TeamSnapshot.empty(red, 1),
                        TeamSnapshot.empty(DefinitionId.of("team", "blue"), 1)),
                Collections.emptyList(), null, null, false, START.plusMillis(3));
        final MatchTransition transition = new MatchTransition(snapshot, snapshot,
                Arrays.asList(new MatchTransition.Fact(DefinitionId.of("game", "join"), PLAYER, red),
                        new MatchTransition.Fact(DefinitionId.of("game", "tick"), null, null)), false);
        final ReplayEventIngestion ingestion = new ReplayEventIngestion();
        final ReplaySession result = ingestion.ingestGame(
                ReplaySession.create(metadata()).start(), transition, "transition-1");
        assertEquals(2, result.timeline().events().size());
        assertEquals("transition-1:1", result.timeline().events().get(1).eventId());
        final MatchTransition duplicate = new MatchTransition(snapshot, snapshot,
                Collections.<MatchTransition.Fact>emptyList(), true);
        assertEquals(result, ingestion.ingestGame(result, duplicate, "transition-1"));
    }

    @Test void serviceDispatchesMatchAndSettlementAdapters() {
        final DefinitionId red = DefinitionId.of("team", "red");
        final MatchSnapshot snapshot = new MatchSnapshot(MatchId.random(), ArenaId.random(), 1,
                MatchSnapshot.State.WAITING, 0,
                Arrays.asList(TeamSnapshot.empty(red, 1),
                        TeamSnapshot.empty(DefinitionId.of("team", "blue"), 1)),
                Collections.emptyList(), null, null, false, START.plusMillis(3));
        final MatchTransition transition = new MatchTransition(snapshot, snapshot,
                Collections.singletonList(new MatchTransition.Fact(
                        DefinitionId.of("game", "join"), PLAYER, red)), false);
        final ReplayIngestionService service = new ReplayIngestionService();
        final ReplaySession recording = ReplaySession.create(metadata()).start();
        final ReplayIngestionResult game = service.ingest(recording, transition, "transition");
        final ReplayIngestionResult gameDuplicate = service.ingest(
                game.session(), transition, "transition");
        final PurchaseOutcome purchase = new PurchaseOutcome(quote(), false, START.plusMillis(5));
        final ReplayIngestionResult settlement = service.ingest(
                gameDuplicate.session(), purchase, "purchase");
        final ReplayIngestionResult settlementDuplicate = service.ingest(settlement.session(),
                new PurchaseOutcome(quote(), true, START.plusMillis(5)), "purchase-retry");
        assertEquals(ReplayIngestionResult.Status.ACCEPTED, game.status());
        assertEquals(ReplayIngestionResult.Status.DUPLICATE, gameDuplicate.status());
        assertEquals(ReplayIngestionResult.Status.ACCEPTED, settlement.status());
        assertEquals(ReplayIngestionResult.Status.DUPLICATE, settlementDuplicate.status());
    }
    @Test void shopOutcomeIsConsumedWithoutSettlementOwnership() {
        final PurchaseOutcome outcome = new PurchaseOutcome(quote(), false, START.plusMillis(5));
        final ReplaySession result = new ReplayEventIngestion().ingestShop(
                ReplaySession.create(metadata()).start(), outcome, "purchase-1");
        assertEquals("false", result.timeline().events().get(0).attributes().get("duplicate"));
        assertEquals(ReplayEvent.Source.SHOP, result.timeline().events().get(0).source());
    }

    @Test void eventBeforeReplayCreationIsRejected() {
        final ReplaySession recording = ReplaySession.create(metadata()).start();
        assertThrows(IllegalArgumentException.class, () -> new ReplayEventIngestion()
                .ingestProgression(recording, input(START.minusMillis(1))));
    }

    private static PurchaseQuote quote() {
        final PurchaseContext context = new PurchaseContext(AuthorizationSubject.of(
                AuthorizationSubject.Kind.PLAYER, DefinitionId.of("player", "one")), PLAYER,
                MatchId.random(), ArenaId.random(), DefinitionId.of("mode", "solo"),
                DefinitionId.of("team", "red"), Optional.<DefinitionId>empty());
        final PurchaseRequest request = new PurchaseRequest(context,
                ShopIds.CatalogId.of("shop", "default"), ShopIds.ItemId.of("shop", "wool"),
                1, true, IdempotencyKey.of("shop", "purchase-1"));
        final ShopCatalog.Price price = new ShopCatalog.Price(Collections.singletonList(
                new ShopCatalog.ResourceAmount(ResourceId.of("resource", "iron"), 4)));
        return new PurchaseQuote(request, 1, 0, price, 16,
                Collections.singleton(PermissionNode.of("zartrabedwars.shop.purchase")),
                START, START.plusSeconds(1));
    }

    private static ReplayMetadata metadata() {
        return new ReplayMetadata(ReplayId.random(), MatchId.random(), START, 1,
                Collections.<PlayerId>emptySet(), false);
    }

    private static ProgressionEventInput input(final Instant occurredAt) {
        final EventMetadata metadata = EventMetadata.of(EventId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000002")),
                EventTypeId.of("progression", "reward"), CorrelationId.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000003")),
                occurredAt, 4, 1, EventMetadata.ThreadContext.APPLICATION_WORKER);
        return new ProgressionEventInput(metadata, PlayerProgressionId.of(PLAYER),
                DefinitionId.of("progression", "reward"), IdempotencyKey.of("replay", "event-1"),
                new byte[] {1, 2});
    }
}
