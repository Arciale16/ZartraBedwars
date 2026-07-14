package io.zartra.bedwars.integration.discord.api;

import io.zartra.bedwars.api.identity.CapabilityId;

/** Stable capability IDs used for Discord provider negotiation. */
public final class DiscordCapabilities {
    /** Outbound notification delivery. */ public static final CapabilityId NOTIFICATIONS = CapabilityId.of("zartra", "discord/notifications");
    /** Scoped statistics queries. */ public static final CapabilityId STATISTICS = CapabilityId.of("zartra", "discord/statistics");
    /** Scoped leaderboard queries. */ public static final CapabilityId LEADERBOARDS = CapabilityId.of("zartra", "discord/leaderboards");
    /** Verified account-link workflows. */ public static final CapabilityId ACCOUNT_LINKING = CapabilityId.of("zartra", "discord/account_linking");
    /** Provider health reporting. */ public static final CapabilityId HEALTH = CapabilityId.of("zartra", "discord/health");
    private DiscordCapabilities() { throw new AssertionError("No instances"); }
}
