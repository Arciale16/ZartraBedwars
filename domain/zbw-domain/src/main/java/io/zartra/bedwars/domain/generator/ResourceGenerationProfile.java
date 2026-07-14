package io.zartra.bedwars.domain.generator;

import io.zartra.bedwars.api.identity.ResourceId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, deterministic native/custom resource multiplier profile. */
public final class ResourceGenerationProfile {
    private final List<Entry> entries;
    private final Map<ResourceId, GenerationMultiplier> byResource;

    private ResourceGenerationProfile(final Collection<Entry> entries) {
        final Map<ResourceId, GenerationMultiplier> collected = new LinkedHashMap<ResourceId, GenerationMultiplier>();
        for (Entry entry : Objects.requireNonNull(entries, "entries")) {
            final Entry checked = Objects.requireNonNull(entry, "entry");
            if (collected.put(checked.resourceId(), checked.multiplier()) != null) {
                throw new DuplicateResourceException(checked.resourceId());
            }
        }
        final List<Entry> sorted = new ArrayList<Entry>();
        for (Map.Entry<ResourceId, GenerationMultiplier> entry : collected.entrySet()) {
            sorted.add(Entry.of(entry.getKey(), entry.getValue()));
        }
        Collections.sort(sorted, (left, right) -> left.resourceId().compareTo(right.resourceId()));
        this.entries = Collections.unmodifiableList(sorted);
        this.byResource = Collections.unmodifiableMap(collected);
    }

    /** @return profile with unique resource entries */
    public static ResourceGenerationProfile of(final Collection<Entry> entries) { return new ResourceGenerationProfile(entries); }
    /** @return immutable entries ordered by resource ID */ public List<Entry> entries() { return entries; }
    /** @return multiplier for a resource, or empty when the profile intentionally leaves it unspecified */
    public Optional<GenerationMultiplier> multiplier(final ResourceId resourceId) { return Optional.ofNullable(byResource.get(Objects.requireNonNull(resourceId, "resourceId"))); }

    /** Immutable resource/multiplier pair. */
    public static final class Entry {
        private final ResourceId resourceId;
        private final GenerationMultiplier multiplier;
        private Entry(final ResourceId resourceId, final GenerationMultiplier multiplier) {
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
            this.multiplier = Objects.requireNonNull(multiplier, "multiplier");
        }
        /** @return pair */ public static Entry of(final ResourceId resourceId, final GenerationMultiplier multiplier) { return new Entry(resourceId, multiplier); }
        /** @return resource ID */ public ResourceId resourceId() { return resourceId; }
        /** @return configured multiplier */ public GenerationMultiplier multiplier() { return multiplier; }
    }

    /** Typed duplicate-resource failure. */
    public static final class DuplicateResourceException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String resourceId;
        private DuplicateResourceException(final ResourceId resourceId) {
            super("Duplicate resource: " + resourceId);
            this.resourceId = resourceId.toString();
        }
        /** @return duplicated resource */ public ResourceId resourceId() { return ResourceId.parse(resourceId); }
    }
}
