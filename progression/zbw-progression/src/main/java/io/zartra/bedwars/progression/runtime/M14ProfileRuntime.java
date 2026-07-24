package io.zartra.bedwars.progression.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.profile.ProfileSettings;
import io.zartra.bedwars.progression.repository.ProfileSettingsRepository;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Objects;
import java.util.Optional;

/** Immutable-profile update and visibility policy over the existing M12 persistence port. */
public final class M14ProfileRuntime {
    private final ProfileSettingsRepository settings;

    /** Creates the profile runtime. */
    public M14ProfileRuntime(final ProfileSettingsRepository settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /** Reads the latest settings in the caller-owned persistence transaction. */
    public Result<Optional<ProfileSettings>> find(final UnitOfWork unitOfWork,
                                                   final PlayerProgressionId owner) {
        return settings.find(Objects.requireNonNull(unitOfWork, "unitOfWork"),
                Objects.requireNonNull(owner, "owner"));
    }

    /** Saves a whole immutable replacement with revision and idempotency protection. */
    public Result<ProfileSettings> update(final UnitOfWork unitOfWork, final ProfileSettings replacement,
                                          final RecordRevision expectedRevision,
                                          final IdempotencyKey idempotencyKey) {
        Objects.requireNonNull(replacement, "replacement");
        return settings.save(Objects.requireNonNull(unitOfWork, "unitOfWork"), replacement,
                Objects.requireNonNull(expectedRevision, "expectedRevision"),
                Objects.requireNonNull(idempotencyKey, "idempotencyKey"));
    }

    /** Builds a revisioned update while retaining an auditable ownership boundary. */
    public ProfileSettings replacement(final ProfileSettings current,
                                       final ProfileSettings.Visibility visibility,
                                       final boolean cosmeticsVisible, final boolean lowPerformanceMode,
                                       final AuditMetadata audit) {
        Objects.requireNonNull(current, "current");
        return new ProfileSettings(current.owner(), Objects.requireNonNull(visibility, "visibility"),
                cosmeticsVisible, lowPerformanceMode, current.revision().next(),
                Objects.requireNonNull(audit, "audit"));
    }

    /** Evaluates visibility using relationship facts supplied by the authorized caller. */
    public boolean mayView(final ProfileSettings profile, final boolean owner, final boolean friend,
                           final boolean partyMember, final boolean staff) {
        Objects.requireNonNull(profile, "profile");
        if (owner || staff) { return true; }
        switch (profile.visibility()) {
            case PUBLIC: return true;
            case FRIENDS: return friend;
            case PARTY: return partyMember;
            case PRIVATE: return false;
            default: throw new IllegalStateException("unknown visibility");
        }
    }
}
