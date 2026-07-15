package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.ApiError;

/** Stable typed M07 application failures. */
public final class ArenaFailures {
    /** Authorization denied. */ public static final ApiError FORBIDDEN = error("forbidden", "arena.error.forbidden", ApiError.RetryDisposition.FORBIDDEN);
    /** Aggregate not found. */ public static final ApiError NOT_FOUND = error("not_found", "arena.error.not_found", ApiError.RetryDisposition.PERMANENT);
    /** Identity, display name or revision conflict. */ public static final ApiError CONFLICT = error("conflict", "arena.error.conflict", ApiError.RetryDisposition.RETRYABLE);
    /** Validation blocks requested transition. */ public static final ApiError INVALID = error("invalid", "arena.error.invalid", ApiError.RetryDisposition.PERMANENT);
    /** Bounded capacity was reached. */ public static final ApiError CAPACITY = error("capacity", "arena.error.capacity", ApiError.RetryDisposition.RETRYABLE);
    /** Preview or proposal is stale. */ public static final ApiError STALE_PREVIEW = error("stale_preview", "arena.error.stale_preview", ApiError.RetryDisposition.PERMANENT);
    /** Archive is malformed, unsupported or fails integrity validation. */ public static final ApiError ARCHIVE = error("archive", "arena.error.archive", ApiError.RetryDisposition.PERMANENT);
    /** World provider operation failed or rolled back. */ public static final ApiError WORLD = error("world", "arena.error.world", ApiError.RetryDisposition.RETRYABLE);
    private ArenaFailures() { throw new AssertionError("No instances"); }
    private static ApiError error(final String code, final String message,
                                  final ApiError.RetryDisposition retry) {
        return ApiError.of(DefinitionId.of("zartra", "arena/error/" + code), message, retry);
    }
}
