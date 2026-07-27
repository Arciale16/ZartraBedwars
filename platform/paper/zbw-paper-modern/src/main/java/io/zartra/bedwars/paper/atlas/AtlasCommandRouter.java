package io.zartra.bedwars.paper.atlas;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Strict `/atlas` command router over {@link PaperAtlasService}. */
public final class AtlasCommandRouter {
    private final PaperAtlasService service;
    public AtlasCommandRouter(final PaperAtlasService service) { this.service = service; }

    public CompletionStage<?> route(final AtlasAudience audience, final String... arguments) {
        if (arguments.length == 0 || "list".equals(arguments[0])) {
            return service.list(audience);
        }
        try {
            String action = arguments[0].toLowerCase(Locale.ROOT);
            if ("open".equals(action) && arguments.length == 2) {
                return service.open(audience, AtlasCaseId.parse(arguments[1]));
            }
            if ("review".equals(action) && arguments.length == 2) {
                return service.review(audience, AtlasCaseId.parse(arguments[1]));
            }
            if ("verdict".equals(action) && arguments.length == 4) {
                return service.verdict(audience, AtlasCaseId.parse(arguments[1]),
                        arguments[2], arguments[3]);
            }
            if ("final".equals(action) && arguments.length == 3) {
                return service.finalReview(audience,
                        AtlasCaseId.parse(arguments[1]), arguments[2]);
            }
            if ("diagnostics".equals(action) && arguments.length == 1) {
                return service.diagnostics(audience);
            }
        } catch (IllegalArgumentException invalid) {
            return CompletableFuture.completedFuture("invalid-case");
        }
        return CompletableFuture.completedFuture("usage");
    }

    public List<String> actions() {
        return Arrays.asList("list", "open", "review", "verdict", "final", "diagnostics");
    }
}
