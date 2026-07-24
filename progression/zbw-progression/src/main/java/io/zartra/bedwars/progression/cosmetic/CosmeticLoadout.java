package io.zartra.bedwars.progression.cosmetic;

import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable player cosmetic selection and favourites snapshot. */
public final class CosmeticLoadout {
    private final PlayerProgressionId owner;
    private final Map<CosmeticCategoryId, CosmeticId> equipped;
    private final Set<CosmeticId> favourites;
    private final List<String> presets;
    private final RecordRevision revision;
    private final AuditMetadata audit;

    /** Creates a validated loadout. */
    public CosmeticLoadout(final PlayerProgressionId owner,
                           final Map<CosmeticCategoryId, CosmeticId> equipped,
                           final Set<CosmeticId> favourites, final List<String> presets,
                           final RecordRevision revision, final AuditMetadata audit) {
        this.owner = Objects.requireNonNull(owner, "owner");
        final Map<CosmeticCategoryId, CosmeticId> equippedCopy =
                new LinkedHashMap<CosmeticCategoryId, CosmeticId>(Objects.requireNonNull(equipped, "equipped"));
        if (equippedCopy.containsKey(null) || equippedCopy.containsValue(null)) {
            throw new IllegalArgumentException("equipped must not contain null");
        }
        this.equipped = Collections.unmodifiableMap(equippedCopy);
        final Set<CosmeticId> favouriteCopy =
                new LinkedHashSet<CosmeticId>(Objects.requireNonNull(favourites, "favourites"));
        if (favouriteCopy.contains(null)) { throw new IllegalArgumentException("favourites contains null"); }
        this.favourites = Collections.unmodifiableSet(favouriteCopy);
        final List<String> presetCopy = new ArrayList<String>(Objects.requireNonNull(presets, "presets"));
        if (presetCopy.size() > 32 || presetCopy.contains(null)) {
            throw new IllegalArgumentException("presets must contain at most 32 non-null values");
        }
        this.presets = Collections.unmodifiableList(presetCopy);
        this.revision = Objects.requireNonNull(revision, "revision");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return owner */ public PlayerProgressionId owner() { return owner; }
    /** @return immutable equipped slots */ public Map<CosmeticCategoryId, CosmeticId> equipped() { return equipped; }
    /** @return immutable favourites */ public Set<CosmeticId> favourites() { return favourites; }
    /** @return immutable preset names */ public List<String> presets() { return presets; }
    /** @return optimistic revision */ public RecordRevision revision() { return revision; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
