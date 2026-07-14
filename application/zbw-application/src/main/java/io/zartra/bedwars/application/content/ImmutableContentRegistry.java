package io.zartra.bedwars.application.content;

import io.zartra.bedwars.api.content.ContentRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable deterministic content-registry snapshot.
 *
 * <p>Construction rejects duplicate IDs before publication. Live snapshot replacement and
 * migration transactions belong to M03.</p>
 */
public final class ImmutableContentRegistry<I extends Comparable<? super I>, T extends ContentRegistry.Definition<I>>
        implements ContentRegistry<I, T> {
    private final Map<I, T> byId;
    private final List<T> definitions;
    private final RegistryVersion version;

    private ImmutableContentRegistry(final Collection<T> definitions, final RegistryVersion version) {
        final Map<I, T> collected = new LinkedHashMap<I, T>();
        for (T definition : Objects.requireNonNull(definitions, "definitions")) {
            final T checked = Objects.requireNonNull(definition, "definition");
            if (checked.schemaVersion() < 1) { throw new IllegalArgumentException("Definition schemaVersion must be positive"); }
            if (collected.put(Objects.requireNonNull(checked.id(), "definition.id"), checked) != null) {
                throw new DuplicateContentIdException(String.valueOf(checked.id()));
            }
        }
        final List<T> sorted = new ArrayList<T>(collected.values());
        Collections.sort(sorted, (left, right) -> left.id().compareTo(right.id()));
        final Map<I, T> sortedMap = new LinkedHashMap<I, T>();
        for (T definition : sorted) { sortedMap.put(definition.id(), definition); }
        this.byId = Collections.unmodifiableMap(sortedMap);
        this.definitions = Collections.unmodifiableList(sorted);
        this.version = Objects.requireNonNull(version, "version");
    }

    /** @return immutable registry after duplicate/schema validation */
    public static <I extends Comparable<? super I>, T extends ContentRegistry.Definition<I>>
            ImmutableContentRegistry<I, T> assemble(final Collection<T> definitions, final RegistryVersion version) {
        return new ImmutableContentRegistry<I, T>(definitions, version);
    }

    @Override public Optional<T> find(final I id) { return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id"))); }
    @Override public List<T> definitions() { return definitions; }
    @Override public RegistryVersion version() { return version; }

    /** Typed duplicate content-ID failure. */
    public static final class DuplicateContentIdException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String identifier;
        private DuplicateContentIdException(final String identifier) {
            super("Duplicate content ID: " + identifier);
            this.identifier = identifier;
        }
        /** @return canonical duplicate identifier */ public String identifier() { return identifier; }
    }
}
