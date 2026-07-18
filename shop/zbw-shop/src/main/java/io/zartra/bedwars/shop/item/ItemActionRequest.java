package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.shop.api.PurchaseContext;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable authenticated request to execute one owned utility item. */
public final class ItemActionRequest {
    private final PurchaseContext context;
    private final DefinitionId actionId;
    private final IdempotencyKey key;
    private final Instant requestedAt;
    private final Target target;

    /** Creates a request; target semantics are checked against the action definition. */
    public ItemActionRequest(final PurchaseContext context, final DefinitionId actionId,
                             final IdempotencyKey key, final Instant requestedAt,
                             final Optional<Target> target) {
        this.context = Objects.requireNonNull(context, "context");
        this.actionId = Objects.requireNonNull(actionId, "actionId");
        this.key = Objects.requireNonNull(key, "key");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.target = Objects.requireNonNull(target, "target").orElse(null);
    }

    /** @return match/player/team context */ public PurchaseContext context() { return context; }
    /** @return requested action */ public DefinitionId actionId() { return actionId; }
    /** @return exactly-once key */ public IdempotencyKey key() { return key; }
    /** @return caller supplied timestamp */ public Instant requestedAt() { return requestedAt; }
    /** @return optional typed target */ public Optional<Target> target() { return Optional.ofNullable(target); }

    /** Platform-neutral target facts already established by the owning adapter. */
    public static final class Target {
        private final DefinitionId type;
        private final DefinitionId identity;
        private final PlayerId player;
        private final DefinitionId team;
        private final boolean active;
        private final boolean buildAllowed;

        /** Creates server-authoritative target facts used by neutral validation. */
        public Target(final DefinitionId type, final DefinitionId identity,
                      final Optional<PlayerId> player, final Optional<DefinitionId> team,
                      final boolean active, final boolean buildAllowed) {
            this.type = Objects.requireNonNull(type, "type");
            this.identity = Objects.requireNonNull(identity, "identity");
            this.player = Objects.requireNonNull(player, "player").orElse(null);
            this.team = Objects.requireNonNull(team, "team").orElse(null);
            this.active = active;
            this.buildAllowed = buildAllowed;
        }
        /** @return semantic target type */ public DefinitionId type() { return type; }
        /** @return target identity */ public DefinitionId identity() { return identity; }
        /** @return target player */ public Optional<PlayerId> player() { return Optional.ofNullable(player); }
        /** @return target team */ public Optional<DefinitionId> team() { return Optional.ofNullable(team); }
        /** @return whether target is active */ public boolean active() { return active; }
        /** @return whether arena policy allows building/effects */ public boolean buildAllowed() { return buildAllowed; }
    }
}
