package io.zartra.bedwars.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PartyLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test
    void lifecycleInvitationPrivacyAndTransferAreDeterministic() {
        PlayerId leader = player(1);
        PlayerId member = player(2);
        Party created = Party.create(PartyId.of(uuid(10)), leader);
        assertEquals(PartyState.CREATED, created.state());
        assertThrows(IllegalStateException.class, () ->
                created.invite(leader, member, NOW, NOW.plusSeconds(30)));

        Party active = created.activate().withPrivacy(PartyPrivacy.CLOSED);
        Party invited = active.invite(leader, member, NOW, NOW.plusSeconds(30));
        assertThrows(IllegalStateException.class, () ->
                invited.invite(leader, member, NOW, NOW.plusSeconds(30)));
        Party accepted = invited.accept(member, NOW.plusSeconds(5));

        assertEquals(PartyState.ACTIVE, accepted.state());
        assertEquals(2, accepted.members().size());
        assertEquals(PartyPrivacy.CLOSED, accepted.privacy());
        assertEquals(accepted.revision(), accepted.transfer("arena_1", NOW.plusSeconds(15))
                .partyVersion());
        assertThrows(UnsupportedOperationException.class,
                () -> accepted.members().add(player(3)));
    }

    @Test
    void expiredInvitationCannotBeAcceptedAndCanBePurged() {
        PlayerId leader = player(1);
        PlayerId invited = player(2);
        Party invitedParty = Party.create(PartyId.of(uuid(11)), leader).activate()
                .invite(leader, invited, NOW, NOW.plusSeconds(1));
        assertThrows(IllegalStateException.class,
                () -> invitedParty.accept(invited, NOW.plusSeconds(1)));
        Party expired = invitedParty.expireInvitations(NOW.plusSeconds(1));
        assertTrue(expired.invitations().isEmpty());
    }

    @Test
    void migrationIsExclusiveAndRollbackOrCompletionPreservesAuthority() {
        PartyMigrationPolicy policy = new PartyMigrationPolicy();
        Party active = Party.create(PartyId.of(uuid(12)), player(1)).activate();
        Party migrating =
                active.beginMigration(ProviderId.of("parties", "alessiodp"), policy);
        assertEquals(PartyState.MIGRATING, migrating.state());
        assertTrue(migrating.migrationTarget().isPresent());
        assertThrows(IllegalStateException.class, () ->
                migrating.beginMigration(ProviderId.of("other", "provider"), policy));

        Party restored = migrating.rollbackMigration(policy);
        assertEquals(PartyState.ACTIVE, restored.state());
        assertFalse(restored.migrationTarget().isPresent());

        Party completed = restored
                .beginMigration(ProviderId.of("parties", "alessiodp"), policy)
                .completeMigration(policy);
        assertEquals(PartyState.DISBANDED, completed.state());
        assertThrows(IllegalStateException.class, completed::activate);
    }

    @Test
    void leaderAndMembershipConflictsAreRejected() {
        PlayerId leader = player(1);
        PlayerId member = player(2);
        Party party = Party.create(PartyId.of(uuid(13)), leader).activate()
                .invite(leader, member, NOW, NOW.plusSeconds(30))
                .accept(member, NOW.plusSeconds(1));
        assertThrows(IllegalStateException.class, () -> party.removeMember(leader));
        Party promoted = party.promote(leader, member);
        assertEquals(member, promoted.leaderId());
        assertThrows(IllegalStateException.class, () -> promoted.promote(leader, leader));
    }

    private static PlayerId player(final int value) { return PlayerId.of(uuid(value)); }

    private static UUID uuid(final int value) { return new UUID(0, value); }
}
