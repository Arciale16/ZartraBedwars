package io.zartra.bedwars.integration.citizens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.npc.NpcProvider;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Citizens presentation delegation and absent-plugin isolation tests. */
final class CitizensNpcAdapterTest {
    @Test void delegatesPresentationOnlyWhenAvailable() {
        AtomicInteger calls = new AtomicInteger();
        NpcProvider.Definition definition = new NpcProvider.Definition(
                DefinitionId.of("zartra", "selector"), NpcProvider.Purpose.SELECTOR,
                "Play", null, 1);
        CitizensNpcAdapter.Gateway gateway = gateway(calls);
        CitizensNpcAdapter absent = new CitizensNpcAdapter(gateway,
                OptionalProviderLifecycle.Probe.ABSENT,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        absent.start().toCompletableFuture().join();
        assertFalse(absent.upsert(definition).toCompletableFuture().join().isSuccess());
        assertEquals(0, calls.get());
        CitizensNpcAdapter present = new CitizensNpcAdapter(gateway,
                OptionalProviderLifecycle.Probe.AVAILABLE,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        present.start().toCompletableFuture().join();
        assertEquals(definition, present.upsert(definition)
                .toCompletableFuture().join().requireValue());
        assertEquals(1, calls.get());
    }

    private static CitizensNpcAdapter.Gateway gateway(final AtomicInteger calls) {
        return new CitizensNpcAdapter.Gateway() {
            @Override public CompletableFuture<NpcProvider.Definition> upsert(
                    final NpcProvider.Definition value) {
                calls.incrementAndGet();
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
        };
    }
}
