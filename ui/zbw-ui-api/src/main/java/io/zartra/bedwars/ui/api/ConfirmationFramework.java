package io.zartra.bedwars.ui.api;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.PresentationActions.ActionId;
import io.zartra.bedwars.command.api.PresentationActions.Definition;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Single-use confirmation service for destructive command and GUI actions. Tokens are bound to
 * actor, action, target, revision and expiry; authorization is re-evaluated on consumption.
 */
public final class ConfirmationFramework {
    private final AuthorizationService authorization;
    private final TimeSource time;
    private final NonceSource nonces;
    private final AuditSink audit;
    private final Duration ttl;
    private final int capacity;
    private final Map<ActionId, Definition> definitions = new LinkedHashMap<ActionId, Definition>();
    private final Map<ConfirmationId, Intent> intents = new LinkedHashMap<ConfirmationId, Intent>();

    /** Creates a bounded confirmation service for a known action catalogue. */
    public ConfirmationFramework(final AuthorizationService authorization, final TimeSource time,
                                 final NonceSource nonces, final AuditSink audit,
                                 final Duration ttl, final int capacity,
                                 final Collection<Definition> definitions) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.time = Objects.requireNonNull(time, "time");
        this.nonces = Objects.requireNonNull(nonces, "nonces");
        this.audit = Objects.requireNonNull(audit, "audit");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) { throw new IllegalArgumentException("ttl must be positive"); }
        if (capacity < 1 || capacity > 100000) { throw new IllegalArgumentException("invalid confirmation capacity"); }
        this.ttl = ttl;
        this.capacity = capacity;
        for (Definition definition : Objects.requireNonNull(definitions, "definitions")) {
            if (this.definitions.put(definition.id(), definition) != null) { throw new IllegalArgumentException("duplicate action definition"); }
        }
    }

    /** Issues a token only for a destructive catalogue action. */
    public synchronized Intent issue(final AuthorizationSubject actor, final ActionId action,
                                     final DefinitionId target, final long revision,
                                     final CorrelationId correlationId) {
        cleanupInternal(time.now());
        if (intents.size() >= capacity) { throw new IllegalStateException("confirmation capacity reached"); }
        if (revision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
        final Definition definition = definitions.get(Objects.requireNonNull(action, "action"));
        if (definition == null || !definition.destructive()) { throw new IllegalArgumentException("action does not require confirmation"); }
        final ConfirmationId id = ConfirmationId.of(nonces.next());
        if (intents.containsKey(id)) { throw new IllegalStateException("nonce collision"); }
        final Intent intent = new Intent(id, actor, action, target, revision,
                time.now().plus(ttl), correlationId);
        intents.put(id, intent);
         audit.record(AuditRecord.issued(intent, time.now()));
        return intent;
    }

    /**
     * Consumes a token exactly once. Any presented token is removed before validation, preventing
     * replay and confused-deputy retries after a failed match.
     */
    public synchronized Decision consume(final ConfirmationId id, final AuthorizationSubject actor,
                                         final ActionId action, final DefinitionId target,
                                         final long currentRevision) {
        final Intent intent = intents.remove(Objects.requireNonNull(id, "id"));
        if (intent == null) { return Decision.rejected(Verdict.UNKNOWN_OR_REPLAYED); }
        final Instant now = time.now();
        Verdict verdict = Verdict.CONFIRMED;
        if (!intent.expiresAt().isAfter(now)) { verdict = Verdict.EXPIRED; }
        else if (!intent.actor().equals(actor) || !intent.action().equals(action)
                || !intent.target().equals(target)) { verdict = Verdict.BINDING_MISMATCH; }
        else if (intent.revision() != currentRevision) { verdict = Verdict.STALE_REVISION; }
        else {
            final Definition definition = definitions.get(action);
            if (definition == null || !authorization.authorize(AuthorizationRequest.of(actor,
                    definition.permission(), target)).isAllowed()) { verdict = Verdict.FORBIDDEN; }
        }
        audit.record(AuditRecord.consumed(intent, verdict, now));
        return verdict == Verdict.CONFIRMED ? Decision.confirmed(intent) : Decision.rejected(verdict);
    }

    /** @return number of expired tokens removed */
    public synchronized int cleanup() { return cleanupInternal(time.now()); }
    private int cleanupInternal(final Instant now) {
        final int before = intents.size(); intents.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        return before - intents.size();
    }

    /** Typed opaque confirmation token. */
    public static final class ConfirmationId implements Comparable<ConfirmationId> {
        private final UUID value;
        private ConfirmationId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return typed token */ public static ConfirmationId of(final UUID value) { return new ConfirmationId(value); }
        /** @return parsed token */ public static ConfirmationId parse(final String value) { return of(UUID.fromString(value)); }
        /** @return UUID */ public UUID value() { return value; }
        @Override public int compareTo(final ConfirmationId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public String toString() { return value.toString(); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ConfirmationId && value.equals(((ConfirmationId) other).value); }
    }

    /** Immutable actor/action/target/revision-bound intent. */
    public static final class Intent {
        private final ConfirmationId id;
         private final AuthorizationSubject actor;
        private final ActionId action;
        private final DefinitionId target;
         private final long revision;
        private final Instant expiresAt;
        private final CorrelationId correlationId;
        private Intent(final ConfirmationId id, final AuthorizationSubject actor, final ActionId action,
                       final DefinitionId target, final long revision, final Instant expiresAt,
                       final CorrelationId correlationId) {
            this.id = id;
             this.actor = Objects.requireNonNull(actor, "actor");
            this.action = action;
            this.target = Objects.requireNonNull(target, "target");
            this.revision = revision;
            this.expiresAt = expiresAt;
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        }
        /** @return token */ public ConfirmationId id() { return id; }
        /** @return actor */ public AuthorizationSubject actor() { return actor; }
        /** @return action */ public ActionId action() { return action; }
        /** @return target */ public DefinitionId target() { return target; }
        /** @return expected revision */ public long revision() { return revision; }
        /** @return expiry */ public Instant expiresAt() { return expiresAt; }
        /** @return audit correlation */ public CorrelationId correlationId() { return correlationId; }
    }

    /** Confirmation decision. */
    public static final class Decision {
        private final Verdict verdict;
        private final Intent intent;
        private Decision(final Verdict verdict, final Intent intent) { this.verdict = verdict;
         this.intent = intent;
        }
        private static Decision confirmed(final Intent intent) { return new Decision(Verdict.CONFIRMED, intent); }
        private static Decision rejected(final Verdict verdict) { return new Decision(verdict, null); }
        /** @return verdict */ public Verdict verdict() { return verdict; }
        /** @return confirmed intent only on success */ public Optional<Intent> intent() { return Optional.ofNullable(intent); }
    }

    /** Fail-closed confirmation outcomes. */ public enum Verdict { /** Valid. */ CONFIRMED, /** Missing or consumed. */ UNKNOWN_OR_REPLAYED, /** Expired. */ EXPIRED, /** Actor/action/target mismatch. */ BINDING_MISMATCH, /** Target changed. */ STALE_REVISION, /** Permission revoked. */ FORBIDDEN }

    /** Cryptographically suitable nonce source supplied by the runtime. */
    public interface NonceSource { /** @return a fresh unpredictable UUID */ UUID next(); }
    /** Secret-free audit sink. */ public interface AuditSink { /** Records one event. */ void record(AuditRecord record); }

    /** Immutable confirmation audit event; it excludes the token value. */
    public static final class AuditRecord {
        private final CorrelationId correlationId;
         private final ActionId action;
        private final DefinitionId target;
        private final Phase phase;
         private final Verdict verdict;
        private final Instant occurredAt;
        private AuditRecord(final Intent intent, final Phase phase, final Verdict verdict, final Instant at) {
            correlationId = intent.correlationId();
             action = intent.action();
            target = intent.target();
            this.phase = phase;
             this.verdict = verdict;
            occurredAt = at;
        }
        private static AuditRecord issued(final Intent intent, final Instant at) { return new AuditRecord(intent, Phase.ISSUED, null, at); }
        private static AuditRecord consumed(final Intent intent, final Verdict verdict, final Instant at) { return new AuditRecord(intent, Phase.CONSUMED, verdict, at); }
        /** @return correlation */ public CorrelationId correlationId() { return correlationId; }
        /** @return action */ public ActionId action() { return action; }
        /** @return target */ public DefinitionId target() { return target; }
        /** @return lifecycle phase */ public Phase phase() { return phase; }
        /** @return verdict when consumed */ public Optional<Verdict> verdict() { return Optional.ofNullable(verdict); }
        /** @return timestamp */ public Instant occurredAt() { return occurredAt; }
        /** Lifecycle phases. */ public enum Phase { /** Token issued. */ ISSUED, /** Token consumed. */ CONSUMED }
    }
}
