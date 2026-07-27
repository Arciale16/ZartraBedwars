package io.zartra.bedwars.integration.placeholderapi;

import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderContext;
import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderId;
import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderRegistry;
import io.zartra.bedwars.integration.placeholderapi.api.PlaceholderResolver;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable registry of typed M16 placeholder resolvers.
 */
public final class PlaceholderApiLifecycle {

    public static final String NAMESPACE = "zbw";

    private final PlaceholderRegistry registry;
    private final PlaceholderApiProviders providers;

    public PlaceholderApiLifecycle(final PlaceholderApiProviders providers) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.registry = new PlaceholderRegistry();
        registerDefaultResolvers();
    }

    public PlaceholderRegistry registry() {
        return registry;
    }

    public String resolve(final String placeholder, final UUID playerId, final String fallback) {
        return registry.resolve(PlaceholderContext.of(NAMESPACE, playerId, placeholder.toLowerCase(), fallback, true)).value();
    }

    private void registerDefaultResolvers() {
        register("progression_level", context -> stringValue(providers.playerLevel, context));
        register("progression_prestige", context -> stringValue(providers.playerPrestige, context));
        register("progression_experience", context -> stringValue(providers.playerExperience, context));
        register("progression_currency", context -> stringValue(providers.playerCurrency, context));

        register("stats_wins", context -> stringValue(providers.wins, context));
        register("stats_losses", context -> stringValue(providers.losses, context));
        register("stats_kills", context -> stringValue(providers.kills, context));
        register("stats_deaths", context -> stringValue(providers.deaths, context));
        register("stats_beds_destroyed", context -> stringValue(providers.bedsDestroyed, context));
        register("stats_leaderboard_position", context -> stringValue(providers.leaderboardPosition, context));

        register("content_quests", context -> stringValue(providers.activeQuests, context));
        register("content_achievements", context -> stringValue(providers.achievements, context));
        register("content_challenges", context -> stringValue(providers.challenges, context));
        register("content_battlepass", context -> stringValue(providers.battlePassProgress, context));

        register("cosmetics_equipped", context -> stringValue(providers.equippedCosmetic, context));
        register("cosmetics_profile_visibility", context -> stringValue(providers.profileVisibility, context));
        register("cosmetics_active_campaign", context -> stringValue(providers.activeCampaign, context));
    }

    private void register(final String identifier, final PlaceholderResolver resolver) {
        registry.register(PlaceholderId.of(identifier), resolver);
    }

    private static io.zartra.bedwars.integration.placeholderapi.api.PlaceholderResult stringValue(
            final PlaceholderDataProvider<String> provider,
            final PlaceholderContext context
    ) {
        if (!context.playerId().isPresent()) {
            return io.zartra.bedwars.integration.placeholderapi.api.PlaceholderResult.unavailable(context.fallback());
        }
        return Optional.ofNullable(context.playerId())
                .flatMap(id -> provider.getValue(id.get()))
                .filter(value -> !value.isEmpty())
                .map(io.zartra.bedwars.integration.placeholderapi.api.PlaceholderResult::found)
                .orElse(io.zartra.bedwars.integration.placeholderapi.api.PlaceholderResult.fallback(context.fallback()));
    }
}
