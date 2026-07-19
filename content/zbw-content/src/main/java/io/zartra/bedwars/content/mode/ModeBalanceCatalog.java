package io.zartra.bedwars.content.mode;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, versioned and identity-ordered collection of original mode balance profiles. */
public final class ModeBalanceCatalog {
    private final int version;
    private final Map<DefinitionId, Profile> profiles;

    /** Creates a non-empty catalogue with unique profile identities. */
    public ModeBalanceCatalog(final int version, final Collection<Profile> profiles) {
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        this.version = version;
        final List<Profile> ordered =
                new ArrayList<Profile>(Objects.requireNonNull(profiles, "profiles"));
        ordered.sort(Comparator.comparing(Profile::id));
        final Map<DefinitionId, Profile> indexed = new LinkedHashMap<DefinitionId, Profile>();
        for (Profile profile : ordered) {
            final Profile checked = Objects.requireNonNull(profile, "profile");
            if (indexed.put(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate mode profile");
            }
        }
        if (indexed.isEmpty()) {
            throw new IllegalArgumentException("mode profiles are required");
        }
        this.profiles = Collections.unmodifiableMap(indexed);
    }

    /** @return content version */
    public int version() {
        return version;
    }

    /** @return immutable ordered profiles */
    public Map<DefinitionId, Profile> profiles() {
        return profiles;
    }

    /** @return required profile */
    public Profile require(final DefinitionId id) {
        final Profile result = profiles.get(Objects.requireNonNull(id, "id"));
        if (result == null) {
            throw new IllegalArgumentException("unknown mode profile");
        }
        return result;
    }

    /** Runs the same pure simulation twice and rejects nondeterminism or budget overflow. */
    public GoldenResult simulate(final DefinitionId profileId, final GoldenSimulation simulation,
                                 final long maximumScore) {
        if (maximumScore < 0) {
            throw new IllegalArgumentException("maximumScore cannot be negative");
        }
        final Profile profile = require(profileId);
        final long first = simulation.score(profile);
        final long second = simulation.score(profile);
        if (first != second) {
            return GoldenResult.failure("nondeterministic");
        }
        return first < 0 || first > maximumScore
                ? GoldenResult.failure("score_out_of_bounds") : GoldenResult.success(first);
    }

    /** Immutable mode profile with bounded integer tuning values. */
    public static final class Profile {
        private final DefinitionId id;
        private final Map<DefinitionId, Integer> values;

        /** Creates a profile. */
        public Profile(final DefinitionId id, final Map<DefinitionId, Integer> values) {
            this.id = Objects.requireNonNull(id, "id");
            final List<Map.Entry<DefinitionId, Integer>> ordered =
                    new ArrayList<Map.Entry<DefinitionId, Integer>>(
                            Objects.requireNonNull(values, "values").entrySet());
            ordered.sort(Comparator.comparing(Map.Entry::getKey));
            final Map<DefinitionId, Integer> checked =
                    new LinkedHashMap<DefinitionId, Integer>();
            for (Map.Entry<DefinitionId, Integer> entry : ordered) {
                final int value = Objects.requireNonNull(entry.getValue(), "value");
                if (value < 0 || value > 1000000) {
                    throw new IllegalArgumentException("balance value outside bounds");
                }
                checked.put(Objects.requireNonNull(entry.getKey(), "key"), value);
            }
            if (checked.isEmpty()) {
                throw new IllegalArgumentException("profile values are required");
            }
            this.values = Collections.unmodifiableMap(checked);
        }

        /** @return profile ID */
        public DefinitionId id() {
            return id;
        }

        /** @return immutable tuning values */
        public Map<DefinitionId, Integer> values() {
            return values;
        }
    }

    /** Pure golden simulator. */
    public interface GoldenSimulation {
        /** Returns the deterministic score for one profile. */
        long score(Profile profile);
    }

    /** Golden simulation result. */
    public static final class GoldenResult {
        private final boolean success;
        private final String code;
        private final long score;

        private GoldenResult(final boolean success, final String code, final long score) {
            this.success = success;
            this.code = code;
            this.score = score;
        }

        private static GoldenResult success(final long score) {
            return new GoldenResult(true, "ok", score);
        }

        private static GoldenResult failure(final String code) {
            return new GoldenResult(false, code, 0);
        }

        /** @return whether simulation passed */
        public boolean success() {
            return success;
        }

        /** @return stable result code */
        public String code() {
            return code;
        }

        /** @return deterministic score, or zero on failure */
        public long score() {
            return score;
        }
    }
}
