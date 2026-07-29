package io.zartra.bedwars.api.integration.npc;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Stable NPC CRUD/render boundary implemented later by internal or vendor adapters. */
public interface NpcProvider extends Provider {
    /** @param definition immutable definition @return asynchronous stored definition */
    CompletionStage<Result<Definition>> upsert(Definition definition);
    /** @param definitionId definition identity @return asynchronous removal outcome */
    CompletionStage<Result<Boolean>> remove(DefinitionId definitionId);
    /** @param definitionId definition identity @param viewerId viewer identity @return render outcome */
    CompletionStage<Result<Boolean>> render(DefinitionId definitionId, PlayerId viewerId);
    /** @return asynchronously exported immutable definitions for provider migration */
    CompletionStage<Result<List<Definition>>> exportDefinitions();

    /** Immutable provider-neutral NPC definition. */
    final class Definition {
        private final DefinitionId definitionId;
        private final Purpose purpose;
        private final String displayName;
        private final String skinReference;
        private final long revision;

        /**
         * Creates an NPC definition.
         *
         * @param definitionId stable definition identity
         * @param purpose semantic use
         * @param displayName bounded display text
         * @param skinReference optional sanitized content reference
         * @param revision non-negative revision
         */
        public Definition(final DefinitionId definitionId, final Purpose purpose,
                          final String displayName, final String skinReference,
                          final long revision) {
            this.definitionId = Objects.requireNonNull(definitionId, "definitionId");
            this.purpose = Objects.requireNonNull(purpose, "purpose");
            if (displayName == null || displayName.isEmpty() || displayName.length() > 128) {
                throw new IllegalArgumentException("displayName must be bounded");
            }
            if (skinReference != null
                    && !skinReference.matches("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,255}")) {
                throw new IllegalArgumentException("skinReference must be sanitized");
            }
            if (revision < 0) { throw new IllegalArgumentException("revision must be non-negative"); }
            this.displayName = displayName;
            this.skinReference = skinReference;
            this.revision = revision;
        }

        /** @return definition identity */
        public DefinitionId definitionId() { return definitionId; }
        /** @return semantic purpose */
        public Purpose purpose() { return purpose; }
        /** @return bounded display text */
        public String displayName() { return displayName; }
        /** @return optional content-addressed skin reference */
        public Optional<String> skinReference() { return Optional.ofNullable(skinReference); }
        /** @return definition revision */
        public long revision() { return revision; }
    }

    /** Supported provider-neutral NPC purposes. */
    enum Purpose { SELECTOR, SHOP, MENU, ADMIN }
}
