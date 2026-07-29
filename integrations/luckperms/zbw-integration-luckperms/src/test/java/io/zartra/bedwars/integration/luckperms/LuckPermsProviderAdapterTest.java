package io.zartra.bedwars.integration.luckperms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.permission.ContextQuery;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** LuckPerms context, metadata and invalidation mapping tests. */
final class LuckPermsProviderAdapterTest {
    @Test void mapsContextPermissionMetadataAndInvalidation() {
        Instant now = Instant.parse("2026-07-29T10:00:00Z");
        PlayerId player = PlayerId.of(new UUID(3, 4));
        AtomicInteger invalidations = new AtomicInteger();
        LuckPermsProviderAdapter adapter = new LuckPermsProviderAdapter(
                new LuckPermsProviderAdapter.Gateway() {
                    @Override public CompletableFuture<Boolean> hasPermission(
                            final ContextQuery query, final PermissionNode permission) {
                        return CompletableFuture.completedFuture(
                                "arena".equals(query.contexts().get("server")));
                    }
                    @Override public CompletableFuture<LuckPermsProviderAdapter.Metadata> metadata(
                            final ContextQuery query) {
                        return CompletableFuture.completedFuture(
                                new LuckPermsProviderAdapter.Metadata("[VIP]", "",
                                        Collections.singletonMap("rank", "vip"), 4));
                    }
                    @Override public void invalidate(final PlayerId playerId) {
                        invalidations.incrementAndGet();
                    }
                }, OptionalProviderLifecycle.Probe.AVAILABLE,
                TimeSource.FixedTimeSource.at(now));
        adapter.start().toCompletableFuture().join();
        ContextQuery query = new ContextQuery(player,
                Collections.singletonMap("server", "arena"), now.plusSeconds(1));
        assertTrue(adapter.hasPermission(query, PermissionNode.of("zartrabedwars.play"))
                .toCompletableFuture().join().requireValue());
        assertEquals("vip", adapter.metadata(query).toCompletableFuture().join()
                .requireValue().metadata().get("rank"));
        adapter.invalidate(player);
        assertEquals(1, invalidations.get());
        assertTrue(adapter.metadata(query).toCompletableFuture().join()
                .requireValue().version() >= 1);
    }
}
