package io.zartra.bedwars.api.integration.hologram;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Stable hologram CRUD/render boundary for internal or vendor adapters. */
public interface HologramProvider extends Provider {
    /** @param definition immutable definition @return asynchronous stored definition */
    CompletionStage<Result<Definition>> upsert(Definition definition);
    /** @param definitionId definition identity @return asynchronous removal outcome */
    CompletionStage<Result<Boolean>> remove(DefinitionId definitionId);
    /** @param definitionId definition identity @param viewerId viewer identity @return render outcome */
    CompletionStage<Result<Boolean>> render(DefinitionId definitionId, PlayerId viewerId);
    /** @return asynchronously exported immutable definitions for provider migration */
    CompletionStage<Result<List<Definition>>> exportDefinitions();

    /** Immutable bounded hologram definition. */
    final class Definition {
        private final DefinitionId definitionId;
        private final List<String> lines;
        private final Duration minimumUpdateInterval;
        private final long revision;

        /**
         * Creates a hologram definition.
         *
         * @param definitionId stable definition identity
         * @param lines one to sixteen bounded lines
         * @param minimumUpdateInterval positive provider update interval
         * @param revision non-negative revision
         */
        public Definition(final DefinitionId definitionId, final List<String> lines,
                          final Duration minimumUpdateInterval, final long revision) {
            this.definitionId = Objects.requireNonNull(definitionId, "definitionId");
            Objects.requireNonNull(lines, "lines");
            this.minimumUpdateInterval =
                    Objects.requireNonNull(minimumUpdateInterval, "minimumUpdateInterval");
            if (lines.isEmpty() || lines.size() > 16 || minimumUpdateInterval.isNegative()
                    || minimumUpdateInterval.isZero() || revision < 0) {
                throw new IllegalArgumentException("invalid hologram definition");
            }
            List<String> copy = new ArrayList<String>(lines.size());
            for (String line : lines) {
                if (line == null || line.length() > 256 || line.indexOf('\u0000') >= 0) {
                    throw new IllegalArgumentException("hologram line must be bounded");
                }
                copy.add(line);
            }
            this.lines = Collections.unmodifiableList(copy);
            this.revision = revision;
        }

        /** @return definition identity */
        public DefinitionId definitionId() { return definitionId; }
        /** @return immutable display lines */
        public List<String> lines() { return lines; }
        /** @return minimum update interval */
        public Duration minimumUpdateInterval() { return minimumUpdateInterval; }
        /** @return definition revision */
        public long revision() { return revision; }
    }
}
