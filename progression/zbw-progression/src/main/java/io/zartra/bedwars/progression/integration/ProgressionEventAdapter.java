package io.zartra.bedwars.progression.integration;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Configurable adapter from M08 completion and M11 settlement events to M12 XP intents. */
public final class ProgressionEventAdapter {
    private final Map<DefinitionId, Rule> rules;

    /** Creates an adapter containing only explicitly configured event kinds. */
    public ProgressionEventAdapter(final Map<DefinitionId, Rule> rules) {
        this.rules = Collections.unmodifiableMap(new LinkedHashMap<DefinitionId, Rule>(
                Objects.requireNonNull(rules, "rules")));
        if (this.rules.containsKey(null) || this.rules.containsValue(null)) {
            throw new IllegalArgumentException("rules must not contain null");
        }
    }

    /** Maps a neutral event without owning or recreating the source lifecycle. */
    public Optional<Intent> adapt(final ProgressionEventInput input) {
        Objects.requireNonNull(input, "input");
        final Rule rule = rules.get(input.eventKind());
        if (rule == null) { return Optional.empty(); }
        return Optional.of(new Intent(input, rule.source(), rule.baseExperience()));
    }

    /** One configured M08/M11 event mapping. */
    public static final class Rule {
        /** Source ownership. */ public enum Owner { /** Match completion from M08. */ M08_MATCH, /** Settled purchase from M11. */ M11_SETTLEMENT }
        private final Owner owner;
        private final DefinitionId source;
        private final long baseExperience;
        /** Creates a mapping. */ public Rule(final Owner owner, final DefinitionId source,
                                              final long baseExperience) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.source = Objects.requireNonNull(source, "source");
            if (baseExperience < 1) { throw new IllegalArgumentException("baseExperience must be positive"); }
            this.baseExperience = baseExperience;
        }
        /** @return source milestone */ public Owner owner() { return owner; }
        /** @return XP source */ public DefinitionId source() { return source; }
        /** @return configured base XP */ public long baseExperience() { return baseExperience; }
    }

    /** Idempotent projection intent retaining the original inbox key. */
    public static final class Intent {
        private final ProgressionEventInput input;
        private final DefinitionId source;
        private final long baseExperience;
        private Intent(final ProgressionEventInput input, final DefinitionId source,
                       final long baseExperience) {
            this.input = input;
            this.source = source;
            this.baseExperience = baseExperience;
        }
        /** @return original event */ public ProgressionEventInput input() { return input; }
        /** @return XP source */ public DefinitionId source() { return source; }
        /** @return base XP */ public long baseExperience() { return baseExperience; }
    }
}
