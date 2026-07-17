package io.zartra.bedwars.game.matchmaking;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.game.mode.ModeFramework.ModeId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Typed queue, deterministic policy, reservation and assignment boundaries. All mutable services
 * are bounded and synchronized; no method performs database, filesystem, network or platform work.
 */
public final class MatchmakingFramework {
    private MatchmakingFramework() { throw new AssertionError("No instances"); }

    /** Stable namespaced queue identity. */
    public static final class QueueId implements Comparable<QueueId> {
        private final DefinitionId value;
        private QueueId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return a queue ID */ public static QueueId of(final String namespace, final String path) { return new QueueId(DefinitionId.of(namespace, "queue/" + path)); }
        /** @return parsed queue ID */ public static QueueId parse(final String value) { return new QueueId(DefinitionId.parse(value)); }
        /** @return underlying ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final QueueId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof QueueId && value.equals(((QueueId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Stable party identity, independent of a particular party plugin. */
    public static final class PartyId implements Comparable<PartyId> {
        private final UUID value;
        private PartyId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return typed identity */ public static PartyId of(final UUID value) { return new PartyId(value); }
        /** @return parsed identity */ public static PartyId parse(final String value) { return of(UUID.fromString(value)); }
        /** @return UUID */ public UUID value() { return value; }
        @Override public int compareTo(final PartyId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof PartyId && value.equals(((PartyId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Immutable provider-neutral party snapshot with optimistic revision. */
    public static final class Party {
        private final PartyId id;
        private final PlayerId leader;
        private final List<PlayerId> members;
        private final long revision;
        /** Creates a party whose leader must be a unique member. */
        public Party(final PartyId id, final PlayerId leader, final Collection<PlayerId> members,
                     final long revision) {
            this.id = Objects.requireNonNull(id, "id");
            this.leader = Objects.requireNonNull(leader, "leader");
            if (revision < 0L) { throw new IllegalArgumentException("party revision must not be negative"); }
            this.revision = revision;
            final Set<PlayerId> unique = new LinkedHashSet<PlayerId>();
            for (PlayerId member : Objects.requireNonNull(members, "members")) {
                if (!unique.add(Objects.requireNonNull(member, "member"))) {
                    throw new IllegalArgumentException("duplicate party member");
                }
            }
            if (unique.isEmpty() || unique.size() > 256 || !unique.contains(leader)) {
                throw new IllegalArgumentException("invalid party membership");
            }
            this.members = Collections.unmodifiableList(new ArrayList<PlayerId>(unique));
        }
        /** @return party ID */ public PartyId id() { return id; }
        /** @return leader */ public PlayerId leader() { return leader; }
        /** @return ordered members */ public List<PlayerId> members() { return members; }
        /** @return membership revision */ public long revision() { return revision; }
        /** @return member count */ public int size() { return members.size(); }
    }

    /** Immutable request carrying all deterministic selection and audit inputs. */
    public static final class Request {
        private final QueueId queueId;
        private final PlayerId actor;
        private final Party party;
        private final ModeId mode;
        private final DefinitionId layout;
        private final int teamSize;
        private final Set<ArenaId> arenaConstraints;
        private final List<DefinitionId> mapPreferences;
        private final String locale;
        private final String region;
        private final int priority;
        private final Instant enqueuedAt;
        private final long revision;
        private final UUID cancellationToken;
        private final Instant deadline;
        private final IdempotencyKey idempotencyKey;
        private final CorrelationId correlationId;
        /** Creates one validated solo or party request. */
        public Request(final QueueId queueId, final PlayerId actor, final Party party,
                       final ModeId mode, final DefinitionId layout, final int teamSize,
                       final Collection<ArenaId> arenaConstraints,
                       final Collection<DefinitionId> mapPreferences, final String locale,
                       final String region, final int priority, final Instant enqueuedAt,
                       final long revision, final UUID cancellationToken, final Instant deadline,
                       final IdempotencyKey idempotencyKey, final CorrelationId correlationId) {
            this.queueId = Objects.requireNonNull(queueId, "queueId");
            this.actor = Objects.requireNonNull(actor, "actor");
            this.party = party;
            this.mode = Objects.requireNonNull(mode, "mode");
            this.layout = Objects.requireNonNull(layout, "layout");
            if (teamSize < 1 || teamSize > 64 || priority < 0 || priority > 1000 || revision < 0L) {
                throw new IllegalArgumentException("request bounds outside policy");
            }
            if (party != null && (!party.leader().equals(actor) || party.size() > teamSize)) {
                throw new IllegalArgumentException("party leader or fit invalid");
            }
            this.teamSize = teamSize;
            this.arenaConstraints = immutableSet(arenaConstraints, "arena constraint", 1024);
            this.mapPreferences = immutableList(mapPreferences, "map preference", 128);
            this.locale = boundedToken(locale, "locale", 32);
            this.region = boundedToken(region, "region", 64);
            this.priority = priority;
            this.enqueuedAt = Objects.requireNonNull(enqueuedAt, "enqueuedAt");
            this.revision = revision;
            this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
            this.deadline = Objects.requireNonNull(deadline, "deadline");
            if (!deadline.isAfter(enqueuedAt)) { throw new IllegalArgumentException("deadline must follow enqueue time"); }
            this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        }
        /** @return queue */ public QueueId queueId() { return queueId; }
        /** @return authorized actor/party leader */ public PlayerId actor() { return actor; }
        /** @return party snapshot */ public Optional<Party> party() { return Optional.ofNullable(party); }
        /** @return requested mode */ public ModeId mode() { return mode; }
        /** @return requested layout */ public DefinitionId layout() { return layout; }
        /** @return requested team size */ public int teamSize() { return teamSize; }
        /** @return allowed arenas, empty for any */ public Set<ArenaId> arenaConstraints() { return arenaConstraints; }
        /** @return ordered map preferences */ public List<DefinitionId> mapPreferences() { return mapPreferences; }
        /** @return locale metadata */ public String locale() { return locale; }
        /** @return region metadata */ public String region() { return region; }
        /** @return base priority */ public int priority() { return priority; }
        /** @return enqueue instant */ public Instant enqueuedAt() { return enqueuedAt; }
        /** @return request revision */ public long revision() { return revision; }
        /** @return cancellation token */ public UUID cancellationToken() { return cancellationToken; }
        /** @return hard deadline */ public Instant deadline() { return deadline; }
        /** @return retry key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
        /** @return trace/audit identity */ public CorrelationId correlationId() { return correlationId; }
        /** @return atomic actor list */ public List<PlayerId> actors() { return party == null ? Collections.singletonList(actor) : party.members(); }
    }

    /** Bounded queue configuration. */
    public static final class Limits {
        private final int maximumQueues;
        private final int maximumRequestsPerQueue;
        private final int maximumActors;
        private final Duration defaultExpiry;
        private final Duration agingInterval;
        /** Creates measurable queue limits. */
        public Limits(final int maximumQueues, final int maximumRequestsPerQueue,
                      final int maximumActors, final Duration defaultExpiry,
                      final Duration agingInterval) {
            if (maximumQueues < 1 || maximumQueues > 1024 || maximumRequestsPerQueue < 1
                    || maximumRequestsPerQueue > 100000 || maximumActors < 1
                    || maximumActors > 1000000) { throw new IllegalArgumentException("invalid matchmaking limits"); }
            this.maximumQueues = maximumQueues;
            this.maximumRequestsPerQueue = maximumRequestsPerQueue;
            this.maximumActors = maximumActors;
            this.defaultExpiry = positive(defaultExpiry, "defaultExpiry");
            this.agingInterval = positive(agingInterval, "agingInterval");
        }
        /** @return queue count ceiling */ public int maximumQueues() { return maximumQueues; }
        /** @return requests per queue ceiling */ public int maximumRequestsPerQueue() { return maximumRequestsPerQueue; }
        /** @return actor index ceiling */ public int maximumActors() { return maximumActors; }
        /** @return default expiry */ public Duration defaultExpiry() { return defaultExpiry; }
        /** @return starvation-aging interval */ public Duration agingInterval() { return agingInterval; }
    }

    /** Queue mutation outcomes. */
    public enum EnqueueVerdict { /** Added. */ ACCEPTED, /** Exact retry already present. */ IDEMPOTENT, /** Actor is queued elsewhere. */ DUPLICATE_ACTOR, /** Queue or actor bound reached. */ CAPACITY, /** Deadline already expired. */ EXPIRED, /** Request revision is older. */ STALE }

    /** Immutable enqueue result. */
    public static final class EnqueueResult {
        private final EnqueueVerdict verdict;
        private final int position;
        private EnqueueResult(final EnqueueVerdict verdict, final int position) { this.verdict = verdict;
        this.position = position;
        }
        /** @return verdict */ public EnqueueVerdict verdict() { return verdict; }
        /** @return one-based position, or zero when rejected */ public int position() { return position; }
    }

    /** Immutable queue status without false-precision wait estimates. */
    public static final class QueueStatus {
        private final QueueId queueId;
        private final int position;
        private final int queuedRequests;
        private final int queuedActors;
        private final Duration age;
        private QueueStatus(final QueueId queueId, final int position, final int queuedRequests,
                            final int queuedActors, final Duration age) {
            this.queueId = queueId;
            this.position = position;
            this.queuedRequests = queuedRequests;
            this.queuedActors = queuedActors;
            this.age = age;
        }
        /** @return queue */ public QueueId queueId() { return queueId; }
        /** @return one-based deterministic position */ public int position() { return position; }
        /** @return request count */ public int queuedRequests() { return queuedRequests; }
        /** @return actor count */ public int queuedActors() { return queuedActors; }
        /** @return elapsed wait */ public Duration age() { return age; }
    }

    /** Immutable operational diagnostics with bounded duration samples. */
    public static final class Diagnostics {
        private final int queues;
        private final int queuedActors;
        private final long expired;
        private final long cancelled;
        private final long staleRejected;
        private final long cleanupCount;
        private Diagnostics(final int queues, final int queuedActors, final long expired,
                            final long cancelled, final long staleRejected, final long cleanupCount) {
            this.queues = queues;
            this.queuedActors = queuedActors;
            this.expired = expired;
            this.cancelled = cancelled;
            this.staleRejected = staleRejected;
            this.cleanupCount = cleanupCount;
        }
        /** @return nonempty queue count */ public int queues() { return queues; }
        /** @return indexed actors */ public int queuedActors() { return queuedActors; }
        /** @return expired requests */ public long expired() { return expired; }
        /** @return cancelled requests */ public long cancelled() { return cancelled; }
        /** @return stale requests rejected */ public long staleRejected() { return staleRejected; }
        /** @return cleanup removals */ public long cleanupCount() { return cleanupCount; }
    }

    /** Thread-safe bounded queue store with atomic party indexing and deterministic cleanup. */
    public static final class QueueService {
        private final Limits limits;
        private final TimeSource time;
        private final Map<QueueId, LinkedHashMap<IdempotencyKey, Request>> queues = new LinkedHashMap<QueueId, LinkedHashMap<IdempotencyKey, Request>>();
        private final Map<PlayerId, IdempotencyKey> actors = new LinkedHashMap<PlayerId, IdempotencyKey>();
        private final Map<IdempotencyKey, Request> requests = new LinkedHashMap<IdempotencyKey, Request>();
        private boolean draining;
        private long expired;
        private long cancelled;
        private long staleRejected;
        private long cleanupCount;
        /** Creates a bounded queue service. */
        public QueueService(final Limits limits, final TimeSource time) {
            this.limits = Objects.requireNonNull(limits, "limits");
            this.time = Objects.requireNonNull(time, "time");
        }
        /** Atomically enqueues a solo actor or complete party. */
        public synchronized EnqueueResult enqueue(final Request request) {
            final Request checked = Objects.requireNonNull(request, "request");
            cleanupInternal(time.now());
            final Request previous = requests.get(checked.idempotencyKey());
            if (previous != null) {
                if (sameRevision(previous, checked)) { return result(EnqueueVerdict.IDEMPOTENT, position(previous)); }
                staleRejected++;
                return result(EnqueueVerdict.STALE, 0);
            }
            if (draining || !checked.deadline().isAfter(time.now())) {
                expired++;
                return result(EnqueueVerdict.EXPIRED, 0);
            }
            for (PlayerId actor : checked.actors()) {
                if (actors.containsKey(actor)) { return result(EnqueueVerdict.DUPLICATE_ACTOR, 0); }
            }
            LinkedHashMap<IdempotencyKey, Request> queue = queues.get(checked.queueId());
            if (queue == null) {
                if (queues.size() >= limits.maximumQueues()) { return result(EnqueueVerdict.CAPACITY, 0); }
                queue = new LinkedHashMap<IdempotencyKey, Request>();
                queues.put(checked.queueId(), queue);
            }
            if (queue.size() >= limits.maximumRequestsPerQueue()
                    || actors.size() + checked.actors().size() > limits.maximumActors()) {
                return result(EnqueueVerdict.CAPACITY, 0);
            }
            queue.put(checked.idempotencyKey(), checked);
            requests.put(checked.idempotencyKey(), checked);
            for (PlayerId actor : checked.actors()) { actors.put(actor, checked.idempotencyKey()); }
            return result(EnqueueVerdict.ACCEPTED, queue.size());
        }
        /** Cancels only when actor, token and revision match. */
        public synchronized boolean cancel(final PlayerId actor, final UUID token, final long revision) {
            final IdempotencyKey key = actors.get(Objects.requireNonNull(actor, "actor"));
            final Request request = key == null ? null : requests.get(key);
            if (request == null || !request.actor().equals(actor)
                    || !request.cancellationToken().equals(Objects.requireNonNull(token, "token"))
                    || request.revision() != revision) { staleRejected++;
                    return false;
                    }
            remove(request);
            cancelled++;
            return true;
        }
        /** @return current queue status for any member */
        public synchronized Optional<QueueStatus> status(final PlayerId actor) {
            cleanupInternal(time.now());
            final IdempotencyKey key = actors.get(Objects.requireNonNull(actor, "actor"));
            final Request request = key == null ? null : requests.get(key);
            if (request == null) { return Optional.empty(); }
            final LinkedHashMap<IdempotencyKey, Request> queue = queues.get(request.queueId());
            int queuedActors = 0;
            for (Request value : queue.values()) { queuedActors += value.actors().size(); }
            return Optional.of(new QueueStatus(request.queueId(), position(request), queue.size(),
                    queuedActors, Duration.between(request.enqueuedAt(), time.now())));
        }
        /** @return immutable queue snapshot for matching */
        public synchronized List<Request> snapshot(final QueueId queueId) {
            cleanupInternal(time.now());
            final Map<IdempotencyKey, Request> values = queues.get(Objects.requireNonNull(queueId, "queueId"));
            return values == null ? Collections.<Request>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Request>(values.values()));
        }
        /** Removes assigned requests atomically; unknown keys fail without partial mutation. */
        public synchronized boolean complete(final Collection<IdempotencyKey> keys) {
            final List<Request> resolved = new ArrayList<Request>();
            for (IdempotencyKey key : Objects.requireNonNull(keys, "keys")) {
                final Request request = requests.get(Objects.requireNonNull(key, "key"));
                if (request == null) { staleRejected++;
                return false;
                }
                resolved.add(request);
            }
            for (Request request : resolved) { remove(request); }
            return true;
        }
        /** Stops admission while retaining observable queued state. */ public synchronized void beginDrain() { draining = true; }
        /** Removes expired requests. @return number removed */ public synchronized int cleanup() { return cleanupInternal(time.now()); }
        /** @return immutable diagnostic counters */
        public synchronized Diagnostics diagnostics() {
            return new Diagnostics(queues.size(), actors.size(), expired, cancelled, staleRejected, cleanupCount);
        }
        private int cleanupInternal(final Instant now) {
            final List<Request> removals = new ArrayList<Request>();
            for (Request request : requests.values()) { if (!request.deadline().isAfter(now)) { removals.add(request); } }
            for (Request request : removals) { remove(request);
            expired++;
            }
            cleanupCount += removals.size();
            return removals.size();
        }
        private void remove(final Request request) {
            requests.remove(request.idempotencyKey());
            final Map<IdempotencyKey, Request> queue = queues.get(request.queueId());
            if (queue != null) {
                queue.remove(request.idempotencyKey());
                if (queue.isEmpty()) { queues.remove(request.queueId()); }
            }
            for (PlayerId actor : request.actors()) { actors.remove(actor); }
        }
        private int position(final Request request) {
            final Map<IdempotencyKey, Request> queue = queues.get(request.queueId());
            int position = 1;
            for (IdempotencyKey key : queue.keySet()) {
                if (key.equals(request.idempotencyKey())) { return position; }
                position++;
            }
            return 0;
        }
    }

    /** Arena facts used for deterministic matching and reservation admission. */
    public static final class ArenaAvailability {
        private final ArenaId arenaId;
        private final long revision;
        private final ModeId mode;
        private final DefinitionId layout;
        private final int teamCapacity;
        private final int totalCapacity;
        private final int occupied;
        private final boolean enabled;
        private final boolean healthy;
        private final boolean worldReady;
        private final boolean joinable;
        private final boolean recovering;
        /** Creates immutable arena admission facts. */
        public ArenaAvailability(final ArenaId arenaId, final long revision, final ModeId mode,
                                 final DefinitionId layout, final int teamCapacity,
                                 final int totalCapacity, final int occupied, final boolean enabled,
                                 final boolean healthy, final boolean worldReady,
                                 final boolean joinable, final boolean recovering) {
            this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
            if (revision < 0L || teamCapacity < 1 || teamCapacity > 64 || totalCapacity < 2
                    || totalCapacity > 256 || occupied < 0 || occupied > totalCapacity) {
                throw new IllegalArgumentException("invalid arena availability");
            }
            this.revision = revision;
            this.mode = Objects.requireNonNull(mode, "mode");
            this.layout = Objects.requireNonNull(layout, "layout");
            this.teamCapacity = teamCapacity;
            this.totalCapacity = totalCapacity;
            this.occupied = occupied;
            this.enabled = enabled;
            this.healthy = healthy;
            this.worldReady = worldReady;
            this.joinable = joinable;
            this.recovering = recovering;
        }
        /** @return arena */ public ArenaId arenaId() { return arenaId; }
        /** @return definition revision */ public long revision() { return revision; }
        /** @return mode */ public ModeId mode() { return mode; }
        /** @return layout */ public DefinitionId layout() { return layout; }
        /** @return team fit capacity */ public int teamCapacity() { return teamCapacity; }
        /** @return total capacity */ public int totalCapacity() { return totalCapacity; }
        /** @return already occupied */ public int occupied() { return occupied; }
        /** @return whether base health/readiness permits admission */ public boolean available() { return enabled && healthy && worldReady && joinable && !recovering; }
        /** @return whether request constraints and fit are satisfied */
        public boolean accepts(final Request request, final int additionalReserved) {
            return available() && mode.equals(request.mode()) && layout.equals(request.layout())
                    && request.actors().size() <= teamCapacity
                    && occupied + additionalReserved + request.actors().size() <= totalCapacity
                    && (request.arenaConstraints().isEmpty() || request.arenaConstraints().contains(arenaId));
        }
    }

    /** Explainable decision for one request. */
    public static final class Decision {
        private final Request request;
        private final ArenaId arenaId;
        private final int effectivePriority;
        private final String reason;
        private Decision(final Request request, final ArenaId arenaId, final int effectivePriority,
                         final String reason) {
            this.request = request;
            this.arenaId = arenaId;
            this.effectivePriority = effectivePriority;
            this.reason = reason;
        }
        /** @return request */ public Request request() { return request; }
        /** @return selected arena */ public ArenaId arenaId() { return arenaId; }
        /** @return base plus bounded aging priority */ public int effectivePriority() { return effectivePriority; }
        /** @return stable structured reason code */ public String reason() { return reason; }
    }

    /** Pluggable deterministic matching policy. */
    public interface MatchingPolicy {
        /** @return immutable decisions for the supplied state */
        List<Decision> match(Collection<Request> requests, Collection<ArenaAvailability> arenas,
                             Instant now, Duration agingInterval);
    }

    /** Safe FIFO-with-aging policy independent of hash iteration and wall-clock races. */
    public static final class FairCapacityPolicy implements MatchingPolicy {
        @Override public List<Decision> match(final Collection<Request> requests,
                                              final Collection<ArenaAvailability> arenas,
                                              final Instant now, final Duration agingInterval) {
            Objects.requireNonNull(now, "now");
            positive(agingInterval, "agingInterval");
            final List<Request> ordered = new ArrayList<Request>(Objects.requireNonNull(requests, "requests"));
            ordered.sort(requestComparator(now, agingInterval));
            final List<ArenaAvailability> candidates = new ArrayList<ArenaAvailability>(Objects.requireNonNull(arenas, "arenas"));
            candidates.sort(Comparator.comparing(value -> value.arenaId().toString()));
            final Map<ArenaId, Integer> reserved = new LinkedHashMap<ArenaId, Integer>();
            final List<Decision> decisions = new ArrayList<Decision>();
            for (Request request : ordered) {
                for (ArenaAvailability arena : candidates) {
                    final int current = reserved.containsKey(arena.arenaId()) ? reserved.get(arena.arenaId()) : 0;
                    if (arena.accepts(request, current)) {
                        reserved.put(arena.arenaId(), current + request.actors().size());
                        decisions.add(new Decision(request, arena.arenaId(), effectivePriority(request, now, agingInterval), "capacity-fit"));
                        break;
                    }
                }
            }
            return Collections.unmodifiableList(decisions);
        }
    }

    /** Opaque reservation identity. */
    public static final class ReservationId implements Comparable<ReservationId> {
        private final UUID value;
        private ReservationId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return supplied UUID */ public static ReservationId of(final UUID value) { return new ReservationId(value); }
        /** @return new identity */ public static ReservationId random() { return of(UUID.randomUUID()); }
        /** @return UUID */ public UUID value() { return value; }
        @Override public int compareTo(final ReservationId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ReservationId && value.equals(((ReservationId) other).value); }
        @Override public String toString() { return value.toString(); }
    }

    /** Reservation lifecycle. */ public enum ReservationState { /** Capacity held. */ ACQUIRED, /** Match creation accepted. */ CONFIRMED, /** Capacity released. */ RELEASED }

    /** Immutable revision-bound arena reservation. */
    public static final class Reservation {
        private final ReservationId id;
        private final ArenaId arenaId;
        private final long arenaRevision;
        private final IdempotencyKey owner;
        private final List<PlayerId> actors;
        private final Instant expiresAt;
        private final ReservationState state;
        private Reservation(final ReservationId id, final ArenaId arenaId, final long arenaRevision,
                            final IdempotencyKey owner, final Collection<PlayerId> actors,
                            final Instant expiresAt, final ReservationState state) {
            this.id = id;
            this.arenaId = arenaId;
            this.arenaRevision = arenaRevision;
            this.owner = owner;
            this.actors = Collections.unmodifiableList(new ArrayList<PlayerId>(actors));
            this.expiresAt = expiresAt;
            this.state = state;
        }
        /** @return reservation */ public ReservationId id() { return id; }
        /** @return arena */ public ArenaId arenaId() { return arenaId; }
        /** @return bound arena revision */ public long arenaRevision() { return arenaRevision; }
        /** @return single idempotent owner */ public IdempotencyKey owner() { return owner; }
        /** @return atomic actor group */ public List<PlayerId> actors() { return actors; }
        /** @return expiry */ public Instant expiresAt() { return expiresAt; }
        /** @return state */ public ReservationState state() { return state; }
        private Reservation withState(final ReservationState value) { return new Reservation(id, arenaId, arenaRevision, owner, actors, expiresAt, value); }
    }

    /** Thread-safe no-overbooking reservation coordinator. */
    public static final class ReservationService {
        private final int maximumReservations;
        private final Duration ttl;
        private final TimeSource time;
        private final Map<ReservationId, Reservation> reservations = new LinkedHashMap<ReservationId, Reservation>();
        private final Map<IdempotencyKey, ReservationId> owners = new LinkedHashMap<IdempotencyKey, ReservationId>();
        private long expired;
        private long rejected;
        /** Creates a bounded coordinator. */
        public ReservationService(final int maximumReservations, final Duration ttl, final TimeSource time) {
            if (maximumReservations < 1 || maximumReservations > 100000) { throw new IllegalArgumentException("invalid reservation capacity"); }
            this.maximumReservations = maximumReservations;
            this.ttl = positive(ttl, "ttl");
            this.time = Objects.requireNonNull(time, "time");
        }
        /** Atomically acquires capacity for the complete request. */
        public synchronized Optional<Reservation> acquire(final Request request,
                                                          final ArenaAvailability arena) {
            cleanup();
            final ReservationId existingId = owners.get(Objects.requireNonNull(request, "request").idempotencyKey());
            if (existingId != null) { return Optional.of(reservations.get(existingId)); }
            int held = 0;
            for (Reservation value : reservations.values()) {
                if (value.arenaId().equals(arena.arenaId()) && value.state() == ReservationState.ACQUIRED) {
                    held += value.actors().size();
                }
            }
            if (reservations.size() >= maximumReservations || !arena.accepts(request, held)) {
                rejected++;
                return Optional.empty();
            }
            final Reservation value = new Reservation(ReservationId.random(), arena.arenaId(),
                    arena.revision(), request.idempotencyKey(), request.actors(), time.now().plus(ttl),
                    ReservationState.ACQUIRED);
            reservations.put(value.id(), value);
            owners.put(value.owner(), value.id());
            return Optional.of(value);
        }
        /** Confirms once and rejects stale arena revisions or duplicate confirmation. */
        public synchronized Reservation confirm(final ReservationId id, final long arenaRevision) {
            final Reservation current = require(id);
            if (current.state() != ReservationState.ACQUIRED || current.arenaRevision() != arenaRevision
                    || !current.expiresAt().isAfter(time.now())) {
                rejected++;
                throw new IllegalStateException("reservation is stale or already terminal");
            }
            final Reservation updated = current.withState(ReservationState.CONFIRMED);
            reservations.put(id, updated);
            return updated;
        }
        /** Releases an acquired/confirmed reservation exactly once. */
        public synchronized boolean release(final ReservationId id) {
            final Reservation current = reservations.get(Objects.requireNonNull(id, "id"));
            if (current == null || current.state() == ReservationState.RELEASED) { rejected++;
            return false;
            }
            reservations.put(id, current.withState(ReservationState.RELEASED));
            owners.remove(current.owner());
            return true;
        }
        /** Removes expired or released records. @return removed count */
        public synchronized int cleanup() {
            final List<ReservationId> removals = new ArrayList<ReservationId>();
            for (Reservation value : reservations.values()) {
                if (value.state() == ReservationState.RELEASED
                        || !value.expiresAt().isAfter(time.now())) { removals.add(value.id()); }
            }
            for (ReservationId id : removals) {
                final Reservation removed = reservations.remove(id);
                owners.remove(removed.owner());
                if (removed.state() == ReservationState.ACQUIRED) { expired++; }
            }
            return removals.size();
        }
        /** @return active reservation count */ public synchronized int active() { return reservations.size(); }
        /** @return expired-acquired count */ public synchronized long expired() { return expired; }
        /** @return rejected mutation count */ public synchronized long rejected() { return rejected; }
        private Reservation require(final ReservationId id) {
            final Reservation value = reservations.get(Objects.requireNonNull(id, "id"));
            if (value == null) { throw new IllegalArgumentException("unknown reservation"); }
            return value;
        }
    }

    /** Existing M08 assignment boundary; implementations invoke the typed game use case. */
    public interface AssignmentPort {
        /** @return eventual assignment outcome without performing work on an owner thread */
        CompletionStage<AssignmentOutcome> assign(Reservation reservation, Request request);
    }

    /** Typed M08 assignment outcome used to confirm or roll back a reservation. */
    public enum AssignmentOutcome { /** Match use case accepted. */ ACCEPTED, /** Current state rejected. */ REJECTED, /** Timed out. */ TIMEOUT, /** Failed and must release. */ FAILED }

    private static boolean sameRevision(final Request first, final Request second) {
        return first.revision() == second.revision() && first.actor().equals(second.actor())
                && first.party().map(Party::revision).equals(second.party().map(Party::revision));
    }
    private static EnqueueResult result(final EnqueueVerdict verdict, final int position) { return new EnqueueResult(verdict, position); }
    private static int effectivePriority(final Request request, final Instant now, final Duration interval) {
        final long age = Math.max(0L, Duration.between(request.enqueuedAt(), now).toMillis());
        final long boost = Math.min(1000L, age / interval.toMillis());
        return request.priority() + (int) boost;
    }
    private static Comparator<Request> requestComparator(final Instant now, final Duration interval) {
        return Comparator.comparingInt((Request value) -> effectivePriority(value, now, interval)).reversed()
                .thenComparing(Request::enqueuedAt).thenComparing(value -> value.idempotencyKey().toString());
    }
    private static Duration positive(final Duration value, final String label) {
        if (value == null || value.isZero() || value.isNegative()) { throw new IllegalArgumentException(label + " must be positive"); }
        return value;
    }
    private static String boundedToken(final String value, final String label, final int maximum) {
        if (value == null || value.isEmpty() || value.length() > maximum || !value.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("invalid " + label);
        }
        return value;
    }
    private static <T> Set<T> immutableSet(final Collection<T> values, final String label, final int maximum) {
        final Set<T> result = new LinkedHashSet<T>();
        for (T value : Objects.requireNonNull(values, label + "s")) {
            if (!result.add(Objects.requireNonNull(value, label))) { throw new IllegalArgumentException("duplicate " + label); }
        }
        if (result.size() > maximum) { throw new IllegalArgumentException("too many " + label + "s"); }
        return Collections.unmodifiableSet(result);
    }
    private static <T> List<T> immutableList(final Collection<T> values, final String label, final int maximum) {
        final List<T> result = new ArrayList<T>();
        for (T value : Objects.requireNonNull(values, label + "s")) { result.add(Objects.requireNonNull(value, label)); }
        if (result.size() > maximum) { throw new IllegalArgumentException("too many " + label + "s"); }
        return Collections.unmodifiableList(result);
    }
}
