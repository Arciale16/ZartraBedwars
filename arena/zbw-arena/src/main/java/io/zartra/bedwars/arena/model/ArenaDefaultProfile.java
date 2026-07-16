package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.domain.team.TeamLayoutLimits;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable, replaceable defaults used when creating a new arena draft.
 *
 * <p>The profile is construction input only. Every selected value is copied into the resulting
 * {@link ArenaDefinition}, so later profile changes cannot alter existing arenas.</p>
 */
public final class ArenaDefaultProfile {
    private static final ArenaDefaultProfile STANDARD = ArenaDefaultProfile.of(
            DefinitionId.of("zartra", "arena-profile/standard"),
            DefinitionId.of("zartra", "world/native"),
            DefinitionId.of("zartra", "group/default"),
            Collections.singleton(DefinitionId.of("zartra", "mode/standard")),
            2, 16, 4, 0, 100, -16.0D, 0.0D, 256.0D);

    private final DefinitionId id;
    private final DefinitionId worldAdapter;
    private final DefinitionId group;
    private final Set<DefinitionId> modes;
    private final int minimumPlayers;
    private final int maximumPlayers;
    private final int teamSize;
    private final int priority;
    private final int rotationWeight;
    private final double voidY;
    private final double buildMinimumY;
    private final double buildMaximumY;

    private ArenaDefaultProfile(
            final DefinitionId id, final DefinitionId worldAdapter, final DefinitionId group,
            final Set<DefinitionId> modes, final int minimumPlayers,
            final int maximumPlayers, final int teamSize, final int priority,
            final int rotationWeight, final double voidY, final double buildMinimumY,
            final double buildMaximumY) {
        this.id = Objects.requireNonNull(id, "id");
        this.worldAdapter = Objects.requireNonNull(worldAdapter, "worldAdapter");
        this.group = Objects.requireNonNull(group, "group");
        final Set<DefinitionId> modeCopy = new TreeSet<DefinitionId>();
        for (DefinitionId mode : Objects.requireNonNull(modes, "modes")) {
            modeCopy.add(Objects.requireNonNull(mode, "mode"));
        }
        if (modeCopy.isEmpty() || modeCopy.size() > 64) {
            throw new IllegalArgumentException("modes must contain between 1 and 64 entries");
        }
        this.modes = Collections.unmodifiableSet(modeCopy);
        TeamLayoutLimits.requireMaximumPlayers(maximumPlayers);
        TeamLayoutLimits.requireTeamCapacity(teamSize);
        if (minimumPlayers < 1 || minimumPlayers > maximumPlayers) {
            throw new IllegalArgumentException("minimum players must not exceed maximum players");
        }
        if (priority < -100000 || priority > 100000
                || rotationWeight < 1 || rotationWeight > 100000) {
            throw new IllegalArgumentException("invalid selection defaults");
        }
        finite(voidY, "voidY");
        finite(buildMinimumY, "buildMinimumY");
        finite(buildMaximumY, "buildMaximumY");
        if (buildMaximumY < buildMinimumY) {
            throw new IllegalArgumentException("build maximum precedes minimum");
        }
        this.minimumPlayers = minimumPlayers;
        this.maximumPlayers = maximumPlayers;
        this.teamSize = teamSize;
        this.priority = priority;
        this.rotationWeight = rotationWeight;
        this.voidY = voidY;
        this.buildMinimumY = buildMinimumY;
        this.buildMaximumY = buildMaximumY;
    }

    /** @return the original starter profile */
    public static ArenaDefaultProfile standard() {
        return STANDARD;
    }

    /**
     * Creates an operator- or extension-defined default profile.
     *
     * @return validated immutable profile
     */
    public static ArenaDefaultProfile of(
            final DefinitionId id, final DefinitionId worldAdapter, final DefinitionId group,
            final Set<DefinitionId> modes, final int minimumPlayers,
            final int maximumPlayers, final int teamSize, final int priority,
            final int rotationWeight, final double voidY, final double buildMinimumY,
            final double buildMaximumY) {
        return new ArenaDefaultProfile(id, worldAdapter, group, modes, minimumPlayers,
                maximumPlayers, teamSize, priority, rotationWeight, voidY, buildMinimumY,
                buildMaximumY);
    }

    private static void finite(final double value, final String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    /** @return stable profile identity */ public DefinitionId id() { return id; }
    /** @return default world-provider adapter */ public DefinitionId worldAdapter() { return worldAdapter; }
    /** @return default arena group */ public DefinitionId group() { return group; }
    /** @return immutable default mode identities */ public Set<DefinitionId> modes() { return modes; }
    /** @return default minimum player count */ public int minimumPlayers() { return minimumPlayers; }
    /** @return default maximum player count */ public int maximumPlayers() { return maximumPlayers; }
    /** @return default per-team capacity */ public int teamSize() { return teamSize; }
    /** @return default selector priority */ public int priority() { return priority; }
    /** @return default selector rotation weight */ public int rotationWeight() { return rotationWeight; }
    /** @return default void boundary */ public double voidY() { return voidY; }
    /** @return default build minimum */ public double buildMinimumY() { return buildMinimumY; }
    /** @return default build maximum */ public double buildMaximumY() { return buildMaximumY; }

    @Override public int hashCode() {
        return Objects.hash(id, worldAdapter, group, modes, minimumPlayers, maximumPlayers,
                teamSize, priority, rotationWeight, voidY, buildMinimumY, buildMaximumY);
    }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ArenaDefaultProfile)) { return false; }
        final ArenaDefaultProfile that = (ArenaDefaultProfile) other;
        return minimumPlayers == that.minimumPlayers && maximumPlayers == that.maximumPlayers
                && teamSize == that.teamSize && priority == that.priority
                && rotationWeight == that.rotationWeight
                && Double.compare(voidY, that.voidY) == 0
                && Double.compare(buildMinimumY, that.buildMinimumY) == 0
                && Double.compare(buildMaximumY, that.buildMaximumY) == 0
                && id.equals(that.id) && worldAdapter.equals(that.worldAdapter)
                && group.equals(that.group) && modes.equals(that.modes);
    }
}
