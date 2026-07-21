package io.zartra.bedwars.progression.model;

import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable aggregate snapshot for persistent player progression. */
public final class ProgressionAccount {
    private final PlayerProgressionId id;
    private final ExperienceAmount experience;
    private final LevelState level;
    private final PrestigeState prestige;
    private final Set<EntitlementId> entitlements;
    private final RecordRevision revision;
    private final AuditMetadata audit;

    /** Creates a validated progression snapshot with a defensive entitlement copy. */
    public ProgressionAccount(final PlayerProgressionId id, final ExperienceAmount experience,
                              final LevelState level, final PrestigeState prestige,
                              final Set<EntitlementId> entitlements,
                              final RecordRevision revision, final AuditMetadata audit) {
        this.id = Objects.requireNonNull(id, "id");
        this.experience = Objects.requireNonNull(experience, "experience");
        this.level = Objects.requireNonNull(level, "level");
        this.prestige = Objects.requireNonNull(prestige, "prestige");
        this.entitlements = Collections.unmodifiableSet(new LinkedHashSet<EntitlementId>(Objects.requireNonNull(entitlements, "entitlements")));
        if (this.entitlements.contains(null)) { throw new IllegalArgumentException("entitlements must not contain null"); }
        this.revision = Objects.requireNonNull(revision, "revision");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return aggregate identity */ public PlayerProgressionId id() { return id; }
    /** @return cumulative experience */ public ExperienceAmount experience() { return experience; }
    /** @return current level */ public LevelState level() { return level; }
    /** @return current prestige */ public PrestigeState prestige() { return prestige; }
    /** @return immutable entitlement snapshot */ public Set<EntitlementId> entitlements() { return entitlements; }
    /** @return optimistic revision */ public RecordRevision revision() { return revision; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
