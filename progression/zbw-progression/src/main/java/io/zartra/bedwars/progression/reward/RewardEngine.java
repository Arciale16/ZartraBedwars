package io.zartra.bedwars.progression.reward;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.RewardId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Exactly-once, offline-capable reward application service over transactional ports. */
public final class RewardEngine {
    private final Store store;
    private final Delivery delivery;
    private final RetryPolicy retryPolicy;

    /** Creates a reward engine with no ambient state. */
    public RewardEngine(final Store store, final Delivery delivery, final RetryPolicy retryPolicy) {
        this.store = Objects.requireNonNull(store, "store");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
    }

    /** Registers a plan exactly once and attempts delivery when the recipient is available. */
    public Outcome grant(final Plan plan, final Instant now, final boolean online) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(now, "now");
        final Claim claim = store.claim(plan);
        if (!claim.accepted()) { return claim.prior().get(); }
        if (plan.expiresAt().isPresent() && !now.isBefore(plan.expiresAt().get())) {
            return store.record(plan, Outcome.expired(plan.idempotencyKey(), now));
        }
        if (!online) { return store.record(plan, Outcome.pending(plan.idempotencyKey(), now)); }
        return attempt(plan, 1, now);
    }

    /** Retries one pending or failed plan according to the bounded policy. */
    public Outcome retry(final Plan plan, final int priorAttempts, final Instant now,
                         final boolean online) {
        final Optional<Outcome> prior = store.outcome(plan.idempotencyKey());
        if (prior.isPresent() && prior.get().status() == Status.DELIVERED) { return prior.get(); }
        if (!online) { return store.record(plan, Outcome.pending(plan.idempotencyKey(), now)); }
        if (!retryPolicy.mayAttempt(priorAttempts, now, plan.expiresAt())) {
            return compensate(plan, priorAttempts, now);
        }
        return attempt(plan, priorAttempts + 1, now);
    }

    private Outcome attempt(final Plan plan, final int attempt, final Instant now) {
        try {
            delivery.deliver(plan);
            return store.record(plan, Outcome.delivered(plan.idempotencyKey(), attempt, now));
        } catch (RuntimeException failure) {
            final Outcome outcome = Outcome.failed(plan.idempotencyKey(), attempt, now,
                    failure.getClass().getSimpleName());
            store.recordFailure(plan, outcome);
            return store.record(plan, outcome);
        }
    }

    private Outcome compensate(final Plan plan, final int attempts, final Instant now) {
        delivery.compensate(plan);
        return store.record(plan, Outcome.compensated(plan.idempotencyKey(), attempts, now));
    }

    /** Transactional durable store boundary. */
    public interface Store {
        /** Atomically claims a new plan or returns its prior outcome. */ Claim claim(Plan plan);
        /** @return prior terminal or pending outcome */ Optional<Outcome> outcome(IdempotencyKey key);
        /** Atomically records the latest outcome and audit trail. */ Outcome record(Plan plan, Outcome outcome);
        /** Appends one sanitized failure queue record. */ void recordFailure(Plan plan, Outcome outcome);
    }

    /** Side-effect delivery boundary; implementations must themselves honor plan idempotency. */
    public interface Delivery {
        /** Delivers all outputs atomically or throws before acknowledgement. */ void deliver(Plan plan);
        /** Applies the configured terminal compensation. */ void compensate(Plan plan);
    }

    /** Versioned reward definition. */
    public static final class Definition {
        private final RewardId id;
        private final int version;
        private final List<Output> outputs;
        private final Optional<Duration> lifetime;
        /** Creates an immutable reward definition. */
        public Definition(final RewardId id, final int version, final List<Output> outputs,
                          final Optional<Duration> lifetime) {
            this.id = Objects.requireNonNull(id, "id");
            if (version < 1) { throw new IllegalArgumentException("version must be positive"); }
            this.version = version;
            final List<Output> copy = new ArrayList<Output>(Objects.requireNonNull(outputs, "outputs"));
            if (copy.isEmpty() || copy.contains(null)) { throw new IllegalArgumentException("outputs must not be empty or contain null"); }
            this.outputs = Collections.unmodifiableList(copy);
            this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
            if (lifetime.isPresent() && (lifetime.get().isNegative() || lifetime.get().isZero())) {
                throw new IllegalArgumentException("lifetime must be positive");
            }
        }
        /** @return reward ID */ public RewardId id() { return id; }
        /** @return schema version */ public int version() { return version; }
        /** @return immutable generic outputs */ public List<Output> outputs() { return outputs; }
        /** @return optional lifetime */ public Optional<Duration> lifetime() { return lifetime; }
    }

    /** Generic reward output; later milestones interpret cosmetic/objective-specific types. */
    public static final class Output {
        /** Generic output kinds whose feature-specific consumers retain later ownership. */
        public enum Kind {
            /** Experience. */ EXPERIENCE, /** Persistent currency. */ CURRENCY,
            /** Generic entitlement. */ ENTITLEMENT, /** Inventory item intent. */ ITEM,
            /** Permission intent. */ PERMISSION, /** Allowlisted command intent. */ COMMAND,
            /** Title intent. */ TITLE, /** Badge intent. */ BADGE,
            /** Pass points intent. */ PASS_POINTS, /** Token intent. */ TOKEN,
            /** Loot intent. */ LOOT, /** Booster intent. */ BOOSTER,
            /** Extension-defined intent. */ CUSTOM
        }
        private final Kind kind;
        private final String reference;
        private final long amount;
        /** Creates an output. */ public Output(final Kind kind, final String reference, final long amount) {
            this.kind = Objects.requireNonNull(kind, "kind");
            if (reference == null || reference.trim().isEmpty() || reference.length() > 128 || amount < 1) {
                throw new IllegalArgumentException("invalid reward output");
            }
            this.reference = reference;
            this.amount = amount;
        }
        /** @return kind */ public Kind kind() { return kind; }
        /** @return typed-ID serialization */ public String reference() { return reference; }
        /** @return positive amount */ public long amount() { return amount; }
    }

    /** One immutable delivery plan. */
    public static final class Plan {
        private final Definition definition;
        private final PlayerProgressionId recipient;
        private final IdempotencyKey idempotencyKey;
        private final Instant createdAt;
        private final Optional<Instant> expiresAt;
        /** Creates a plan and derives expiration from the snapshotted definition. */
        public Plan(final Definition definition, final PlayerProgressionId recipient,
                    final IdempotencyKey idempotencyKey, final Instant createdAt) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.recipient = Objects.requireNonNull(recipient, "recipient");
            this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
            this.expiresAt = definition.lifetime().map(createdAt::plus);
        }
        /** @return definition snapshot */ public Definition definition() { return definition; }
        /** @return recipient */ public PlayerProgressionId recipient() { return recipient; }
        /** @return duplicate-suppression key */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
        /** @return creation time */ public Instant createdAt() { return createdAt; }
        /** @return optional expiration */ public Optional<Instant> expiresAt() { return expiresAt; }
    }

    /** Bounded retry policy. */
    public static final class RetryPolicy {
        private final int maximumAttempts;
        /** Creates a retry limit. */ public RetryPolicy(final int maximumAttempts) {
            if (maximumAttempts < 1 || maximumAttempts > 100) { throw new IllegalArgumentException("maximumAttempts must be 1..100"); }
            this.maximumAttempts = maximumAttempts;
        }
        /** @return whether another attempt is safe */
        public boolean mayAttempt(final int priorAttempts, final Instant now,
                                  final Optional<Instant> expiresAt) {
            return priorAttempts < maximumAttempts && (!expiresAt.isPresent() || now.isBefore(expiresAt.get()));
        }
        /** @return maximum attempts */ public int maximumAttempts() { return maximumAttempts; }
    }

    /** Delivery state. */ public enum Status { /** Waiting for player/provider. */ PENDING, /** Delivered. */ DELIVERED, /** Retryable failure. */ FAILED, /** Compensated. */ COMPENSATED, /** Expired. */ EXPIRED }

    /** Atomic duplicate-claim result. */
    public static final class Claim {
        private final boolean accepted;
        private final Optional<Outcome> prior;
        private Claim(final boolean accepted, final Optional<Outcome> prior) {
            this.accepted = accepted;
            this.prior = prior;
        }
        /** @return newly accepted claim */ public static Claim newClaim() { return new Claim(true, Optional.empty()); }
        /** @return duplicate with prior evidence */ public static Claim duplicate(final Outcome prior) { return new Claim(false, Optional.of(Objects.requireNonNull(prior, "prior"))); }
        /** @return whether this caller owns delivery */ public boolean accepted() { return accepted; }
        /** @return prior evidence */ public Optional<Outcome> prior() { return prior; }
    }

    /** Immutable audited outcome. */
    public static final class Outcome {
        private final IdempotencyKey key;
        private final Status status;
        private final int attempts;
        private final Instant occurredAt;
        private final String failureCode;
        private Outcome(final IdempotencyKey key, final Status status, final int attempts,
                        final Instant occurredAt, final String failureCode) {
            this.key = key;
            this.status = status;
            this.attempts = attempts;
            this.occurredAt = occurredAt;
            this.failureCode = failureCode;
        }
        private static Outcome pending(final IdempotencyKey key, final Instant now) { return new Outcome(key, Status.PENDING, 0, now, ""); }
        private static Outcome delivered(final IdempotencyKey key, final int attempts, final Instant now) { return new Outcome(key, Status.DELIVERED, attempts, now, ""); }
        private static Outcome failed(final IdempotencyKey key, final int attempts, final Instant now, final String code) { return new Outcome(key, Status.FAILED, attempts, now, code); }
        private static Outcome compensated(final IdempotencyKey key, final int attempts, final Instant now) { return new Outcome(key, Status.COMPENSATED, attempts, now, ""); }
        private static Outcome expired(final IdempotencyKey key, final Instant now) { return new Outcome(key, Status.EXPIRED, 0, now, "expired"); }
        /** @return idempotency key */ public IdempotencyKey key() { return key; }
        /** @return status */ public Status status() { return status; }
        /** @return delivery attempts */ public int attempts() { return attempts; }
        /** @return audit timestamp */ public Instant occurredAt() { return occurredAt; }
        /** @return sanitized failure code */ public String failureCode() { return failureCode; }
    }
}
