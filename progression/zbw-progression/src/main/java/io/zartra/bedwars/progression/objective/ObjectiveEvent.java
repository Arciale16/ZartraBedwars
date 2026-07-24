package io.zartra.bedwars.progression.objective;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable allow-listed fact evaluated by the M13 objective engine. */
public final class ObjectiveEvent {
    /** Authoritative producer of the fact. */
    public enum Source {
        /** M08 match/game lifecycle. */ M08_GAME,
        /** M11 settled shop operation. */ M11_SETTLEMENT,
        /** M12 progression or reward event. */ M12_PROGRESSION
    }

    private final Source source;
    private final ObjectiveEventType type;
    private final PlayerProgressionId playerId;
    private final long amount;
    private final Map<String, String> attributes;
    private final IdempotencyKey idempotencyKey;
    private final AuditMetadata audit;

    /** Creates a bounded objective fact. */
    public ObjectiveEvent(final Source source, final ObjectiveEventType type,
                          final PlayerProgressionId playerId, final long amount,
                          final Map<String, String> attributes,
                          final IdempotencyKey idempotencyKey, final AuditMetadata audit) {
        this.source = Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(type, "type");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        if (amount < 1) { throw new IllegalArgumentException("amount must be positive"); }
        this.amount = amount;
        final Map<String, String> copy = new LinkedHashMap<String, String>(
                Objects.requireNonNull(attributes, "attributes"));
        if (copy.size() > 32 || copy.containsKey(null) || copy.containsValue(null)) {
            throw new IllegalArgumentException("attributes must contain at most 32 non-null entries");
        }
        for (Map.Entry<String, String> entry : copy.entrySet()) {
            if (entry.getKey().isEmpty() || entry.getKey().length() > 64
                    || entry.getValue().length() > 256) {
                throw new IllegalArgumentException("objective attribute is outside its bounds");
            }
        }
        this.attributes = Collections.unmodifiableMap(copy);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** @return authoritative event source */ public Source source() { return source; }
    /** @return allow-listed objective event type */ public ObjectiveEventType type() { return type; }
    /** @return affected player */ public PlayerProgressionId playerId() { return playerId; }
    /** @return positive progress amount */ public long amount() { return amount; }
    /** @return immutable filter attributes */ public Map<String, String> attributes() { return attributes; }
    /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    /** @return immutable audit context */ public AuditMetadata audit() { return audit; }
}
