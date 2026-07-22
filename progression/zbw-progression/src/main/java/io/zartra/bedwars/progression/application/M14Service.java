package io.zartra.bedwars.progression.application;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.calendar.CalendarCampaign;
import io.zartra.bedwars.progression.calendar.CalendarCampaignId;
import io.zartra.bedwars.progression.catalog.M14Catalog;
import io.zartra.bedwars.progression.cosmetic.CosmeticDefinition;
import io.zartra.bedwars.progression.cosmetic.CosmeticId;
import io.zartra.bedwars.progression.cosmetic.CosmeticLoadout;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.profile.ProfileSettings;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.util.Optional;

/** Java 8-neutral M14 query and mutation contract for later M09 adapters. */
public interface M14Service {
    /** @return active immutable catalogue */ M14Catalog catalog();
    /** Finds a definition. */ Optional<CosmeticDefinition> cosmetic(CosmeticId id);
    /** Finds a campaign. */ Optional<CalendarCampaign> campaign(CalendarCampaignId id);
    /** Reads loadout state. */ Result<Optional<CosmeticLoadout>> loadout(UnitOfWork unitOfWork,
                                                                          PlayerProgressionId owner);
    /** Reads private settings. */ Result<Optional<ProfileSettings>> settings(UnitOfWork unitOfWork,
                                                                               PlayerProgressionId owner);
    /** Persists a validated loadout mutation. */
    Result<CosmeticLoadout> saveLoadout(UnitOfWork unitOfWork, CosmeticLoadout loadout,
                                        RecordRevision expectedRevision,
                                        IdempotencyKey idempotencyKey, AuditMetadata audit);
    /** Persists a validated settings mutation. */
    Result<ProfileSettings> saveSettings(UnitOfWork unitOfWork, ProfileSettings settings,
                                         RecordRevision expectedRevision,
                                         IdempotencyKey idempotencyKey, AuditMetadata audit);
}
