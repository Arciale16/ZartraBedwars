package io.zartra.bedwars.paper.bootstrap;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.observability.doctor.ProviderCompatibilityCheck;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Nonblocking Paper composition lifecycle for optional provider adapters. */
public final class PaperProviderIntegrationRuntime {
    private static final Duration DRAIN_DEADLINE = Duration.ofSeconds(2);
    private final Map<ProviderId, Provider> providers =
            new LinkedHashMap<ProviderId, Provider>();
    private final Set<ProviderId> rejectedDuplicates = new TreeSet<ProviderId>();
    private boolean started;
    private boolean closed;

    /**
     * Installs one isolated adapter and starts it when the runtime is already active.
     *
     * @param provider optional provider adapter
     * @return asynchronous lifecycle result, or NEW before runtime start
     */
    public synchronized CompletionStage<Result<Provider.LifecycleState>> install(
            final Provider provider) {
        Objects.requireNonNull(provider, "provider");
        if (closed) { throw new IllegalStateException("provider runtime is closed"); }
        ProviderId id = provider.descriptor().id();
        if (providers.putIfAbsent(id, provider) != null) {
            rejectedDuplicates.add(id);
            throw new IllegalStateException("duplicate provider ID: " + id);
        }
        return started ? provider.start() : CompletableFuture.completedFuture(
                Result.success(Provider.LifecycleState.NEW));
    }

    /** @return asynchronous startup of all registered optional providers */
    public synchronized CompletionStage<List<Result<Provider.LifecycleState>>> start() {
        if (closed) { throw new IllegalStateException("provider runtime is closed"); }
        if (started) { throw new IllegalStateException("provider runtime already started"); }
        started = true;
        return collect(new ArrayList<Provider>(providers.values()), false);
    }

    /**
     * Drains then stops all providers in reverse installation order.
     *
     * @return asynchronous completion; no owner-thread wait is performed
     */
    public synchronized CompletionStage<Void> close() {
        if (closed) { return CompletableFuture.completedFuture(null); }
        closed = true;
        List<Provider> reverse = new ArrayList<Provider>(providers.values());
        Collections.reverse(reverse);
        CompletableFuture<?>[] futures = new CompletableFuture<?>[reverse.size()];
        for (int index = 0; index < reverse.size(); index++) {
            Provider provider = reverse.get(index);
            futures[index] = provider.drain(DRAIN_DEADLINE)
                    .handle((ignored, failure) -> null)
                    .thenCompose(ignored -> provider.stop())
                    .handle((ignored, failure) -> null).toCompletableFuture();
        }
        providers.clear();
        return CompletableFuture.allOf(futures);
    }

    /** @return immutable installed provider IDs in deterministic order */
    public synchronized List<ProviderId> providerIds() {
        return Collections.unmodifiableList(
                new ArrayList<ProviderId>(providers.keySet()));
    }

    /**
     * Creates the secret-safe M21 provider compatibility check for Plugin Doctor.
     *
     * @return deterministic compatibility check over the current provider registry
     */
    public synchronized ProviderCompatibilityCheck compatibilityCheck() {
        return new ProviderCompatibilityCheck(
                ProviderCompatibilityCheck.m21ProviderIds(),
                providers.values(), rejectedDuplicates);
    }
    private static CompletionStage<List<Result<Provider.LifecycleState>>> collect(
            final List<Provider> values, final boolean stopping) {
        List<CompletableFuture<Result<Provider.LifecycleState>>> futures =
                new ArrayList<CompletableFuture<Result<Provider.LifecycleState>>>();
        for (Provider provider : values) {
            CompletionStage<Result<Provider.LifecycleState>> operation =
                    stopping ? provider.stop() : provider.start();
            futures.add(operation.toCompletableFuture());
        }
        return CompletableFuture.allOf(
                futures.toArray(new CompletableFuture<?>[futures.size()]))
                .thenApply(ignored -> {
                    List<Result<Provider.LifecycleState>> results =
                            new ArrayList<Result<Provider.LifecycleState>>();
                    for (CompletableFuture<Result<Provider.LifecycleState>> future : futures) {
                        results.add(future.join());
                    }
                    return Collections.unmodifiableList(results);
                });
    }
}
