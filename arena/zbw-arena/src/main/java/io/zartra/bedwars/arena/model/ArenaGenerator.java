package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Immutable team, diamond, emerald or custom generator placement. */
public final class ArenaGenerator implements Comparable<ArenaGenerator> {
    private final DefinitionId id;
    private final GeneratorTypeId type;
    private final ResourceId resource;
    private final DefinitionId teamId;
    private final ArenaLocation location;
    private final Duration interval;

    private ArenaGenerator(final DefinitionId id, final GeneratorTypeId type,
                           final ResourceId resource, final DefinitionId teamId,
                           final ArenaLocation location, final Duration interval) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.teamId = teamId;
        this.location = Objects.requireNonNull(location, "location");
        this.interval = Objects.requireNonNull(interval, "interval");
        if (interval.isZero() || interval.isNegative() || interval.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("interval must be positive and at most one hour");
        }
    }

    /** @return a validated generator definition */
    public static ArenaGenerator of(final DefinitionId id, final GeneratorTypeId type,
                                    final ResourceId resource, final DefinitionId teamId,
                                    final ArenaLocation location, final Duration interval) {
        return new ArenaGenerator(id, type, resource, teamId, location, interval);
    }

    /** @return generator identity */ public DefinitionId id() { return id; }
    /** @return extension-safe generator type */ public GeneratorTypeId type() { return type; }
    /** @return generated resource */ public ResourceId resource() { return resource; }
    /** @return owning team for team generators */ public Optional<DefinitionId> teamId() { return Optional.ofNullable(teamId); }
    /** @return generator position */ public ArenaLocation location() { return location; }
    /** @return base generation interval */ public Duration interval() { return interval; }
    @Override public int compareTo(final ArenaGenerator other) { return id.compareTo(Objects.requireNonNull(other, "other").id); }
    @Override public int hashCode() { return Objects.hash(id, type, resource, teamId, location, interval); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof ArenaGenerator)) { return false; }
        final ArenaGenerator that = (ArenaGenerator) other;
        return Objects.deepEquals(new Object[] {id, type, resource, teamId, location, interval},
                new Object[] {that.id, that.type, that.resource, that.teamId, that.location,
                    that.interval});
    }
}
