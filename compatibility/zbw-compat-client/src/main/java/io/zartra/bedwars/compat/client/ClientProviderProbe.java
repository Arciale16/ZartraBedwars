package io.zartra.bedwars.compat.client;

import java.util.Objects;
import java.util.Optional;

/** Immutable, privacy-safe discovery evidence for one optional provider. */
public final class ClientProviderProbe {
    private final ClientProvider provider;
    private final ClientProviderStatus status;
    private final String version;
    private final int bindingCount;

    private ClientProviderProbe(final ClientProvider provider,
                                final ClientProviderStatus status,
                                final String version,
                                final int bindingCount) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.status = Objects.requireNonNull(status, "status");
        if (bindingCount < 0 || bindingCount > 16) {
            throw new IllegalArgumentException("bindingCount must be between 0 and 16");
        }
        if ((status == ClientProviderStatus.ABSENT) != (bindingCount == 0)) {
            throw new IllegalArgumentException("only absent probes have zero bindings");
        }
        if (status == ClientProviderStatus.DUPLICATE && bindingCount < 2) {
            throw new IllegalArgumentException("duplicate probes require multiple bindings");
        }
        if (status != ClientProviderStatus.ABSENT
                && (version == null || !version.matches("[A-Za-z0-9_.+-]{1,32}"))) {
            throw new IllegalArgumentException("installed provider version must be safe");
        }
        this.version = version;
        this.bindingCount = bindingCount;
    }

    /** @return a present provider probe */
    public static ClientProviderProbe present(final ClientProvider provider,
                                              final String version) {
        return new ClientProviderProbe(provider, ClientProviderStatus.PRESENT, version, 1);
    }

    /** @return an absent provider probe */
    public static ClientProviderProbe absent(final ClientProvider provider) {
        return new ClientProviderProbe(provider, ClientProviderStatus.ABSENT, null, 0);
    }

    /** @return an incompatible provider probe */
    public static ClientProviderProbe incompatible(final ClientProvider provider,
                                                   final String version) {
        return new ClientProviderProbe(
                provider, ClientProviderStatus.INCOMPATIBLE, version, 1);
    }

    /** @return a duplicate provider probe */
    public static ClientProviderProbe duplicate(final ClientProvider provider,
                                                final String version,
                                                final int bindingCount) {
        return new ClientProviderProbe(
                provider, ClientProviderStatus.DUPLICATE, version, bindingCount);
    }

    /** @return selected provider */ public ClientProvider provider() { return provider; }
    /** @return discovery state */ public ClientProviderStatus status() { return status; }
    /** @return installed version when present */
    public Optional<String> version() { return Optional.ofNullable(version); }
    /** @return bounded number of discovered bindings */ public int bindingCount() {
        return bindingCount;
    }

    /** @return true only for one provider at its exact selected version */
    public boolean exactlyCompatible() {
        return status == ClientProviderStatus.PRESENT
                && provider.requiredVersion().equals(version);
    }

    @Override public int hashCode() {
        return Objects.hash(provider, status, version, bindingCount);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ClientProviderProbe)) { return false; }
        final ClientProviderProbe that = (ClientProviderProbe) other;
        return provider == that.provider && status == that.status
                && Objects.equals(version, that.version)
                && bindingCount == that.bindingCount;
    }
}
