package io.zartra.bedwars.game.mode;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.domain.team.TeamLayoutLimits;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable mode metadata and a bounded, deterministic extension registry. */
public final class ModeFramework {
    private ModeFramework() { throw new AssertionError("No instances"); }

    /** Stable identity independent of display text or implementation class. */
    public static final class ModeId implements Comparable<ModeId> {
        private final DefinitionId value;
        private ModeId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return a mode identity in the supplied namespace */
        public static ModeId of(final String namespace, final String path) {
            return new ModeId(DefinitionId.of(namespace, "mode/" + path));
        }
        /** @return parsed canonical mode identity */
        public static ModeId parse(final String value) { return new ModeId(DefinitionId.parse(value)); }
        /** @return underlying definition identity */ public DefinitionId value() { return value; }
        @Override public int compareTo(final ModeId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ModeId && value.equals(((ModeId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Monotonic contract and configuration version. */
    public static final class Version implements Comparable<Version> {
        private final int major;
        private final int minor;
        /** Creates a non-negative version. */
        public Version(final int major, final int minor) {
            if (major < 0 || minor < 0) { throw new IllegalArgumentException("version must not be negative"); }
            this.major = major;
            this.minor = minor;
        }
        /** @return major compatibility boundary */ public int major() { return major; }
        /** @return additive revision */ public int minor() { return minor; }
        @Override public int compareTo(final Version other) {
            final int byMajor = Integer.compare(major, Objects.requireNonNull(other, "other").major);
            return byMajor == 0 ? Integer.compare(minor, other.minor) : byMajor;
        }
        @Override public int hashCode() { return 31 * major + minor; }
        @Override public boolean equals(final Object other) { return this == other || other instanceof Version && major == ((Version) other).major && minor == ((Version) other).minor; }
        @Override public String toString() { return major + "." + minor; }
    }

    /** Supported typed configuration value families. */
    public enum FieldType { /** Boolean. */ BOOLEAN, /** Bounded integer. */ INTEGER, /** Duration in milliseconds. */ DURATION, /** Stable definition ID. */ DEFINITION_ID, /** Localized text key. */ MESSAGE_KEY }

    /** One immutable configuration-schema field. */
    public static final class ConfigField {
        private final DefinitionId id;
        private final FieldType type;
        private final String defaultValue;
        private final boolean required;
        /** Creates a schema field with a validated textual default boundary. */
        public ConfigField(final DefinitionId id, final FieldType type, final String defaultValue,
                           final boolean required) {
            this.id = Objects.requireNonNull(id, "id");
            this.type = Objects.requireNonNull(type, "type");
            if (defaultValue == null || defaultValue.length() > 256
                    || defaultValue.indexOf('\r') >= 0 || defaultValue.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("invalid configuration default");
            }
            this.defaultValue = defaultValue;
            this.required = required;
        }
        /** @return field identity */ public DefinitionId id() { return id; }
        /** @return field type */ public FieldType type() { return type; }
        /** @return serialized safe default */ public String defaultValue() { return defaultValue; }
        /** @return whether omission is invalid */ public boolean required() { return required; }
    }

    /** Validated standard or custom team layout accepted by selectors and matchmaking. */
    public static final class Layout {
        private final DefinitionId id;
        private final List<DefinitionId> teams;
        private final int teamCapacity;
        /** Creates a layout from arbitrary semantic team identities. */
        public Layout(final DefinitionId id, final Collection<DefinitionId> teams,
                      final int teamCapacity) {
            this.id = Objects.requireNonNull(id, "id");
            final Set<DefinitionId> unique = new LinkedHashSet<DefinitionId>();
            for (DefinitionId team : Objects.requireNonNull(teams, "teams")) {
                if (!unique.add(Objects.requireNonNull(team, "team"))) {
                    throw new IllegalArgumentException("duplicate team identity");
                }
            }
            TeamLayoutLimits.requireTeamCount(unique.size());
            this.teamCapacity = TeamLayoutLimits.requireTeamCapacity(teamCapacity);
            TeamLayoutLimits.requireMaximumPlayers(unique.size() * teamCapacity);
            this.teams = Collections.unmodifiableList(new ArrayList<DefinitionId>(unique));
        }
        /** @return stable layout identity */ public DefinitionId id() { return id; }
        /** @return ordered semantic teams */ public List<DefinitionId> teams() { return teams; }
        /** @return team count */ public int teamCount() { return teams.size(); }
        /** @return capacity of each team */ public int teamCapacity() { return teamCapacity; }
        /** @return total player capacity */ public int totalCapacity() { return teamCount() * teamCapacity; }
    }

    /** Requirement-aware declaration for a mode whose mechanics belong to a later milestone. */
    public static final class DeferredBinding {
        private final String requirementId;
        private final String ownerMilestone;
        /** Creates one explicit later-owner binding. */
        public DeferredBinding(final String requirementId, final String ownerMilestone) {
            if (requirementId == null || !requirementId.matches("ZBW-(?:GAME|ADDON)-[0-9]{3}")) {
                throw new IllegalArgumentException("invalid deferred requirement ID");
            }
            if (ownerMilestone == null || !ownerMilestone.matches("M(?:1[1-9]|2[0-4])")) {
                throw new IllegalArgumentException("deferred owner must be M11 or later");
            }
            this.requirementId = requirementId;
            this.ownerMilestone = ownerMilestone;
        }
        /** @return deferred requirement */ public String requirementId() { return requirementId; }
        /** @return owning later milestone */ public String ownerMilestone() { return ownerMilestone; }
    }

    /** Complete immutable registration metadata, not a gameplay implementation marker. */
    public static final class Definition {
        private final ModeId id;
        private final MessageKey displayName;
        private final MessageKey description;
        private final Version version;
        private final boolean enabled;
        private final int minimumTeams;
        private final int maximumTeams;
        private final int minimumPlayers;
        private final int maximumPlayers;
        private final Set<DefinitionId> capabilities;
        private final List<ConfigField> schema;
        private final List<DeferredBinding> deferredBindings;
        /** Creates validated mode selection metadata. */
        public Definition(final ModeId id, final MessageKey displayName, final MessageKey description,
                          final Version version, final boolean enabled, final int minimumTeams,
                          final int maximumTeams, final int minimumPlayers, final int maximumPlayers,
                          final Collection<DefinitionId> capabilities,
                          final Collection<ConfigField> schema,
                          final Collection<DeferredBinding> deferredBindings) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.description = Objects.requireNonNull(description, "description");
            this.version = Objects.requireNonNull(version, "version");
            if (minimumTeams < TeamLayoutLimits.MINIMUM_TEAM_COUNT
                    || maximumTeams > TeamLayoutLimits.MAXIMUM_TEAM_COUNT
                    || minimumTeams > maximumTeams
                    || minimumPlayers < TeamLayoutLimits.MINIMUM_TEAM_CAPACITY
                    || maximumPlayers > TeamLayoutLimits.MAXIMUM_MATCH_PLAYERS
                    || minimumPlayers > maximumPlayers) {
                throw new IllegalArgumentException("mode bounds outside supported layout limits");
            }
            this.enabled = enabled;
            this.minimumTeams = minimumTeams;
            this.maximumTeams = maximumTeams;
            this.minimumPlayers = minimumPlayers;
            this.maximumPlayers = maximumPlayers;
            this.capabilities = immutableUnique(capabilities, "capability");
            this.schema = immutableUniqueFields(schema);
            this.deferredBindings = immutableDeferred(deferredBindings);
        }
        /** @return stable ID */ public ModeId id() { return id; }
        /** @return localized name */ public MessageKey displayName() { return displayName; }
        /** @return localized description */ public MessageKey description() { return description; }
        /** @return definition version */ public Version version() { return version; }
        /** @return configured availability */ public boolean enabled() { return enabled; }
        /** @return immutable capability set */ public Set<DefinitionId> capabilities() { return capabilities; }
        /** @return immutable configuration schema */ public List<ConfigField> schema() { return schema; }
        /** @return explicit later-owned mechanics */ public List<DeferredBinding> deferredBindings() { return deferredBindings; }
        /** @return whether the layout satisfies team and total-player constraints */
        public boolean supports(final Layout layout) {
            Objects.requireNonNull(layout, "layout");
            return enabled && layout.teamCount() >= minimumTeams && layout.teamCount() <= maximumTeams
                    && layout.totalCapacity() >= minimumPlayers && layout.totalCapacity() <= maximumPlayers;
        }
        /** @return a copy with changed availability and a newer version */
        public Definition withEnabled(final boolean value, final Version newerVersion) {
            if (Objects.requireNonNull(newerVersion, "newerVersion").compareTo(version) <= 0) {
                throw new IllegalArgumentException("mode update version must increase");
            }
            return new Definition(id, displayName, description, newerVersion, value, minimumTeams,
                    maximumTeams, minimumPlayers, maximumPlayers, capabilities, schema, deferredBindings);
        }
    }

    /** Typed migration of configuration values between compatible definition versions. */
    public interface Migration {
        /** @return source version */ Version from();
        /** @return target version */ Version to();
        /** @return immutable migrated values keyed by schema field identity */
        Map<DefinitionId, String> migrate(Map<DefinitionId, String> source);
    }

    /** Immutable registry lifecycle event. */
    public static final class Event {
        private final Type type;
        private final ModeId modeId;
        private final Version version;
        private Event(final Type type, final ModeId modeId, final Version version) {
            this.type = type;
            this.modeId = modeId;
            this.version = version;
        }
        /** @return event type */ public Type type() { return type; }
        /** @return affected mode */ public ModeId modeId() { return modeId; }
        /** @return resulting version */ public Version version() { return version; }
        /** Registry lifecycle types. */ public enum Type { /** Added. */ REGISTERED, /** Availability changed. */ UPDATED }
    }

    /** Non-blocking event sink. Implementations must not call platform APIs off their owner thread. */
    public interface Listener { /** Observes an immutable registry event. */ void onEvent(Event event); }

    /** Bounded thread-safe registry with deterministic identity ordering. */
    public static final class Registry {
        private final int capacity;
        private final Listener listener;
        private final Map<ModeId, Definition> definitions = new LinkedHashMap<ModeId, Definition>();
        /** Creates a registry with a fixed capacity. */
        public Registry(final int capacity, final Listener listener) {
            if (capacity < 1 || capacity > 4096) { throw new IllegalArgumentException("invalid mode registry capacity"); }
            this.capacity = capacity;
            this.listener = Objects.requireNonNull(listener, "listener");
        }
        /** Registers a unique definition. */
        public synchronized void register(final Definition definition) {
            final Definition checked = Objects.requireNonNull(definition, "definition");
            if (definitions.size() >= capacity) { throw new IllegalStateException("mode registry capacity reached"); }
            if (definitions.put(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate mode ID " + checked.id());
            }
            listener.onEvent(new Event(Event.Type.REGISTERED, checked.id(), checked.version()));
        }
        /** Changes enablement using an optimistic expected version. */
        public synchronized Definition setEnabled(final ModeId id, final Version expected,
                                                  final boolean enabled, final Version updated) {
            final Definition current = require(id);
            if (!current.version().equals(Objects.requireNonNull(expected, "expected"))) {
                throw new IllegalStateException("stale mode definition");
            }
            final Definition replacement = current.withEnabled(enabled, updated);
            definitions.put(id, replacement);
            listener.onEvent(new Event(Event.Type.UPDATED, id, updated));
            return replacement;
        }
        /** @return exact definition */ public synchronized Definition require(final ModeId id) {
            final Definition value = definitions.get(Objects.requireNonNull(id, "id"));
            if (value == null) { throw new IllegalArgumentException("unknown mode " + id); }
            return value;
        }
        /** @return definition when present */ public synchronized Optional<Definition> find(final ModeId id) { return Optional.ofNullable(definitions.get(Objects.requireNonNull(id, "id"))); }
        /** @return immutable identity-ordered registry snapshot */
        public synchronized List<Definition> snapshot() {
            final List<Definition> result = new ArrayList<Definition>(definitions.values());
            result.sort(Comparator.comparing(Definition::id));
            return Collections.unmodifiableList(result);
        }
    }

    private static Set<DefinitionId> immutableUnique(final Collection<DefinitionId> values,
                                                     final String label) {
        final Set<DefinitionId> result = new LinkedHashSet<DefinitionId>();
        for (DefinitionId value : Objects.requireNonNull(values, label + "s")) {
            if (!result.add(Objects.requireNonNull(value, label))) {
                throw new IllegalArgumentException("duplicate " + label);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static List<ConfigField> immutableUniqueFields(final Collection<ConfigField> values) {
        final Map<DefinitionId, ConfigField> result = new LinkedHashMap<DefinitionId, ConfigField>();
        for (ConfigField field : Objects.requireNonNull(values, "schema")) {
            final ConfigField checked = Objects.requireNonNull(field, "field");
            if (result.put(checked.id(), checked) != null) { throw new IllegalArgumentException("duplicate schema field"); }
        }
        return Collections.unmodifiableList(new ArrayList<ConfigField>(result.values()));
    }

    private static List<DeferredBinding> immutableDeferred(final Collection<DeferredBinding> values) {
        final Map<String, DeferredBinding> result = new LinkedHashMap<String, DeferredBinding>();
        for (DeferredBinding binding : Objects.requireNonNull(values, "deferredBindings")) {
            final DeferredBinding checked = Objects.requireNonNull(binding, "binding");
            if (result.put(checked.requirementId(), checked) != null) { throw new IllegalArgumentException("duplicate deferred binding"); }
        }
        return Collections.unmodifiableList(new ArrayList<DeferredBinding>(result.values()));
    }
}
