package io.zartra.bedwars.api.failure;

/** Stable operational failure taxonomy used by retry, health and alert policies. */
public enum FailureKind {
    /** A required provider or resource is not available. */ UNAVAILABLE,
    /** A declared deadline elapsed. */ TIMEOUT,
    /** Bounded admission rejected work. */ REJECTED,
    /** Concurrent state prevented a safe update. */ CONFLICT,
    /** Caller input failed deterministic validation. */ INVALID,
    /** The caller lacks required authority. */ UNAUTHORIZED,
    /** Persisted or transferred state failed integrity validation. */ CORRUPT,
    /** The active capability set cannot perform the operation. */ UNSUPPORTED,
    /** A component failed unexpectedly without compromising process integrity. */ INTERNAL
}
