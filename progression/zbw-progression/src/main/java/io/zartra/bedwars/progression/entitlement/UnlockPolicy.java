package io.zartra.bedwars.progression.entitlement;

import io.zartra.bedwars.progression.model.EntitlementId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Versioned generic level/prestige unlock policy. */
public final class UnlockPolicy {
    private final int version;
    private final List<Definition> definitions;
    /** Creates an immutable unlock policy. */
    public UnlockPolicy(final int version, final List<Definition> definitions) {
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        final List<Definition> copy = new ArrayList<Definition>(Objects.requireNonNull(definitions, "definitions"));
        if (copy.contains(null)) { throw new IllegalArgumentException("definitions must not contain null"); }
        this.version = version;
        this.definitions = Collections.unmodifiableList(copy);
    }
    /** Calculates only newly satisfied generic entitlements. */
    public Set<EntitlementId> newlyUnlocked(final int level, final int prestige,
                                            final Set<EntitlementId> existing) {
        if (level < 1 || prestige < 0) { throw new IllegalArgumentException("invalid progression state"); }
        final Set<EntitlementId> result = new LinkedHashSet<EntitlementId>();
        final Set<EntitlementId> prior = Objects.requireNonNull(existing, "existing");
        for (Definition definition : definitions) {
            if (level >= definition.minimumLevel() && prestige >= definition.minimumPrestige()
                    && !prior.contains(definition.id())) { result.add(definition.id()); }
        }
        return Collections.unmodifiableSet(result);
    }
    /** @return version */ public int version() { return version; }

    /** One generic entitlement threshold. */
    public static final class Definition {
        private final EntitlementId id;
        private final int minimumLevel;
        private final int minimumPrestige;
        /** Creates a threshold. */ public Definition(final EntitlementId id, final int minimumLevel,
                                                       final int minimumPrestige) {
            this.id = Objects.requireNonNull(id, "id");
            if (minimumLevel < 1 || minimumPrestige < 0) { throw new IllegalArgumentException("invalid unlock threshold"); }
            this.minimumLevel = minimumLevel;
            this.minimumPrestige = minimumPrestige;
        }
        /** @return entitlement */ public EntitlementId id() { return id; }
        /** @return minimum level */ public int minimumLevel() { return minimumLevel; }
        /** @return minimum prestige */ public int minimumPrestige() { return minimumPrestige; }
    }
}
