package io.zartra.bedwars.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class PlaceholderApiIntegrationTest {

    @Test
    void initializationWithoutPlaceholderApiIsDisabled() {
        final PlaceholderApiLifecycle lifecycle = new PlaceholderApiLifecycle(PlaceholderApiProviders.fallback());
        final PlaceholderApiIntegration integration = new PlaceholderApiIntegration(lifecycle);
        assertFalse(integration.initialize(null));
        assertFalse(integration.isRegistered());
    }

    @Test
    void integrationClassExistsWithoutFailure() {
        final PlaceholderApiIntegration integration = new PlaceholderApiIntegration(
                new PlaceholderApiLifecycle(PlaceholderApiProviders.fallback())
        );
        assertFalse(integration.isRegistered());
    }
}
