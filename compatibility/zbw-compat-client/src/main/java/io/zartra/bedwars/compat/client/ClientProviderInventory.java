package io.zartra.bedwars.compat.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable complete inventory of every optional M22 client provider. */
public final class ClientProviderInventory {
    private final Map<ClientProvider, ClientProviderProbe> probes;

    private ClientProviderInventory(
            final Map<ClientProvider, ClientProviderProbe> probes) {
        this.probes = Collections.unmodifiableMap(
                new EnumMap<ClientProvider, ClientProviderProbe>(probes));
    }

    /**
     * Builds a complete inventory; repeated provider rows become DUPLICATE.
     *
     * @param discovered bounded discovery results
     * @return complete deterministic inventory
     */
    public static ClientProviderInventory of(
            final Collection<ClientProviderProbe> discovered) {
        Objects.requireNonNull(discovered, "discovered");
        if (discovered.size() > 16) {
            throw new IllegalArgumentException("provider inventory is bounded to 16 rows");
        }
        final Map<ClientProvider, List<ClientProviderProbe>> grouped =
                new EnumMap<ClientProvider, List<ClientProviderProbe>>(ClientProvider.class);
        for (ClientProviderProbe probe : discovered) {
            final ClientProviderProbe safe = Objects.requireNonNull(probe, "probe");
            grouped.computeIfAbsent(safe.provider(),
                    ignored -> new ArrayList<ClientProviderProbe>()).add(safe);
        }
        final Map<ClientProvider, ClientProviderProbe> result =
                new EnumMap<ClientProvider, ClientProviderProbe>(ClientProvider.class);
        for (ClientProvider provider : ClientProvider.values()) {
            final List<ClientProviderProbe> rows = grouped.get(provider);
            if (rows == null || rows.isEmpty()) {
                result.put(provider, ClientProviderProbe.absent(provider));
            } else if (rows.size() == 1) {
                result.put(provider, rows.get(0));
            } else {
                final String version = rows.get(0).version()
                        .orElse(provider.requiredVersion());
                result.put(provider, ClientProviderProbe.duplicate(
                        provider, version, rows.size()));
            }
        }
        return new ClientProviderInventory(result);
    }

    /** @return inventory with every optional provider absent */
    public static ClientProviderInventory empty() {
        return of(Collections.<ClientProviderProbe>emptyList());
    }

    /** @return probe for the requested provider */
    public ClientProviderProbe probe(final ClientProvider provider) {
        return probes.get(Objects.requireNonNull(provider, "provider"));
    }

    /** @return immutable complete provider map */
    public Map<ClientProvider, ClientProviderProbe> probes() {
        return probes;
    }

    /** @return whether every provider required by a path is exactly compatible */
    public boolean supports(final ClientPath path) {
        switch (Objects.requireNonNull(path, "path")) {
            case NATIVE:
                return true;
            case VIAVERSION:
                return exact(ClientProvider.VIAVERSION);
            case VIABACKWARDS:
                return exact(ClientProvider.VIAVERSION)
                        && exact(ClientProvider.VIABACKWARDS);
            case VIAREWIND:
                return exact(ClientProvider.VIAVERSION)
                        && exact(ClientProvider.VIABACKWARDS)
                        && exact(ClientProvider.VIAREWIND);
            case GEYSER_FLOODGATE:
                return exact(ClientProvider.GEYSER)
                        && exact(ClientProvider.FLOODGATE);
            default:
                throw new IllegalStateException("unhandled client path");
        }
    }

    private boolean exact(final ClientProvider provider) {
        return probes.get(provider).exactlyCompatible();
    }

    @Override public int hashCode() { return probes.hashCode(); }

    @Override public boolean equals(final Object other) {
        return this == other || other instanceof ClientProviderInventory
                && probes.equals(((ClientProviderInventory) other).probes);
    }
}
