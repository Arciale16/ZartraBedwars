package io.zartra.bedwars.scripting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ScriptingReferenceTest {
    @Test void validatesTypedIdentitiesAndRoundTrips() {
        final ScriptId script = ScriptId.of("zbw", "shop/fire_charge");
        final ScriptActionId action = ScriptActionId.of("zbw", "shop/purchase");
        assertEquals(script, ScriptId.parse(script.toString()));
        assertEquals(action, ScriptActionId.parse(action.toString()));
        assertEquals(script, script);
        assertEquals(action, action);
        assertEquals(script.value().toString(), script.toString());
        assertEquals(action.value().toString(), action.toString());
        assertEquals(0, script.compareTo(ScriptId.parse(script.toString())));
        assertEquals(0, action.compareTo(ScriptActionId.parse(action.toString())));
        assertNotEquals(script, ScriptId.of("zbw", "shop/tnt"));
        assertNotEquals(action, ScriptActionId.of("zbw", "shop/use"));
        assertNotEquals(script, "not-a-script");
        assertNotEquals(action, "not-an-action");
    }

    @Test void rejectsWrongFamiliesAndInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> ScriptId.parse("zbw:item/a"));
        assertThrows(IllegalArgumentException.class, () -> ScriptId.parse("zbw:script/"));
        assertThrows(IllegalArgumentException.class, () -> ScriptActionId.parse("zbw:item/a"));
        assertThrows(IllegalArgumentException.class, () -> ScriptActionId.parse("zbw:action/"));
        assertThrows(NullPointerException.class, () -> new ActionReference(null,
                ScriptActionId.of("zbw", "x"), 1));
        assertThrows(NullPointerException.class, () -> new ActionReference(
                ScriptId.of("zbw", "x"), null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ActionReference(
                ScriptId.of("zbw", "x"), ScriptActionId.of("zbw", "x"), 0));
    }

    @Test void actionReferencesHaveValueSemantics() {
        final ActionReference first = new ActionReference(ScriptId.of("zbw", "shop"),
                ScriptActionId.of("zbw", "purchase"), 2);
        final ActionReference same = new ActionReference(ScriptId.parse("zbw:script/shop"),
                ScriptActionId.parse("zbw:action/purchase"), 2);
        assertEquals(first, same);
        assertEquals(first, first);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(2, first.schemaVersion());
        assertEquals("zbw:script/shop", first.scriptId().toString());
        assertEquals("zbw:action/purchase", first.actionId().toString());
        assertNotEquals(first, new ActionReference(first.scriptId(), first.actionId(), 3));
        assertNotEquals(first, new ActionReference(ScriptId.of("zbw", "other"),
                first.actionId(), 2));
        assertNotEquals(first, new ActionReference(first.scriptId(),
                ScriptActionId.of("zbw", "other"), 2));
        assertNotEquals(first, "not-a-reference");
    }
}
