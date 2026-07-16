package io.zartra.bedwars.arena.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.domain.team.TeamLayoutLimits;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable map metadata whose identity survives rename, import, backup and migration. */
public final class MapDefinition {
    private final MapId id;
    private final String displayName;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;
    private final DefinitionId template;
    private final DefinitionId group;
    private final DefinitionId author;
    private final String description;
    private final Set<DefinitionId> supportedModes;
    private final int minimumTeamSize;
    private final int maximumTeamSize;
    private final Set<DefinitionId> tags;
    private final Map<DefinitionId, String> metadata;
    private final DefinitionId validationStatus;

    /** Creates validated immutable map metadata. */
    public MapDefinition(final MapId id, final String displayName, final Instant createdAt,
                         final Instant updatedAt, final long version, final DefinitionId template,
                         final DefinitionId group, final DefinitionId author,
                         final String description, final Set<DefinitionId> supportedModes,
                         final int minimumTeamSize, final int maximumTeamSize,
                         final Set<DefinitionId> tags, final Map<DefinitionId, String> metadata,
                         final DefinitionId validationStatus) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = text(displayName, "displayName", 64, false);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) { throw new IllegalArgumentException("updatedAt precedes createdAt"); }
        if (version < 0L) { throw new IllegalArgumentException("version must be non-negative"); }
        this.version = version;
        this.template = Objects.requireNonNull(template, "template");
        this.group = Objects.requireNonNull(group, "group");
        this.author = Objects.requireNonNull(author, "author");
        this.description = text(description, "description", 1024, true);
        this.supportedModes = immutableSet(supportedModes, "supportedModes", 64, false);
        TeamLayoutLimits.requireTeamCapacity(minimumTeamSize);
        TeamLayoutLimits.requireTeamCapacity(maximumTeamSize);
        if (maximumTeamSize < minimumTeamSize) {
            throw new IllegalArgumentException("team-size range must be between 1 and 64");
        }
        this.minimumTeamSize = minimumTeamSize;
        this.maximumTeamSize = maximumTeamSize;
        this.tags = immutableSet(tags, "tags", 64, true);
        this.metadata = immutableMetadata(metadata);
        this.validationStatus = Objects.requireNonNull(validationStatus, "validationStatus");
    }

    private static String text(final String value, final String label, final int maximum,
                               final boolean emptyAllowed) {
        if (value == null || (!emptyAllowed && value.trim().isEmpty()) || value.length() > maximum
                || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return value;
    }

    private static Set<DefinitionId> immutableSet(final Set<DefinitionId> source,
                                                   final String label, final int maximum,
                                                   final boolean emptyAllowed) {
        final Set<DefinitionId> copy = new TreeSet<DefinitionId>();
        for (DefinitionId value : Objects.requireNonNull(source, label)) {
            copy.add(Objects.requireNonNull(value, label + " entry"));
        }
        if ((!emptyAllowed && copy.isEmpty()) || copy.size() > maximum) {
            throw new IllegalArgumentException(label + " has an invalid size or null entry");
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Map<DefinitionId, String> immutableMetadata(final Map<DefinitionId, String> source) {
        final Map<DefinitionId, String> copy = new TreeMap<DefinitionId, String>();
        for (Map.Entry<DefinitionId, String> entry
                : Objects.requireNonNull(source, "metadata").entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "metadata key"),
                    text(entry.getValue(), "metadata value", 1024, true));
        }
        if (copy.size() > 128) { throw new IllegalArgumentException("metadata exceeds 128 entries"); }
        return Collections.unmodifiableMap(copy);
    }

    /** @return a renamed copy retaining identity and every reference */
    public MapDefinition rename(final String name, final Instant changedAt) {
        return new MapDefinition(id, name, createdAt, changedAt, version + 1L, template, group,
                author, description, supportedModes, minimumTeamSize, maximumTeamSize, tags,
                metadata, validationStatus);
    }

    /** @return immutable map identity */ public MapId id() { return id; }
    /** @return mutable-through-copy display name */ public String displayName() { return displayName; }
    /** @return creation time */ public Instant createdAt() { return createdAt; }
    /** @return last metadata update time */ public Instant updatedAt() { return updatedAt; }
    /** @return optimistic metadata version */ public long version() { return version; }
    /** @return map template category */ public DefinitionId template() { return template; }
    /** @return arena group */ public DefinitionId group() { return group; }
    /** @return stable author identity */ public DefinitionId author() { return author; }
    /** @return operator-authored description */ public String description() { return description; }
    /** @return supported mode identities */ public Set<DefinitionId> supportedModes() { return supportedModes; }
    /** @return minimum supported team size */ public int minimumTeamSize() { return minimumTeamSize; }
    /** @return maximum supported team size */ public int maximumTeamSize() { return maximumTeamSize; }
    /** @return searchable semantic tags */ public Set<DefinitionId> tags() { return tags; }
    /** @return bounded extension metadata */ public Map<DefinitionId, String> metadata() { return metadata; }
    /** @return most recent validation status identity */ public DefinitionId validationStatus() { return validationStatus; }

    @Override public int hashCode() {
        return Objects.hash(id, displayName, createdAt, updatedAt, version, template, group,
                author, description, supportedModes, minimumTeamSize, maximumTeamSize, tags,
                metadata, validationStatus);
    }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof MapDefinition)) { return false; }
        final MapDefinition that = (MapDefinition) other;
        return Objects.deepEquals(equalityState(), that.equalityState());
    }

    private Object[] equalityState() {
        return new Object[] {id, displayName, createdAt, updatedAt, version, template, group,
            author, description, supportedModes, minimumTeamSize, maximumTeamSize, tags, metadata,
            validationStatus};
    }
}
