package io.zartra.bedwars.api.content;

import io.zartra.bedwars.api.identity.ContentPackId;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only versioned content registry.
 *
 * @param <I> immutable identifier type
 * @param <T> immutable content definition type
 */
public interface ContentRegistry<I, T extends ContentRegistry.Definition<I>> {
    /** @return immutable definition by stable ID */ Optional<T> find(I id);
    /** @return deterministic immutable definition list */ List<T> definitions();
    /** @return immutable registry version */ RegistryVersion version();

    /** Base contract for an immutable content definition. */
    interface Definition<I> {
        /** @return stable identity preserved across compatible migrations */ I id();
        /** @return positive definition schema version */ int schemaVersion();
    }

    /** Content provider contract used by extension points. */
    interface Provider<I, T extends Definition<I>> {
        /** @return immutable pack metadata */ PackMetadata metadata();
        /** @return immutable registry snapshot supplied by this provider */ ContentRegistry<I, T> registry();
    }

    /** Immutable registry version and schema pair. */
    final class RegistryVersion {
        private final SemanticVersion contentVersion;
        private final int schemaVersion;
        private RegistryVersion(final SemanticVersion contentVersion, final int schemaVersion) {
            if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
            this.contentVersion = Objects.requireNonNull(contentVersion, "contentVersion");
            this.schemaVersion = schemaVersion;
        }
        /** @return registry version */ public static RegistryVersion of(final SemanticVersion contentVersion, final int schemaVersion) { return new RegistryVersion(contentVersion, schemaVersion); }
        /** @return content version */ public SemanticVersion contentVersion() { return contentVersion; }
        /** @return schema version */ public int schemaVersion() { return schemaVersion; }
    }

    /** Immutable content-pack compatibility and dependency metadata. */
    final class PackMetadata {
        private final ContentPackId id;
        private final SemanticVersion version;
        private final VersionRange apiVersions;
        private final List<ContentPackId> dependencies;
        private PackMetadata(final ContentPackId id, final SemanticVersion version,
                             final VersionRange apiVersions, final List<ContentPackId> dependencies) {
            this.id = Objects.requireNonNull(id, "id");
            this.version = Objects.requireNonNull(version, "version");
            this.apiVersions = Objects.requireNonNull(apiVersions, "apiVersions");
            this.dependencies = java.util.Collections.unmodifiableList(new java.util.ArrayList<ContentPackId>(Objects.requireNonNull(dependencies, "dependencies")));
            for (ContentPackId dependency : this.dependencies) { Objects.requireNonNull(dependency, "dependency"); }
        }
        /** @return content-pack metadata */ public static PackMetadata of(final ContentPackId id, final SemanticVersion version, final VersionRange apiVersions, final List<ContentPackId> dependencies) { return new PackMetadata(id, version, apiVersions, dependencies); }
        /** @return stable pack ID */ public ContentPackId id() { return id; }
        /** @return pack version */ public SemanticVersion version() { return version; }
        /** @return supported public API versions */ public VersionRange apiVersions() { return apiVersions; }
        /** @return immutable dependency IDs */ public List<ContentPackId> dependencies() { return dependencies; }
    }
}
