package io.zartra.bedwars.scripting.api;

import java.util.Objects;

/**
 * Immutable, version-bound reference from content to one declarative script entry point.
 *
 * <p>Resolution is explicit and fail-closed: a consumer must reject a reference when its graph,
 * entry point or exact schema version is unavailable. This class neither executes code nor grants
 * capabilities.</p>
 */
public final class ActionReference {
    private final ScriptId scriptId;
    private final ScriptActionId actionId;
    private final int schemaVersion;

    /** Creates a reference to a positive schema version. */
    public ActionReference(final ScriptId scriptId, final ScriptActionId actionId,
                           final int schemaVersion) {
        this.scriptId = Objects.requireNonNull(scriptId, "scriptId");
        this.actionId = Objects.requireNonNull(actionId, "actionId");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        this.schemaVersion = schemaVersion;
    }

    /** @return referenced graph */ public ScriptId scriptId() { return scriptId; }
    /** @return referenced entry point */ public ScriptActionId actionId() { return actionId; }
    /** @return exact compatible schema version */ public int schemaVersion() { return schemaVersion; }

    @Override public int hashCode() { return Objects.hash(scriptId, actionId, schemaVersion); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ActionReference)) { return false; }
        final ActionReference that = (ActionReference) other;
        return schemaVersion == that.schemaVersion && scriptId.equals(that.scriptId)
                && actionId.equals(that.actionId);
    }
}
