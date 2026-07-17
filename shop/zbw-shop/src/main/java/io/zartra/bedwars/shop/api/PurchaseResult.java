package io.zartra.bedwars.shop.api;

import java.util.Objects;
import java.util.Optional;

/** Typed success-or-failure result for expected purchase outcomes. */
public final class PurchaseResult<T> {
    private final T value;
    private final PurchaseFailure failure;

    private PurchaseResult(final T value, final PurchaseFailure failure) {
        this.value = value;
        this.failure = failure;
    }

    /** @return successful result */ public static <T> PurchaseResult<T> success(final T value) {
        return new PurchaseResult<T>(Objects.requireNonNull(value, "value"), null);
    }
    /** @return expected failed result */ public static <T> PurchaseResult<T> failure(final PurchaseFailure failure) {
        return new PurchaseResult<T>(null, Objects.requireNonNull(failure, "failure"));
    }
    /** @return whether a value is present */ public boolean isSuccess() { return value != null; }
    /** @return successful value when present */ public Optional<T> value() { return Optional.ofNullable(value); }
    /** @return expected failure when present */ public Optional<PurchaseFailure> failure() { return Optional.ofNullable(failure); }
}
