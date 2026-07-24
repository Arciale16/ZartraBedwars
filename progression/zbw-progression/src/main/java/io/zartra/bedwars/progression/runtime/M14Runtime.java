package io.zartra.bedwars.progression.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.calendar.CalendarCampaign;
import io.zartra.bedwars.progression.catalog.M14Catalog;
import io.zartra.bedwars.progression.catalog.M14Configuration;
import io.zartra.bedwars.progression.cosmetic.CosmeticDefinition;
import io.zartra.bedwars.progression.cosmetic.CosmeticId;
import io.zartra.bedwars.progression.cosmetic.CosmeticLoadout;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.repository.CosmeticStateRepository;
import io.zartra.bedwars.progression.repository.EntitlementRepository;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Deterministic M14 ownership, loadout and campaign runtime over existing M12 ports. */
public final class M14Runtime {
    private final M14Catalog catalog;
    private final M14Configuration configuration;
    private final CosmeticStateRepository loadouts;
    private final EntitlementRepository entitlements;

    /** Creates the runtime with immutable catalogue/configuration and existing persistence ports. */
    public M14Runtime(final M14Catalog catalog, final M14Configuration configuration,
                      final CosmeticStateRepository loadouts,
                      final EntitlementRepository entitlements) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.loadouts = Objects.requireNonNull(loadouts, "loadouts");
        this.entitlements = Objects.requireNonNull(entitlements, "entitlements");
    }

    /** Resolves currently active campaigns without querying a platform or reward provider. */
    public List<CalendarCampaign> activeCampaigns(final Instant now) {
        Objects.requireNonNull(now, "now");
        final List<CalendarCampaign> active = new ArrayList<CalendarCampaign>();
        for (CalendarCampaign campaign : catalog.campaigns()) {
            if (!now.isBefore(campaign.startsAt()) && now.isBefore(campaign.endsAt())) { active.add(campaign); }
        }
        return Collections.unmodifiableList(active);
    }

    /** Validates a cosmetic selection against definition, entitlement and emergency policy. */
    public Decision validateSelection(final UnitOfWork unitOfWork, final PlayerProgressionId owner,
                                      final CosmeticId cosmeticId, final Instant now) {
        Objects.requireNonNull(unitOfWork, "unitOfWork");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(cosmeticId, "cosmeticId");
        Objects.requireNonNull(now, "now");
        if (configuration.emergencyDisable()) { return Decision.rejected(Status.DISABLED); }
        final Optional<CosmeticDefinition> definition = definition(cosmeticId);
        if (!definition.isPresent()) { return Decision.rejected(Status.NOT_FOUND); }
        if (!definition.get().enabled()) { return Decision.rejected(Status.UNAVAILABLE); }
        final Result<Set<EntitlementId>> owned = entitlements.findAll(unitOfWork, owner);
        if (owned.isFailure()) { return Decision.rejected(Status.RETRY); }
        if (definition.get().entitlement().isPresent()
                && !owned.requireValue().contains(definition.get().entitlement().get())) {
            return Decision.rejected(Status.LOCKED);
        }
        return Decision.accepted(definition.get());
    }

    /** Applies one validated selection exactly through caller-owned persistence boundaries. */
    public Result<CosmeticLoadout> equip(final UnitOfWork unitOfWork, final PlayerProgressionId owner,
                                         final CosmeticId cosmeticId,
                                         final RecordRevision expectedRevision,
                                         final IdempotencyKey idempotencyKey,
                                         final AuditMetadata audit, final Instant now) {
        final Decision decision = validateSelection(unitOfWork, owner, cosmeticId, now);
        if (!decision.accepted()) { throw new IllegalArgumentException("cosmetic selection " + decision.status()); }
        final Result<Optional<CosmeticLoadout>> existing = loadouts.find(unitOfWork, owner);
        if (existing.isFailure()) { return Result.failure(existing.error().get()); }
        final CosmeticLoadout current = existing.requireValue().orElseThrow(
                () -> new IllegalArgumentException("loadout absent"));
        if (!current.revision().equals(expectedRevision)) {
            throw new IllegalArgumentException("stale cosmetic loadout revision");
        }
        final Map<io.zartra.bedwars.progression.cosmetic.CosmeticCategoryId, CosmeticId> equipped =
                new LinkedHashMap<io.zartra.bedwars.progression.cosmetic.CosmeticCategoryId, CosmeticId>(
                        current.equipped());
        equipped.put(decision.definition().get().categoryId(), cosmeticId);
        final CosmeticLoadout next = new CosmeticLoadout(owner, equipped, current.favourites(),
                current.presets(), current.revision().next(), audit);
        return loadouts.save(unitOfWork, next, expectedRevision, idempotencyKey);
    }

    /** @return immutable catalogue definition if available */
    public Optional<CosmeticDefinition> definition(final CosmeticId cosmeticId) {
        for (CosmeticDefinition definition : catalog.cosmetics()) {
            if (definition.id().equals(cosmeticId)) { return Optional.of(definition); }
        }
        return Optional.empty();
    }

    /** Selection outcome that avoids a platform exception path for expected invalid states. */
    public static final class Decision {
        private final Status status;
        private final CosmeticDefinition definition;
        private Decision(final Status status, final CosmeticDefinition definition) {
            this.status = status;
            this.definition = definition;
        }
        /** @return accepted decision */ public static Decision accepted(final CosmeticDefinition value) {
            return new Decision(Status.ACCEPTED, Objects.requireNonNull(value, "value"));
        }
        /** @return rejected decision */ public static Decision rejected(final Status value) {
            if (value == Status.ACCEPTED) { throw new IllegalArgumentException("accepted requires definition"); }
            return new Decision(Objects.requireNonNull(value, "value"), null);
        }
        /** @return whether selection may proceed */ public boolean accepted() { return status == Status.ACCEPTED; }
        /** @return deterministic status */ public Status status() { return status; }
        /** @return validated definition when accepted */ public Optional<CosmeticDefinition> definition() {
            return Optional.ofNullable(definition);
        }
    }

    /** Cosmetic selection outcomes. */
    public enum Status { ACCEPTED, NOT_FOUND, UNAVAILABLE, LOCKED, DISABLED, RETRY }
}
