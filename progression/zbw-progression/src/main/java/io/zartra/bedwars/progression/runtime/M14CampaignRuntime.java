package io.zartra.bedwars.progression.runtime;

import io.zartra.bedwars.progression.calendar.CalendarCampaign;
import io.zartra.bedwars.progression.calendar.CalendarCampaignId;
import io.zartra.bedwars.progression.catalog.M14Catalog;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Calendar availability policy that hands validated reward references to the M12 engine. */
public final class M14CampaignRuntime {
    private final M14Catalog catalog;

    /** Creates a campaign resolver for the immutable M14 catalogue. */
    public M14CampaignRuntime(final M14Catalog catalog) { this.catalog = Objects.requireNonNull(catalog, "catalog"); }

    /** Resolves a campaign without attempting delivery or creating a second reward system. */
    public Decision evaluate(final CalendarCampaignId id, final Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(now, "now");
        for (CalendarCampaign candidate : catalog.campaigns()) {
            if (candidate.id().equals(id)) {
                if (now.isBefore(candidate.startsAt())) { return Decision.of(Status.NOT_STARTED, candidate); }
                if (!now.isBefore(candidate.endsAt())) { return Decision.of(Status.EXPIRED, candidate); }
                return Decision.of(Status.ACTIVE, candidate);
            }
        }
        return Decision.of(Status.NOT_FOUND, null);
    }

    /** Campaign availability outcome; ACTIVE carries M12 reward identities for the caller. */
    public static final class Decision {
        private final Status status;
        private final CalendarCampaign campaign;
        private Decision(final Status status, final CalendarCampaign campaign) {
            this.status = status; this.campaign = campaign;
        }
        /** Creates a decision. */ public static Decision of(final Status status, final CalendarCampaign campaign) {
            return new Decision(Objects.requireNonNull(status, "status"), campaign);
        }
        /** @return availability state */ public Status status() { return status; }
        /** @return campaign, including its M12 reward references, when known */
        public Optional<CalendarCampaign> campaign() { return Optional.ofNullable(campaign); }
    }
    /** Campaign lifecycle states. */ public enum Status { ACTIVE, NOT_STARTED, EXPIRED, NOT_FOUND }
}
