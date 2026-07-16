package io.zartra.bedwars.arena.validation;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Typed, immutable arena prerequisite profile.
 *
 * <p>Required shared generator identities are compared exactly. Custom profiles may require any
 * registered generator types, including none, without naming heuristics.</p>
 */
public final class ArenaValidationProfile {
    private static final ArenaValidationProfile STANDARD = ArenaValidationProfile.of(
            DefinitionId.of("zartra", "arena-validation/standard"),
            new TreeSet<GeneratorTypeId>(Arrays.asList(
                    GeneratorTypeId.of("zartra", "diamond"),
                    GeneratorTypeId.of("zartra", "emerald"))),
            true, true, true);

    private final DefinitionId id;
    private final Set<GeneratorTypeId> requiredSharedGeneratorTypes;
    private final boolean teamGeneratorRequired;
    private final boolean shopNpcRequired;
    private final boolean upgradeNpcRequired;

    private ArenaValidationProfile(
            final DefinitionId id, final Set<GeneratorTypeId> requiredSharedGeneratorTypes,
            final boolean teamGeneratorRequired, final boolean shopNpcRequired,
            final boolean upgradeNpcRequired) {
        this.id = Objects.requireNonNull(id, "id");
        final Set<GeneratorTypeId> copy = new TreeSet<GeneratorTypeId>();
        for (GeneratorTypeId type : Objects.requireNonNull(
                requiredSharedGeneratorTypes, "requiredSharedGeneratorTypes")) {
            copy.add(Objects.requireNonNull(type, "generator type"));
        }
        if (copy.size() > 64) {
            throw new IllegalArgumentException("too many required shared generator types");
        }
        this.requiredSharedGeneratorTypes = Collections.unmodifiableSet(copy);
        this.teamGeneratorRequired = teamGeneratorRequired;
        this.shopNpcRequired = shopNpcRequired;
        this.upgradeNpcRequired = upgradeNpcRequired;
    }

    /** @return original starter prerequisites with exact diamond and emerald types */
    public static ArenaValidationProfile standard() {
        return STANDARD;
    }

    /** @return a validated custom prerequisite profile */
    public static ArenaValidationProfile of(
            final DefinitionId id, final Set<GeneratorTypeId> requiredSharedGeneratorTypes,
            final boolean teamGeneratorRequired, final boolean shopNpcRequired,
            final boolean upgradeNpcRequired) {
        return new ArenaValidationProfile(id, requiredSharedGeneratorTypes,
                teamGeneratorRequired, shopNpcRequired, upgradeNpcRequired);
    }

    /** @return stable profile identity */ public DefinitionId id() { return id; }
    /** @return exact shared generator type requirements */
    public Set<GeneratorTypeId> requiredSharedGeneratorTypes() {
        return requiredSharedGeneratorTypes;
    }
    /** @return whether every team needs an owned generator */
    public boolean teamGeneratorRequired() { return teamGeneratorRequired; }
    /** @return whether every team needs a shop NPC */
    public boolean shopNpcRequired() { return shopNpcRequired; }
    /** @return whether every team needs an upgrade NPC */
    public boolean upgradeNpcRequired() { return upgradeNpcRequired; }

    @Override public int hashCode() {
        return Objects.hash(id, requiredSharedGeneratorTypes, teamGeneratorRequired,
                shopNpcRequired, upgradeNpcRequired);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ArenaValidationProfile)) { return false; }
        final ArenaValidationProfile that = (ArenaValidationProfile) other;
        return teamGeneratorRequired == that.teamGeneratorRequired
                && shopNpcRequired == that.shopNpcRequired
                && upgradeNpcRequired == that.upgradeNpcRequired && id.equals(that.id)
                && requiredSharedGeneratorTypes.equals(that.requiredSharedGeneratorTypes);
    }
}
