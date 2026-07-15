package io.zartra.bedwars.arena.setup;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.arena.model.ArenaBundle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable isolated setup session with bounded undo/redo history. */
public final class SetupSession {
    private static final int MAXIMUM_HISTORY = 128;
    private final SetupSessionId id;
    private final ArenaId arenaId;
    private final AuthorizationSubject actor;
    private final long baseRepositoryRevision;
    private final long draftRevision;
    private final ArenaBundle draft;
    private final List<ArenaBundle> undo;
    private final List<ArenaBundle> redo;
    private final State state;

    private SetupSession(final SetupSessionId id, final ArenaId arenaId,
                         final AuthorizationSubject actor, final long baseRepositoryRevision,
                         final long draftRevision, final ArenaBundle draft,
                         final List<ArenaBundle> undo, final List<ArenaBundle> redo,
                         final State state) {
        this.id = Objects.requireNonNull(id, "id");
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
        this.actor = Objects.requireNonNull(actor, "actor");
        if (baseRepositoryRevision < 1L || draftRevision < 0L) {
            throw new IllegalArgumentException("session revisions are invalid");
        }
        this.baseRepositoryRevision = baseRepositoryRevision;
        this.draftRevision = draftRevision;
        this.draft = Objects.requireNonNull(draft, "draft");
        if (!arenaId.equals(draft.arenaId())) { throw new IllegalArgumentException("session arena mismatch"); }
        this.undo = history(undo, "undo");
        this.redo = history(redo, "redo");
        this.state = Objects.requireNonNull(state, "state");
    }

    /** @return a new active setup session for a durable record */
    public static SetupSession begin(final SetupSessionId id, final AuthorizationSubject actor,
                                     final long baseRepositoryRevision, final ArenaBundle draft) {
        return new SetupSession(id, draft.arenaId(), actor, baseRepositoryRevision, 0L, draft,
                Collections.<ArenaBundle>emptyList(), Collections.<ArenaBundle>emptyList(), State.ACTIVE);
    }

    private static List<ArenaBundle> history(final List<ArenaBundle> source, final String label) {
        final List<ArenaBundle> copy = new ArrayList<ArenaBundle>(Objects.requireNonNull(source, label));
        if (copy.size() > MAXIMUM_HISTORY || copy.contains(null)) {
            throw new IllegalArgumentException(label + " history is invalid");
        }
        return Collections.unmodifiableList(copy);
    }

    /** @return a copy with one mutation applied and redo history cleared */
    public SetupSession mutate(final SetupMutation mutation, final Instant changedAt) {
        requireActive();
        final List<ArenaBundle> nextUndo = append(undo, draft);
        return new SetupSession(id, arenaId, actor, baseRepositoryRevision, draftRevision + 1L,
                Objects.requireNonNull(mutation, "mutation").apply(draft, changedAt), nextUndo,
                Collections.<ArenaBundle>emptyList(), state);
    }

    /** @return a copy replacing the draft with an explicitly previewed candidate */
    public SetupSession applyPreview(final ArenaBundle candidate) {
        requireActive();
        final ArenaBundle value = Objects.requireNonNull(candidate, "candidate");
        if (!arenaId.equals(value.arenaId())) {
            throw new IllegalArgumentException("preview targets another arena");
        }
        return new SetupSession(id, arenaId, actor, baseRepositoryRevision, draftRevision + 1L,
                value, append(undo, draft), Collections.<ArenaBundle>emptyList(), state);
    }

    /** @return a copy one history step earlier */
    public SetupSession undo() {
        requireActive();
        if (undo.isEmpty()) { throw new IllegalStateException("undo history is empty"); }
        final List<ArenaBundle> nextUndo = new ArrayList<ArenaBundle>(undo);
        final ArenaBundle previous = nextUndo.remove(nextUndo.size() - 1);
        return new SetupSession(id, arenaId, actor, baseRepositoryRevision, draftRevision + 1L,
                previous, nextUndo, append(redo, draft), state);
    }

    /** @return a copy one history step later */
    public SetupSession redo() {
        requireActive();
        if (redo.isEmpty()) { throw new IllegalStateException("redo history is empty"); }
        final List<ArenaBundle> nextRedo = new ArrayList<ArenaBundle>(redo);
        final ArenaBundle following = nextRedo.remove(nextRedo.size() - 1);
        return new SetupSession(id, arenaId, actor, baseRepositoryRevision, draftRevision + 1L,
                following, append(undo, draft), nextRedo, state);
    }

    /** @return a copy terminally marked committed */
    public SetupSession committed() {
        requireActive();
        return new SetupSession(id, arenaId, actor, baseRepositoryRevision, draftRevision, draft,
                undo, redo, State.COMMITTED);
    }
    /** @return a copy terminally marked abandoned */
    public SetupSession abandoned() {
        requireActive();
        return new SetupSession(id, arenaId, actor, baseRepositoryRevision, draftRevision, draft,
                undo, redo, State.ABANDONED);
    }
    private void requireActive() {
        if (state != State.ACTIVE) { throw new IllegalStateException("setup session is terminal"); }
    }
    private static List<ArenaBundle> append(final List<ArenaBundle> source,
                                            final ArenaBundle value) {
        final List<ArenaBundle> result = new ArrayList<ArenaBundle>(source);
        if (result.size() == MAXIMUM_HISTORY) { result.remove(0); }
        result.add(value);
        return result;
    }
    /** @return session identity */ public SetupSessionId id() { return id; }
    /** @return arena identity */ public ArenaId arenaId() { return arenaId; }
    /** @return authenticated owner */ public AuthorizationSubject actor() { return actor; }
    /** @return repository revision observed at session start */ public long baseRepositoryRevision() { return baseRepositoryRevision; }
    /** @return monotonically increasing draft revision */ public long draftRevision() { return draftRevision; }
    /** @return current immutable draft */ public ArenaBundle draft() { return draft; }
    /** @return whether an undo operation is available */ public boolean canUndo() { return !undo.isEmpty(); }
    /** @return whether a redo operation is available */ public boolean canRedo() { return !redo.isEmpty(); }
    /** @return lifecycle state */ public State state() { return state; }

    /** Isolated setup-session lifecycle. */ public enum State { ACTIVE, COMMITTED, ABANDONED }
}
