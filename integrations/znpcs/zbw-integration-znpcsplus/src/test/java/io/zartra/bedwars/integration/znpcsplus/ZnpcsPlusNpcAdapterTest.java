package io.zartra.bedwars.integration.znpcsplus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.npc.NpcProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** ZNPCsPlus incompatible-provider fallback tests. */
final class ZnpcsPlusNpcAdapterTest {
    @Test void incompatibleProviderFailsClosedWithoutVendorClassloading() {
        ZnpcsPlusNpcAdapter adapter = new ZnpcsPlusNpcAdapter(new Gateway(),
                OptionalProviderLifecycle.Probe.INCOMPATIBLE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        adapter.start().toCompletableFuture().join();
        assertEquals(Provider.HealthStatus.UNAVAILABLE, adapter.health().status());
        assertFalse(adapter.exportDefinitions().toCompletableFuture().join().isSuccess());
    }

    private static final class Gateway implements ZnpcsPlusNpcAdapter.Gateway {
        @Override public CompletableFuture<NpcProvider.Definition> upsert(
                final NpcProvider.Definition value) {
            return CompletableFuture.completedFuture(value);
        }
        @Override public CompletableFuture<Boolean> remove(final DefinitionId id) {
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletableFuture<Boolean> render(
                final DefinitionId id, final PlayerId viewerId) {
            return CompletableFuture.completedFuture(true);
        }
        @Override public CompletableFuture<List<NpcProvider.Definition>> exportDefinitions() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
}
