package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Typed expected result of utility-item validation and atomic execution. */
public final class ItemActionResult {
    /** Stable outcomes. */
    public enum Status {
        /** Action committed. */ EXECUTED, /** Same key was already committed. */ DUPLICATE,
        /** Action is unknown. */ UNKNOWN_ACTION, /** Match is not playable. */ INVALID_STATE,
        /** Exact permission denied. */ DENIED, /** Target is absent or invalid. */ INVALID_TARGET,
        /** Cooldown has not elapsed. */ COOLDOWN, /** Per-match limit reached. */ LIMIT_REACHED,
        /** Resources or inventory item are unavailable. */ INSUFFICIENT_RESOURCES,
        /** Atomic commit lost a revision race. */ CONFLICT, /** Runtime effect rejected safely. */ EFFECT_REJECTED
    }
    private final Status status;
    private final DefinitionId effect;
    private final Instant retryAt;
    private ItemActionResult(final Status status, final DefinitionId effect, final Instant retryAt) {
        this.status = Objects.requireNonNull(status, "status");
        this.effect = effect;
        this.retryAt = retryAt;
    }
    /** Creates an outcome. */ public static ItemActionResult of(final Status status) { return new ItemActionResult(status, null, null); }
    /** Creates a successful effect outcome. */ public static ItemActionResult executed(final DefinitionId effect) { return new ItemActionResult(Status.EXECUTED, Objects.requireNonNull(effect, "effect"), null); }
    /** Creates a cooldown outcome. */ public static ItemActionResult cooldown(final Instant retryAt) { return new ItemActionResult(Status.COOLDOWN, null, Objects.requireNonNull(retryAt, "retryAt")); }
    /** @return status */ public Status status() { return status; }
    /** @return committed effect identity */ public Optional<DefinitionId> effect() { return Optional.ofNullable(effect); }
    /** @return earliest retry instant */ public Optional<Instant> retryAt() { return Optional.ofNullable(retryAt); }
}
