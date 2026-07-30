package io.zartra.bedwars.compat.client;

/** Fail-closed discovery state for one optional provider. */
public enum ClientProviderStatus {
    /** Exactly one compatible provider is present. */
    PRESENT,
    /** The provider is not installed. */
    ABSENT,
    /** The provider is installed with an unsupported version or capability set. */
    INCOMPATIBLE,
    /** More than one binding was discovered. */
    DUPLICATE
}
