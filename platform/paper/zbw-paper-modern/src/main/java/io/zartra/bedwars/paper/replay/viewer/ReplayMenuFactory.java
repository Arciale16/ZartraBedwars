package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.paper.replay.SpectatorReplaySession;
import io.zartra.bedwars.replay.api.ReplayEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Builds bounded immutable replay menu projections from the authorized runtime session. */
public final class ReplayMenuFactory {
    private static final int MAX_IMPORTANT_EVENTS = 64;

    /** Builds one synchronized information/menu projection. */
    public ReplayMenuState create(final ReplayViewerSession viewer,
                                  final SpectatorReplaySession runtime) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(runtime, "runtime");
        if (!viewer.viewerId().equals(runtime.playerId())
                || !viewer.replayId().equals(runtime.context().replay().metadata().replayId())) {
            throw new IllegalArgumentException("viewer and runtime session differ");
        }
        final List<ReplayEvent> events = runtime.context().replay().timeline().events();
        final long duration = events.isEmpty() ? 0L
                : events.get(events.size() - 1).offsetMillis();
        final int cursor = runtime.context().playback().cursor().position().eventIndex();
        final long current = cursor < 0 ? 0L : events.get(cursor).offsetMillis();
        final ReplayInformationPanel information = new ReplayInformationPanel(
                viewer.replayId(), current, duration,
                ReplayViewerSpeed.fromPlayback(runtime.context().playback().speed()),
                viewer.state(), runtime.context().replay().metadata().protectedEvidence());
        final List<String> players = new ArrayList<String>();
        runtime.context().replay().metadata().participants().forEach(
                participant -> players.add(participant.toString()));
        Collections.sort(players);
        final List<ReplayMenuEvent> important = new ArrayList<ReplayMenuEvent>();
        for (ReplayEvent event : events) {
            if (important.size() == MAX_IMPORTANT_EVENTS) { break; }
            if (important(event.type())) {
                important.add(new ReplayMenuEvent(Math.toIntExact(event.sequence()), event.offsetMillis(),
                        event.type()));
            }
        }
        return new ReplayMenuState(viewer.viewerId(), viewer, information, players, important);
    }

    private static boolean important(final String type) {
        final String normalized = type.toUpperCase(Locale.ROOT);
        return normalized.contains("KILL") || normalized.contains("DEATH")
                || normalized.contains("BED") || normalized.contains("MATCH")
                || normalized.contains("ELIMINAT");
    }
}
