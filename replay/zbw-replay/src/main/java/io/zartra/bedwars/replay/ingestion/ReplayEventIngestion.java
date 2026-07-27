package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serial, non-owning ingestion boundary for existing M08/M11/M12 facts.
 * Callers preserve their authoritative lifecycle and serialize calls per replay.
 */
public final class ReplayEventIngestion {
    /** Converts each ordered M08 transition fact without mutating the transition. */
    public ReplaySession ingestGame(final ReplaySession session, final MatchTransition transition,
                                    final String sourceEventId) {
        Objects.requireNonNull(transition, "transition");
        ReplaySession current = Objects.requireNonNull(session, "session");
        if (transition.duplicate()) { return current; }
        int index = 0;
        for (MatchTransition.Fact fact : transition.facts()) {
            final Map<String, String> attributes = new LinkedHashMap<String, String>();
            if (fact.playerId().isPresent()) {
                attributes.put("playerId", fact.playerId().get().toString());
            }
            if (fact.teamId().isPresent()) {
                attributes.put("teamId", fact.teamId().get().toString());
            }
            current = append(current, sourceEventId + ":" + index,
                    transition.after().updatedAt(), ReplayEvent.Source.GAME,
                    fact.type().toString(), attributes);
            index++;
        }
        return current;
    }

    /** Converts one successful M11 settlement result; its duplicate flag remains observable. */
    public ReplaySession ingestShop(final ReplaySession session, final PurchaseOutcome outcome,
                                    final String sourceEventId) {
        Objects.requireNonNull(outcome, "outcome");
        if (outcome.duplicate()) { return Objects.requireNonNull(session, "session"); }
        return append(session, sourceEventId, outcome.observedAt(), ReplayEvent.Source.SHOP,
                "shop.purchase", Collections.singletonMap("duplicate", "false"));
    }

    /** Converts one immutable M12 projection input using the source event identity. */
    public ReplaySession ingestProgression(final ReplaySession session,
                                           final ProgressionEventInput input) {
        Objects.requireNonNull(input, "input");
        final Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("playerId", input.playerId().toString());
        attributes.put("schemaVersion", String.valueOf(input.metadata().schemaVersion()));
        return append(session, input.metadata().eventId().toString(), input.metadata().occurredAt(),
                ReplayEvent.Source.PROGRESSION, input.eventKind().toString(), attributes);
    }

    private ReplaySession append(final ReplaySession session, final String eventId,
                                 final Instant occurredAt, final ReplayEvent.Source source,
                                 final String type, final Map<String, String> attributes) {
        final ReplaySession current = Objects.requireNonNull(session, "session");
        final long offset = Duration.between(current.metadata().createdAt(), occurredAt).toMillis();
        if (offset < 0) { throw new IllegalArgumentException("event predates replay metadata"); }
        final ReplayEvent event = new ReplayEvent(eventId, current.timeline().nextSequence(), offset,
                occurredAt, source, type, attributes);
        return current.record(event);
    }
}
