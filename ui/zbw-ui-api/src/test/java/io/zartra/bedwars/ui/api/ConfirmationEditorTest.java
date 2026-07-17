package io.zartra.bedwars.ui.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.command.api.PresentationActions;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmationEditorTest {
    private static final AuthorizationSubject ACTOR = AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER, DefinitionId.of("zartra", "player/test"));
    private static final DefinitionId TARGET = DefinitionId.of("zartra", "target/test");

    @Test void typedIdentifiersAndResultBoundariesAreDeterministic() {
        UUID value = UUID.fromString("00000000-0000-0000-0000-000000000010");
        ConfirmationFramework.ConfirmationId confirmation =
                ConfirmationFramework.ConfirmationId.of(value);
        ConfirmationFramework.ConfirmationId parsedConfirmation =
                ConfirmationFramework.ConfirmationId.parse(value.toString());
        assertEquals(confirmation, confirmation);
        assertEquals(confirmation, parsedConfirmation);
        assertEquals(confirmation.hashCode(), parsedConfirmation.hashCode());
        assertEquals(0, confirmation.compareTo(parsedConfirmation));
        assertFalse(confirmation.equals("not-an-id"));
        assertThrows(NullPointerException.class, () -> confirmation.compareTo(null));
        EditorFramework.EditorSessionId editor = EditorFramework.EditorSessionId.parse(value.toString());
        EditorFramework.EditorSessionId parsedEditor = EditorFramework.EditorSessionId.parse(editor.toString());
        assertEquals(editor, editor);
        assertEquals(editor, parsedEditor);
        assertEquals(editor.hashCode(), parsedEditor.hashCode());
        assertEquals(0, editor.compareTo(parsedEditor));
        assertFalse(editor.equals("not-an-id"));
        assertThrows(NullPointerException.class, () -> editor.compareTo(null));
        assertThrows(IllegalArgumentException.class, () -> new EditorFramework.Preview(
                MessageKey.of("editor.preview"), -1));
        assertThrows(IllegalArgumentException.class, () -> EditorFramework.ApplyResult.applied(
                "value", -1, MessageKey.of("editor.applied")));
        EditorFramework.ApplyResult<String> conflict = EditorFramework.ApplyResult.conflict(
                MessageKey.of("editor.conflict"));
        assertFalse(conflict.applied());
        assertThrows(IllegalStateException.class, conflict::value);
    }

    @Test void confirmationIsSingleUseBoundRevisionAwareAndReauthorizes() {
        MutableTime time = new MutableTime();
        List<ConfirmationFramework.AuditRecord> audit = new ArrayList<ConfirmationFramework.AuditRecord>();
        PresentationActions.Definition destructive = PresentationActions.Catalog.standard().stream()
                .filter(PresentationActions.Definition::destructive).findFirst()
                .orElseThrow(AssertionError::new);
        PresentationActions.Definition safe = PresentationActions.Catalog.standard().stream()
                .filter(value -> !value.destructive()).findFirst()
                .orElseThrow(AssertionError::new);
        ConfirmationFramework framework = confirmations(true, time, audit, destructive, safe);
        ConfirmationFramework.Intent intent = framework.issue(ACTOR, destructive.id(), TARGET, 4, CorrelationId.random());
        assertEquals(ConfirmationFramework.Verdict.CONFIRMED, framework.consume(intent.id(), ACTOR, destructive.id(), TARGET, 4).verdict());
        assertEquals(ConfirmationFramework.Verdict.UNKNOWN_OR_REPLAYED, framework.consume(intent.id(), ACTOR, destructive.id(), TARGET, 4).verdict());
        ConfirmationFramework.Intent mismatch = framework.issue(ACTOR, destructive.id(), TARGET, 4, CorrelationId.random());
        assertEquals(ConfirmationFramework.Verdict.BINDING_MISMATCH, framework.consume(mismatch.id(), AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER, DefinitionId.of("zartra", "player/other")), destructive.id(), TARGET, 4).verdict());
        ConfirmationFramework.Intent stale = framework.issue(ACTOR, destructive.id(), TARGET, 4, CorrelationId.random());
        assertEquals(ConfirmationFramework.Verdict.STALE_REVISION, framework.consume(stale.id(), ACTOR, destructive.id(), TARGET, 5).verdict());
        assertThrows(IllegalArgumentException.class, () -> framework.issue(ACTOR, safe.id(), TARGET, 1, CorrelationId.random()));
        assertEquals(6, audit.size());
        assertFalse(audit.get(0).verdict().isPresent());
    }

    @Test void confirmationExpiryCapacityAndRevokedPermissionFailClosed() {
        MutableTime time = new MutableTime();
        PresentationActions.Definition destructive = PresentationActions.Catalog.standard().stream()
                .filter(PresentationActions.Definition::destructive).findFirst()
                .orElseThrow(AssertionError::new);
        ConfirmationFramework denied = confirmations(false, time, new ArrayList<ConfirmationFramework.AuditRecord>(), destructive);
        ConfirmationFramework.Intent forbidden = denied.issue(ACTOR, destructive.id(), TARGET, 1, CorrelationId.random());
        assertEquals(ConfirmationFramework.Verdict.FORBIDDEN, denied.consume(forbidden.id(), ACTOR, destructive.id(), TARGET, 1).verdict());
        ConfirmationFramework expiring = confirmations(true, time, new ArrayList<ConfirmationFramework.AuditRecord>(), destructive);
        ConfirmationFramework.Intent expired = expiring.issue(ACTOR, destructive.id(), TARGET, 1, CorrelationId.random());
        time.now = time.now.plusSeconds(6);
        assertEquals(1, expiring.cleanup());
        assertEquals(ConfirmationFramework.Verdict.UNKNOWN_OR_REPLAYED, expiring.consume(expired.id(), ACTOR, destructive.id(), TARGET, 1).verdict());
    }

    @Test void editorSupportsCompleteLifecycleAndOptimisticApply() {
        MutableTime time = new MutableTime();
        TextPolicy policy = new TextPolicy();
        EditorFramework<String> editor = new EditorFramework<String>(policy, time, Duration.ofMinutes(5), 2, 2);
        EditorFramework.Session<String> session = editor.begin(ACTOR, TARGET, 2, "one");
        assertEquals("one", session.draft());
        assertEquals("two", editor.edit(session.id(), value -> "two").session().draft());
        assertEquals("one", editor.undo(session.id()).session().draft());
        assertEquals("two", editor.redo(session.id()).session().draft());
        assertEquals(EditorFramework.Status.NO_HISTORY, editor.redo(session.id()).status());
        assertTrue(editor.validate(session.id()).valid());
        assertEquals(3, editor.preview(session.id()).changes());
        assertArrayEquals("two".getBytes(StandardCharsets.UTF_8), editor.exportData(session.id()));
        assertEquals("imported", editor.importData(session.id(), "imported".getBytes(StandardCharsets.UTF_8)).session().draft());
        assertEquals("duplicate", editor.duplicate(session.id(), DefinitionId.of("zartra", "duplicate")).session().draft());
        assertEquals("default", editor.reset(session.id()).session().draft());
        assertEquals("default-migrated", editor.migrate(session.id(), DefinitionId.of("zartra", "version/v2")).session().draft());
        EditorFramework.Outcome<String> applied = editor.apply(session.id());
        assertEquals(EditorFramework.Status.APPLIED, applied.status());
        assertEquals(3, applied.session().sourceRevision());
        assertFalse(editor.session(session.id()).isPresent());
    }

    @Test void editorRejectsInvalidConflictExpiresAndCancels() {
        MutableTime time = new MutableTime();
        TextPolicy policy = new TextPolicy();
        EditorFramework<String> editor = new EditorFramework<String>(policy, time, Duration.ofSeconds(2), 1, 1);
        EditorFramework.Session<String> invalid = editor.begin(ACTOR, TARGET, 1, "ok");
        editor.edit(invalid.id(), value -> "bad");
        assertEquals(EditorFramework.Status.INVALID, editor.apply(invalid.id()).status());
        policy.conflict = true; editor.edit(invalid.id(), value -> "ok");
        assertEquals(EditorFramework.Status.CONFLICT, editor.apply(invalid.id()).status());
        assertTrue(editor.cancel(invalid.id()));
        assertFalse(editor.cancel(invalid.id()));
        EditorFramework.Session<String> expiring = editor.begin(ACTOR, TARGET, 1, "ok");
        time.now = time.now.plusSeconds(3);
        assertEquals(1, editor.cleanup());
        assertThrows(IllegalArgumentException.class, () -> editor.validate(expiring.id()));
    }

    private static ConfirmationFramework confirmations(boolean allowed, MutableTime time, List<ConfirmationFramework.AuditRecord> audit, PresentationActions.Definition... definitions) {
        return new ConfirmationFramework(request -> allowed ? AuthorizationDecision.allow(DefinitionId.of("zartra", "allow")) : AuthorizationDecision.deny(DefinitionId.of("zartra", "deny")), time, UUID::randomUUID, audit::add, Duration.ofSeconds(5), 8, java.util.Arrays.asList(definitions));
    }
    private static final class MutableTime implements TimeSource { private Instant now = Instant.parse("2026-01-01T00:00:00Z");
     @Override public Instant now() { return now;
    } }
    private static final class TextPolicy implements EditorFramework.Policy<String> {
        private boolean conflict;
        @Override public String copy(String value) { return new String(value); }
        @Override public EditorFramework.Validation validate(String value) { return new EditorFramework.Validation(value.equals("bad") ? Collections.singletonList(MessageKey.of("editor.invalid")) : Collections.<MessageKey>emptyList()); }
        @Override public EditorFramework.Preview preview(String value) { return new EditorFramework.Preview(MessageKey.of("editor.preview"), value.length()); }
        @Override public String importData(byte[] data) { return new String(data, StandardCharsets.UTF_8); }
        @Override public byte[] exportData(String value) { return value.getBytes(StandardCharsets.UTF_8); }
        @Override public String duplicate(String value, DefinitionId target) { return "duplicate"; }
        @Override public String reset(DefinitionId target) { return "default"; }
        @Override public String migrate(String value, DefinitionId version) { return value + "-migrated"; }
        @Override public EditorFramework.ApplyResult<String> apply(AuthorizationSubject actor, DefinitionId target, long expectedRevision, String value) { return conflict ? EditorFramework.ApplyResult.conflict(MessageKey.of("editor.conflict")) : EditorFramework.ApplyResult.applied(value, expectedRevision + 1, MessageKey.of("editor.applied")); }
    }
}
