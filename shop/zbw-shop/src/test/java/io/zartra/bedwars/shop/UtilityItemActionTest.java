package io.zartra.bedwars.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.PlayerSession;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.item.AddonMechanics;
import io.zartra.bedwars.shop.item.ItemActionPorts;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.ItemActionResult;
import io.zartra.bedwars.shop.item.ItemActionService;
import io.zartra.bedwars.shop.item.UtilityItemCatalog;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class UtilityItemActionTest {
    private static final Instant NOW = Instant.parse("2026-07-18T10:00:00Z");
    private static final MatchId MATCH = MatchId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final ArenaId ARENA = ArenaId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final PlayerId PLAYER = PlayerId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));
    private static final DefinitionId RED = DefinitionId.of("test", "team/red");
    private static final DefinitionId BLUE = DefinitionId.of("test", "team/blue");
    private static final DefinitionId MODE = DefinitionId.of("test", "mode/standard");

    @Test
    void catalogContainsEveryAllocatedAddonFamilyAndValidDefinitions() {
        final UtilityItemCatalog catalog = AddonMechanics.starterCatalog();
        assertEquals(8, catalog.definitions().size());
        for (UtilityItemDefinition.Kind kind : UtilityItemDefinition.Kind.values()) {
            assertTrue(catalog.definitions().stream().anyMatch(value -> value.kind() == kind));
        }
        assertThrows(IllegalArgumentException.class, () -> new UtilityItemCatalog(Collections.emptyList()));
    }

    @Test
    void popupTowerExecutesWithAtomicCostPermissionAndValidatedLocation() {
        final Ports ports = new Ports();
        final ItemActionService service = service(ports);
        final ItemActionResult result = service.execute(playing(), request("popup-tower", "popup/1",
                target("location", null, null, true, true), NOW));
        assertEquals(ItemActionResult.Status.EXECUTED, result.status());
        assertEquals(1, ports.commits);
        assertEquals(1, ports.effects);
        assertEquals(1L, service.revision());
    }

    @Test
    void rejectsPermissionInvalidTargetsStateAndUnknownActionsWithoutDebit() {
        final Ports ports = new Ports();
        ports.allowed = false;
        assertEquals(ItemActionResult.Status.DENIED, service(ports).execute(playing(), request(
                "sponge-effect", "deny/1", target("location", null, null, true, true), NOW)).status());
        ports.allowed = true;
        assertEquals(ItemActionResult.Status.INVALID_TARGET, service(ports).execute(playing(), request(
                "popup-tower", "target/1", target("location", null, null, true, false), NOW)).status());
        assertEquals(ItemActionResult.Status.INVALID_STATE, service(ports).execute(snapshot(MatchSnapshot.State.RESETTING),
                request("popup-tower", "state/1", target("location", null, null, true, true), NOW)).status());
        assertEquals(ItemActionResult.Status.UNKNOWN_ACTION, service(ports).execute(playing(), new ItemActionRequest(
                context(), DefinitionId.of("test", "missing"), key("missing/1"), NOW, Optional.empty())).status());
        assertEquals(0, ports.commits);
    }

    @Test
    void preventsDuplicatesAndEnforcesCooldownAcrossConcurrentCalls() throws Exception {
        final Ports ports = new Ports();
        final ItemActionService service = service(ports);
        final ItemActionRequest first = request("rotation-item", "rotation/1", null, NOW);
        assertEquals(ItemActionResult.Status.EXECUTED, service.execute(playing(), first).status());
        assertEquals(ItemActionResult.Status.DUPLICATE, service.execute(playing(), first).status());
        assertEquals(ItemActionResult.Status.COOLDOWN, service.execute(playing(),
                request("rotation-item", "rotation/2", null, NOW.plusSeconds(1))).status());
        final Thread left = new Thread(() -> service.execute(playing(), request("rotation-item",
                "rotation/3", null, NOW.plusSeconds(20))));
        final Thread right = new Thread(() -> service.execute(playing(), request("rotation-item",
                "rotation/3", null, NOW.plusSeconds(20))));
        left.start();
        right.start();
        left.join();
        right.join();
        assertEquals(2, ports.commits);
    }

    @Test
    void mapsTransactionFailuresAndEffectRejectionDeterministically() {
        final Ports insufficient = new Ports();
        insufficient.outcome = ItemActionPorts.Outcome.INSUFFICIENT;
        assertEquals(ItemActionResult.Status.INSUFFICIENT_RESOURCES, service(insufficient).execute(playing(),
                request("ultimate-ability", "insufficient/1", target("player", PLAYER, BLUE, true, true), NOW)).status());
        final Ports conflict = new Ports();
        conflict.outcome = ItemActionPorts.Outcome.CONFLICT;
        assertEquals(ItemActionResult.Status.CONFLICT, service(conflict).execute(playing(),
                request("voidless-recovery", "conflict/1", target("player", PLAYER, RED, true, true), NOW)).status());
        final Ports rejected = new Ports();
        rejected.effectAccepted = false;
        assertEquals(ItemActionResult.Status.EFFECT_REJECTED, service(rejected).execute(playing(),
                request("rush-bridge", "effect/1", target("location", null, null, true, true), NOW)).status());
        assertEquals(1, rejected.compensations);
    }

    @Test
    void validatesTeamEnemyBedAndGeneratorRelationships() {
        final Ports ports = new Ports();
        final ItemActionService service = service(ports);
        assertEquals(ItemActionResult.Status.EXECUTED, service.execute(playing(), request("bedsteal-token",
                "bed/1", target("bed", null, RED, true, true), NOW)).status());
        assertEquals(ItemActionResult.Status.INVALID_TARGET, service.execute(playing(), request("bedsteal-token",
                "bed/2", target("bed", null, BLUE, true, true), NOW.plusSeconds(3))).status());
        assertEquals(ItemActionResult.Status.EXECUTED, service.execute(playing(), request("generator-boost",
                "generator/1", target("generator", null, RED, true, true), NOW)).status());
    }

    @Test
    void cleanupCoversMatchEndAndReconnectStateRemainsMatchOwned() {
        final Ports ports = new Ports();
        final ItemActionService service = service(ports);
        assertEquals(ItemActionResult.Status.EXECUTED, service.execute(playing(), request("sponge-effect",
                "recover/1", target("location", null, null, true, true), NOW)).status());
        assertFalse(service.cleaned());
        service.synchronize(snapshot(MatchSnapshot.State.RESETTING));
        assertTrue(service.cleaned());
        assertEquals(1, ports.cleanups);
        service.cleanup();
        assertEquals(1, ports.cleanups);
        assertEquals(ItemActionResult.Status.INVALID_STATE, service.execute(playing(), request("sponge-effect",
                "recover/2", target("location", null, null, true, true), NOW.plusSeconds(5))).status());
    }

    private static ItemActionService service(final Ports ports) {
        return new ItemActionService(AddonMechanics.starterCatalog(), ports, ports, ports,
                DefinitionId.of("test", "runtime/items"));
    }
    private static ItemActionRequest request(final String action, final String key,
                                             final ItemActionRequest.Target target, final Instant now) {
        return new ItemActionRequest(context(), DefinitionId.of("zartra", "utility/" + action), key(key), now,
                Optional.ofNullable(target));
    }
    private static ItemActionRequest.Target target(final String type, final PlayerId player,
                                                   final DefinitionId team, final boolean active,
                                                   final boolean buildAllowed) {
        return new ItemActionRequest.Target(DefinitionId.of("test", type), DefinitionId.of("test", type + "/1"),
                Optional.ofNullable(player), Optional.ofNullable(team), active, buildAllowed);
    }
    private static PurchaseContext context() {
        return new PurchaseContext(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                DefinitionId.of("test", "subject/player")), PLAYER, MATCH, ARENA, MODE, RED, Optional.empty());
    }
    private static MatchSnapshot playing() { return snapshot(MatchSnapshot.State.PLAYING); }
    private static MatchSnapshot snapshot(final MatchSnapshot.State state) {
        final PlayerStateSnapshot captured = new PlayerStateSnapshot(PLAYER,
                new PlayerStateSnapshot.Inventory(36, Collections.emptyMap()),
                new PlayerStateSnapshot.Location(DefinitionId.of("test", "world/arena"), 0, 64, 0, 0, 0),
                PlayerStateSnapshot.Mode.SURVIVAL, true);
        final PlayerSession session = PlayerSession.waiting(RED, captured).activate();
        return new MatchSnapshot(MATCH, ARENA, 0, state, 0,
                Arrays.asList(TeamSnapshot.empty(BLUE, 2), TeamSnapshot.empty(RED, 2)),
                Collections.singletonList(session), null, null, false, NOW);
    }
    private static IdempotencyKey key(final String path) { return IdempotencyKey.of("test", path); }

    private static final class Ports implements ItemActionPorts.Authorization,
            ItemActionPorts.Transaction, ItemActionPorts.Effect {
        private boolean allowed = true;
        private ItemActionPorts.Outcome outcome = ItemActionPorts.Outcome.COMMITTED;
        private boolean effectAccepted = true;
        private int commits;
        private int effects;
        private int cleanups;
        private int compensations;
        private final Set<IdempotencyKey> keys = new HashSet<IdempotencyKey>();
        @Override public boolean allowed(final AuthorizationRequest request) { return allowed; }
        @Override public ItemActionPorts.Outcome commit(final UtilityItemDefinition definition,
                                                       final ItemActionRequest request,
                                                       final long expectedRevision) {
            if (!keys.add(request.key())) { return ItemActionPorts.Outcome.DUPLICATE; }
            if (outcome == ItemActionPorts.Outcome.COMMITTED) { commits++; }
            return outcome;
        }
        @Override public void compensate(final UtilityItemDefinition definition,
                                         final ItemActionRequest request) {
            compensations++;
            keys.remove(request.key());
        }
        @Override public boolean apply(final DefinitionId effect, final UtilityItemDefinition definition,
                                       final ItemActionRequest request, final IdempotencyKey key) {
            if (effectAccepted) { effects++; }
            return effectAccepted;
        }
        @Override public void cleanup(final DefinitionId owner) { cleanups++; }
    }
}
