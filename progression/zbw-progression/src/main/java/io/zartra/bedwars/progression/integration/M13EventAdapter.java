package io.zartra.bedwars.progression.integration;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.objective.ObjectiveEvent;
import io.zartra.bedwars.progression.objective.ObjectiveEventType;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Configured bridge from existing M08/M11/M12 facts to M13 objective facts. */
public final class M13EventAdapter {
    private final Map<DefinitionId, Rule> rules;

    /** Creates an adapter containing only explicitly allow-listed source event kinds. */
    public M13EventAdapter(final Map<DefinitionId, Rule> rules) {
        final Map<DefinitionId, Rule> copy = new LinkedHashMap<DefinitionId, Rule>(
                Objects.requireNonNull(rules, "rules"));
        if (copy.containsKey(null) || copy.containsValue(null)) {
            throw new IllegalArgumentException("rules must not contain null");
        }
        this.rules = Collections.unmodifiableMap(copy);
    }

    /** Maps one existing event without duplicating its source pipeline. */
    public Optional<ObjectiveEvent> adapt(final ProgressionEventInput input,
                                          final Map<String, String> attributes) {
        Objects.requireNonNull(input, "input");
        final Rule rule = rules.get(input.eventKind());
        if (rule == null) { return Optional.empty(); }
        final AuditMetadata audit = new AuditMetadata(rule.auditActor(),
                input.metadata().correlationId(), input.metadata().occurredAt(),
                input.metadata().occurredAt());
        return Optional.of(new ObjectiveEvent(rule.source(), rule.objectiveType(), input.playerId(),
                rule.amount(), attributes, input.idempotencyKey(), audit));
    }

    /** Immutable source-to-objective mapping. */
    public static final class Rule {
        private final ObjectiveEvent.Source source;
        private final ObjectiveEventType objectiveType;
        private final long amount;
        private final String auditActor;
        /** Creates a validated event rule. */
        public Rule(final ObjectiveEvent.Source source, final ObjectiveEventType objectiveType,
                    final long amount, final String auditActor) {
            this.source = Objects.requireNonNull(source, "source");
            this.objectiveType = Objects.requireNonNull(objectiveType, "objectiveType");
            if (amount < 1) { throw new IllegalArgumentException("amount must be positive"); }
            if (auditActor == null || auditActor.trim().isEmpty() || auditActor.length() > 128) {
                throw new IllegalArgumentException("auditActor must contain 1..128 characters");
            }
            this.amount = amount;
            this.auditActor = auditActor;
        }
        /** @return authoritative source */ public ObjectiveEvent.Source source() { return source; }
        /** @return objective event type */ public ObjectiveEventType objectiveType() { return objectiveType; }
        /** @return configured progress amount */ public long amount() { return amount; }
        /** @return audit actor */ public String auditActor() { return auditActor; }
    }
}
