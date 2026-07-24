package io.zartra.bedwars.progression.profile;

import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.Objects;

/** Immutable privacy and cosmetic-performance settings snapshot. */
public final class ProfileSettings {
    /** Profile visibility. */ public enum Visibility { PRIVATE, FRIENDS, PARTY, PUBLIC }
    private final PlayerProgressionId owner;
    private final Visibility visibility;
    private final boolean cosmeticsVisible;
    private final boolean lowPerformanceMode;
    private final RecordRevision revision;
    private final AuditMetadata audit;

    /** Creates validated settings. */
    public ProfileSettings(final PlayerProgressionId owner, final Visibility visibility,
                           final boolean cosmeticsVisible, final boolean lowPerformanceMode,
                           final RecordRevision revision, final AuditMetadata audit) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        this.cosmeticsVisible = cosmeticsVisible;
        this.lowPerformanceMode = lowPerformanceMode;
        this.revision = Objects.requireNonNull(revision, "revision");
        this.audit = Objects.requireNonNull(audit, "audit");
    }
    /** @return owner */ public PlayerProgressionId owner() { return owner; }
    /** @return visibility */ public Visibility visibility() { return visibility; }
    /** @return cosmetic visibility */ public boolean cosmeticsVisible() { return cosmeticsVisible; }
    /** @return reduced-effects preference */ public boolean lowPerformanceMode() { return lowPerformanceMode; }
    /** @return optimistic revision */ public RecordRevision revision() { return revision; }
    /** @return audit metadata */ public AuditMetadata audit() { return audit; }
}
