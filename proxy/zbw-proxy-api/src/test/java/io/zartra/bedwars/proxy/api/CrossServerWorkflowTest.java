package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** ZBW-DEPLOY-002/004 cross-server flows and ZBW-ADDON-041..060/102..107/252..259/291..299/387/464..473. */
class CrossServerWorkflowTest {
    private static final Instant NOW = Instant.ofEpochSecond(2000);
    private static final Collection<String> CAPABILITIES = Arrays.asList("arena.general",
            "flow.private", "flow.spectate", "flow.rejoin", "mode.bedwars");

    private static ProxyAdapterRuntime runtime(final BackendStatus primaryStatus) {
        BackendRegistry registry = new BackendRegistry();
        BackendCapabilities capabilities = BackendCapabilities.of(CAPABILITIES);
        registry.register(BackendRegistration.of(BackendId.of("backend-a"), InstanceEpoch.of(2),
                capabilities, primaryStatus, NOW));
        registry.register(BackendRegistration.of(BackendId.of("lobby"), InstanceEpoch.of(1),
                capabilities, BackendStatus.ONLINE, NOW));
        registry.heartbeat(Heartbeat.of(BackendId.of("backend-a"), InstanceEpoch.of(2),
                CapacitySnapshot.of(20, 2, 0), HealthSnapshot.of(HealthSnapshot.State.HEALTHY,
                        "ok", NOW), NOW, NOW.plusSeconds(30)), NOW);
        registry.heartbeat(Heartbeat.of(BackendId.of("lobby"), InstanceEpoch.of(1),
                CapacitySnapshot.of(50, 4, 0), HealthSnapshot.of(HealthSnapshot.State.HEALTHY,
                        "ok", NOW), NOW, NOW.plusSeconds(30)), NOW);
        Map<String, byte[]> keys = new LinkedHashMap<String, byte[]>();
        keys.put("key-a", new byte[32]);
        ProxyAdapterRuntime runtime = new ProxyAdapterRuntime(registry,
                new ProxyRoutingEngine(registry, BackendId.of("lobby")),
                new ProxyReservationCoordinator(), new ProxyMessageSecurity(
                        ProtocolVersion.of(1, 0), "test", "proxy", keys));
        runtime.start();
        return runtime;
    }

    private static CrossServerIntent intent(final CrossServerFlowType type, final String subject,
            final Map<String, String> attributes, final Collection<String> roster) {
        return CrossServerIntent.of(UUID.randomUUID(), type, subject, "proxy", "owner-decision",
                attributes, roster, NOW, NOW.plusSeconds(30));
    }

    private static OwnerHandoff handoff(final CrossServerIntent intent, final String capability) {
        return OwnerHandoff.of(intent.operationId(), "decision-v1", 1, true,
                Collections.singleton(capability));
    }

    @Test void remoteQueueAndMatchHandoffUseOwnerDecision() {
        ProxyAdapterRuntime runtime = runtime(BackendStatus.ONLINE);
        CrossServerWorkflow workflow = new CrossServerWorkflow(runtime);
        CrossServerIntent queue = intent(CrossServerFlowType.REMOTE_QUEUE, "subject-queue",
                Collections.singletonMap("queue_ref", "queue-01"), Collections.<String>emptyList());
        CrossServerTransferResult ready = workflow.prepare(queue, handoff(queue, "mode.bedwars"),
                UUID.randomUUID(), NOW);
        Assertions.assertEquals(CrossServerTransferResult.Status.READY, ready.status());
        TransferToken token = ready.token().get();
        Assertions.assertEquals(TokenConsumptionResult.Status.CONSUMED,
                workflow.complete(token, "backend-a", InstanceEpoch.of(2), NOW).status());
        CrossServerIntent match = intent(CrossServerFlowType.MATCH_HANDOFF, "subject-match",
                Collections.singletonMap("assignment_ref", "match-assignment-7"),
                Collections.<String>emptyList());
        Assertions.assertEquals(CrossServerTransferResult.Status.OWNER_REJECTED,
                workflow.prepare(match, OwnerHandoff.of(match.operationId(), "decision-v2", 2,
                        false, Collections.singleton("arena.general")), UUID.randomUUID(), NOW).status());
    }

    @Test void privateGameTransportsBoundedRosterAndSettingsReferences() {
        List<String> roster = new ArrayList<String>(Arrays.asList("member-a", "member-b"));
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("private_game_ref", "private-01");
        attributes.put("settings_version", "settings-v4");
        CrossServerIntent intent = intent(CrossServerFlowType.PRIVATE_GAME, "member-a", attributes, roster);
        roster.clear();
        Assertions.assertEquals(2, intent.rosterReferences().size());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> intent.rosterReferences().add("member-c"));
        CrossServerTransferResult result = new CrossServerWorkflow(runtime(BackendStatus.ONLINE))
                .prepare(intent, handoff(intent, "flow.private"), UUID.randomUUID(), NOW);
        Assertions.assertEquals(CrossServerTransferResult.Status.READY, result.status());
        Assertions.assertThrows(IllegalArgumentException.class, () -> intent(
                CrossServerFlowType.PRIVATE_GAME, "member-x", Collections.<String, String>emptyMap(),
                Collections.<String>emptyList()));
    }

    @Test void spectateAndReplaySpectateRemainReferenceOnly() {
        for (CrossServerFlowType type : Arrays.asList(CrossServerFlowType.SPECTATE,
                CrossServerFlowType.REPLAY_SPECTATE)) {
            CrossServerIntent intent = intent(type, "spectator-" + type.name().toLowerCase(),
                    Collections.singletonMap("target_ref", "target-session"),
                    Collections.<String>emptyList());
            CrossServerTransferResult result = new CrossServerWorkflow(runtime(BackendStatus.ONLINE))
                    .prepare(intent, handoff(intent, "flow.spectate"), UUID.randomUUID(), NOW);
            Assertions.assertEquals(CrossServerTransferResult.Status.READY, result.status());
            Assertions.assertFalse(intent.attributes().containsKey("replay_events"));
        }
    }

    @Test void rejoinPlayAgainAndExpiredTransferRecoverSafely() {
        ProxyAdapterRuntime baseRuntime = runtime(BackendStatus.ONLINE);
        CrossServerWorkflow workflow = new CrossServerWorkflow(baseRuntime);
        CrossServerIntent rejoin = intent(CrossServerFlowType.REJOIN, "subject-rejoin",
                Collections.singletonMap("session_ref", "session-9"), Collections.<String>emptyList());
        CrossServerTransferResult first = workflow.prepare(rejoin, handoff(rejoin, "flow.rejoin"),
                UUID.randomUUID(), NOW);
        TransferToken expired = first.token().get();
        Assertions.assertEquals(TokenConsumptionResult.Status.EXPIRED,
                workflow.complete(expired, "backend-a", InstanceEpoch.of(2), NOW.plusSeconds(16)).status());
        CrossServerTransferResult recovered = workflow.prepare(rejoin, handoff(rejoin, "flow.rejoin"),
                UUID.randomUUID(), NOW.plusSeconds(16));
        Assertions.assertEquals(CrossServerTransferResult.Status.READY, recovered.status());
        Assertions.assertEquals(TokenConsumptionResult.Status.CONSUMED,
                workflow.complete(recovered.token().get(), "backend-a", InstanceEpoch.of(2),
                        NOW.plusSeconds(16)).status());
        Assertions.assertEquals(TokenConsumptionResult.Status.DUPLICATE,
                workflow.complete(recovered.token().get(), "backend-a", InstanceEpoch.of(2),
                        NOW.plusSeconds(16)).status());
        CrossServerIntent playAgain = intent(CrossServerFlowType.PLAY_AGAIN, "subject-play-again",
                Collections.singletonMap("mode_ref", "bedwars-standard"),
                Collections.<String>emptyList());
        CrossServerTransferResult fallback = new CrossServerWorkflow(runtime(BackendStatus.DRAINING))
                .prepare(playAgain, handoff(playAgain, "mode.bedwars"), UUID.randomUUID(), NOW);
        Assertions.assertEquals(BackendId.of("lobby"), fallback.routing().get().backendId().get());
    }

    @Test void mapSelectionRoutesWithoutOwningSelectorPolicy() {
        CrossServerIntent map = intent(CrossServerFlowType.MAP_SELECTION, "subject-map",
                Collections.singletonMap("selection_ref", "map-choice-17"),
                Collections.<String>emptyList());
        CrossServerTransferResult result = new CrossServerWorkflow(runtime(BackendStatus.ONLINE))
                .prepare(map, handoff(map, "arena.general"), UUID.randomUUID(), NOW);
        Assertions.assertEquals(CrossServerTransferResult.Status.READY, result.status());
        Assertions.assertEquals("map-choice-17", map.attributes().get("selection_ref"));
    }

    @Test void resourceScarcityAndItemRotationVersionsAreMonotonic() {
        VersionPropagationTracker tracker = new VersionPropagationTracker();
        DomainVersionNotification scarcity = DomainVersionNotification.of(UUID.randomUUID(),
                DomainVersionNotification.Family.RESOURCE_SCARCITY, "arena-group-a", 4, NOW);
        DomainVersionNotification rotation = DomainVersionNotification.of(UUID.randomUUID(),
                DomainVersionNotification.Family.ITEM_ROTATION, "rotation-main", 8, NOW);
        Assertions.assertTrue(tracker.accept(scarcity));
        Assertions.assertFalse(tracker.accept(scarcity));
        Assertions.assertTrue(tracker.accept(rotation));
        Assertions.assertEquals(8, tracker.version(DomainVersionNotification.Family.ITEM_ROTATION,
                "rotation-main"));
        Assertions.assertEquals(0, tracker.version(DomainVersionNotification.Family.ITEM_ROTATION,
                "unknown"));
        Assertions.assertFalse(tracker.accept(DomainVersionNotification.of(UUID.randomUUID(),
                DomainVersionNotification.Family.ITEM_ROTATION, "rotation-main", 7, NOW)));
    }
}
