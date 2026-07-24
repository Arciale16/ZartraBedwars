package io.zartra.bedwars.progression.achievement;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable typed identity of an achievement definition. */
public final class AchievementId extends NamespacedIdentifier {
    private AchievementId(final String namespace, final String path) { super(namespace, path); }
    /** @return validated identity */ public static AchievementId of(final String namespace, final String path) {
        return new AchievementId(namespace, path);
    }
    /** @return parsed identity */ public static AchievementId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
