package io.zartra.bedwars.party;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable transport intent consumed by the M20 proxy boundary. */
public final class PartyTransferIntent {
    private final PartyId partyId;
    private final List<PlayerId> members;
    private final String destination;
    private final long partyVersion;
    private final Instant deadline;

    /**
     * Creates a transfer intent.
     *
     * @param partyId party identity
     * @param members immutable member source
     * @param destination sanitized destination
     * @param partyVersion authoritative party version
     * @param deadline transfer deadline
     */
    public PartyTransferIntent(final PartyId partyId, final List<PlayerId> members,
                               final String destination, final long partyVersion,
                               final Instant deadline) {
        this.partyId = Objects.requireNonNull(partyId, "partyId");
        Objects.requireNonNull(members, "members");
        if (members.isEmpty() || destination == null
                || !destination.matches("[a-z0-9][a-z0-9_.:-]{0,127}")
                || partyVersion < 0) {
            throw new IllegalArgumentException("invalid transfer intent");
        }
        this.members = Collections.unmodifiableList(new ArrayList<PlayerId>(members));
        this.destination = destination;
        this.partyVersion = partyVersion;
        this.deadline = Objects.requireNonNull(deadline, "deadline");
    }

    /** @return party identity */
    public PartyId partyId() { return partyId; }
    /** @return immutable party members */
    public List<PlayerId> members() { return members; }
    /** @return destination identifier */
    public String destination() { return destination; }
    /** @return party version */
    public long partyVersion() { return partyVersion; }
    /** @return transfer deadline */
    public Instant deadline() { return deadline; }
}
