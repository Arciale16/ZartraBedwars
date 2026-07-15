package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.PermissionNode;

/** Canonical M07 arena/setup operation and permission mapping. */
public enum ArenaOperation {
    /** Create an arena/map aggregate. */ CREATE("zartrabedwars.admin.arena.create"),
    /** Read arena diagnostics or definitions. */ READ("zartrabedwars.admin.arena.read"),
    /** Rename without changing identity. */ RENAME("zartrabedwars.admin.arena.rename"),
    /** Deep duplicate into independent identities. */ DUPLICATE("zartrabedwars.admin.arena.duplicate"),
    /** Delete an exact revision. */ DELETE("zartrabedwars.admin.arena.delete"),
    /** Import a validated archive. */ IMPORT("zartrabedwars.admin.arena.import"),
    /** Export a sanitized archive. */ EXPORT("zartrabedwars.admin.arena.export"),
    /** Capture a backup archive. */ BACKUP("zartrabedwars.admin.arena.backup"),
    /** Restore an archive or last-known-good image. */ RESTORE("zartrabedwars.admin.arena.restore"),
    /** Enable a fully valid arena. */ ENABLE("zartrabedwars.admin.arena.enable"),
    /** Disable arena admission. */ DISABLE("zartrabedwars.admin.arena.disable"),
    /** Reset or recover a world lifecycle. */ RESET("zartrabedwars.admin.arena.reset"),
    /** Enter an isolated setup session. */ SETUP_ENTER("zartrabedwars.admin.setup.enter"),
    /** Mutate a setup draft. */ SETUP_EDIT("zartrabedwars.admin.setup.edit"),
    /** Validate a setup draft. */ SETUP_VALIDATE("zartrabedwars.admin.setup.validate"),
    /** Apply an explicit preview or marker proposal. */ SETUP_APPLY("zartrabedwars.admin.setup.apply"),
    /** Atomically save a setup session. */ SETUP_SAVE("zartrabedwars.admin.setup.save"),
    /** Exit or abandon a setup session. */ SETUP_EXIT("zartrabedwars.admin.setup.exit");

    private final PermissionNode permission;
    ArenaOperation(final String permission) { this.permission = PermissionNode.of(permission); }
    /** @return canonical exact permission node */ public PermissionNode permission() { return permission; }
}
