package io.zartra.bedwars.paper.replay.staff;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.paper.replay.ReplayAudience;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerAdapter;
import io.zartra.bedwars.paper.replay.viewer.ReplayViewerResult;
import io.zartra.bedwars.replay.api.ReplayId;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Strict `/replay staff` router for bounded search and moderation operations. */
public final class ReplayStaffCommandRouter {
    private static final int DEFAULT_LIMIT = 50;
    private final ReplayStaffService staff;
    private final ReplayStaffOpener opener;

    /** Creates a staff router over existing staff and viewer services. */
    public ReplayStaffCommandRouter(final ReplayStaffService staff,
                                    final ReplayViewerAdapter viewer) {
        this(staff, (actor, replayId) -> Objects.requireNonNull(viewer, "viewer")
                .view(actor, replayId.toString()).thenApply(ReplayViewerResult::status));
    }


    ReplayStaffCommandRouter(final ReplayStaffService staff,
                             final ReplayStaffOpener opener) {
        this.staff = Objects.requireNonNull(staff, "staff");
        this.opener = Objects.requireNonNull(opener, "opener");
    }
    /** Routes tokens following `/replay staff`. */
    public CompletionStage<ReplayStaffResult> route(final ReplayAudience actor,
                                                    final List<String> tokens) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty() || tokens.contains(null)) { return invalid(); }
        try {
            switch (tokens.get(0).toLowerCase(Locale.ROOT)) {
                case "search": return search(actor, tokens);
                case "inspect": return tokens.size() == 2
                        ? staff.inspect(actor, ReplayId.parse(tokens.get(1))) : invalid();
                case "open": return open(actor, tokens);
                case "mark": return mark(actor, tokens);
                case "archive": return tokens.size() == 2
                        ? staff.archive(actor, ReplayId.parse(tokens.get(1))) : invalid();
                case "remove-invalid": return tokens.size() == 2
                        ? staff.removeInvalid(actor, ReplayId.parse(tokens.get(1))) : invalid();
                default: return invalid();
            }
        } catch (IllegalArgumentException malformed) {
            return invalid();
        }
    }

    private CompletionStage<ReplayStaffResult> search(final ReplayAudience actor,
                                                      final List<String> tokens) {
        if (tokens.size() < 3 || tokens.size() > 4) { return invalid(); }
        final String filter = tokens.get(1).toLowerCase(Locale.ROOT);
        final ReplayStaffQuery query;
        switch (filter) {
            case "player":
                query = new ReplayStaffQuery(PlayerId.of(UUID.fromString(tokens.get(2))),
                        null, null, null, null, null, tokens.size() == 4
                                ? Integer.parseInt(tokens.get(3)) : DEFAULT_LIMIT);
                break;
            case "match":
                query = new ReplayStaffQuery(null, MatchId.parse(tokens.get(2)),
                        null, null, null, null, tokens.size() == 4
                                ? Integer.parseInt(tokens.get(3)) : DEFAULT_LIMIT);
                break;
            case "date":
                if (tokens.size() != 4) { return invalid(); }
                query = new ReplayStaffQuery(null, null, Instant.parse(tokens.get(2)),
                        Instant.parse(tokens.get(3)), null, null, DEFAULT_LIMIT);
                break;
            case "duration":
                if (tokens.size() != 4) { return invalid(); }
                query = new ReplayStaffQuery(null, null, null, null,
                        Long.valueOf(tokens.get(2)), Long.valueOf(tokens.get(3)),
                        DEFAULT_LIMIT);
                break;
            default:
                return invalid();
        }
        return staff.search(actor, query);
    }

    private CompletionStage<ReplayStaffResult> open(final ReplayAudience actor,
                                                    final List<String> tokens) {
        if (tokens.size() != 2) { return invalid(); }
        final ReplayId replayId = ReplayId.parse(tokens.get(1));
        return opener.open(actor, replayId).thenCompose(status ->
                staff.auditOpen(actor, replayId, map(status)));
    }

    private CompletionStage<ReplayStaffResult> mark(final ReplayAudience actor,
                                                    final List<String> tokens) {
        if (tokens.size() != 3
                || !"true".equalsIgnoreCase(tokens.get(2))
                && !"false".equalsIgnoreCase(tokens.get(2))) {
            return invalid();
        }
        return staff.mark(actor, ReplayId.parse(tokens.get(1)),
                Boolean.parseBoolean(tokens.get(2)));
    }

    private static ReplayStaffResult.Status map(final ReplayViewerResult.Status status) {
        switch (status) {
            case SUCCESS: return ReplayStaffResult.Status.SUCCESS;
            case FORBIDDEN: return ReplayStaffResult.Status.FORBIDDEN;
            case NOT_FOUND:
            case NO_SESSION: return ReplayStaffResult.Status.NOT_FOUND;
            case INVALID_STATE:
            case INVALID_COMMAND: return ReplayStaffResult.Status.INVALID_STATE;
            default: return ReplayStaffResult.Status.FAILED;
        }
    }

    private static CompletionStage<ReplayStaffResult> invalid() {
        return CompletableFuture.completedFuture(
                ReplayStaffResult.of(ReplayStaffResult.Status.INVALID_STATE));
    }

    interface ReplayStaffOpener {
        CompletionStage<ReplayViewerResult.Status> open(
                ReplayAudience actor, ReplayId replayId);
    }}
