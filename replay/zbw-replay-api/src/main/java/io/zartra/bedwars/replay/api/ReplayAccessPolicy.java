package io.zartra.bedwars.replay.api;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;

/** Privacy-by-default replay access rules (ZBW-REPLAY-007, ZBW-REPLAY-010). */
public final class ReplayAccessPolicy {
    /** Access purpose supplied by an authorized adapter. */
    public enum Purpose { PERSONAL_HISTORY, STAFF_EVIDENCE, PUBLIC_SHARE }

    /** Returns whether the requester may read replay metadata/timeline. */
    public boolean mayView(final ReplayMetadata metadata, final PlayerId requester,
                           final Purpose purpose, final boolean authorizedStaff) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(purpose, "purpose");
        if (purpose == Purpose.STAFF_EVIDENCE) { return authorizedStaff; }
        if (metadata.protectedEvidence()) { return false; }
        return purpose == Purpose.PERSONAL_HISTORY && metadata.participants().contains(requester);
    }

    /** Full participant identity is limited to self or authorized staff evidence access. */
    public boolean mayRevealIdentity(final PlayerId requester, final PlayerId subject,
                                     final boolean authorizedStaff) {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(subject, "subject");
        return authorizedStaff || requester.equals(subject);
    }

    /** Chat payload recording is disabled by this foundation policy. */
    public boolean mayRecordChatPayload() { return false; }
}
