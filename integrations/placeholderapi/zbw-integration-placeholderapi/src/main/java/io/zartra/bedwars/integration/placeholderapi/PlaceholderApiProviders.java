package io.zartra.bedwars.integration.placeholderapi;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Stable injection point for all PlaceholderAPI values.
 */
public final class PlaceholderApiProviders {

    public final PlaceholderDataProvider<String> playerLevel;
    public final PlaceholderDataProvider<String> playerPrestige;
    public final PlaceholderDataProvider<String> playerExperience;
    public final PlaceholderDataProvider<String> playerCurrency;
    public final PlaceholderDataProvider<String> wins;
    public final PlaceholderDataProvider<String> losses;
    public final PlaceholderDataProvider<String> kills;
    public final PlaceholderDataProvider<String> deaths;
    public final PlaceholderDataProvider<String> bedsDestroyed;
    public final PlaceholderDataProvider<String> activeQuests;
    public final PlaceholderDataProvider<String> achievements;
    public final PlaceholderDataProvider<String> challenges;
    public final PlaceholderDataProvider<String> battlePassProgress;
    public final PlaceholderDataProvider<String> equippedCosmetic;
    public final PlaceholderDataProvider<String> profileVisibility;
    public final PlaceholderDataProvider<String> activeCampaign;
    public final PlaceholderDataProvider<String> leaderboardPosition;

    public PlaceholderApiProviders(
            final PlaceholderDataProvider<String> playerLevel,
            final PlaceholderDataProvider<String> playerPrestige,
            final PlaceholderDataProvider<String> playerExperience,
            final PlaceholderDataProvider<String> playerCurrency,
            final PlaceholderDataProvider<String> wins,
            final PlaceholderDataProvider<String> losses,
            final PlaceholderDataProvider<String> kills,
            final PlaceholderDataProvider<String> deaths,
            final PlaceholderDataProvider<String> bedsDestroyed,
            final PlaceholderDataProvider<String> activeQuests,
            final PlaceholderDataProvider<String> achievements,
            final PlaceholderDataProvider<String> challenges,
            final PlaceholderDataProvider<String> battlePassProgress,
            final PlaceholderDataProvider<String> equippedCosmetic,
            final PlaceholderDataProvider<String> profileVisibility,
            final PlaceholderDataProvider<String> activeCampaign,
            final PlaceholderDataProvider<String> leaderboardPosition
    ) {
        this.playerLevel = Objects.requireNonNull(playerLevel, "playerLevel");
        this.playerPrestige = Objects.requireNonNull(playerPrestige, "playerPrestige");
        this.playerExperience = Objects.requireNonNull(playerExperience, "playerExperience");
        this.playerCurrency = Objects.requireNonNull(playerCurrency, "playerCurrency");
        this.wins = Objects.requireNonNull(wins, "wins");
        this.losses = Objects.requireNonNull(losses, "losses");
        this.kills = Objects.requireNonNull(kills, "kills");
        this.deaths = Objects.requireNonNull(deaths, "deaths");
        this.bedsDestroyed = Objects.requireNonNull(bedsDestroyed, "bedsDestroyed");
        this.activeQuests = Objects.requireNonNull(activeQuests, "activeQuests");
        this.achievements = Objects.requireNonNull(achievements, "achievements");
        this.challenges = Objects.requireNonNull(challenges, "challenges");
        this.battlePassProgress = Objects.requireNonNull(battlePassProgress, "battlePassProgress");
        this.equippedCosmetic = Objects.requireNonNull(equippedCosmetic, "equippedCosmetic");
        this.profileVisibility = Objects.requireNonNull(profileVisibility, "profileVisibility");
        this.activeCampaign = Objects.requireNonNull(activeCampaign, "activeCampaign");
        this.leaderboardPosition = Objects.requireNonNull(leaderboardPosition, "leaderboardPosition");
    }

    public static PlaceholderApiProviders fallback() {
        final PlaceholderDataProvider<String> absent = id -> Optional.empty();
        return new PlaceholderApiProviders(
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent,
                absent
        );
    }

    public String resolveOrElse(final PlaceholderDataProvider<String> provider, final UUID playerId, final String fallback) {
        return provider.getValue(playerId).filter(value -> !value.isEmpty()).orElse(fallback);
    }
}
