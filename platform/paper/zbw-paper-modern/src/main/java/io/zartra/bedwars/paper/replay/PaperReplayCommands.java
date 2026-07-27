package io.zartra.bedwars.paper.replay;

import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.playback.PlaybackSpeed;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Typed command-service foundation; command registration and GUI remain later M17 work. */
public final class PaperReplayCommands {
    private final PaperReplayService service;

    /** Creates command operations over the Paper replay service. */
    public PaperReplayCommands(final PaperReplayService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    /** @return asynchronous open outcome */
    public CompletionStage<ReplayRuntimeResult> open(final ReplayAudience audience,
                                                     final String replayId) {
        Objects.requireNonNull(replayId, "replayId");
        try {
            return service.open(audience, ReplayId.parse(replayId));
        } catch (IllegalArgumentException invalid) {
            return CompletableFuture.completedFuture(
                    ReplayRuntimeResult.of(ReplayRuntimeResult.Status.NOT_FOUND));
        }
    }

    /** @return start/resume outcome */ public ReplayRuntimeResult start(final UUID playerId) { return service.startPlayback(playerId); }
    /** @return pause outcome */ public ReplayRuntimeResult pause(final UUID playerId) { return service.pause(playerId); }
    /** @return stop/cleanup outcome */ public ReplayRuntimeResult stop(final UUID playerId) { return service.stop(playerId); }
    /** @return event-index seek outcome */
    public ReplayRuntimeResult seek(final UUID playerId, final int eventIndex) { return service.seek(playerId, eventIndex); }
    /** @return exact playback-speed outcome */
    public ReplayRuntimeResult speed(final UUID playerId, final PlaybackSpeed speed) {
        return service.changeSpeed(playerId, speed);
    }
    /** @return current authorized replay projection without mutation */
    public ReplayRuntimeResult info(final UUID playerId) { return service.inspect(playerId); }
}
