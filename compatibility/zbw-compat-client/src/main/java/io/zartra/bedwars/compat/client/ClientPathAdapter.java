package io.zartra.bedwars.compat.client;

import io.zartra.bedwars.compat.api.CompatibilityAdapter;

/** Deterministic client-path parity adapter layered after server adapter selection. */
public interface ClientPathAdapter {
    /** @return client path handled by this adapter */
    ClientPath path();

    /** @return whether exact optional provider prerequisites are satisfied */
    boolean available(ClientProviderInventory inventory);

    /**
     * Evaluates feature parity without changing or wrapping the selected server adapter.
     *
     * @param serverAdapter already selected exact server adapter
     * @param session privacy-safe client observation
     * @return complete fail-closed feature report
     */
    ClientCompatibilityReport evaluate(
            CompatibilityAdapter serverAdapter, ClientSession session);
}
