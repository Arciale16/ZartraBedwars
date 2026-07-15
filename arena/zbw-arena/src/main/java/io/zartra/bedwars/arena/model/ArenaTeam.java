package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;
import java.util.Optional;

/** Immutable team setup definition including spawn and bed orientation. */
public final class ArenaTeam implements Comparable<ArenaTeam> {
    private final DefinitionId id;
    private final String displayName;
    private final DefinitionId color;
    private final ArenaLocation spawn;
    private final ArenaLocation bed;
    private final DefinitionId bedFacing;

    private ArenaTeam(final DefinitionId id, final String displayName, final DefinitionId color,
                      final ArenaLocation spawn, final ArenaLocation bed,
                      final DefinitionId bedFacing) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = text(displayName, "displayName", 48);
        this.color = Objects.requireNonNull(color, "color");
        this.spawn = spawn;
        this.bed = bed;
        this.bedFacing = bedFacing;
        if ((bed == null) != (bedFacing == null)) {
            throw new IllegalArgumentException("bed and bedFacing must be configured together");
        }
    }

    /** @return a team whose spatial setup may be completed later */
    public static ArenaTeam create(final DefinitionId id, final String displayName,
                                   final DefinitionId color) {
        return new ArenaTeam(id, displayName, color, null, null, null);
    }

    private static String text(final String value, final String label, final int maximum) {
        if (value == null || value.trim().isEmpty() || value.length() > maximum
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is blank, unsafe or too long");
        }
        return value;
    }

    /** @return a copy with a player spawn */
    public ArenaTeam withSpawn(final ArenaLocation value) {
        return new ArenaTeam(id, displayName, color, Objects.requireNonNull(value, "value"),
                bed, bedFacing);
    }
    /** @return a copy with a bed position and semantic facing */
    public ArenaTeam withBed(final ArenaLocation value, final DefinitionId facing) {
        return new ArenaTeam(id, displayName, color, spawn,
                Objects.requireNonNull(value, "value"), Objects.requireNonNull(facing, "facing"));
    }
    /** @return stable team identity */ public DefinitionId id() { return id; }
    /** @return mutable-through-copy display name */ public String displayName() { return displayName; }
    /** @return semantic team color */ public DefinitionId color() { return color; }
    /** @return configured player spawn */ public Optional<ArenaLocation> spawn() { return Optional.ofNullable(spawn); }
    /** @return configured bed position */ public Optional<ArenaLocation> bed() { return Optional.ofNullable(bed); }
    /** @return semantic bed facing */ public Optional<DefinitionId> bedFacing() { return Optional.ofNullable(bedFacing); }
    @Override public int compareTo(final ArenaTeam other) { return id.compareTo(Objects.requireNonNull(other, "other").id); }
    @Override public int hashCode() { return Objects.hash(id, displayName, color, spawn, bed, bedFacing); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaTeam)) { return false; }
        final ArenaTeam that = (ArenaTeam) other;
        return Objects.deepEquals(new Object[] {id, displayName, color, spawn, bed, bedFacing},
                new Object[] {that.id, that.displayName, that.color, that.spawn, that.bed,
                    that.bedFacing});
    }
}
