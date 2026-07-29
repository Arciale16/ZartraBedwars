package io.zartra.bedwars.compat.client;

import java.util.concurrent.CompletionStage;

/**
 * Nonblocking runtime boundary implemented against independently installed providers.
 *
 * <p>No Via, Geyser or Floodgate class crosses this interface. Implementations perform
 * vendor calls away from the owner/tick thread and return privacy-safe observations.</p>
 */
public interface ClientTranslationGateway {
    /** @return asynchronously discovered provider inventory */
    CompletionStage<ClientProviderInventory> discover();

    /**
     * Detects the actual client path after server adapter selection.
     *
     * @param opaqueSessionKey opaque non-player identity
     * @return asynchronous client observation
     */
    CompletionStage<ClientSession> inspect(String opaqueSessionKey);

    /**
     * Releases provider-side session resources.
     *
     * @param opaqueSessionKey opaque non-player identity
     * @return asynchronous completion
     */
    CompletionStage<Void> release(String opaqueSessionKey);
}
