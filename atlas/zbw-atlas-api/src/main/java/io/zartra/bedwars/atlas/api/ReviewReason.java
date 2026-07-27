package io.zartra.bedwars.atlas.api;

/** Stable built-in, localization-safe Atlas review reasons (ZBW-ATLAS-006). */
public enum ReviewReason {
    /** Combat automation evidence. */ KILL_AURA,
    /** Invalid attack-distance evidence. */ REACH,
    /** Invalid velocity or knockback evidence. */ VELOCITY,
    /** Automated clicking evidence. */ AUTO_CLICKER,
    /** Automated aiming evidence. */ AIM_ASSIST,
    /** Automated bridging evidence. */ SCAFFOLD,
    /** Invalid speed evidence. */ SPEED,
    /** Invalid flight evidence. */ FLY,
    /** Other movement modification evidence. */ MOVEMENT_MODIFICATION,
    /** Impossible rotation evidence. */ IMPOSSIBLE_ROTATION,
    /** Suspicious clicks-per-second evidence. */ SUSPICIOUS_CPS,
    /** Cross-team cooperation evidence. */ CROSS_TEAMING,
    /** Boosting evidence. */ BOOSTING,
    /** Bug abuse evidence. */ BUG_ABUSE,
    /** Game exploit evidence. */ GAME_EXPLOIT,
    /** Evidence does not support a reliable finding. */ INSUFFICIENT_EVIDENCE,
    /** Replay or evidence integrity prevented review. */ EVIDENCE_ERROR,
    /** Configured rule evidence outside another built-in reason. */ OTHER
}
