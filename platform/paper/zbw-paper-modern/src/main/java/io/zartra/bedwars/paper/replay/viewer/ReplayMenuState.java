package io.zartra.bedwars.paper.replay.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable replay menu containing information, players and important events. */
public final class ReplayMenuState {
    private final UUID viewerId;
    private final ReplayViewerSession viewerSession;
    private final ReplayInformationPanel information;
    private final List<String> players;
    private final List<ReplayMenuEvent> importantEvents;

    /** Creates a bounded defensive menu state. */
    public ReplayMenuState(final UUID viewerId,
                           final ReplayViewerSession viewerSession,
                           final ReplayInformationPanel information,
                           final List<String> players,
                           final List<ReplayMenuEvent> importantEvents) {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(importantEvents, "importantEvents");
        if (players.size() > 128 || importantEvents.size() > 64
                || players.contains(null) || importantEvents.contains(null)) {
            throw new IllegalArgumentException("replay menu exceeds bounds or contains null");
        }
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.viewerSession = Objects.requireNonNull(viewerSession, "viewerSession");
        this.information = Objects.requireNonNull(information, "information");
        if (!viewerId.equals(viewerSession.viewerId())
                || !viewerSession.replayId().equals(information.replayId())) {
            throw new IllegalArgumentException("replay menu identities differ");
        }
        this.players = Collections.unmodifiableList(new ArrayList<String>(players));
        this.importantEvents = Collections.unmodifiableList(
                new ArrayList<ReplayMenuEvent>(importantEvents));
    }

    /** @return viewer identity */ public UUID viewerId() { return viewerId; }
    /** @return viewer lifecycle projection */ public ReplayViewerSession viewerSession() {
        return viewerSession;
    }
    /** @return information panel */ public ReplayInformationPanel information() {
        return information;
    }
    /** @return stable pseudonymous participant list */ public List<String> players() {
        return players;
    }
    /** @return bounded important-event rows */ public List<ReplayMenuEvent> importantEvents() {
        return importantEvents;
    }
}
