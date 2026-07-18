package io.zartra.bedwars.shop.upgrade;

import java.util.Objects;
import java.util.Optional;

/** Typed outcome of validation plus atomic team-upgrade purchase. */
public final class UpgradePurchaseResult {
    /** Stable expected outcomes. */
    public enum Status {
        PURCHASED, DUPLICATE, UNKNOWN_UPGRADE, INVALID_MATCH, UNKNOWN_TEAM, MAXIMUM_LEVEL,
        DEPENDENCY_MISSING, INSUFFICIENT_RESOURCES, REVISION_CONFLICT, CLEANED
    }
    private final Status status;
    private final TeamUpgradeState state;
    private UpgradePurchaseResult(final Status status, final TeamUpgradeState state) {
        this.status = Objects.requireNonNull(status, "status");
        this.state = state;
    }
    static UpgradePurchaseResult of(final Status status, final TeamUpgradeState state) {
        return new UpgradePurchaseResult(status, state);
    }
    /** @return typed status */ public Status status() { return status; }
    /** @return new/current state for successful or duplicate outcomes */ public Optional<TeamUpgradeState> state() { return Optional.ofNullable(state); }
}
