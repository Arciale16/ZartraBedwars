package io.zartra.bedwars.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.ArenaAvailability;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.EnqueueVerdict;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.FairCapacityPolicy;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.Limits;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.Party;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.PartyId;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.QueueId;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.QueueService;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.Request;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.Reservation;
import io.zartra.bedwars.game.matchmaking.MatchmakingFramework.ReservationService;
import io.zartra.bedwars.game.mode.ModeFramework.ModeId;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchmakingM10Test {
    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private static final QueueId QUEUE = QueueId.of("zartra", "standard");
    private static final ModeId MODE = ModeId.of("zartra", "standard");
    private static final DefinitionId LAYOUT = DefinitionId.of("zartra", "layout/solo");

    @Test void enqueueIsAtomicAndIdempotentForParties() {
        final QueueService service = service();
        final Request request = request(1, 0, NOW.plusSeconds(60), party(1, 3, 4L));
        assertEquals(EnqueueVerdict.ACCEPTED, service.enqueue(request).verdict());
        assertEquals(EnqueueVerdict.IDEMPOTENT, service.enqueue(request).verdict());
        assertEquals(3, service.status(request.party().get().members().get(2)).get().queuedActors());
        assertEquals(EnqueueVerdict.DUPLICATE_ACTOR,
                service.enqueue(request(2, 0, NOW.plusSeconds(60), request.party().get())).verdict());
    }

    @Test void leaderAndPartyFitAreValidated() {
        final Party party = party(1, 3, 1L);
        assertThrows(IllegalArgumentException.class, () -> requestFor(player(9), 1, NOW.plusSeconds(30), party, 4));
        assertThrows(IllegalArgumentException.class, () -> requestFor(party.leader(), 1, NOW.plusSeconds(30), party, 2));
    }

    @Test void cancellationRequiresLeaderTokenAndRevision() {
        final QueueService service = service();
        final Request request = request(1, 0, NOW.plusSeconds(60), null);
        service.enqueue(request);
        assertFalse(service.cancel(request.actor(), UUID.randomUUID(), 0L));
        assertTrue(service.cancel(request.actor(), request.cancellationToken(), 0L));
        assertFalse(service.status(request.actor()).isPresent());
        assertEquals(1L, service.diagnostics().cancelled());
    }

    @Test void expiryCleanupAndDrainAreDeterministic() {
        final QueueService service = service();
        assertEquals(EnqueueVerdict.EXPIRED, service.enqueue(requestAt(1, 0,
                NOW.minusSeconds(60), NOW.minusSeconds(1), null)).verdict());
        final Request live = request(2, 0, NOW.plusSeconds(60), null);
        service.enqueue(live);
        service.beginDrain();
        assertEquals(EnqueueVerdict.EXPIRED, service.enqueue(request(3, 0, NOW.plusSeconds(60), null)).verdict());
        assertEquals(1, service.snapshot(QUEUE).size());
    }

    @Test void boundedQueuesRejectWithoutPartialActorIndex() {
        final QueueService service = new QueueService(new Limits(1, 1, 2, Duration.ofMinutes(1),
                Duration.ofSeconds(10)), TimeSource.FixedTimeSource.at(NOW));
        assertEquals(EnqueueVerdict.CAPACITY, service.enqueue(request(1, 0, NOW.plusSeconds(60), party(1, 3, 1L))).verdict());
        assertEquals(0, service.diagnostics().queuedActors());
        assertEquals(EnqueueVerdict.ACCEPTED, service.enqueue(request(2, 0, NOW.plusSeconds(60), null)).verdict());
        assertEquals(EnqueueVerdict.CAPACITY, service.enqueue(request(3, 0, NOW.plusSeconds(60), null)).verdict());
    }

    @Test void fairPolicyIsDeterministicAndAgesOldRequests() {
        final Request old = requestAt(1, 0, NOW.minusSeconds(100), NOW.plusSeconds(60), null);
        final Request newerPriority = requestAt(2, 5, NOW.minusSeconds(1), NOW.plusSeconds(60), null);
        final ArenaAvailability arena = arena(1, 8, 0, true, true, true, true, false, 3L);
        final FairCapacityPolicy policy = new FairCapacityPolicy();
        final List<MatchmakingFramework.Decision> first = policy.match(Arrays.asList(newerPriority, old),
                Collections.singleton(arena), NOW, Duration.ofSeconds(10));
        final List<MatchmakingFramework.Decision> second = policy.match(Arrays.asList(old, newerPriority),
                Collections.singleton(arena), NOW, Duration.ofSeconds(10));
        assertEquals(first.get(0).request().idempotencyKey(), second.get(0).request().idempotencyKey());
        assertEquals(old.idempotencyKey(), first.get(0).request().idempotencyKey());
        assertEquals("capacity-fit", first.get(0).reason());
    }

    @Test void policyNeverSplitsPartyOrAssignsActorTwice() {
        final Request party = request(1, 0, NOW.plusSeconds(60), party(1, 4, 1L));
        final ArenaAvailability small = arena(1, 8, 0, true, true, true, true, false, 3L);
        final List<MatchmakingFramework.Decision> decisions = new FairCapacityPolicy().match(
                Collections.singleton(party), Collections.singleton(small), NOW, Duration.ofSeconds(5));
        assertTrue(decisions.isEmpty());
    }

    @Test void reservationPreventsOverbookingAndIsIdempotent() {
        final ReservationService service = reservations();
        final ArenaAvailability arena = arena(1, 4, 2, true, true, true, true, false, 9L);
        final Request first = request(1, 0, NOW.plusSeconds(60), null);
        final Reservation reservation = service.acquire(first, arena).get();
        assertEquals(reservation.id(), service.acquire(first, arena).get().id());
        assertFalse(service.acquire(request(2, 0, NOW.plusSeconds(60), party(2, 2, 1L)), arena).isPresent());
        assertEquals(1L, service.rejected());
    }

    @Test void reservationConfirmationIsRevisionBoundAndSingleUse() {
        final ReservationService service = reservations();
        final ArenaAvailability arena = arena(1, 8, 0, true, true, true, true, false, 9L);
        final Reservation reservation = service.acquire(request(1, 0, NOW.plusSeconds(60), null), arena).get();
        assertThrows(IllegalStateException.class, () -> service.confirm(reservation.id(), 8L));
        assertEquals(MatchmakingFramework.ReservationState.CONFIRMED, service.confirm(reservation.id(), 9L).state());
        assertThrows(IllegalStateException.class, () -> service.confirm(reservation.id(), 9L));
        assertTrue(service.release(reservation.id()));
        assertFalse(service.release(reservation.id()));
    }

    @Test void unavailableArenaNeverReceivesReservation() {
        final Request request = request(1, 0, NOW.plusSeconds(60), null);
        assertFalse(reservations().acquire(request, arena(1, 8, 0, false, true, true, true, false, 1L)).isPresent());
        assertFalse(reservations().acquire(request, arena(1, 8, 0, true, false, true, true, false, 1L)).isPresent());
        assertFalse(reservations().acquire(request, arena(1, 8, 0, true, true, false, true, false, 1L)).isPresent());
        assertFalse(reservations().acquire(request, arena(1, 8, 0, true, true, true, false, false, 1L)).isPresent());
        assertFalse(reservations().acquire(request, arena(1, 8, 0, true, true, true, true, true, 1L)).isPresent());
    }

    @Test void malformedRequestsAndLimitsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new Limits(0, 1, 1, Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> requestFor(player(1), 1, NOW, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ReservationService(0, Duration.ofSeconds(1), TimeSource.FixedTimeSource.at(NOW)));
    }

    private static QueueService service() {
        return new QueueService(new Limits(8, 32, 256, Duration.ofMinutes(2), Duration.ofSeconds(10)),
                TimeSource.FixedTimeSource.at(NOW));
    }
    private static ReservationService reservations() {
        return new ReservationService(32, Duration.ofSeconds(30), TimeSource.FixedTimeSource.at(NOW));
    }
    private static Request request(final int seed, final int priority, final Instant deadline, final Party party) {
        return requestAt(seed, priority, NOW, deadline, party);
    }
    private static Request requestAt(final int seed, final int priority, final Instant enqueued,
                                     final Instant deadline, final Party party) {
        final PlayerId actor = party == null ? player(seed) : party.leader();
        return new Request(QUEUE, actor, party, MODE, LAYOUT, party == null ? 1 : party.size(),
                Collections.<ArenaId>emptySet(), Collections.<DefinitionId>emptyList(), "en_GB", "local",
                priority, enqueued, 0L, new UUID(seed, 99L), deadline,
                IdempotencyKey.of("m10", "request-" + seed), CorrelationId.of(new UUID(seed, 100L)));
    }
    private static Request requestFor(final PlayerId actor, final int seed, final Instant deadline,
                                      final Party party, final int teamSize) {
        return new Request(QUEUE, actor, party, MODE, LAYOUT, teamSize,
                Collections.<ArenaId>emptySet(), Collections.<DefinitionId>emptyList(), "en_GB", "local",
                0, NOW, 0L, new UUID(seed, 99L), deadline,
                IdempotencyKey.of("m10", "request-special-" + seed), CorrelationId.of(new UUID(seed, 100L)));
    }
    private static Party party(final int seed, final int size, final long revision) {
        final List<PlayerId> members = new java.util.ArrayList<PlayerId>();
        for (int value = 0; value < size; value++) { members.add(player(seed * 10 + value)); }
        return new Party(PartyId.of(new UUID(0L, seed)), members.get(0), members, revision);
    }
    private static PlayerId player(final int seed) { return PlayerId.of(new UUID(0L, seed)); }
    private static ArenaAvailability arena(final int seed, final int total, final int occupied,
                                           final boolean enabled, final boolean healthy,
                                           final boolean worldReady, final boolean joinable,
                                           final boolean recovering, final long revision) {
        return new ArenaAvailability(ArenaId.of(new UUID(1L, seed)), revision, MODE, LAYOUT,
                Math.min(2, total), total, occupied, enabled, healthy, worldReady, joinable, recovering);
    }
}
