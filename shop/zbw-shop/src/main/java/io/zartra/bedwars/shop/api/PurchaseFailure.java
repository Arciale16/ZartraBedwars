package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.localization.MessageKey;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Immutable, localized-by-adapter expected purchase rejection. */
public final class PurchaseFailure {
    private final Code code;
    private final MessageKey message;
    private final Instant retryAt;

    private PurchaseFailure(final Code code, final MessageKey message, final Instant retryAt) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
        this.retryAt = retryAt;
    }

    /** @return failure without a retry instant */
    public static PurchaseFailure of(final Code code) {
        return new PurchaseFailure(
                code,
                MessageKey.of("shop.failure." + code.name().toLowerCase(Locale.ROOT)),
                null);
    }
    /** @return retryable cooldown failure */
    public static PurchaseFailure retryAt(final Instant retryAt) {
        return new PurchaseFailure(Code.COOLDOWN, MessageKey.of("shop.failure.cooldown"),
                Objects.requireNonNull(retryAt, "retryAt"));
    }
    /** @return stable failure code */ public Code code() { return code; }
    /** @return localization key with no sensitive details */ public MessageKey message() { return message; }
    /** @return optional next eligible instant */ public Optional<Instant> retryAt() { return Optional.ofNullable(retryAt); }

    /** Expected validation and commit failures. */
    public enum Code {
        /** Wrong catalog identity. */ CATALOG_MISMATCH, /** Context outside catalog scope. */ SCOPE_MISMATCH,
        /** Unknown item. */ UNKNOWN_ITEM, /** Hidden or disabled definition. */ UNAVAILABLE,
        /** Authorization denied. */ FORBIDDEN, /** Explicit confirmation is missing. */ CONFIRMATION_REQUIRED,
        /** Requested bulk exceeds the item policy. */ BULK_LIMIT, /** No tender resolves the price. */ UNKNOWN_TENDER,
        /** Current balance cannot cover all price components. */ INSUFFICIENT_RESOURCES,
        /** Inventory cannot accept the complete grant. */ INVENTORY_FULL,
        /** Current inventory ownership cap would be exceeded. */ INVENTORY_LIMIT,
        /** Player match limit would be exceeded. */ PLAYER_LIMIT,
        /** Team match limit would be exceeded. */ TEAM_LIMIT,
        /** Arena match limit would be exceeded. */ ARENA_LIMIT, /** Item cooldown is active. */ COOLDOWN,
        /** Configured or extension restriction rejected. */ CONDITION_REJECTED,
        /** Quote expired before commit. */ QUOTE_EXPIRED, /** State changed since the quote. */ STALE_QUOTE,
        /** Adapter rejected the complete atomic mutation. */ TRANSACTION_REJECTED
    }
}
