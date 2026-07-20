package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.integration.ProgressionEventAdapter;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.progression.projection.ProgressionProjectionService;
import io.zartra.bedwars.progression.projection.ProjectionRecoveryState;
import io.zartra.bedwars.progression.projection.ProjectionResult;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies neutral M08/M11 integration ownership and restart-safe identity propagation. */
class ProgressionIntegrationTest {
    @Test void mapsConfiguredMatchAndSettlementEventsWithoutOwningTheirLifecycle() {
        final DefinitionId match = DefinitionId.of("game", "match/completed");
        final DefinitionId settlement = DefinitionId.of("shop", "purchase/settled");
        final Map<DefinitionId, ProgressionEventAdapter.Rule> rules = new LinkedHashMap<DefinitionId, ProgressionEventAdapter.Rule>();
        rules.put(match, new ProgressionEventAdapter.Rule(ProgressionEventAdapter.Rule.Owner.M08_MATCH,
                DefinitionId.of("xp", "match-complete"), 50));
        rules.put(settlement, new ProgressionEventAdapter.Rule(ProgressionEventAdapter.Rule.Owner.M11_SETTLEMENT,
                DefinitionId.of("xp", "purchase-settlement"), 5));
        final ProgressionEventAdapter adapter = new ProgressionEventAdapter(rules);
        final ProgressionEventInput matchInput = input(match, "match/1");
        final ProgressionEventAdapter.Intent matchIntent = adapter.adapt(matchInput).get();
        assertEquals(50, matchIntent.baseExperience());
        assertEquals(matchInput, matchIntent.input());
        assertEquals(DefinitionId.of("xp", "match-complete"), matchIntent.source());
        assertEquals(ProgressionEventAdapter.Rule.Owner.M08_MATCH, rules.get(match).owner());
        assertEquals(5, rules.get(settlement).baseExperience());
        assertEquals(DefinitionId.of("xp", "purchase-settlement"), rules.get(settlement).source());
        assertFalse(adapter.adapt(input(DefinitionId.of("other", "ignored"), "other/1")).isPresent());
    }

    @Test void rejectsMalformedRuleSetsAndInputs() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressionEventAdapter.Rule(
                ProgressionEventAdapter.Rule.Owner.M08_MATCH, DefinitionId.of("xp", "match"), 0));
        final Map<DefinitionId, ProgressionEventAdapter.Rule> invalid = new LinkedHashMap<DefinitionId, ProgressionEventAdapter.Rule>();
        invalid.put(DefinitionId.of("game", "event"), null);
        assertThrows(IllegalArgumentException.class, () -> new ProgressionEventAdapter(invalid));
        final ProgressionEventAdapter empty = new ProgressionEventAdapter(Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> empty.adapt(null));
    }

    @Test void projectionCoordinatorDelegatesMappedInputRecoveryAndRejectsUnknownInput() {
        final DefinitionId match = DefinitionId.of("game", "match/completed");
        final Map<DefinitionId, ProgressionEventAdapter.Rule> rules = Collections.singletonMap(match,
                new ProgressionEventAdapter.Rule(ProgressionEventAdapter.Rule.Owner.M08_MATCH,
                        DefinitionId.of("xp", "match"), 10));
        final ProjectionPort port = new ProjectionPort();
        final ProgressionProjectionService service = new ProgressionProjectionService(
                new ProgressionEventAdapter(rules), port);
        final UnitOfWork unit = new MemoryUnit();
        assertEquals(ProjectionResult.Status.APPLIED,
                service.project(unit, input(match, "project/1")).requireValue().status());
        assertTrue(port.projected);
        assertEquals(ProjectionResult.Status.REJECTED, service.project(unit,
                input(DefinitionId.of("other", "ignored"), "project/2")).requireValue().status());
        final ProjectionRecoveryState recovery = new ProjectionRecoveryState(input(match, "recover/1"),
                1, Instant.parse("2026-07-20T10:01:00Z"), "timeout");
        assertEquals(ProjectionResult.Status.DUPLICATE, service.recover(unit, recovery).requireValue().status());
        assertTrue(port.recovered);
    }

    private ProgressionEventInput input(final DefinitionId kind, final String key) {
        final Instant now = Instant.parse("2026-07-20T10:00:00Z");
        final EventMetadata metadata = EventMetadata.of(EventId.of(new UUID(1, 1)),
                EventTypeId.of("progression", "input"), CorrelationId.of(new UUID(2, 2)),
                now, 1, 1, EventMetadata.ThreadContext.APPLICATION_WORKER);
        return new ProgressionEventInput(metadata,
                PlayerProgressionId.of(PlayerId.of(new UUID(3, 3))), kind,
                IdempotencyKey.of("test", key), new byte[] {1});
    }

    private static final class ProjectionPort implements ProgressionProjectionService.Port {
        private boolean projected;
        private boolean recovered;
        @Override public Result<ProjectionResult> project(final UnitOfWork unitOfWork,
                final ProgressionEventAdapter.Intent intent) {
            projected = true;
            return Result.success(new ProjectionResult(
                    ProjectionResult.Status.APPLIED, null, null, null));
        }
        @Override public Result<ProjectionResult> recover(final UnitOfWork unitOfWork,
                final ProjectionRecoveryState recoveryState) {
            recovered = true;
            return Result.success(new ProjectionResult(
                    ProjectionResult.Status.DUPLICATE, null, null, null));
        }
    }

    private static final class MemoryUnit implements UnitOfWork {
        @Override public State state() { return State.ACTIVE; }
        @Override public Result<State> commit() { return Result.success(State.COMMITTED); }
        @Override public Result<State> rollback() { return Result.success(State.ROLLED_BACK); }
        @Override public void close() { }
    }
}
