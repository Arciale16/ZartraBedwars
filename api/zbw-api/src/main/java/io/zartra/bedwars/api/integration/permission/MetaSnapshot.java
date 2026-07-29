package io.zartra.bedwars.api.integration.permission;

import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable privacy-safe permission metadata projection. */
public final class MetaSnapshot {
    private static final int MAX_META = 64;
    private final PlayerId playerId;
    private final String prefix;
    private final String suffix;
    private final Map<String, String> metadata;
    private final long version;
    private final Instant observedAt;

    /**
     * Creates a metadata snapshot.
     *
     * @param playerId player identity
     * @param prefix display prefix, never an identity
     * @param suffix display suffix
     * @param metadata allow-listed metadata
     * @param version non-negative provider version
     * @param observedAt observation timestamp
     */
    public MetaSnapshot(final PlayerId playerId, final String prefix, final String suffix,
                        final Map<String, String> metadata, final long version,
                        final Instant observedAt) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.prefix = bounded(prefix, "prefix");
        this.suffix = bounded(suffix, "suffix");
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
        Objects.requireNonNull(metadata, "metadata");
        if (metadata.size() > MAX_META || version < 0) {
            throw new IllegalArgumentException("metadata or version exceeds bounds");
        }
        TreeMap<String, String> copy = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.matches("[a-z0-9][a-z0-9_.-]{0,63}")) {
                throw new IllegalArgumentException("metadata key must be safe");
            }
            copy.put(key, bounded(entry.getValue(), "metadata value"));
        }
        this.metadata = Collections.unmodifiableMap(copy);
        this.version = version;
    }

    private static String bounded(final String value, final String name) {
        if (value == null || value.length() > 256 || value.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException(name + " must be bounded");
        }
        return value;
    }

    /** @return player identity */
    public PlayerId playerId() { return playerId; }
    /** @return display prefix */
    public String prefix() { return prefix; }
    /** @return display suffix */
    public String suffix() { return suffix; }
    /** @return sorted immutable allow-listed metadata */
    public Map<String, String> metadata() { return metadata; }
    /** @return provider data version */
    public long version() { return version; }
    /** @return observation timestamp */
    public Instant observedAt() { return observedAt; }
}
