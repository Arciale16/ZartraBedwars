package io.zartra.bedwars.statistics.integration;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.progression.projection.ProjectionResult;
import io.zartra.bedwars.shop.api.PurchaseOutcome;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.projection.StatisticProjection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministically maps committed M08, M11 and M12 boundaries to statistics facts. */
public final class ConfiguredStatisticsEventAdapter {
    private final Map<DefinitionId, Mapping> mappings;

    /** Creates an immutable mapping table containing only explicitly enabled source kinds. */
    public ConfiguredStatisticsEventAdapter(final Map<DefinitionId, Mapping> mappings) {
        final Map<DefinitionId, Mapping> copy = new LinkedHashMap<DefinitionId, Mapping>(
                Objects.requireNonNull(mappings, "mappings"));
        if (copy.containsKey(null) || copy.containsValue(null)) {
            throw new IllegalArgumentException("mappings must not contain null");
        }
        this.mappings = Collections.unmodifiableMap(copy);
    }

    /** Converts ordered M08 facts without owning or replaying the match state machine. */
    public List<PlayerEvent> fromM08(final M08Event event) {
        Objects.requireNonNull(event, "event");
        if (event.transition().duplicate()) {
            return Collections.emptyList();
        }
        final List<PlayerEvent> converted = new ArrayList<PlayerEvent>();
        int ordinal = 0;
        for (MatchTransition.Fact fact : event.transition().facts()) {
            final Optional<PlayerId> player = fact.playerId();
            final Mapping mapping = mappings.get(fact.type());
            if (player.isPresent() && mapping != null) {
                converted.add(playerEvent(event.metadata(), event.idempotencyKey(), ordinal,
                        StatisticProjection.Source.MATCH, player.get(), mapping));
            }
            ordinal++;
        }
        return ordered(converted);
    }

    /** Converts a committed M11 purchase without duplicating settlement or resource charging. */
    public List<PlayerEvent> fromM11(final M11Event event) {
        Objects.requireNonNull(event, "event");
        if (event.outcome().duplicate()) {
            return Collections.emptyList();
        }
        final DefinitionId item = event.outcome().quote().request().itemId().value();
        final Mapping mapping = mappings.get(item);
        if (mapping == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(playerEvent(event.metadata(), event.idempotencyKey(), 0,
                StatisticProjection.Source.SETTLEMENT,
                event.outcome().quote().request().context().playerId(), mapping));
    }

    /** Converts only an applied M12 projection, retaining its durable source key and order. */
    public List<PlayerEvent> fromM12(final M12Event event) {
        Objects.requireNonNull(event, "event");
        if (event.result().status() != ProjectionResult.Status.APPLIED) {
            return Collections.emptyList();
        }
        final Mapping mapping = mappings.get(event.input().eventKind());
        if (mapping == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(playerEvent(event.input().metadata(),
                event.input().idempotencyKey(), 0, StatisticProjection.Source.PROGRESSION,
                event.input().playerId().playerId(), mapping));
    }

    private static PlayerEvent playerEvent(final EventMetadata metadata,
                                           final IdempotencyKey sourceKey, final int ordinal,
                                           final StatisticProjection.Source source,
                                           final PlayerId playerId, final Mapping mapping) {
        return new PlayerEvent(playerId, new StatisticProjection.Event(metadata.eventId(),
                childKey(sourceKey, ordinal), source, mapping.statisticId(), mapping.scope(),
                mapping.delta(), new StatisticAudit("statistics-adapter",
                        metadata.correlationId(), metadata.occurredAt())), metadata.sequence(), ordinal);
    }

    private static IdempotencyKey childKey(final IdempotencyKey sourceKey, final int ordinal) {
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative");
        }
        final String path = sourceKey.path() + "/statistics-" + ordinal;
        if (path.length() > 94) {
            throw new IllegalArgumentException("source idempotency key is too long for statistics");
        }
        return IdempotencyKey.of(sourceKey.namespace(), path);
    }

    private static List<PlayerEvent> ordered(final List<PlayerEvent> events) {
        Collections.sort(events, new Comparator<PlayerEvent>() {
            @Override
            public int compare(final PlayerEvent left, final PlayerEvent right) {
                final int sequence = Long.compare(left.sequence(), right.sequence());
                return sequence != 0 ? sequence : Integer.compare(left.ordinal(), right.ordinal());
            }
        });
        return Collections.unmodifiableList(new ArrayList<PlayerEvent>(events));
    }

    /** Explicit configured contribution mapping; no statistic is inferred from an event name. */
    public static final class Mapping {
        private final StatisticId statisticId;
        private final StatisticScope scope;
        private final long delta;

        /** Creates a positive, deterministic configured contribution. */
        public Mapping(final StatisticId statisticId, final StatisticScope scope, final long delta) {
            this.statisticId = Objects.requireNonNull(statisticId, "statisticId");
            this.scope = Objects.requireNonNull(scope, "scope");
            if (delta < 1) {
                throw new IllegalArgumentException("delta must be positive");
            }
            this.delta = delta;
        }

        /** @return target statistic */
        public StatisticId statisticId() { return statisticId; }
        /** @return target partition */
        public StatisticScope scope() { return scope; }
        /** @return configured positive contribution */
        public long delta() { return delta; }
    }

    /** M08 transition plus the durable metadata and key supplied by its event boundary. */
    public static final class M08Event {
        private final EventMetadata metadata;
        private final IdempotencyKey idempotencyKey;
        private final MatchTransition transition;
        /** Creates a replay-safe M08 statistics input. */
        public M08Event(final EventMetadata metadata, final IdempotencyKey idempotencyKey,
                        final MatchTransition transition) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            this.transition = Objects.requireNonNull(transition, "transition");
        }
        /** @return original event metadata */ public EventMetadata metadata() { return metadata; }
        /** @return replay-safe source key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
        /** @return committed M08 transition */ public MatchTransition transition() { return transition; }
    }

    /** M11 settled purchase plus metadata supplied by its committed settlement boundary. */
    public static final class M11Event {
        private final EventMetadata metadata;
        private final IdempotencyKey idempotencyKey;
        private final PurchaseOutcome outcome;
        /** Creates a replay-safe M11 statistics input. */
        public M11Event(final EventMetadata metadata, final IdempotencyKey idempotencyKey,
                        final PurchaseOutcome outcome) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            this.outcome = Objects.requireNonNull(outcome, "outcome");
        }
        /** @return original event metadata */ public EventMetadata metadata() { return metadata; }
        /** @return replay-safe source key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
        /** @return committed M11 purchase */ public PurchaseOutcome outcome() { return outcome; }
    }

    /** M12 projection result paired with its immutable input boundary. */
    public static final class M12Event {
        private final ProgressionEventInput input;
        private final ProjectionResult result;
        /** Creates an M12 input/result pair without changing progression ownership. */
        public M12Event(final ProgressionEventInput input, final ProjectionResult result) {
            this.input = Objects.requireNonNull(input, "input");
            this.result = Objects.requireNonNull(result, "result");
        }
        /** @return original M12 projection input */ public ProgressionEventInput input() { return input; }
        /** @return original M12 projection result */ public ProjectionResult result() { return result; }
    }

    /** Player-targeted normalized fact ready for the existing M15 projection engine. */
    public static final class PlayerEvent {
        private final PlayerId playerId;
        private final StatisticProjection.Event event;
        private final long sequence;
        private final int ordinal;
        private PlayerEvent(final PlayerId playerId, final StatisticProjection.Event event,
                            final long sequence, final int ordinal) {
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            this.event = Objects.requireNonNull(event, "event");
            this.sequence = sequence;
            this.ordinal = ordinal;
        }
        /** @return affected player */ public PlayerId playerId() { return playerId; }
        /** @return normalized M15 fact */ public StatisticProjection.Event event() { return event; }
        /** @return original source sequence */ public long sequence() { return sequence; }
        /** @return stable position within its source event */ public int ordinal() { return ordinal; }
    }
}
