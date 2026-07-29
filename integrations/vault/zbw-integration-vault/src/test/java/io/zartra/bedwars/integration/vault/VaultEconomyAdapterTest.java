package io.zartra.bedwars.integration.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.economy.TransactionIntent;
import io.zartra.bedwars.api.provider.OptionalProviderLifecycle;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.time.TimeSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Vault presence, absence, mapping and classloading-isolation tests. */
final class VaultEconomyAdapterTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(new UUID(1, 2));
    private static final DefinitionId CURRENCY = DefinitionId.of("zartra", "coins");

    @Test void presentProviderMapsBalanceAndTransactionWithoutLedgerOwnership() {
        VaultEconomyAdapter adapter = adapter(OptionalProviderLifecycle.Probe.AVAILABLE,
                new AtomicInteger());
        adapter.start().toCompletableFuture().join();
        assertEquals(new BigDecimal("25.50"), adapter.balance(PLAYER, CURRENCY)
                .toCompletableFuture().join().requireValue().balance());
        TransactionIntent intent = new TransactionIntent(
                IdempotencyKey.of("test", "vault-1"), PLAYER, CURRENCY,
                TransactionIntent.Direction.CREDIT, BigDecimal.ONE, "test.credit",
                NOW.plusSeconds(1));
        assertEquals(new BigDecimal("26.50"), adapter.transact(intent)
                .toCompletableFuture().join().requireValue().balance());
    }

    @Test void absentAndIncompatibleProvidersFailClosedWithoutTouchingGateway() {
        AtomicInteger calls = new AtomicInteger();
        VaultEconomyAdapter absent =
                adapter(OptionalProviderLifecycle.Probe.ABSENT, calls);
        assertEquals(Provider.LifecycleState.STOPPED,
                absent.start().toCompletableFuture().join().requireValue());
        assertFalse(absent.balance(PLAYER, CURRENCY).toCompletableFuture().join().isSuccess());
        VaultEconomyAdapter incompatible =
                adapter(OptionalProviderLifecycle.Probe.INCOMPATIBLE, calls);
        incompatible.start().toCompletableFuture().join();
        assertEquals(Provider.HealthStatus.UNAVAILABLE, incompatible.health().status());
        assertEquals(0, calls.get());
    }

    @Test void adapterLoadsWithoutVaultClasses() {
        assertEquals("io.zartra.bedwars.integration.vault.VaultEconomyAdapter",
                VaultEconomyAdapter.class.getName());
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.milkbowl.vault.economy.Economy", false,
                        VaultEconomyAdapter.class.getClassLoader()));
    }

    private static VaultEconomyAdapter adapter(
            final OptionalProviderLifecycle.Probe probe, final AtomicInteger calls) {
        return new VaultEconomyAdapter(new VaultEconomyAdapter.Gateway() {
            @Override public CompletableFuture<BigDecimal> balance(
                    final PlayerId playerId, final DefinitionId currencyId) {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture(new BigDecimal("25.50"));
            }
            @Override public CompletableFuture<BigDecimal> transact(
                    final TransactionIntent intent) {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture(new BigDecimal("26.50"));
            }
        }, probe, TimeSource.FixedTimeSource.at(NOW));
    }
}
