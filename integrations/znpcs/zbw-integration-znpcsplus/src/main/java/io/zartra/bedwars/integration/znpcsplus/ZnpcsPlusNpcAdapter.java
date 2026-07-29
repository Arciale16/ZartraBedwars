package io.zartra.bedwars.integration.znpcsplus;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.integration.npc.NpcProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.SemanticVersion;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Optional ZNPCsPlus presentation adapter; no NPC gameplay policy is duplicated. */
public final class ZnpcsPlusNpcAdapter implements NpcProvider {
    private final Gateway gateway;
    private final OptionalProviderLifecycle lifecycle;

    /** @param gateway ZNPCsPlus presentation boundary @param probe availability
     * @param timeSource observation clock */
    public ZnpcsPlusNpcAdapter(final Gateway gateway,
                               final OptionalProviderLifecycle.Probe probe,
                               final TimeSource timeSource) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        lifecycle = new OptionalProviderLifecycle(ProviderId.of("zartra", "znpcsplus"),
                SemanticVersion.parse("2.0.0"),
                CapabilitySet.of(Collections.singletonList(
                        CapabilityId.of("zartra", "npc"))),
                timeSource, "provider.znpcsplus", probe);
    }

    @Override public Descriptor descriptor() { return lifecycle.descriptor(); }
    @Override public Health health() { return lifecycle.health(); }
    @Override public CompletionStage<Result<LifecycleState>> start() { return lifecycle.start(); }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        return lifecycle.drain(deadline);
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() { return lifecycle.stop(); }
    @Override public CompletionStage<Result<Definition>> upsert(final Definition definition) {
        Objects.requireNonNull(definition, "definition");
        return available(() -> gateway.upsert(definition));
    }
    @Override public CompletionStage<Result<Boolean>> remove(final DefinitionId definitionId) {
        Objects.requireNonNull(definitionId, "definitionId");
        return available(() -> gateway.remove(definitionId));
    }
    @Override public CompletionStage<Result<Boolean>> render(
            final DefinitionId definitionId, final PlayerId viewerId) {
        Objects.requireNonNull(definitionId, "definitionId");
        Objects.requireNonNull(viewerId, "viewerId");
        return available(() -> gateway.render(definitionId, viewerId));
    }
    @Override public CompletionStage<Result<List<Definition>>> exportDefinitions() {
        return available(gateway::exportDefinitions);
    }

    private <T> CompletionStage<Result<T>> available(
            final Supplier<CompletionStage<T>> operation) {
        if (!lifecycle.available()) {
            return CompletableFuture.completedFuture(failure());
        }
        try {
            return operation.get().handle((value, error) ->
                    error == null ? Result.success(value) : failure());
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(failure());
        }
    }
    private static <T> Result<T> failure() {
        return Result.failure(ApiError.of(
                DefinitionId.of("zartra", "provider/znpcsplus-unavailable"),
                "provider.znpcsplus_unavailable", ApiError.RetryDisposition.RETRYABLE));
    }

    /** Narrow runtime binding to the operator-installed ZNPCsPlus API. */
    public interface Gateway {
        /** Upserts one presentation definition. */ CompletionStage<Definition> upsert(Definition value);
        /** Removes one presentation definition. */ CompletionStage<Boolean> remove(DefinitionId id);
        /** Renders one definition to one viewer. */
        CompletionStage<Boolean> render(DefinitionId id, PlayerId viewerId);
        /** Exports definitions for controlled provider migration. */
        CompletionStage<List<Definition>> exportDefinitions();
    }
}
