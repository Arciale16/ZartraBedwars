package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.identity.ResourceId;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Owner-thread integration boundary for inventory inspection and atomic purchase mutation.
 *
 * <p>{@link #commit(CommitRequest)} must compare the expected state revision and idempotency key,
 * revalidate capacity and balances, debit every priced resource, grant every item unit, update
 * player/team/arena counters and cooldown state, and append purchase history in one indivisible
 * mutation. Any failure leaves all state unchanged. Implementations must never block the Minecraft
 * owner thread or call a remote/database provider from this contract.</p>
 */
public interface PurchaseTransactionPort {
    /** Returns a complete immutable state snapshot for the exact item and context. */
    State inspect(PurchaseRequest request, ShopCatalog.ItemDefinition item);
    /** Attempts the complete transaction and returns an explicit race/idempotency outcome. */
    CommitResult commit(CommitRequest request);

    /** Immutable state required to quote one item. */
    final class State {
        private final long revision;
        private final Map<ResourceId, Long> balances;
        private final boolean canReceive;
        private final int ownedUnits;
        private final int playerPurchases;
        private final int teamPurchases;
        private final int arenaPurchases;
        private final Instant lastPurchaseAt;

        /** Creates a complete, non-negative quote state. */
        public State(final long revision, final Map<ResourceId, Long> balances,
                     final boolean canReceive, final int ownedUnits,
                     final int playerPurchases, final int teamPurchases,
                     final int arenaPurchases, final Optional<Instant> lastPurchaseAt) {
            if (revision < 0 || ownedUnits < 0 || playerPurchases < 0
                    || teamPurchases < 0 || arenaPurchases < 0) {
                throw new IllegalArgumentException("purchase state values must not be negative");
            }
            this.revision = revision;
            final Map<ResourceId, Long> copy = new LinkedHashMap<ResourceId, Long>();
            for (Map.Entry<ResourceId, Long> entry
                    : Objects.requireNonNull(balances, "balances").entrySet()) {
                final ResourceId resource = Objects.requireNonNull(entry.getKey(), "resource");
                final Long amount = Objects.requireNonNull(entry.getValue(), "balance");
                if (amount < 0) { throw new IllegalArgumentException("balance must not be negative"); }
                copy.put(resource, amount);
            }
            this.balances = Collections.unmodifiableMap(copy);
            this.canReceive = canReceive;
            this.ownedUnits = ownedUnits;
            this.playerPurchases = playerPurchases;
            this.teamPurchases = teamPurchases;
            this.arenaPurchases = arenaPurchases;
            this.lastPurchaseAt = Objects.requireNonNull(lastPurchaseAt, "lastPurchaseAt").orElse(null);
        }
        /** @return optimistic inventory revision */ public long revision() { return revision; }
        /** @return immutable balances for native and custom resources */ public Map<ResourceId, Long> balances() { return balances; }
        /** @return whether the entire quoted grant currently fits */ public boolean canReceive() { return canReceive; }
        /** @return currently owned units of the semantic item */ public int ownedUnits() { return ownedUnits; }
        /** @return player purchases of this item in the match */ public int playerPurchases() { return playerPurchases; }
        /** @return team purchases of this item in the match */ public int teamPurchases() { return teamPurchases; }
        /** @return arena purchases of this item in the match */ public int arenaPurchases() { return arenaPurchases; }
        /** @return previous committed purchase instant */ public Optional<Instant> lastPurchaseAt() { return Optional.ofNullable(lastPurchaseAt); }
    }

    /** Immutable commit command carrying the exact accepted quote. */
    final class CommitRequest {
        private final PurchaseQuote quote;
        /** Creates a commit command. */ public CommitRequest(final PurchaseQuote quote) { this.quote = Objects.requireNonNull(quote, "quote"); }
        /** @return exact quote */ public PurchaseQuote quote() { return quote; }
    }

    /** Commit status. */
    enum Status { /** Mutation applied once. */ APPLIED, /** Same key was already applied. */ DUPLICATE, /** Revision changed. */ STALE, /** Validation failed without mutation. */ REJECTED }

    /** Immutable atomic commit outcome. */
    final class CommitResult {
        private final Status status;
        private final PurchaseFailure failure;
        private CommitResult(final Status status, final PurchaseFailure failure) {
            this.status = Objects.requireNonNull(status, "status");
            this.failure = failure;
        }
        /** @return successful applied or duplicate outcome */ public static CommitResult success(final Status status) {
            if (status != Status.APPLIED && status != Status.DUPLICATE) { throw new IllegalArgumentException("status is not successful"); }
            return new CommitResult(status, null);
        }
        /** @return stale revision outcome */ public static CommitResult stale() { return new CommitResult(Status.STALE, null); }
        /** @return atomic rejection with no mutation */ public static CommitResult rejected(final PurchaseFailure failure) {
            return new CommitResult(Status.REJECTED, Objects.requireNonNull(failure, "failure"));
        }
        /** @return commit status */ public Status status() { return status; }
        /** @return rejection details when status is rejected */ public Optional<PurchaseFailure> failure() { return Optional.ofNullable(failure); }
    }
}
