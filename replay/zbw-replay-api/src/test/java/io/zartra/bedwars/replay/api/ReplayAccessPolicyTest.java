package io.zartra.bedwars.replay.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-007/010 privacy and access regression tests. */
class ReplayAccessPolicyTest {
    @Test void accessIsParticipantOrAuthorizedStaffAndProtectedEvidenceIsRestricted() {
        final ReplayAccessPolicy policy = new ReplayAccessPolicy();
        final PlayerId participant = PlayerId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final PlayerId stranger = PlayerId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertTrue(policy.mayView(ReplayModelTest.metadata(false), participant,
                ReplayAccessPolicy.Purpose.PERSONAL_HISTORY, false));
        assertFalse(policy.mayView(ReplayModelTest.metadata(false), stranger,
                ReplayAccessPolicy.Purpose.PERSONAL_HISTORY, false));
        assertTrue(policy.mayView(ReplayModelTest.metadata(true), stranger,
                ReplayAccessPolicy.Purpose.STAFF_EVIDENCE, true));
        assertFalse(policy.mayView(ReplayModelTest.metadata(true), participant,
                ReplayAccessPolicy.Purpose.PERSONAL_HISTORY, false));
        assertFalse(policy.mayView(ReplayModelTest.metadata(false), participant,
                ReplayAccessPolicy.Purpose.PUBLIC_SHARE, false));
        assertTrue(policy.mayRevealIdentity(participant, participant, false));
        assertFalse(policy.mayRevealIdentity(stranger, participant, false));
        assertTrue(policy.mayRevealIdentity(stranger, participant, true));
        assertFalse(policy.mayRecordChatPayload());
    }
}
