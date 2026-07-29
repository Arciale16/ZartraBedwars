package io.zartra.bedwars.compat.client;

/** Exhaustive M22 client-facing feature parity surfaces. */
public enum ClientFeature {
    /** Inventory and modal interfaces. */ GUI,
    /** Shop presentation and purchase input. */ SHOP,
    /** Spectator controls and information. */ SPECTATOR,
    /** Replay discovery and controls. */ REPLAY_ACCESS,
    /** Hotbar state and actions. */ HOTBAR,
    /** Text, warnings and choices. */ TEXT,
    /** Informational and decorative sound. */ SOUND,
    /** Informational and decorative particles. */ PARTICLE,
    /** Player, NPC and replay entity display. */ ENTITY_DISPLAY,
    /** Click, command, inventory and movement input. */ INPUT
}
