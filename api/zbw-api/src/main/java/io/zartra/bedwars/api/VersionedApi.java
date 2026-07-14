package io.zartra.bedwars.api;

import io.zartra.bedwars.api.version.SemanticVersion;

/** Common contract implemented by each independently versioned public API family. */
public interface VersionedApi {
    /**
     * Returns the contract version implemented by this API object.
     *
     * @return immutable semantic API version
     */
    SemanticVersion apiVersion();
}
