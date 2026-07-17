package io.zartra.bedwars.ui.api;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Generic bounded editor state machine supporting preview, validation, undo/redo, import/export,
 * duplicate, reset and migration. Feature policies own all domain-specific rules.
 */
public final class EditorFramework<D> {
    private final Policy<D> policy;
     private final TimeSource time;
    private final Duration ttl;
    private final int capacity;
    private final int historyLimit;
    private final Map<EditorSessionId, MutableSession<D>> sessions = new LinkedHashMap<EditorSessionId, MutableSession<D>>();

    /** Creates a bounded editor engine. */
    public EditorFramework(final Policy<D> policy, final TimeSource time, final Duration ttl,
                           final int capacity, final int historyLimit) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.time = Objects.requireNonNull(time, "time");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) { throw new IllegalArgumentException("ttl must be positive"); }
        if (capacity < 1 || capacity > 100000 || historyLimit < 1 || historyLimit > 256) { throw new IllegalArgumentException("invalid editor bounds"); }
        this.ttl = ttl;
         this.capacity = capacity;
        this.historyLimit = historyLimit;
    }

    /** Begins an editor session over an immutable policy-owned snapshot. */
    public synchronized Session<D> begin(final AuthorizationSubject actor, final DefinitionId target,
                                         final long revision, final D source) {
        cleanupInternal(time.now());
         if (sessions.size() >= capacity) { throw new IllegalStateException("editor capacity reached");
        }
        if (revision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
        final D checked = policy.copy(Objects.requireNonNull(source, "source"));
        final MutableSession<D> session = new MutableSession<D>(EditorSessionId.random(), actor, target,
                revision, checked, policy.copy(checked), time.now().plus(ttl));
        sessions.put(session.id, session);
        return session.snapshot();
    }

    /** Applies one pure edit and records bounded undo history. */
    public synchronized Outcome<D> edit(final EditorSessionId id, final Edit<D> edit) {
        final MutableSession<D> session = require(id);
        final D candidate = Objects.requireNonNull(edit, "edit").apply(policy.copy(session.draft));
        return replace(session, candidate, "editor.edit.applied");
    }
    /** Undoes one edit. */ public synchronized Outcome<D> undo(final EditorSessionId id) {
        final MutableSession<D> session = require(id);
         if (session.undo.isEmpty()) { return Outcome.rejected(Status.NO_HISTORY, session.snapshot(), "editor.undo.empty");
        }
        session.redo.addLast(policy.copy(session.draft));
         session.draft = session.undo.removeLast();
         session.localRevision++;
        touch(session);
        return Outcome.accepted(session.snapshot(), "editor.undo.applied");
    }
    /** Redoes one edit. */ public synchronized Outcome<D> redo(final EditorSessionId id) {
        final MutableSession<D> session = require(id);
         if (session.redo.isEmpty()) { return Outcome.rejected(Status.NO_HISTORY, session.snapshot(), "editor.redo.empty");
        }
        pushUndo(session);
         session.draft = session.redo.removeLast();
         session.localRevision++;
        touch(session);
        return Outcome.accepted(session.snapshot(), "editor.redo.applied");
    }
    /** @return policy validation without mutation */ public synchronized Validation validate(final EditorSessionId id) { final MutableSession<D> session = require(id);
     touch(session);
     return policy.validate(policy.copy(session.draft));
    }
    /** @return immutable policy preview without mutation */ public synchronized Preview preview(final EditorSessionId id) { final MutableSession<D> session = require(id);
     touch(session);
     return policy.preview(policy.copy(session.draft));
    }
    /** Imports policy-validated external data into undoable draft state. */ public synchronized Outcome<D> importData(final EditorSessionId id, final byte[] data) { return replace(require(id), policy.importData(copy(data)), "editor.import.applied"); }
    /** @return exported copy of the current draft */ public synchronized byte[] exportData(final EditorSessionId id) { final MutableSession<D> session = require(id);
     touch(session);
     return copy(policy.exportData(policy.copy(session.draft)));
    }
    /** Duplicates the draft using policy-defined identity replacement. */ public synchronized Outcome<D> duplicate(final EditorSessionId id, final DefinitionId newTarget) { return replace(require(id), policy.duplicate(require(id).draft, newTarget), "editor.duplicate.applied"); }
    /** Resets the draft to policy-owned defaults. */ public synchronized Outcome<D> reset(final EditorSessionId id) { return replace(require(id), policy.reset(require(id).target), "editor.reset.applied"); }
    /** Migrates the draft to a requested schema/version. */ public synchronized Outcome<D> migrate(final EditorSessionId id, final DefinitionId version) { return replace(require(id), policy.migrate(require(id).draft, version), "editor.migrate.applied"); }

    /** Validates and atomically delegates the final optimistic apply to the policy. */
    public synchronized Outcome<D> apply(final EditorSessionId id) {
        final MutableSession<D> session = require(id);
        final Validation validation = policy.validate(policy.copy(session.draft));
        if (!validation.valid()) { return Outcome.rejected(Status.INVALID, session.snapshot(), "editor.apply.invalid"); }
        final ApplyResult<D> result = policy.apply(session.actor, session.target, session.sourceRevision, policy.copy(session.draft));
        if (!result.applied()) { return Outcome.rejected(Status.CONFLICT, session.snapshot(), result.message().value()); }
        sessions.remove(id);
        return Outcome.accepted(new Session<D>(session.id, session.actor, session.target,
                result.revision(), session.localRevision, policy.copy(result.value()), session.expiresAt), result.message().value());
    }
    /** Cancels and removes one editor session. */ public synchronized boolean cancel(final EditorSessionId id) { return sessions.remove(Objects.requireNonNull(id, "id")) != null; }
    /** @return immutable live session */ public synchronized Optional<Session<D>> session(final EditorSessionId id) { final MutableSession<D> value = sessions.get(id);
     return value == null ? Optional.empty() : Optional.of(value.snapshot());
    }
    /** @return number of expired sessions removed */ public synchronized int cleanup() { return cleanupInternal(time.now()); }

    private Outcome<D> replace(final MutableSession<D> session, final D candidate, final String message) {
        final D checked = policy.copy(Objects.requireNonNull(candidate, "candidate"));
        pushUndo(session);
        session.redo.clear();
         session.draft = checked;
         session.localRevision++;
        touch(session);
        return Outcome.accepted(session.snapshot(), message);
    }
    private void pushUndo(final MutableSession<D> session) { session.undo.addLast(policy.copy(session.draft));
     while (session.undo.size() > historyLimit) { session.undo.removeFirst();
    } }
    private MutableSession<D> require(final EditorSessionId id) { final MutableSession<D> session = sessions.get(Objects.requireNonNull(id, "id"));
     if (session == null || !session.expiresAt.isAfter(time.now())) { sessions.remove(id);
     throw new IllegalArgumentException("unknown or expired editor session");
     } return session;
    }
    private void touch(final MutableSession<D> session) { session.expiresAt = time.now().plus(ttl); }
    private int cleanupInternal(final Instant now) { final int before = sessions.size();
     sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
     return before - sessions.size();
    }
    private static byte[] copy(final byte[] value) { return Objects.requireNonNull(value, "data").clone(); }

    /** Domain policy SPI for editor operations. Implementations must be deterministic and thread-safe. */
    public interface Policy<D> {
        /** @return independent immutable/deep copy */ D copy(D value);
        /** @return validation report */ Validation validate(D value);
        /** @return safe preview */ Preview preview(D value);
        /** @return parsed imported value */ D importData(byte[] data);
        /** @return serialized value */ byte[] exportData(D value);
        /** @return duplicated value with new identity */ D duplicate(D value, DefinitionId newTarget);
        /** @return default value for target */ D reset(DefinitionId target);
        /** @return migrated value */ D migrate(D value, DefinitionId version);
        /** @return optimistic persistence result */ ApplyResult<D> apply(AuthorizationSubject actor, DefinitionId target, long expectedRevision, D value);
    }
    /** Pure draft transformation. */ public interface Edit<D> { /** @return transformed non-null value */ D apply(D value); }

    /** Typed editor session ID. */ public static final class EditorSessionId implements Comparable<EditorSessionId> {
        private final UUID value;
         private EditorSessionId(final UUID value) { this.value = Objects.requireNonNull(value, "value");
        }
        /** @return random ID */ public static EditorSessionId random() { return new EditorSessionId(UUID.randomUUID()); }
        /** @return parsed ID */ public static EditorSessionId parse(final String value) { return new EditorSessionId(UUID.fromString(value)); }
        /** @return UUID */ public UUID value() { return value; }
        @Override public int compareTo(final EditorSessionId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public String toString() { return value.toString(); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof EditorSessionId && value.equals(((EditorSessionId) other).value); }
    }

    /** Immutable editor session projection. */
    public static final class Session<D> {
        private final EditorSessionId id;
         private final AuthorizationSubject actor;
        private final DefinitionId target;
        private final long sourceRevision;
         private final long localRevision;
         private final D draft;
        private final Instant expiresAt;
        private Session(final EditorSessionId id, final AuthorizationSubject actor, final DefinitionId target,
                        final long sourceRevision, final long localRevision, final D draft, final Instant expiresAt) {
            this.id = id;
             this.actor = actor;
             this.target = target;
            this.sourceRevision = sourceRevision;
            this.localRevision = localRevision;
             this.draft = draft;
            this.expiresAt = expiresAt;
        }
        /** @return session ID */ public EditorSessionId id() { return id; }
        /** @return actor */ public AuthorizationSubject actor() { return actor; }
        /** @return target */ public DefinitionId target() { return target; }
        /** @return persisted revision at start */ public long sourceRevision() { return sourceRevision; }
        /** @return local edit revision */ public long localRevision() { return localRevision; }
        /** @return immutable/deep-copied draft */ public D draft() { return draft; }
        /** @return expiry */ public Instant expiresAt() { return expiresAt; }
    }
    /** Validation report. */
    public static final class Validation {
        private final List<MessageKey> errors;
        /** Creates a report;
         no errors means valid. */ public Validation(final List<MessageKey> errors) { final List<MessageKey> copy = new ArrayList<MessageKey>();
         for (MessageKey error : Objects.requireNonNull(errors, "errors")) { copy.add(Objects.requireNonNull(error, "error"));
         } this.errors = Collections.unmodifiableList(copy);
        }
        /** @return whether valid */ public boolean valid() { return errors.isEmpty(); }
        /** @return error keys */ public List<MessageKey> errors() { return errors; }
    }
    /** Safe preview description. */ public static final class Preview { private final MessageKey summary;
     private final int changes;
     /** Creates a preview. */ public Preview(final MessageKey summary, final int changes) { this.summary = Objects.requireNonNull(summary, "summary");
     if (changes < 0) { throw new IllegalArgumentException("changes must not be negative");
     } this.changes = changes;
     } /** @return summary */ public MessageKey summary() { return summary;
     } /** @return change count */ public int changes() { return changes;
    } }
    /** Optimistic persistence result. */
    public static final class ApplyResult<D> {
        private final boolean applied;
         private final D value;
         private final long revision;
        private final MessageKey message;
        private ApplyResult(final boolean applied, final D value, final long revision, final MessageKey message) { this.applied = applied;
         this.value = value;
         this.revision = revision;
         this.message = message;
        }
        /** @return successful result */ public static <D> ApplyResult<D> applied(final D value, final long revision, final MessageKey message) { if (revision < 0L) { throw new IllegalArgumentException("revision must not be negative");
         } return new ApplyResult<D>(true, Objects.requireNonNull(value, "value"), revision, Objects.requireNonNull(message, "message"));
        }
        /** @return conflict result */ public static <D> ApplyResult<D> conflict(final MessageKey message) { return new ApplyResult<D>(false, null, 0L, Objects.requireNonNull(message, "message")); }
        /** @return whether applied */ public boolean applied() { return applied; }
        /** @return applied value */ public D value() { if (!applied) { throw new IllegalStateException("no applied value");
         } return value;
        }
        /** @return resulting revision */ public long revision() { return revision; }
        /** @return message */ public MessageKey message() { return message; }
    }
    /** Editor operation outcome. */
    public static final class Outcome<D> {
        private final Status status;
         private final Session<D> session;
        private final MessageKey message;
        private Outcome(final Status status, final Session<D> session, final String message) { this.status = status;
         this.session = session;
         this.message = MessageKey.of(message);
        }
        private static <D> Outcome<D> accepted(final Session<D> session, final String message) { return new Outcome<D>(Status.APPLIED, session, message); }
        private static <D> Outcome<D> rejected(final Status status, final Session<D> session, final String message) { return new Outcome<D>(status, session, message); }
        /** @return status */ public Status status() { return status; }
        /** @return current snapshot */ public Session<D> session() { return session; }
        /** @return localized message */ public MessageKey message() { return message; }
    }
    /** Editor outcomes. */ public enum Status { /** Operation applied. */ APPLIED, /** Validation failed. */ INVALID, /** Source revision changed. */ CONFLICT, /** Undo/redo empty. */ NO_HISTORY }

    private static final class MutableSession<D> {
        private final EditorSessionId id;
         private final AuthorizationSubject actor;
        private final DefinitionId target;
        private final long sourceRevision;
         private final D original;
        private final Deque<D> undo = new ArrayDeque<D>();
        private final Deque<D> redo = new ArrayDeque<D>();
         private D draft;
         private long localRevision;
        private Instant expiresAt;
        private MutableSession(final EditorSessionId id, final AuthorizationSubject actor, final DefinitionId target,
                               final long sourceRevision, final D original, final D draft, final Instant expiresAt) {
            this.id = id;
             this.actor = Objects.requireNonNull(actor, "actor");
            this.target = Objects.requireNonNull(target, "target");
            this.sourceRevision = sourceRevision;
             this.original = original;
             this.draft = draft;
            this.expiresAt = expiresAt;
        }
        private Session<D> snapshot() { return new Session<D>(id, actor, target, sourceRevision, localRevision, draft, expiresAt); }
    }
}
