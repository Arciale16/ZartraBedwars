package io.zartra.bedwars.game.addon;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Plans one atomic private Ender Chest resource transfer without platform access. */
public final class DepositPolicy {
    private DepositPolicy() { }

    /** Validates and produces an all-or-accounted inventory mutation. */
    public static Outcome plan(final Request request,
                               final PlayerStateSnapshot.Inventory source,
                               final PlayerStateSnapshot.Inventory enderChest,
                               final Rules rules, final Instant now) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(enderChest, "enderChest");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(now, "now");
        if (!rules.allowedStates.contains(request.state)) { return Outcome.rejected(Status.STATE); }
        if (now.isBefore(request.lastDeposit.plus(rules.cooldown))) { return Outcome.rejected(Status.COOLDOWN); }
        if (request.depositedThisMatch >= rules.perMatchLimit) { return Outcome.rejected(Status.LIMIT); }
        final Map<Integer, PlayerStateSnapshot.Item> nextSource =
                new LinkedHashMap<Integer, PlayerStateSnapshot.Item>(source.occupied());
        final Map<Integer, PlayerStateSnapshot.Item> nextChest =
                new LinkedHashMap<Integer, PlayerStateSnapshot.Item>(enderChest.occupied());
        int wanted = request.quantity;
        int transferred = 0;
        for (Map.Entry<Integer, PlayerStateSnapshot.Item> entry
                : new ArrayList<Map.Entry<Integer, PlayerStateSnapshot.Item>>(nextSource.entrySet())) {
            if (wanted == 0) { break; }
            final PlayerStateSnapshot.Item item = entry.getValue();
            if (!item.itemId().equals(request.resource)) { continue; }
            if (!rules.allowedResources.contains(item.itemId())) { return Outcome.rejected(Status.RESOURCE); }
            if ("true".equals(item.metadata().get("zartra.protected"))) {
                return Outcome.rejected(Status.PROTECTED);
            }
            final int requested = Math.min(wanted, item.amount());
            final int accepted = insert(nextChest, enderChest.size(), item, requested);
            if (accepted == 0) { break; }
            final int remaining = item.amount() - accepted;
            if (remaining == 0) { nextSource.remove(entry.getKey()); }
            else { nextSource.put(entry.getKey(), item.withAmount(remaining)); }
            transferred += accepted;
            wanted -= accepted;
        }
        if (transferred == 0) { return Outcome.rejected(Status.NO_CAPACITY); }
        final Status status = wanted == 0 ? Status.COMPLETE : Status.PARTIAL;
        return new Outcome(status, transferred, wanted,
                new PlayerStateSnapshot.Inventory(source.size(), nextSource),
                new PlayerStateSnapshot.Inventory(enderChest.size(), nextChest));
    }

    private static int insert(final Map<Integer, PlayerStateSnapshot.Item> target,
                              final int size, final PlayerStateSnapshot.Item item,
                              final int requested) {
        int remaining = requested;
        for (Map.Entry<Integer, PlayerStateSnapshot.Item> entry : target.entrySet()) {
            final PlayerStateSnapshot.Item existing = entry.getValue();
            if (existing.itemId().equals(item.itemId())
                    && existing.metadata().equals(item.metadata()) && existing.amount() < 64) {
                final int accepted = Math.min(64 - existing.amount(), remaining);
                entry.setValue(existing.withAmount(existing.amount() + accepted));
                remaining -= accepted;
                if (remaining == 0) { return requested; }
            }
        }
        for (int slot = 0; slot < size && remaining > 0; slot++) {
            if (!target.containsKey(Integer.valueOf(slot))) {
                final int accepted = Math.min(64, remaining);
                target.put(Integer.valueOf(slot), item.withAmount(accepted));
                remaining -= accepted;
            }
        }
        return requested - remaining;
    }

    /** Immutable per-request facts already authorized by its use case. */
    public static final class Request {
        private final DefinitionId resource;
        private final int quantity;
        private final HotbarPolicy.State state;
        private final Instant lastDeposit;
        private final int depositedThisMatch;
        /** Creates a bounded explicit-quantity request. */
        public Request(final DefinitionId resource, final int quantity,
                       final HotbarPolicy.State state, final Instant lastDeposit,
                       final int depositedThisMatch) {
            if (quantity < 1 || quantity > 4096 || depositedThisMatch < 0) {
                throw new IllegalArgumentException("deposit quantities are out of bounds");
            }
            this.resource = Objects.requireNonNull(resource, "resource");
            this.quantity = quantity;
            this.state = Objects.requireNonNull(state, "state");
            this.lastDeposit = Objects.requireNonNull(lastDeposit, "lastDeposit");
            this.depositedThisMatch = depositedThisMatch;
        }
    }

    /** Immutable allow-list, state, cooldown and match-limit rules. */
    public static final class Rules {
        private final Set<DefinitionId> allowedResources;
        private final Set<HotbarPolicy.State> allowedStates;
        private final Duration cooldown;
        private final int perMatchLimit;
        /** Creates validated deposit rules. */
        public Rules(final Set<DefinitionId> resources, final Set<HotbarPolicy.State> states,
                     final Duration cooldown, final int perMatchLimit) {
            if (resources == null || resources.isEmpty() || resources.contains(null)
                    || states == null || states.isEmpty() || states.contains(null)) {
                throw new IllegalArgumentException("deposit allow lists cannot be empty");
            }
            if (cooldown == null || cooldown.isNegative() || cooldown.compareTo(Duration.ofMinutes(5)) > 0
                    || perMatchLimit < 1 || perMatchLimit > 100000) {
                throw new IllegalArgumentException("deposit cooldown or limit is invalid");
            }
            this.allowedResources = Collections.unmodifiableSet(
                    new LinkedHashSet<DefinitionId>(resources));
            this.allowedStates = Collections.unmodifiableSet(
                    new LinkedHashSet<HotbarPolicy.State>(states));
            this.cooldown = cooldown;
            this.perMatchLimit = perMatchLimit;
        }
    }

    /** Complete atomic transfer result suitable for owner-thread compare/apply. */
    public static final class Outcome {
        private final Status status;
        private final int transferred;
        private final int unfulfilled;
        private final PlayerStateSnapshot.Inventory source;
        private final PlayerStateSnapshot.Inventory enderChest;
        private Outcome(final Status status, final int transferred, final int unfulfilled,
                        final PlayerStateSnapshot.Inventory source,
                        final PlayerStateSnapshot.Inventory enderChest) {
            this.status = status;
            this.transferred = transferred;
            this.unfulfilled = unfulfilled;
            this.source = source;
            this.enderChest = enderChest;
        }
        private static Outcome rejected(final Status status) { return new Outcome(status, 0, 0, null, null); }
        /** @return transfer disposition */ public Status status() { return status; }
        /** @return exact accepted count */ public int transferred() { return transferred; }
        /** @return requested count not transferred */ public int unfulfilled() { return unfulfilled; }
        /** @return whether owner-thread inventories should be replaced */ public boolean accepted() { return source != null; }
        /** @return resulting source inventory */ public PlayerStateSnapshot.Inventory source() { return require(source); }
        /** @return resulting private Ender Chest */ public PlayerStateSnapshot.Inventory enderChest() { return require(enderChest); }
        private static PlayerStateSnapshot.Inventory require(final PlayerStateSnapshot.Inventory value) {
            if (value == null) { throw new IllegalStateException("rejected transfer has no inventory mutation"); }
            return value;
        }
    }

    /** Typed transfer outcomes rendered through localization. */
    public enum Status {
        /** Entire request transferred. */ COMPLETE,
        /** Available capacity accepted a strict subset. */ PARTIAL,
        /** State is not eligible. */ STATE,
        /** Cooldown has not elapsed. */ COOLDOWN,
        /** Per-match limit reached. */ LIMIT,
        /** Resource is not allowed. */ RESOURCE,
        /** Synthetic/protected item rejected. */ PROTECTED,
        /** Destination has no capacity. */ NO_CAPACITY
    }
}
