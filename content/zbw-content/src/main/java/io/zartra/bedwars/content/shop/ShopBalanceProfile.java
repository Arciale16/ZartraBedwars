package io.zartra.bedwars.content.shop;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.shop.api.ShopCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable original shop price, cooldown, stock and limit multiplier profile. */
public final class ShopBalanceProfile {
    private static final BigDecimal MINIMUM = new BigDecimal("0.05");
    private static final BigDecimal MAXIMUM = new BigDecimal("10.00");
    private final DefinitionId id;
    private final BigDecimal priceMultiplier;
    private final BigDecimal cooldownMultiplier;
    private final BigDecimal teamStockMultiplier;
    private final BigDecimal personalLimitMultiplier;

    /** Creates a profile with bounded positive decimal multipliers. */
    public ShopBalanceProfile(final DefinitionId id, final BigDecimal priceMultiplier,
                              final BigDecimal cooldownMultiplier,
                              final BigDecimal teamStockMultiplier,
                              final BigDecimal personalLimitMultiplier) {
        this.id = Objects.requireNonNull(id, "id");
        this.priceMultiplier = checked(priceMultiplier, "priceMultiplier");
        this.cooldownMultiplier = checked(cooldownMultiplier, "cooldownMultiplier");
        this.teamStockMultiplier = checked(teamStockMultiplier, "teamStockMultiplier");
        this.personalLimitMultiplier = checked(personalLimitMultiplier, "personalLimitMultiplier");
    }

    private static BigDecimal checked(final BigDecimal value, final String label) {
        final BigDecimal checked = Objects.requireNonNull(value, label).stripTrailingZeros();
        if (checked.compareTo(MINIMUM) < 0 || checked.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException(label + " is out of range");
        }
        return checked;
    }

    /** @return stable profile identity */ public DefinitionId id() { return id; }
    /** @return price multiplier */ public BigDecimal priceMultiplier() { return priceMultiplier; }
    /** @return cooldown multiplier */ public BigDecimal cooldownMultiplier() { return cooldownMultiplier; }
    /** @return team-stock multiplier */ public BigDecimal teamStockMultiplier() { return teamStockMultiplier; }
    /** @return personal-limit multiplier */ public BigDecimal personalLimitMultiplier() { return personalLimitMultiplier; }

    /** Applies deterministic ceiling rounding independently to each tender amount. */
    public ShopCatalog.Price applyPrice(final ShopCatalog.Price price) {
        final List<ShopCatalog.ResourceAmount> result = new ArrayList<ShopCatalog.ResourceAmount>();
        for (ShopCatalog.ResourceAmount amount : Objects.requireNonNull(price, "price").amounts()) {
            final long adjusted = BigDecimal.valueOf(amount.amount()).multiply(priceMultiplier)
                    .setScale(0, RoundingMode.CEILING).longValueExact();
            result.add(new ShopCatalog.ResourceAmount(amount.resourceId(), adjusted));
        }
        return new ShopCatalog.Price(result);
    }

    /** Applies deterministic ceiling rounding to non-zero cooldown milliseconds. */
    public Duration applyCooldown(final Duration cooldown) {
        final long millis = Objects.requireNonNull(cooldown, "cooldown").toMillis();
        if (millis == 0) { return Duration.ZERO; }
        return Duration.ofMillis(BigDecimal.valueOf(millis).multiply(cooldownMultiplier)
                .setScale(0, RoundingMode.CEILING).longValueExact());
    }

    /** Applies personal-limit rounding; zero remains the unlimited sentinel. */
    public int applyPersonalLimit(final int limit) {
        return adjustedLimit(limit, personalLimitMultiplier);
    }

    /** Applies team-stock rounding; zero remains the unlimited sentinel. */
    public int applyTeamStock(final int limit) {
        return adjustedLimit(limit, teamStockMultiplier);
    }

    private static int adjustedLimit(final int limit, final BigDecimal multiplier) {
        if (limit < 0) { throw new IllegalArgumentException("limit must not be negative"); }
        if (limit == 0) { return 0; }
        return BigDecimal.valueOf(limit).multiply(multiplier)
                .setScale(0, RoundingMode.CEILING).intValueExact();
    }
}
