package io.zartra.bedwars.progression.calendar;

import io.zartra.bedwars.progression.model.RewardId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable calendar window that references the existing M12 reward engine. */
public final class CalendarCampaign {
    private final CalendarCampaignId id;
    private final int version;
    private final Instant startsAt;
    private final Instant endsAt;
    private final List<RewardId> rewards;
    private final Optional<String> eligibilityPolicy;

    /** Creates a validated campaign. */
    public CalendarCampaign(final CalendarCampaignId id, final int version,
                            final Instant startsAt, final Instant endsAt,
                            final List<RewardId> rewards,
                            final Optional<String> eligibilityPolicy) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
        this.version = version;
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt");
        if (!endsAt.isAfter(startsAt)) { throw new IllegalArgumentException("endsAt must follow startsAt"); }
        final List<RewardId> copy = new ArrayList<RewardId>(Objects.requireNonNull(rewards, "rewards"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("rewards must not be empty or contain null");
        }
        this.rewards = Collections.unmodifiableList(copy);
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy");
        if (eligibilityPolicy.isPresent()
                && !eligibilityPolicy.get().matches("[a-z0-9_.-]{3,128}")) {
            throw new IllegalArgumentException("eligibilityPolicy must be a safe identifier");
        }
    }
    /** @return identity */ public CalendarCampaignId id() { return id; }
    /** @return version */ public int version() { return version; }
    /** @return inclusive start */ public Instant startsAt() { return startsAt; }
    /** @return exclusive end */ public Instant endsAt() { return endsAt; }
    /** @return immutable M12 reward identities */ public List<RewardId> rewards() { return rewards; }
    /** @return optional local eligibility policy */ public Optional<String> eligibilityPolicy() { return eligibilityPolicy; }
}
