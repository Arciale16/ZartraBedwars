package io.zartra.bedwars.paper.replay.viewer;

import io.zartra.bedwars.paper.replay.ReplayAudience;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Strict router for replay open, information, playback and timeline controls. */
public final class ReplayViewerCommandRouter {
    private final ReplayViewerAdapter viewer;

    /** Creates a router over one viewer adapter. */
    public ReplayViewerCommandRouter(final ReplayViewerAdapter viewer) {
        this.viewer = Objects.requireNonNull(viewer, "viewer");
    }

    /** Routes tokens following `/replay` to exactly one supported viewer action. */
    public CompletionStage<ReplayViewerResult> route(final ReplayAudience audience,
                                                     final List<String> tokens) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty() || tokens.contains(null)) { return invalid(); }
        final String action = tokens.get(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "open":
            case "view":
                return tokens.size() == 2 ? viewer.view(audience, tokens.get(1)) : invalid();
            case "info":
                return immediate(tokens.size() == 1 ? viewer.info(audience.playerId()) : invalidResult());
            case "play":
            case "resume":
                return immediate(tokens.size() == 1 ? viewer.play(audience.playerId()) : invalidResult());
            case "pause":
                return immediate(tokens.size() == 1 ? viewer.pause(audience.playerId()) : invalidResult());
            case "speed":
                return routeSpeed(audience, tokens);
            case "seek":
                return routeSeek(audience, tokens);
            case "stop":
                return immediate(tokens.size() == 1 ? viewer.stop(audience.playerId()) : invalidResult());
            default:
                return invalid();
        }
    }

    private CompletionStage<ReplayViewerResult> routeSpeed(final ReplayAudience audience,
                                                           final List<String> tokens) {
        if (tokens.size() != 2) { return invalid(); }
        try {
            return immediate(viewer.speed(audience.playerId(), ReplayViewerSpeed.parse(tokens.get(1))));
        } catch (IllegalArgumentException invalidSpeed) {
            return invalid();
        }
    }

    private CompletionStage<ReplayViewerResult> routeSeek(final ReplayAudience audience,
                                                          final List<String> tokens) {
        if (tokens.size() != 2) { return invalid(); }
        try {
            return immediate(viewer.seek(audience.playerId(), Integer.parseInt(tokens.get(1))));
        } catch (NumberFormatException invalidIndex) {
            return invalid();
        }
    }

    private static CompletionStage<ReplayViewerResult> invalid() { return immediate(invalidResult()); }
    private static ReplayViewerResult invalidResult() {
        return ReplayViewerResult.of(ReplayViewerResult.Status.INVALID_COMMAND);
    }
    private static CompletionStage<ReplayViewerResult> immediate(final ReplayViewerResult result) {
        return CompletableFuture.completedFuture(result);
    }
}
