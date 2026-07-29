package io.zartra.bedwars.party;

import io.zartra.bedwars.api.identity.ProviderId;
import java.util.Objects;

/** Explicit migration guard preventing concurrent native/external party authority. */
public final class PartyMigrationPolicy {
    /**
     * Validates a migration start.
     *
     * @param party current party
     * @param targetProvider external target
     * @return target provider when migration may begin
     */
    public ProviderId validateStart(final Party party, final ProviderId targetProvider) {
        Objects.requireNonNull(party, "party");
        Objects.requireNonNull(targetProvider, "targetProvider");
        if (party.state() != PartyState.ACTIVE || party.migrationTarget().isPresent()) {
            throw new IllegalStateException("only one active party migration is allowed");
        }
        return targetProvider;
    }

    /**
     * Validates migration completion.
     *
     * @param party migrating party
     */
    public void validateCompletion(final Party party) {
        Objects.requireNonNull(party, "party");
        if (party.state() != PartyState.MIGRATING || !party.migrationTarget().isPresent()) {
            throw new IllegalStateException("party has no migration in progress");
        }
    }
}
