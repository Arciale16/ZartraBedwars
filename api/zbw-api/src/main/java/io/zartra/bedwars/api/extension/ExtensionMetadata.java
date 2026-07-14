package io.zartra.bedwars.api.extension;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.ExtensionId;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable extension descriptor consumed before extension code is loaded. */
public final class ExtensionMetadata {
    /** Current metadata document schema. */
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private final int schemaVersion;
    private final ExtensionId id;
    private final String displayName;
    private final SemanticVersion version;
    private final String entrypoint;
    private final VersionRange apiVersions;
    private final VersionRange productVersions;
    private final MinecraftRange minecraftVersions;
    private final List<Dependency> dependencies;
    private final CapabilitySet requiredApis;
    private final CapabilitySet providedCapabilities;
    private final List<PermissionNode> permissions;
    private final List<ConfigurationKey> configurationKeys;

    private ExtensionMetadata(final Builder builder) {
        schemaVersion = builder.schemaVersion;
        id = Objects.requireNonNull(builder.id, "id");
        displayName = Objects.requireNonNull(builder.displayName, "displayName");
        version = Objects.requireNonNull(builder.version, "version");
        entrypoint = Objects.requireNonNull(builder.entrypoint, "entrypoint");
        apiVersions = Objects.requireNonNull(builder.apiVersions, "apiVersions");
        productVersions = Objects.requireNonNull(builder.productVersions, "productVersions");
        minecraftVersions = Objects.requireNonNull(builder.minecraftVersions, "minecraftVersions");
        dependencies = immutableCopy(builder.dependencies, "dependency");
        requiredApis = Objects.requireNonNull(builder.requiredApis, "requiredApis");
        providedCapabilities = Objects.requireNonNull(builder.providedCapabilities, "providedCapabilities");
        permissions = immutableCopy(builder.permissions, "permission");
        configurationKeys = immutableCopy(builder.configurationKeys, "configurationKey");
    }

    private static <T> List<T> immutableCopy(final List<T> source, final String label) {
        final List<T> copy = new ArrayList<T>();
        for (T value : Objects.requireNonNull(source, label + "s")) {
            copy.add(Objects.requireNonNull(value, label));
        }
        return Collections.unmodifiableList(copy);
    }

    /** @return new mutable builder whose output is immutable */
    public static Builder builder() { return new Builder(); }
    /** @return metadata schema version */ public int schemaVersion() { return schemaVersion; }
    /** @return stable extension ID */ public ExtensionId id() { return id; }
    /** @return operator-facing display name, never used as identity */ public String displayName() { return displayName; }
    /** @return extension version */ public SemanticVersion version() { return version; }
    /** @return Java entrypoint class name */ public String entrypoint() { return entrypoint; }
    /** @return supported public API range */ public VersionRange apiVersions() { return apiVersions; }
    /** @return supported ZartraBedWars product range */ public VersionRange productVersions() { return productVersions; }
    /** @return supported Minecraft server range */ public MinecraftRange minecraftVersions() { return minecraftVersions; }
    /** @return immutable declared dependencies */ public List<Dependency> dependencies() { return dependencies; }
    /** @return immutable APIs required to load */ public CapabilitySet requiredApis() { return requiredApis; }
    /** @return immutable capabilities supplied by the extension */ public CapabilitySet providedCapabilities() { return providedCapabilities; }
    /** @return immutable permissions introduced by the extension */ public List<PermissionNode> permissions() { return permissions; }
    /** @return immutable configuration keys introduced by the extension */ public List<ConfigurationKey> configurationKeys() { return configurationKeys; }

    /** Builder used only while reading a metadata document. It is not thread-safe. */
    public static final class Builder {
        private int schemaVersion = CURRENT_SCHEMA_VERSION;
        private ExtensionId id;
        private String displayName;
        private SemanticVersion version;
        private String entrypoint;
        private VersionRange apiVersions;
        private VersionRange productVersions;
        private MinecraftRange minecraftVersions;
        private List<Dependency> dependencies = Collections.emptyList();
        private CapabilitySet requiredApis = CapabilitySet.empty();
        private CapabilitySet providedCapabilities = CapabilitySet.empty();
        private List<PermissionNode> permissions = Collections.emptyList();
        private List<ConfigurationKey> configurationKeys = Collections.emptyList();
        private Builder() { }
        /** @return this builder */ public Builder schemaVersion(final int value) {
            schemaVersion = value;
            return this;
        }
        /** @return this builder */ public Builder id(final ExtensionId value) {
            id = value;
            return this;
        }
        /** @return this builder */ public Builder displayName(final String value) {
            displayName = value;
            return this;
        }
        /** @return this builder */ public Builder version(final SemanticVersion value) {
            version = value;
            return this;
        }
        /** @return this builder */ public Builder entrypoint(final String value) {
            entrypoint = value;
            return this;
        }
        /** @return this builder */ public Builder apiVersions(final VersionRange value) {
            apiVersions = value;
            return this;
        }
        /** @return this builder */ public Builder productVersions(final VersionRange value) {
            productVersions = value;
            return this;
        }
        /** @return this builder */ public Builder minecraftVersions(final MinecraftRange value) {
            minecraftVersions = value;
            return this;
        }
        /** @return this builder */ public Builder dependencies(final List<Dependency> value) {
            dependencies = value;
            return this;
        }
        /** @return this builder */ public Builder requiredApis(final CapabilitySet value) {
            requiredApis = value;
            return this;
        }
        /** @return this builder */ public Builder providedCapabilities(final CapabilitySet value) {
            providedCapabilities = value;
            return this;
        }
        /** @return this builder */ public Builder permissions(final List<PermissionNode> value) {
            permissions = value;
            return this;
        }
        /** @return this builder */ public Builder configurationKeys(final List<ConfigurationKey> value) {
            configurationKeys = value;
            return this;
        }
        /** @return immutable metadata */ public ExtensionMetadata build() { return new ExtensionMetadata(this); }
    }

    /** Immutable extension dependency declaration. */
    public static final class Dependency {
        private final ExtensionId id;
        private final VersionRange versions;
        private final boolean optional;
        private Dependency(final ExtensionId id, final VersionRange versions, final boolean optional) {
            this.id = Objects.requireNonNull(id, "id");
            this.versions = Objects.requireNonNull(versions, "versions");
            this.optional = optional;
        }
        /** @return dependency declaration */ public static Dependency of(final ExtensionId id, final VersionRange versions, final boolean optional) { return new Dependency(id, versions, optional); }
        /** @return dependency ID */ public ExtensionId id() { return id; }
        /** @return accepted versions */ public VersionRange versions() { return versions; }
        /** @return whether absence is allowed */ public boolean optional() { return optional; }
    }

    /** Inclusive Minecraft compatibility range. */
    public static final class MinecraftRange {
        private final MinecraftVersion minimum;
        private final MinecraftVersion maximum;
        private MinecraftRange(final MinecraftVersion minimum, final MinecraftVersion maximum) {
            if (minimum.compareTo(maximum) > 0) { throw new IllegalArgumentException("minimum exceeds maximum"); }
            this.minimum = minimum;
            this.maximum = maximum;
        }
        /** @return inclusive range */ public static MinecraftRange inclusive(final MinecraftVersion minimum, final MinecraftVersion maximum) { return new MinecraftRange(Objects.requireNonNull(minimum, "minimum"), Objects.requireNonNull(maximum, "maximum")); }
        /** @return whether version is inside the inclusive range */ public boolean contains(final MinecraftVersion value) {
            Objects.requireNonNull(value, "value");
            return minimum.compareTo(value) <= 0 && value.compareTo(maximum) <= 0;
        }
        /** @return minimum supported version */ public MinecraftVersion minimum() { return minimum; }
        /** @return maximum supported version */ public MinecraftVersion maximum() { return maximum; }
    }

    /** Typed permission declaration; it grants nothing until runtime authorization allows it. */
    public static final class PermissionNode implements Comparable<PermissionNode> {
        private final String value;
        private PermissionNode(final String value) {
            if (value == null || !value.matches("[a-z0-9][a-z0-9_.-]{2,127}")) { throw new IllegalArgumentException("Invalid permission node"); }
            this.value = value;
        }
        /** @return validated permission node */ public static PermissionNode of(final String value) { return new PermissionNode(value); }
        /** @return canonical node */ public String value() { return value; }
        @Override public int compareTo(final PermissionNode other) { return value.compareTo(other.value); }
        @Override public String toString() { return value; }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof PermissionNode && value.equals(((PermissionNode) other).value); }
    }

    /** Typed configuration declaration; values and secrets are never part of metadata. */
    public static final class ConfigurationKey implements Comparable<ConfigurationKey> {
        private final String value;
        private ConfigurationKey(final String value) {
            if (value == null || !value.matches("[a-z0-9][a-z0-9_.-]{2,127}")) { throw new IllegalArgumentException("Invalid configuration key"); }
            this.value = value;
        }
        /** @return validated configuration key */ public static ConfigurationKey of(final String value) { return new ConfigurationKey(value); }
        /** @return canonical key */ public String value() { return value; }
        @Override public int compareTo(final ConfigurationKey other) { return value.compareTo(other.value); }
        @Override public String toString() { return value; }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ConfigurationKey && value.equals(((ConfigurationKey) other).value); }
    }
}
