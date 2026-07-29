package io.zartra.bedwars.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.integration.discovery.ServiceDiscoveryProvider;
import io.zartra.bedwars.api.integration.economy.BalanceSnapshot;
import io.zartra.bedwars.api.integration.economy.EconomyProvider;
import io.zartra.bedwars.api.integration.economy.TransactionIntent;
import io.zartra.bedwars.api.integration.hologram.HologramProvider;
import io.zartra.bedwars.api.integration.npc.NpcProvider;
import io.zartra.bedwars.api.integration.party.PartyProvider;
import io.zartra.bedwars.api.integration.party.PartySnapshot;
import io.zartra.bedwars.api.integration.permission.ContextQuery;
import io.zartra.bedwars.api.integration.permission.MetaSnapshot;
import io.zartra.bedwars.api.integration.permission.PermissionProvider;
import io.zartra.bedwars.api.provider.Provider;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ProviderSpiContractTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test
    void providerTypesExtendTheSharedLifecycle() {
        assertTrue(Provider.class.isAssignableFrom(EconomyProvider.class));
        assertTrue(Provider.class.isAssignableFrom(PermissionProvider.class));
        assertTrue(Provider.class.isAssignableFrom(PartyProvider.class));
        assertTrue(Provider.class.isAssignableFrom(NpcProvider.class));
        assertTrue(Provider.class.isAssignableFrom(HologramProvider.class));
        assertTrue(Provider.class.isAssignableFrom(ServiceDiscoveryProvider.class));
    }

    @Test
    void economyModelsAreBoundedAndDeterministic() {
        PlayerId player = player(1);
        DefinitionId currency = DefinitionId.of("zartra", "coins");
        BalanceSnapshot first =
                new BalanceSnapshot(player, currency, new BigDecimal("12.50"), 4, NOW);
        BalanceSnapshot second =
                new BalanceSnapshot(player, currency, new BigDecimal("12.50"), 4, NOW);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertThrows(IllegalArgumentException.class, () ->
                new BalanceSnapshot(player, currency, BigDecimal.valueOf(-1), 0, NOW));
        assertThrows(IllegalArgumentException.class, () -> new TransactionIntent(
                IdempotencyKey.of("party", "operation"), player, currency,
                TransactionIntent.Direction.DEBIT, BigDecimal.ZERO, "party.reward", NOW));
    }

    @Test
    void permissionMetadataDefensivelyCopiesContexts() {
        Map<String, String> contexts = new HashMap<String, String>();
        contexts.put("server", "lobby_1");
        ContextQuery query = new ContextQuery(player(1), contexts, NOW);
        contexts.put("world", "late");
        assertEquals(Collections.singletonMap("server", "lobby_1"), query.contexts());
        assertThrows(UnsupportedOperationException.class,
                () -> query.contexts().put("world", "arena"));

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("rank", "moderator");
        MetaSnapshot snapshot =
                new MetaSnapshot(player(1), "[M]", "", metadata, 2, NOW);
        metadata.put("secret", "late");
        assertEquals(Collections.singletonMap("rank", "moderator"), snapshot.metadata());
    }

    @Test
    void partyAndPresentationDefinitionsRejectDuplicatesAndUnboundedData() {
        PlayerId leader = player(1);
        assertThrows(IllegalArgumentException.class, () -> new PartySnapshot(
                PartyId.of(uuid(3)), leader, Arrays.asList(leader, leader),
                PartySnapshot.Visibility.PRIVATE, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new HologramProvider.Definition(DefinitionId.of("zartra", "status"),
                        Collections.singletonList("line"), Duration.ZERO, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new ServiceDiscoveryProvider.ServiceSnapshot(
                        DefinitionId.of("zartra", "service"),
                        ServiceDiscoveryProvider.ServiceKind.ARENA,
                        ServiceDiscoveryProvider.ServiceState.ONLINE, 1, 2, 1));
    }

    private static PlayerId player(final int value) { return PlayerId.of(uuid(value)); }

    private static UUID uuid(final int value) {
        return new UUID(0, value);
    }
}
