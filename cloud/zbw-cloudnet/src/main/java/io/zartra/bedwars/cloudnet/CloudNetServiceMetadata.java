package io.zartra.bedwars.cloudnet;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable, secret-free projection of CloudNet service metadata.
 *
 * <p>ZBW-ADDON-226/227: the projection carries lifecycle and capacity data only; CloudNet
 * credentials and domain state never cross this boundary.</p>
 */
public final class CloudNetServiceMetadata implements Comparable<CloudNetServiceMetadata> {
    private final DefinitionId serviceId;
    private final String backendId;
    private final ServiceDiscoveryProvider.ServiceKind kind;
    private final ServiceDiscoveryProvider.ServiceState state;
    private final DefinitionId templateId;
    private final int capacity;
    private final int occupancy;
    private final long epoch;
    private final long revision;
    private final Instant observedAt;

    /**
     * Creates validated CloudNet metadata.
     *
     * @param serviceId stable service identity
     * @param backendId proxy-safe backend identity
     * @param kind service purpose
     * @param state lifecycle state
     * @param templateId configured CloudNet template identity
     * @param capacity advertised capacity
     * @param occupancy current occupancy
     * @param epoch positive service instance epoch
     * @param revision positive metadata revision
     * @param observedAt observation time
     */
    public CloudNetServiceMetadata(
            final DefinitionId serviceId,
            final String backendId,
            final ServiceDiscoveryProvider.ServiceKind kind,
            final ServiceDiscoveryProvider.ServiceState state,
            final DefinitionId templateId,
            final int capacity,
            final int occupancy,
            final long epoch,
            final long revision,
            final Instant observedAt) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        if (backendId == null || !backendId.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("backendId must be a safe token");
        }
        this.backendId = backendId;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.state = Objects.requireNonNull(state, "state");
        this.templateId = Objects.requireNonNull(templateId, "templateId");
        if (capacity < 0 || occupancy < 0 || occupancy > capacity
                || epoch < 1 || revision < 1) {
            throw new IllegalArgumentException("invalid capacity, epoch or revision");
        }
        this.capacity = capacity;
        this.occupancy = occupancy;
        this.epoch = epoch;
        this.revision = revision;
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }

    /** @return stable service identity */
    public DefinitionId serviceId() { return serviceId; }
    /** @return proxy-safe backend identity */
    public String backendId() { return backendId; }
    /** @return service purpose */
    public ServiceDiscoveryProvider.ServiceKind kind() { return kind; }
    /** @return lifecycle state */
    public ServiceDiscoveryProvider.ServiceState state() { return state; }
    /** @return configured template identity */
    public DefinitionId templateId() { return templateId; }
    /** @return advertised capacity */
    public int capacity() { return capacity; }
    /** @return current occupancy */
    public int occupancy() { return occupancy; }
    /** @return positive service epoch */
    public long epoch() { return epoch; }
    /** @return positive metadata revision */
    public long revision() { return revision; }
    /** @return observation time */
    public Instant observedAt() { return observedAt; }

    /** @return neutral discovery snapshot */
    public ServiceDiscoveryProvider.ServiceSnapshot snapshot() {
        return new ServiceDiscoveryProvider.ServiceSnapshot(
                serviceId, kind, state, capacity, occupancy, epoch);
    }

    /** @return whether this metadata supersedes another snapshot for the same service */
    public boolean supersedes(final CloudNetServiceMetadata other) {
        Objects.requireNonNull(other, "other");
        return serviceId.equals(other.serviceId)
                && (epoch > other.epoch || epoch == other.epoch && revision > other.revision);
    }

    @Override public int compareTo(final CloudNetServiceMetadata other) {
        return serviceId.toString().compareTo(other.serviceId.toString());
    }
}
