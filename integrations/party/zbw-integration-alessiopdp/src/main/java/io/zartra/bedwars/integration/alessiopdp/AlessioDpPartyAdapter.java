package io.zartra.bedwars.integration.alessiopdp;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.party.PartyIntent;
import io.zartra.bedwars.api.integration.party.PartyProvider;
import io.zartra.bedwars.api.integration.party.PartySnapshot;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Migration-only AlessioDP Parties adapter.
 *
 * <p>Native SQL remains authoritative. Mutation is accepted only for a pre-authorized TRANSFER
 * operation, so native and external providers can never be live writers simultaneously.</p>
 */
public final class AlessioDpPartyAdapter implements PartyProvider {
    private final Gateway gateway;
    private final OptionalProviderLifecycle lifecycle;
    private final TimeSource timeSource;

    /** @param gateway external party migration boundary @param probe availability
     * @param timeSource deadline and health clock */
    public AlessioDpPartyAdapter(final Gateway gateway,
                                 final OptionalProviderLifecycle.Probe probe,
                                 final TimeSource timeSource) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        lifecycle = new OptionalProviderLifecycle(
                ProviderId.of("zartra", "alessiodp-parties"),
                SemanticVersion.parse("3.2.17"),
                CapabilitySet.of(Collections.singletonList(
                        CapabilityId.of("zartra", "party-migration"))),
                timeSource, "provider.alessiodp", probe);
    }

    @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
    @Override public Health health() { return lifecycle.health(); }
    @Override public CompletionStage<Result<LifecycleState>> start() { return lifecycle.start(); }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        return lifecycle.drain(deadline);
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() { return lifecycle.stop(); }

    @Override
    public CompletionStage<Result<Optional<PartySnapshot>>> find(final PartyId partyId) {
        Objects.requireNonNull(partyId, "partyId");
        if (!lifecycle.available()) { return unavailable(); }
        return gateway.find(partyId).handle((value, failure) ->
                failure == null ? Result.success(value) : failure());
    }

    @Override
    public CompletionStage<Result<Optional<PartySnapshot>>> findByMember(
            final PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (!lifecycle.available()) { return unavailable(); }
        return gateway.findByMember(playerId).handle((value, failure) ->
                failure == null ? Result.success(value) : failure());
    }

    @Override
    public CompletionStage<Result<PartySnapshot>> execute(final PartyIntent intent) {
        Objects.requireNonNull(intent, "intent");
        if (!lifecycle.available()) { return unavailable(); }
        if (intent.action() != PartyIntent.Action.TRANSFER
                || !intent.deadline().isAfter(timeSource.now())
                || !gateway.migrationAuthorized(intent)) {
            return CompletableFuture.completedFuture(Result.failure(ApiError.of(
                    DefinitionId.of("zartra", "provider/party-migration-denied"),
                    "provider.party_migration_denied",
                    ApiError.RetryDisposition.FORBIDDEN)));
        }
        return gateway.migrate(intent).handle((value, failure) ->
                failure == null ? Result.success(value) : failure());
    }

    private static <T> CompletionStage<Result<T>> unavailable() {
        return CompletableFuture.completedFuture(failure());
    }
    private static <T> Result<T> failure() {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "provider/alessiodp-unavailable"),
                "provider.alessiodp_unavailable", ApiError.RetryDisposition.RETRYABLE));
    }

    /** Narrow operator runtime binding; it cannot bypass native migration authorization. */
    public interface Gateway {
        /** Reads an external migration projection. */
        CompletionStage<Optional<PartySnapshot>> find(PartyId partyId);
        /** Reads an external migration projection by member. */
        CompletionStage<Optional<PartySnapshot>> findByMember(PlayerId playerId);
        /** Verifies that native SQL recorded the matching migration fence. */
        boolean migrationAuthorized(PartyIntent intent);
        /** Performs one idempotent, fenced migration operation. */
        CompletionStage<PartySnapshot> migrate(PartyIntent intent);
    }
}
