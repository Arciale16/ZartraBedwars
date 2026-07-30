package io.zartra.bedwars.compat.client;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded asynchronous client compatibility lifecycle.
 *
 * <p>The selected server adapter is read-only input. Provider absence leaves native clients
 * available, while an unavailable translated path fails closed before presentation activation.</p>
 */
public final class ClientCompatibilityLayer {
    private static final int DEFAULT_MAX_SESSIONS = 4096;
    private final ClientTranslationGateway gateway;
    private final int maxSessions;
    private final Map<ClientPath, ClientPathAdapter> adapters;
    private final ConcurrentHashMap<String, ClientCompatibilityReport> sessions =
            new ConcurrentHashMap<String, ClientCompatibilityReport>();
    private final AtomicReference<State> state =
            new AtomicReference<State>(State.NEW);
    private volatile ClientProviderInventory inventory =
            ClientProviderInventory.empty();

    /** Creates a layer with the bounded production session limit. */
    public ClientCompatibilityLayer(final ClientTranslationGateway gateway) {
        this(gateway, DEFAULT_MAX_SESSIONS);
    }

    /** Creates a layer with an explicit positive session limit. */
    public ClientCompatibilityLayer(
            final ClientTranslationGateway gateway, final int maxSessions) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        if (maxSessions < 1 || maxSessions > DEFAULT_MAX_SESSIONS) {
            throw new IllegalArgumentException(
                    "maxSessions must be between 1 and 4096");
        }
        this.maxSessions = maxSessions;
        final Map<ClientPath, ClientPathAdapter> configured =
                new EnumMap<ClientPath, ClientPathAdapter>(ClientPath.class);
        add(configured, new NativeClientAdapter());
        add(configured, new ViaVersionClientAdapter());
        add(configured, new ViaBackwardsClientAdapter());
        add(configured, new ViaRewindClientAdapter());
        add(configured, new GeyserFloodgateClientAdapter());
        adapters = Collections.unmodifiableMap(configured);
    }

    /** @return asynchronous successful start; discovery failure fails closed */
    public CompletionStage<Boolean> start() {
        if (!state.compareAndSet(State.NEW, State.STARTING)
                && !state.compareAndSet(State.STOPPED, State.STARTING)) {
            return completed(state.get() == State.RUNNING);
        }
        try {
            return gateway.discover().handle((discovered, failure) -> {
                if (failure != null || discovered == null) {
                    state.set(State.FAILED);
                    return false;
                }
                inventory = discovered;
                state.set(State.RUNNING);
                return true;
            });
        } catch (RuntimeException failure) {
            state.set(State.FAILED);
            return completed(false);
        }
    }

    /**
     * Opens one client parity session without blocking.
     *
     * @param opaqueSessionKey privacy-safe correlation key
     * @param serverAdapter already selected exact server adapter
     * @return complete report; unsafe paths return a blocked report
     */
    public CompletionStage<ClientCompatibilityReport> open(
            final String opaqueSessionKey,
            final CompatibilityAdapter serverAdapter) {
        Objects.requireNonNull(serverAdapter, "serverAdapter");
        validateKey(opaqueSessionKey);
        final ClientCompatibilityReport existing = sessions.get(opaqueSessionKey);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        if (state.get() != State.RUNNING || sessions.size() >= maxSessions) {
            return CompletableFuture.completedFuture(blocked(
                    ClientPath.NATIVE, serverAdapter, "layer-unavailable"));
        }
        try {
            return gateway.inspect(opaqueSessionKey).handle((session, failure) -> {
                if (failure != null || session == null
                        || !opaqueSessionKey.equals(session.sessionKey())) {
                    return blocked(ClientPath.NATIVE, serverAdapter,
                            "inspection-failed");
                }
                final ClientPathAdapter adapter = adapters.get(session.path());
                final ClientCompatibilityReport report;
                if (adapter == null || !adapter.available(inventory)) {
                    report = blocked(session.path(), serverAdapter,
                            "provider-unavailable");
                } else {
                    report = adapter.evaluate(serverAdapter, session);
                }
                final ClientCompatibilityReport raced =
                        sessions.putIfAbsent(opaqueSessionKey, report);
                return raced == null ? report : raced;
            });
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(blocked(
                    ClientPath.NATIVE, serverAdapter, "inspection-failed"));
        }
    }

    /**
     * Releases one client parity session asynchronously.
     *
     * @return true when an active session existed and provider cleanup succeeded
     */
    public CompletionStage<Boolean> close(final String opaqueSessionKey) {
        validateKey(opaqueSessionKey);
        if (sessions.remove(opaqueSessionKey) == null) {
            return completed(false);
        }
        try {
            return gateway.release(opaqueSessionKey)
                    .handle((ignored, failure) -> failure == null);
        } catch (RuntimeException failure) {
            return completed(false);
        }
    }

    /** Releases every bounded session and stops the layer. */
    public CompletionStage<Boolean> stop() {
        state.set(State.STOPPING);
        final List<CompletableFuture<Void>> releases =
                new ArrayList<CompletableFuture<Void>>();
        for (String sessionKey : sessions.keySet()) {
            try {
                releases.add(gateway.release(sessionKey)
                        .handle((ignored, failure) -> (Void) null)
                        .toCompletableFuture());
            } catch (RuntimeException failure) {
                releases.add(CompletableFuture.completedFuture(null));
            }
        }
        return CompletableFuture.allOf(
                releases.toArray(new CompletableFuture<?>[releases.size()]))
                .handle((ignored, failure) -> {
                    sessions.clear();
                    state.set(State.STOPPED);
                    return failure == null;
                });
    }

    /** @return current lifecycle state */ public State state() { return state.get(); }
    /** @return bounded number of active client sessions */
    public int activeSessions() { return sessions.size(); }
    /** @return immutable exact provider inventory */
    public ClientProviderInventory inventory() { return inventory; }

    private static void add(final Map<ClientPath, ClientPathAdapter> target,
                            final ClientPathAdapter adapter) {
        if (target.put(adapter.path(), adapter) != null) {
            throw new IllegalArgumentException("duplicate client path adapter");
        }
    }

    private static ClientCompatibilityReport blocked(
            final ClientPath path,
            final CompatibilityAdapter serverAdapter,
            final String reason) {
        final List<ClientFeatureOutcome> outcomes =
                new ArrayList<ClientFeatureOutcome>();
        for (ClientFeature feature : ClientFeature.values()) {
            outcomes.add(new ClientFeatureOutcome(
                    feature, ClientFeatureOutcome.State.BLOCKED,
                    DefinitionId.of("zartra", "compat/client/" + reason),
                    false, false));
        }
        final CompatibilityAdapter.RuntimeClaim claim =
                serverAdapter.runtimeClaim();
        return new ClientCompatibilityReport(path,
                claim.platform() + "/" + claim.minecraftVersion()
                        + "@" + claim.build(), outcomes);
    }

    private static void validateKey(final String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{8,64}")) {
            throw new IllegalArgumentException("invalid opaque session key");
        }
    }

    private static CompletionStage<Boolean> completed(final boolean value) {
        return CompletableFuture.completedFuture(value);
    }

    /** Client compatibility lifecycle state. */
    public enum State { NEW, STARTING, RUNNING, STOPPING, STOPPED, FAILED }
}
