package io.zartra.bedwars.domain.privategame;

import io.zartra.bedwars.api.content.ContentRegistry;
import io.zartra.bedwars.api.identity.PrivateGameModifierId;

/** Base content definition for a private-game modifier. */
public interface PrivateGameModifierDefinition extends ContentRegistry.Definition<PrivateGameModifierId> {
    /** @return whether the modifier supports native and extension-defined resources */
    boolean supportsCustomResources();
}
