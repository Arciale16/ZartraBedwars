package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable registry of native and extension-defined match-resource tenders. */
public final class TenderRegistry {
    /** Native iron resource identity. */ public static final ResourceId IRON = ResourceId.of("zbw", "resource/iron");
    /** Native gold resource identity. */ public static final ResourceId GOLD = ResourceId.of("zbw", "resource/gold");
    /** Native diamond resource identity. */ public static final ResourceId DIAMOND = ResourceId.of("zbw", "resource/diamond");
    /** Native emerald resource identity. */ public static final ResourceId EMERALD = ResourceId.of("zbw", "resource/emerald");

    private final Map<ResourceId, TenderDefinition> definitions;
    private final List<TenderDefinition> snapshot;

    /** Creates a registry with one owner for each resource identity. */
    public TenderRegistry(final Collection<TenderDefinition> definitions) {
        final Map<ResourceId, TenderDefinition> unique = new LinkedHashMap<ResourceId, TenderDefinition>();
        for (TenderDefinition value : Objects.requireNonNull(definitions, "definitions")) {
            final TenderDefinition checked = Objects.requireNonNull(value, "definition");
            if (unique.put(checked.resourceId(), checked) != null) {
                throw new IllegalArgumentException("duplicate tender " + checked.resourceId());
            }
        }
        if (unique.isEmpty()) { throw new IllegalArgumentException("tender registry is empty"); }
        final List<TenderDefinition> sorted = new ArrayList<TenderDefinition>(unique.values());
        sorted.sort(Comparator.comparing(TenderDefinition::resourceId));
        this.definitions = Collections.unmodifiableMap(unique);
        this.snapshot = Collections.unmodifiableList(sorted);
    }

    /** @return registry containing iron, gold, diamond and emerald match tenders */
    public static TenderRegistry nativeMatchResources() {
        final ProviderId provider = ProviderId.of("zbw", "match-inventory");
        final List<TenderDefinition> definitions = new ArrayList<TenderDefinition>();
        definitions.add(new TenderDefinition(IRON, provider, Kind.NATIVE_MATCH_RESOURCE));
        definitions.add(new TenderDefinition(GOLD, provider, Kind.NATIVE_MATCH_RESOURCE));
        definitions.add(new TenderDefinition(DIAMOND, provider, Kind.NATIVE_MATCH_RESOURCE));
        definitions.add(new TenderDefinition(EMERALD, provider, Kind.NATIVE_MATCH_RESOURCE));
        return new TenderRegistry(definitions);
    }

    /** @return tender definition when the exact resource is registered */
    public Optional<TenderDefinition> find(final ResourceId resourceId) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(resourceId, "resourceId")));
    }

    /** @return deterministic immutable tender definitions */ public List<TenderDefinition> snapshot() { return snapshot; }

    /** Validates that every catalog price resolves to an explicit tender. */
    public void validate(final ShopCatalog catalog) {
        for (ShopCatalog.ItemDefinition item : Objects.requireNonNull(catalog, "catalog").items()) {
            for (ShopCatalog.ResourceAmount amount : item.price().amounts()) {
                if (!definitions.containsKey(amount.resourceId())) {
                    throw new IllegalArgumentException("unknown tender " + amount.resourceId());
                }
            }
        }
    }

    /** Match-resource tender classes available in M11 Phase 1. */
    public enum Kind {
        /** Built-in resource stored in the live match inventory. */ NATIVE_MATCH_RESOURCE,
        /** Extension-defined resource stored through the same atomic match transaction. */ CUSTOM_MATCH_RESOURCE
    }

    /** Immutable mapping from a priced resource to its atomic match provider. */
    public static final class TenderDefinition {
        private final ResourceId resourceId;
        private final ProviderId providerId;
        private final Kind kind;
        /** Creates a provider-owned tender definition. */
        public TenderDefinition(final ResourceId resourceId, final ProviderId providerId, final Kind kind) {
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
            this.providerId = Objects.requireNonNull(providerId, "providerId");
            this.kind = Objects.requireNonNull(kind, "kind");
        }
        /** @return priced resource */ public ResourceId resourceId() { return resourceId; }
        /** @return provider identity */ public ProviderId providerId() { return providerId; }
        /** @return provider kind */ public Kind kind() { return kind; }
    }
}
