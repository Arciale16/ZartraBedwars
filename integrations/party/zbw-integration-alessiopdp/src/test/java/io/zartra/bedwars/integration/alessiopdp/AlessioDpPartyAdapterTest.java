package io.zartra.bedwars.integration.alessiopdp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.party.PartyIntent;
import io.zartra.bedwars.api.integration.party.PartySnapshot;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Native-authority and migration-fencing tests for AlessioDP Parties. */
final class AlessioDpPartyAdapterTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");
    private static final PartyId PARTY = PartyId.of(new UUID(5, 6));
    private static final PlayerId PLAYER = PlayerId.of(new UUID(7, 8));
    private static final PartySnapshot SNAPSHOT = new PartySnapshot(PARTY, PLAYER,
            Collections.singletonList(PLAYER), PartySnapshot.Visibility.PRIVATE, 3);

    @Test void rejectsLiveMutationAndRequiresNativeMigrationFence() {
        AlessioDpPartyAdapter denied = adapter(false);
        denied.start().toCompletableFuture().join();
        PartyIntent invite = intent(PartyIntent.Action.INVITE);
        assertFalse(denied.execute(invite).toCompletableFuture().join().isSuccess());
        assertFalse(denied.execute(intent(PartyIntent.Action.TRANSFER))
                .toCompletableFuture().join().isSuccess());

        AlessioDpPartyAdapter allowed = adapter(true);
        allowed.start().toCompletableFuture().join();
        assertTrue(allowed.execute(intent(PartyIntent.Action.TRANSFER))
                .toCompletableFuture().join().isSuccess());
    }

    private static PartyIntent intent(final PartyIntent.Action action) {
        return new PartyIntent(IdempotencyKey.of("test", "migration"), action,
                PARTY, PLAYER, PLAYER, "external", NOW.plusSeconds(5));
    }

    private static AlessioDpPartyAdapter adapter(final boolean authorized) {
        return new AlessioDpPartyAdapter(new AlessioDpPartyAdapter.Gateway() {
            @Override public CompletableFuture<Optional<PartySnapshot>> find(
                    final PartyId partyId) {
                return CompletableFuture.completedFuture(Optional.of(SNAPSHOT));
            }
            @Override public CompletableFuture<Optional<PartySnapshot>> findByMember(
                    final PlayerId playerId) {
                return CompletableFuture.completedFuture(Optional.of(SNAPSHOT));
            }
            @Override public boolean migrationAuthorized(final PartyIntent intent) {
                return authorized;
            }
            @Override public CompletableFuture<PartySnapshot> migrate(
                    final PartyIntent intent) {
                return CompletableFuture.completedFuture(SNAPSHOT);
            }
        }, OptionalProviderLifecycle.Probe.AVAILABLE,
                TimeSource.FixedTimeSource.at(NOW));
    }
}
