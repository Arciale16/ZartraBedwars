package io.zartra.bedwars.party;

import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Objects;

/** Immutable expiring invitation. */
public final class PartyInvitation {
    private final PlayerId invitedBy;
    private final PlayerId invitee;
    private final Instant createdAt;
    private final Instant expiresAt;

    /**
     * Creates an invitation.
     *
     * @param invitedBy inviter identity
     * @param invitee invited player
     * @param createdAt creation timestamp
     * @param expiresAt strict expiry after creation
     */
    public PartyInvitation(final PlayerId invitedBy, final PlayerId invitee,
                           final Instant createdAt, final Instant expiresAt) {
        this.invitedBy = Objects.requireNonNull(invitedBy, "invitedBy");
        this.invitee = Objects.requireNonNull(invitee, "invitee");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (invitedBy.equals(invitee) || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("invalid invitation identities or expiry");
        }
    }

    /** @return inviter identity */
    public PlayerId invitedBy() { return invitedBy; }
    /** @return invited player */
    public PlayerId invitee() { return invitee; }
    /** @return creation timestamp */
    public Instant createdAt() { return createdAt; }
    /** @return expiry timestamp */
    public Instant expiresAt() { return expiresAt; }
    /** @param instant comparison instant @return whether the invitation has expired */
    public boolean expiredAt(final Instant instant) {
        return !Objects.requireNonNull(instant, "instant").isBefore(expiresAt);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof PartyInvitation)) { return false; }
        PartyInvitation that = (PartyInvitation) other;
        return invitedBy.equals(that.invitedBy) && invitee.equals(that.invitee)
                && createdAt.equals(that.createdAt) && expiresAt.equals(that.expiresAt);
    }

    @Override public int hashCode() { return Objects.hash(invitedBy, invitee, createdAt, expiresAt); }
}
