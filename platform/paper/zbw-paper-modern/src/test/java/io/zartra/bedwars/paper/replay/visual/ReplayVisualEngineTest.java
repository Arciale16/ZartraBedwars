package io.zartra.bedwars.paper.replay.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.playback.PlaybackSession;
import io.zartra.bedwars.replay.playback.ReplayPlaybackEngine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-REPLAY-003/004/005/009 visual reconstruction and rendering tests. */
final class ReplayVisualEngineTest {
    private static final UUID VIEWER =
            UUID.fromString("00000000-0000-0000-0000-000000000077");

    @Test
    void reconstructsMovementEquipmentHealthAndImportantEventsDeterministically() {
        final PlaybackSession playback = at(recording(), 5);
        final ReplayVisualEngine engine = new ReplayVisualEngine(8, 8);

        final ReplayVisualState first = engine.reconstruct(playback).state().get();
        final ReplayVisualState second = engine.reconstruct(playback).state().get();

        assertEquals(first, second);
        assertEquals(List.of("alpha"), new ArrayList<String>(first.entities().keySet()));
        final VisualEntityState alpha = first.entities().get("alpha");
        assertEquals(4.0D, alpha.position().x());
        assertEquals("IRON_SWORD", alpha.equipment().items().get("main_hand"));
        assertEquals(0.0D, alpha.health());
        assertFalse(alpha.alive());
        assertEquals(List.of("BED_DESTROYED", "PLAYER_DEATH"),
                first.importantEvents().stream().map(VisualMatchEvent::type).toList());
    }

    @Test
    void renderingIsOrderedCadenceBoundedSeekSafeAndCleaned() {
        final ReplaySession source = recording();
        final RecordingRenderer renderer = new RecordingRenderer();
        final ReplayVisualAdapter adapter = new ReplayVisualAdapter(
                new ReplayVisualEngine(8, 8), renderer, 2L);

        assertEquals(ReplayVisualResult.Status.APPLIED,
                adapter.synchronize(VIEWER, at(source, 0), 0L).status());
        assertEquals(List.of("spawn:alpha"), renderer.actions);
        assertEquals(ReplayVisualResult.Status.UNCHANGED,
                adapter.synchronize(VIEWER, at(source, 1), 1L).status());
        assertEquals(0, adapter.state(VIEWER).get().eventIndex());
        assertEquals(ReplayVisualResult.Status.APPLIED,
                adapter.synchronize(VIEWER, at(source, 1), 2L).status());
        assertEquals(ReplayVisualResult.Status.APPLIED,
                adapter.synchronize(VIEWER, at(source, 0), 2L).status());
        assertEquals(0, adapter.state(VIEWER).get().eventIndex());

        adapter.cleanup(VIEWER);
        adapter.cleanup(VIEWER);
        assertFalse(adapter.state(VIEWER).isPresent());
        assertEquals(1L, renderer.actions.stream().filter("remove:alpha"::equals).count());
    }

    @Test
    void deadEntitiesAreRemovedAndShutdownCleansEveryViewer() {
        final RecordingRenderer renderer = new RecordingRenderer();
        final ReplayVisualAdapter adapter = new ReplayVisualAdapter(
                new ReplayVisualEngine(8, 8), renderer, 1L);
        final ReplaySession source = recording();

        adapter.synchronize(VIEWER, at(source, 0), 0L);
        adapter.synchronize(VIEWER, at(source, 5), 1L);
        assertTrue(renderer.actions.contains("remove:alpha"));

        final UUID second = UUID.fromString("00000000-0000-0000-0000-000000000078");
        adapter.synchronize(second, at(source, 0), 0L);
        adapter.close();
        assertFalse(adapter.state(second).isPresent());
    }

    @Test
    void rejectsCorruptedEventsAndEntityOverflowWithoutPartialRendering() {
        final ReplaySession malformed = base().start()
                .record(event("bad", 0, "HEALTH_CHANGE",
                        attributes("entity_id", "missing", "health", "10")))
                .complete();
        final ReplaySession overflow = base().start()
                .record(event("a", 0, "PLAYER_SPAWN", position("a", "0")))
                .record(event("b", 1, "PLAYER_SPAWN", position("b", "1")))
                .complete();
        final RecordingRenderer renderer = new RecordingRenderer();
        final ReplayVisualAdapter adapter = new ReplayVisualAdapter(
                new ReplayVisualEngine(1, 8), renderer, 1L);

        assertEquals(ReplayVisualResult.Status.CORRUPT,
                adapter.synchronize(VIEWER, at(malformed, 0), 0L).status());
        assertEquals(ReplayVisualResult.Status.OVER_CAPACITY,
                adapter.synchronize(VIEWER, at(overflow, 1), 1L).status());
        assertTrue(renderer.actions.isEmpty());
    }

    @Test
    void modelsAreImmutableAndRejectInvalidValues() {
        final Map<String, String> equipment = new LinkedHashMap<String, String>();
        equipment.put("head", "RED_WOOL");
        final VisualEquipmentState state = new VisualEquipmentState(equipment);
        equipment.clear();

        assertEquals("RED_WOOL", state.items().get("head"));
        assertThrows(UnsupportedOperationException.class,
                () -> state.items().put("feet", "LEATHER_BOOTS"));
        assertThrows(IllegalArgumentException.class,
                () -> new VisualPosition("world", Double.NaN, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new VisualEntityState("id", "name",
                        new VisualPosition("world", 0, 0, 0, 0, 0),
                        VisualEquipmentState.empty(), -1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ReplayVisualAdapter(new ReplayVisualEngine(1, 1),
                        new RecordingRenderer(), 0));
    }

    private static ReplaySession recording() {
        return base().start()
                .record(event("spawn", 0, "PLAYER_SPAWN", position("alpha", "1")))
                .record(event("move", 1, "MOVEMENT", position("alpha", "4")))
                .record(event("equip", 2, "ITEM_CHANGE",
                        attributes("entity_id", "alpha", "slot", "main_hand",
                                "item", "IRON_SWORD")))
                .record(event("health", 3, "HEALTH_CHANGE",
                        attributes("entity_id", "alpha", "health", "7.5")))
                .record(event("bed", 4, "BED_DESTROYED",
                        attributes("team_id", "red")))
                .record(event("death", 5, "PLAYER_DEATH",
                        attributes("entity_id", "alpha")))
                .complete();
    }

    private static Map<String, String> position(final String id, final String x) {
        return attributes("entity_id", id, "display_name", id, "world", "replay",
                "x", x, "y", "64", "z", "2", "health", "20",
                "equipment.head", "RED_WOOL");
    }

    private static Map<String, String> attributes(final String... pairs) {
        final Map<String, String> values = new LinkedHashMap<String, String>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
        return values;
    }

    private static ReplayEvent event(final String id, final long sequence, final String type,
                                     final Map<String, String> attributes) {
        return new ReplayEvent(id, sequence, sequence * 50L,
                Instant.parse("2026-07-26T00:00:00Z").plusMillis(sequence * 50L),
                ReplayEvent.Source.GAME, type, attributes);
    }

    private static PlaybackSession at(final ReplaySession session, final int eventIndex) {
        final ReplayPlaybackEngine engine = new ReplayPlaybackEngine();
        return engine.seekToEvent(engine.load(session), eventIndex);
    }

    private static ReplaySession base() {
        return ReplaySession.create(new ReplayMetadata(
                ReplayId.parse("00000000-0000-0000-0000-000000000017"),
                MatchId.parse("00000000-0000-0000-0000-000000000008"),
                Instant.parse("2026-07-26T00:00:00Z"), 1,
                Collections.emptySet(), false));
    }

    private static final class RecordingRenderer implements ReplayVisualRenderer {
        private final List<String> actions = new ArrayList<String>();
        @Override public Object spawn(final UUID viewerId, final VisualEntityState state) {
            actions.add("spawn:" + state.entityId());
            return state.entityId();
        }
        @Override public void update(final UUID viewerId, final Object handle,
                                     final VisualEntityState state) {
            actions.add("update:" + state.entityId());
        }
        @Override public void remove(final UUID viewerId, final Object handle) {
            actions.add("remove:" + handle);
        }
    }
}
