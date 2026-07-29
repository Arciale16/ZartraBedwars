package io.zartra.bedwars.party;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Asynchronous SQL-authoritative party repository port. */
public interface PartyRepository {
    /** @param party new aggregate @return false when the party or a member already exists */
    CompletionStage<Boolean> create(Party party);
    /**
     * Saves an aggregate using optimistic revision control.
     *
     * @param party updated aggregate
     * @param expectedRevision previously loaded revision
     * @return save outcome
     */
    CompletionStage<SaveResult> save(Party party, long expectedRevision);
    /** @param partyId party identity @return optional aggregate */
    CompletionStage<Optional<Party>> find(PartyId partyId);
    /** @param memberId member identity @return optional containing aggregate */
    CompletionStage<Optional<Party>> findByMember(PlayerId memberId);

    /** Optimistic save outcome. */
    enum SaveResult { UPDATED, CONFLICT, NOT_FOUND }
}
