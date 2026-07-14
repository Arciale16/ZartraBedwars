package io.zartra.bedwars.api.version;

/** Public API version constants and compatibility rules. */
public final class ApiVersions {
    /** Initial stable ZartraBedWars public API version. */
    public static final SemanticVersion CURRENT = SemanticVersion.parse("1.0.0");
    /** Versions accepted by this API artifact. */
    public static final VersionRange SUPPORTED = VersionRange.parse("[1.0.0,2.0.0)");

    private ApiVersions() {
        throw new AssertionError("No instances");
    }

    /** @return whether the requested API version is supported by this artifact */
    public static boolean supports(final SemanticVersion requested) {
        return SUPPORTED.contains(requested);
    }
}
