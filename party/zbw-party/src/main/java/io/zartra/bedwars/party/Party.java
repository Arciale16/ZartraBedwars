package io.zartra.bedwars.party;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.party.PartySnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable SQL-authoritative native party aggregate. */
public final class Party {
    private static final int MAX_MEMBERS = 64;
    private static final int MAX_INVITATIONS = 128;
    private final PartyId partyId;
    private final PartyState state;
    private final PlayerId leaderId;
    private final List<PlayerId> members;
    private final List<PartyInvitation> invitations;
    private final PartyPrivacy privacy;
    private final ProviderId migrationTarget;
    private final long revision;

    /**
     * Reconstructs a validated party aggregate.
     *
     * @param partyId party identity
     * @param state lifecycle state
     * @param leaderId leader identity
     * @param members ordered unique members
     * @param invitations unique pending invitations
     * @param privacy admission privacy
     * @param migrationTarget target only while migrating
     * @param revision non-negative revision
     */
    public Party(final PartyId partyId, final PartyState state, final PlayerId leaderId,
                 final List<PlayerId> members, final List<PartyInvitation> invitations,
                 final PartyPrivacy privacy, final ProviderId migrationTarget,
                 final long revision) {
        this.partyId = Objects.requireNonNull(partyId, "partyId");
        this.state = Objects.requireNonNull(state, "state");
        this.leaderId = Objects.requireNonNull(leaderId, "leaderId");
        this.privacy = Objects.requireNonNull(privacy, "privacy");
        Objects.requireNonNull(members, "members");
        Objects.requireNonNull(invitations, "invitations");
        Set<PlayerId> uniqueMembers = new LinkedHashSet<PlayerId>(members);
        Set<PlayerId> invitedPlayers = new LinkedHashSet<PlayerId>();
        for (PartyInvitation invitation : invitations) {
            if (!invitedPlayers.add(Objects.requireNonNull(invitation, "invitation").invitee())
                    || uniqueMembers.contains(invitation.invitee())) {
                throw new IllegalArgumentException("duplicate or existing invited member");
            }
        }
        if (members.isEmpty() || members.size() > MAX_MEMBERS
                || uniqueMembers.size() != members.size() || !uniqueMembers.contains(leaderId)
                || invitations.size() > MAX_INVITATIONS || revision < 0) {
            throw new IllegalArgumentException("invalid party membership or revision");
        }
        if ((state == PartyState.MIGRATING) != (migrationTarget != null)) {
            throw new IllegalArgumentException("migration target must match migrating state");
        }
        this.members = Collections.unmodifiableList(new ArrayList<PlayerId>(members));
        this.invitations =
                Collections.unmodifiableList(new ArrayList<PartyInvitation>(invitations));
        this.migrationTarget = migrationTarget;
        this.revision = revision;
    }

    /** @param partyId new party identity @param leaderId initial leader @return created aggregate */
    public static Party create(final PartyId partyId, final PlayerId leaderId) {
        return new Party(partyId, PartyState.CREATED, leaderId,
                Collections.singletonList(leaderId), Collections.<PartyInvitation>emptyList(),
                PartyPrivacy.INVITE_ONLY, null, 0);
    }

    /** @return activated aggregate */
    public Party activate() {
        requireState(PartyState.CREATED);
        return copy(PartyState.ACTIVE, leaderId, members, invitations, privacy, null);
    }

    /** @param value new privacy @return updated aggregate */
    public Party withPrivacy(final PartyPrivacy value) {
        requireMutable();
        return copy(state, leaderId, members, invitations,
                Objects.requireNonNull(value, "value"), migrationTarget);
    }

    /**
     * Adds a pending invitation.
     *
     * @param invitedBy inviter who must be a member
     * @param invitee invited non-member
     * @param now current time
     * @param expiresAt invitation expiry
     * @return updated aggregate
     */
    public Party invite(final PlayerId invitedBy, final PlayerId invitee,
                        final Instant now, final Instant expiresAt) {
        requireState(PartyState.ACTIVE);
        Objects.requireNonNull(invitedBy, "invitedBy");
        Objects.requireNonNull(invitee, "invitee");
        if (!members.contains(invitedBy) || members.contains(invitee)
                || invitation(invitee).isPresent() || invitations.size() >= MAX_INVITATIONS) {
            throw new IllegalStateException("invitation conflicts with party state");
        }
        List<PartyInvitation> updated = new ArrayList<PartyInvitation>(invitations);
        updated.add(new PartyInvitation(invitedBy, invitee, now, expiresAt));
        return copy(state, leaderId, members, updated, privacy, migrationTarget);
    }

    /**
     * Accepts a non-expired invitation.
     *
     * @param playerId invited player
     * @param now current time
     * @return updated aggregate
     */
    public Party accept(final PlayerId playerId, final Instant now) {
        requireState(PartyState.ACTIVE);
        PartyInvitation pending = invitation(playerId)
                .orElseThrow(() -> new IllegalStateException("invitation not found"));
        if (pending.expiredAt(now) || members.size() >= MAX_MEMBERS) {
            throw new IllegalStateException("invitation expired or party full");
        }
        List<PlayerId> updatedMembers = new ArrayList<PlayerId>(members);
        updatedMembers.add(playerId);
        return copy(state, leaderId, updatedMembers, withoutInvitation(playerId),
                privacy, migrationTarget);
    }

    /** @param now current time @return aggregate without expired invitations */
    public Party expireInvitations(final Instant now) {
        Objects.requireNonNull(now, "now");
        List<PartyInvitation> updated = new ArrayList<PartyInvitation>();
        for (PartyInvitation invitation : invitations) {
            if (!invitation.expiredAt(now)) { updated.add(invitation); }
        }
        return updated.size() == invitations.size() ? this
                : copy(state, leaderId, members, updated, privacy, migrationTarget);
    }

    /** @param memberId departing non-leader member @return updated aggregate */
    public Party removeMember(final PlayerId memberId) {
        requireState(PartyState.ACTIVE);
        if (leaderId.equals(memberId) || !members.contains(memberId)) {
            throw new IllegalStateException("leader cannot leave and member must exist");
        }
        List<PlayerId> updated = new ArrayList<PlayerId>(members);
        updated.remove(memberId);
        return copy(state, leaderId, updated, invitations, privacy, migrationTarget);
    }

    /** @param actorId current leader @param newLeaderId member to promote @return updated aggregate */
    public Party promote(final PlayerId actorId, final PlayerId newLeaderId) {
        requireState(PartyState.ACTIVE);
        if (!leaderId.equals(actorId) || !members.contains(newLeaderId)
                || leaderId.equals(newLeaderId)) {
            throw new IllegalStateException("invalid leader promotion");
        }
        return copy(state, newLeaderId, members, invitations, privacy, migrationTarget);
    }

    /**
     * Starts an explicitly guarded provider migration.
     *
     * @param targetProvider destination provider
     * @param policy migration policy
     * @return migrating aggregate
     */
    public Party beginMigration(final ProviderId targetProvider,
                                final PartyMigrationPolicy policy) {
        ProviderId target = Objects.requireNonNull(policy, "policy")
                .validateStart(this, targetProvider);
        return copy(PartyState.MIGRATING, leaderId, members, invitations, privacy, target);
    }

    /** @param policy migration policy @return native authority restored after a failed migration */
    public Party rollbackMigration(final PartyMigrationPolicy policy) {
        Objects.requireNonNull(policy, "policy").validateCompletion(this);
        return copy(PartyState.ACTIVE, leaderId, members, invitations, privacy, null);
    }

    /** @param policy migration policy @return disbanded native aggregate after external commit */
    public Party completeMigration(final PartyMigrationPolicy policy) {
        Objects.requireNonNull(policy, "policy").validateCompletion(this);
        return copy(PartyState.DISBANDED, leaderId, members,
                Collections.<PartyInvitation>emptyList(), privacy, null);
    }

    /** @return disbanded aggregate */
    public Party disband() {
        requireMutable();
        return copy(PartyState.DISBANDED, leaderId, members,
                Collections.<PartyInvitation>emptyList(), privacy, null);
    }

    /**
     * Creates a transport-only intent.
     *
     * @param destination proxy destination
     * @param deadline transfer deadline
     * @return immutable transfer intent
     */
    public PartyTransferIntent transfer(final String destination, final Instant deadline) {
        requireState(PartyState.ACTIVE);
        return new PartyTransferIntent(partyId, members, destination, revision, deadline);
    }

    /** @return provider-neutral privacy-filtered snapshot */
    public PartySnapshot snapshot() {
        PartySnapshot.Visibility visibility = privacy == PartyPrivacy.OPEN
                ? PartySnapshot.Visibility.PUBLIC
                : privacy == PartyPrivacy.CLOSED
                ? PartySnapshot.Visibility.PRIVATE : PartySnapshot.Visibility.MEMBERS_ONLY;
        return new PartySnapshot(partyId, leaderId, members, visibility, revision);
    }

    private Party copy(final PartyState nextState, final PlayerId nextLeader,
                       final List<PlayerId> nextMembers,
                       final List<PartyInvitation> nextInvitations,
                       final PartyPrivacy nextPrivacy, final ProviderId nextTarget) {
        return new Party(partyId, nextState, nextLeader, nextMembers, nextInvitations,
                nextPrivacy, nextTarget, Math.addExact(revision, 1));
    }

    private List<PartyInvitation> withoutInvitation(final PlayerId playerId) {
        List<PartyInvitation> updated = new ArrayList<PartyInvitation>(invitations);
        updated.removeIf(value -> value.invitee().equals(playerId));
        return updated;
    }

    private Optional<PartyInvitation> invitation(final PlayerId playerId) {
        for (PartyInvitation value : invitations) {
            if (value.invitee().equals(playerId)) { return Optional.of(value); }
        }
        return Optional.empty();
    }

    private void requireMutable() {
        if (state == PartyState.DISBANDED || state == PartyState.MIGRATING) {
            throw new IllegalStateException("party is not mutable");
        }
    }

    private void requireState(final PartyState required) {
        if (state != required) {
            throw new IllegalStateException("expected " + required + " but was " + state);
        }
    }

    /** @return party identity */
    public PartyId partyId() { return partyId; }
    /** @return lifecycle state */
    public PartyState state() { return state; }
    /** @return leader identity */
    public PlayerId leaderId() { return leaderId; }
    /** @return immutable ordered members */
    public List<PlayerId> members() { return members; }
    /** @return immutable pending invitations */
    public List<PartyInvitation> invitations() { return invitations; }
    /** @return party privacy */
    public PartyPrivacy privacy() { return privacy; }
    /** @return optional external migration target */
    public Optional<ProviderId> migrationTarget() { return Optional.ofNullable(migrationTarget); }
    /** @return aggregate revision */
    public long revision() { return revision; }
}
