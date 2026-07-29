package io.zartra.bedwars.api.integration.party;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable privacy-filtered party projection. */
public final class PartySnapshot {
    private static final int MAX_MEMBERS = 64;
    private final PartyId partyId;
    private final PlayerId leaderId;
    private final List<PlayerId> members;
    private final Visibility visibility;
    private final long version;

    /**
     * Creates a party projection.
     *
     * @param partyId party identity
     * @param leaderId leader identity
     * @param members ordered members including the leader
     * @param visibility privacy projection
     * @param version non-negative authoritative version
     */
    public PartySnapshot(final PartyId partyId, final PlayerId leaderId,
                         final List<PlayerId> members, final Visibility visibility,
                         final long version) {
        this.partyId = Objects.requireNonNull(partyId, "partyId");
        this.leaderId = Objects.requireNonNull(leaderId, "leaderId");
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(members, "members");
        Set<PlayerId> unique = new LinkedHashSet<PlayerId>(members);
        if (members.isEmpty() || members.size() > MAX_MEMBERS || unique.size() != members.size()
                || !unique.contains(leaderId) || version < 0) {
            throw new IllegalArgumentException("invalid party projection");
        }
        this.members = Collections.unmodifiableList(new ArrayList<PlayerId>(members));
        this.version = version;
    }

    /** @return party identity */
    public PartyId partyId() { return partyId; }
    /** @return leader identity */
    public PlayerId leaderId() { return leaderId; }
    /** @return immutable ordered member identities */
    public List<PlayerId> members() { return members; }
    /** @return privacy projection */
    public Visibility visibility() { return visibility; }
    /** @return authoritative party version */
    public long version() { return version; }

    /** Privacy-safe party visibility. */
    public enum Visibility { PUBLIC, MEMBERS_ONLY, PRIVATE }
}
