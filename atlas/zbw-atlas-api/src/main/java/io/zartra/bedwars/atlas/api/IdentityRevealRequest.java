package io.zartra.bedwars.atlas.api;

import java.util.Objects;

/** Auditable result of an external authorization decision for identity reveal. */
public final class IdentityRevealRequest {
    private final AtlasCaseId caseId;
    private final AtlasReviewerId requesterId;
    private final String authorizationReference;
    private final boolean authorized;

    private IdentityRevealRequest(final AtlasCaseId caseId, final AtlasReviewerId requesterId,
                                  final String authorizationReference, final boolean authorized) {
        if (authorizationReference == null || authorizationReference.trim().isEmpty()
                || authorizationReference.length() > 160
                || !authorizationReference.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("authorizationReference must be an opaque token");
        }
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId");
        this.authorizationReference = authorizationReference;
        this.authorized = authorized;
    }

    /** Records an explicitly authorized reveal decision. */
    public static IdentityRevealRequest authorized(final AtlasCaseId caseId,
                                                   final AtlasReviewerId requesterId,
                                                   final String authorizationReference) {
        return new IdentityRevealRequest(caseId, requesterId, authorizationReference, true);
    }

    /** Records a denied reveal decision for audit without exposing identity. */
    public static IdentityRevealRequest denied(final AtlasCaseId caseId,
                                               final AtlasReviewerId requesterId,
                                               final String authorizationReference) {
        return new IdentityRevealRequest(caseId, requesterId, authorizationReference, false);
    }

    /** Returns target case identity. */ public AtlasCaseId caseId() { return caseId; }
    /** Returns authorized caller identity. */ public AtlasReviewerId requesterId() { return requesterId; }
    /** Returns opaque authorization/audit decision reference. */
    public String authorizationReference() { return authorizationReference; }
    /** Returns whether the external policy explicitly granted reveal. */
    public boolean authorized() { return authorized; }
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdentityRevealRequest)) {
            return false;
        }
        IdentityRevealRequest that = (IdentityRevealRequest) other;
        return authorized == that.authorized
                && caseId.equals(that.caseId)
                && requesterId.equals(that.requesterId)
                && authorizationReference.equals(that.authorizationReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, requesterId, authorizationReference, authorized);
    }
}
