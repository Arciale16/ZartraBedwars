package io.zartra.bedwars.progression.calendar;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable identity of a calendar campaign. */
public final class CalendarCampaignId extends NamespacedIdentifier {
    private CalendarCampaignId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed campaign identity */
    public static CalendarCampaignId of(final String namespace, final String path) {
        return new CalendarCampaignId(namespace, path);
    }
}
