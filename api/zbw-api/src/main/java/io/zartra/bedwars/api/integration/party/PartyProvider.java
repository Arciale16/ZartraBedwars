package io.zartra.bedwars.api.integration.party;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Native or external party provider boundary. */
public interface PartyProvider extends Provider {
    /**
     * Finds a party by identity.
     *
     * @param partyId party identity
     * @return asynchronous optional projection
     */
    CompletionStage<Result<Optional<PartySnapshot>>> find(PartyId partyId);

    /**
     * Finds the party containing a player.
     *
     * @param playerId player identity
     * @return asynchronous optional projection
     */
    CompletionStage<Result<Optional<PartySnapshot>>> findByMember(PlayerId playerId);

    /**
     * Executes one idempotent party intent.
     *
     * @param intent immutable intent
     * @return asynchronous resulting projection
     */
    CompletionStage<Result<PartySnapshot>> execute(PartyIntent intent);
}
