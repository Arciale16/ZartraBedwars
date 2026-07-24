package io.zartra.bedwars.progression.challenge;

import io.zartra.bedwars.api.identity.NamespacedIdentifier;

/** Stable typed identity of a challenge definition. */
public final class ChallengeId extends NamespacedIdentifier {
    private ChallengeId(final String namespace, final String path) { super(namespace, path); }
    /** @return validated identity */ public static ChallengeId of(final String namespace, final String path) {
        return new ChallengeId(namespace, path);
    }
    /** @return parsed identity */ public static ChallengeId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
